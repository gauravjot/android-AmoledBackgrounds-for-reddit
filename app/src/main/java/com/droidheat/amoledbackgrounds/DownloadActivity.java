package com.droidheat.amoledbackgrounds;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.droidheat.amoledbackgrounds.utils.AppUtils;
import com.droidheat.amoledbackgrounds.utils.SharedPrefsUtils;
import com.droidheat.amoledbackgrounds.utils.FetchUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executors;

public class DownloadActivity extends AppCompatActivity {
	
	private static final int UI_ANIMATION_DELAY = 300;
	private final Handler mHideHandler = new Handler();
	private View mContentView;
	private final Runnable mHidePart2Runnable = new Runnable() {
		@Override
		public void run() {
			mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		}
	};
	private View mControlsView;
	private final Runnable mShowPart2Runnable = new Runnable() {
		@Override
		public void run() {
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.show();
				actionBar.setDisplayHomeAsUpEnabled(true);
			}
			mControlsView.setVisibility(View.VISIBLE);
			mControlsView.animate()
							.alpha(1.0f)
							.translationY(0)
							.setDuration(300)
							.setListener(new AnimatorListenerAdapter() {
								@Override
								public void onAnimationEnd(Animator animation) {
									super.onAnimationEnd(animation);
									mControlsView.setVisibility(View.VISIBLE);
								}
							});
		}
	};
	private boolean mVisible;
	
	final private int MY_PERMISSIONS_REQUEST = 832;
	private HashMap<String, String> wallpaper;
	private String ext, titleStr;
	private ImageSwitcher primaryActionButton;
	private ProgressBar progressBar;
	private AlertDialog alertDialog;
	private WebView commentsWebView;
	
	
	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// UI Back button
		OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (findViewById(R.id.commentsView).getVisibility() == View.VISIBLE) {
					hideCommentWebView();
				} else {
					finish();
				}
			}
		};
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setHomeButtonEnabled(true);
			getSupportActionBar().setDisplayShowHomeEnabled(true);
			getSupportActionBar().setTitle("Hello");
		}
		
		
		setContentView(R.layout.activity_download);
		
		Intent intent = getIntent();
		//noinspection unchecked
		wallpaper = (HashMap<String, String>) intent.getSerializableExtra("map");
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
		
		progressBar = findViewById(R.id.progress_circular);
		progressBar.setVisibility(View.INVISIBLE);
		
		commentsWebView = findViewById(R.id.webView);
		commentsWebView.getSettings();
		commentsWebView.setBackgroundColor(Color.TRANSPARENT);
		findViewById(R.id.commentsView).setVisibility(View.INVISIBLE);
		
		// close comments window button
		findViewById(R.id.button_Close).setOnClickListener(v -> hideCommentWebView());
		
		// go to thread button
		findViewById(R.id.button_browser).setOnClickListener(v ->
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com" + wallpaper.get("postlink"))))
		);
		
		mVisible = true;
		mControlsView = findViewById(R.id.fullscreen_content_controls);
		mContentView = findViewById(R.id.fullscreen_content);
		
		ImageView preview = findViewById(R.id.fullscreen_image);
		if (!(new SharedPrefsUtils(this)).readSharedPrefsBoolean("lower_thumbnail_quality", false)) {
			Picasso.get().load(wallpaper.get("image")).into(preview);
		} else {
			Picasso.get().load(wallpaper.get("preview")).into(preview);
		}
		
		primaryActionButton = findViewById(R.id.download);
		primaryActionButton.setFactory(() -> {
			ImageView myView = new ImageView(getApplicationContext());
			myView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			myView.setLayoutParams(new
							ImageSwitcher.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
							ActionBar.LayoutParams.MATCH_PARENT));
			return myView;
		});
		primaryActionButton.setImageResource(R.drawable.ic_file_download_black_24dp);
		Animation in = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		Animation out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		primaryActionButton.setInAnimation(in);
		primaryActionButton.setOutAnimation(out);
		
		TextView title = findViewById(R.id.title);
		TextView author = findViewById(R.id.author);
		TextView post_flair = findViewById(R.id.post_flair);
		TextView user_flair = findViewById(R.id.user_flair);
		TextView resolution = findViewById(R.id.resolution);
		TextView score = findViewById(R.id.score);
		TextView viewCommentsTextView = findViewById(R.id.comments);
		
		title.setOnClickListener(v -> showCommentWebView());
		
		viewCommentsTextView.setOnClickListener(v -> showCommentWebView());
		
		findViewById(R.id.imageView1908).setOnClickListener(v -> showCommentWebView());
		
		titleStr = (new AppUtils()).purifyRedditFileTitle(
						wallpaper.get("title"),
						wallpaper.get("name")
		);
		ext = (new AppUtils()).purifyRedditFileExtension(wallpaper.get("image"));
		
		String post_title = Objects.requireNonNull(wallpaper.get("title"))
						.replaceAll("\\(.*?\\) ?", "")
						.replaceAll("\\[.*?\\] ?", "")
						.replaceAll("\\{[^}]*\\}", "")
						.replaceAll("\\u00A0", " ")
						.replaceAll("-", "")
						.replaceAll("&amp;", "&")
						.trim();
		
		title.setText(post_title);
		author.setText(String.format("u/%s", wallpaper.get("author")));
		if (!Objects.equals(wallpaper.get("flair"), "null")) {
			post_flair.setText(wallpaper.get("flair"));
		} else {
			post_flair.setVisibility(View.INVISIBLE);
		}
		if (!Objects.equals(wallpaper.get("author_flair"), "null")) {
			user_flair.setText(wallpaper.get("author_flair"));
		} else {
			user_flair.setVisibility(View.INVISIBLE);
		}
		resolution.setText(String.format("%sx%s", wallpaper.get("width"), wallpaper.get("height")));
		score.setText(wallpaper.get("score"));
		viewCommentsTextView.setText(String.format("%s • View Comments", wallpaper.get("comments")));
		
		findViewById(R.id.open_reddit).setOnClickListener(v -> {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com" + wallpaper.get("postlink")));
			startActivity(browserIntent);
		});
		
		primaryActionButton.setOnClickListener(v -> {
			// See if file is already downloaded and set button action accordingly
			String filePath = (new AppUtils()).getFilePath(titleStr + ext);
			if (new File(filePath).exists()) {
				// Set as Wallpaper
				primaryActionButton.setImageResource(R.drawable.ic_wallpaper_black_24dp);
				primaryActionButton.setVisibility(View.INVISIBLE);
				progressBar.setVisibility(View.VISIBLE);
				Executors.newSingleThreadExecutor().execute(() -> {
					try {
						Boolean isSuccess = (new AppUtils()).changeWallpaper(
										getBaseContext(),
										filePath
						);
						
						runOnUiThread(() -> {
							primaryActionButton.setVisibility(View.VISIBLE);
							progressBar.setVisibility(View.GONE);
							if (isSuccess) {
								Toast.makeText(getApplicationContext(), "Wallpaper set!", Toast.LENGTH_SHORT).show();
								primaryActionButton.setImageResource(R.drawable.ic_check_black_24dp);
							} else {
								Toast.makeText(getApplicationContext(), "Failed to set wallpaper.", Toast.LENGTH_SHORT).show();
								primaryActionButton.setImageResource(R.drawable.ic_wallpaper_black_24dp);
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} else {
				// Download Wallpaper
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
				} else {
					registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
				}
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					// Download using createWriteRequest
					file_download();
				} else {
					if (ContextCompat.checkSelfPermission(DownloadActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
									!= PackageManager.PERMISSION_GRANTED) {
						ActivityCompat.requestPermissions(DownloadActivity.this,
										new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
										MY_PERMISSIONS_REQUEST);
					} else {
						file_download();
					}
				}
			}
		});
		
		// Load comments
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				String comments = loadComments(Objects.requireNonNull(wallpaper.get("name")).replace("t3_", ""));
				
				runOnUiThread(() -> commentsWebView.loadDataWithBaseURL(null, comments, null, "UTF-8", null));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		// Set up the user interaction to manually show or hide the UI.
		mContentView.setOnClickListener(view -> toggle());
	}
	
	private void showCommentWebView() {
		hide();
		Animation in = AnimationUtils.makeInAnimation(this, false);
		in.setDuration(300);
		findViewById(R.id.commentsView).startAnimation(in);
		findViewById(R.id.commentsView).setVisibility(View.VISIBLE);
	}
	
	private void hideCommentWebView() {
		// Show the system bar
		mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		mVisible = true;
		
		// Schedule a runnable to display UI elements after a delay
		mHideHandler.removeCallbacks(mHidePart2Runnable);
		mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
		
		Animation out = AnimationUtils.makeOutAnimation(this, true);
		out.setDuration(300);
		findViewById(R.id.commentsView).startAnimation(out);
		findViewById(R.id.commentsView).setVisibility(View.INVISIBLE);
	}
	
	private long downloadID;
	private boolean downloadStatus = true;
	
	public void file_download() {
		findViewById(R.id.download).setEnabled(false);
		
		final DownloadManager mgr = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
		
		Uri downloadUri = Uri.parse(wallpaper.get("image"));
		DownloadManager.Request request = new DownloadManager.Request(
						downloadUri);
		
		request.setAllowedNetworkTypes(
										DownloadManager.Request.NETWORK_WIFI
														| DownloadManager.Request.NETWORK_MOBILE)
						.setAllowedOverRoaming(false)
						.setTitle(titleStr)
						.setDescription("AmoledBackgrounds")
						.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, titleStr + ".download")
						.setAllowedOverMetered(true)// Set if download is allowed on Mobile network
						.setAllowedOverRoaming(true)
						.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
		
		try {
			downloadID = mgr.enqueue(request);
			progressBar.setVisibility(View.VISIBLE);
			primaryActionButton.setVisibility(View.INVISIBLE);
			
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setCancelable(false);
			alertDialogBuilder.setMessage(titleStr);
			alertDialogBuilder.setTitle("Downloading");
			alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		} catch (Exception e) {
			Toast.makeText(this, "Unable to download: " + titleStr + ext, Toast.LENGTH_LONG).show();
			e.printStackTrace();
			findViewById(R.id.download).setEnabled(true);
			downloadStatus = false;
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		// in case receiver is not unregistered
		try {
			unregisterReceiver(onDownloadComplete);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Fetching the download id received with the broadcast
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
			//Checking if the received broadcast is for our enqueued download by matching download id
			if (downloadID == id && downloadStatus) {
				File from = new File((new AppUtils()).getFilePath(titleStr + ".download"));
				try {
					Files.move(from.toPath(), from.toPath().resolveSibling(titleStr + ext));
					Log.d("renaming after download: ", "success");
					ContentValues values = new ContentValues();
					values.put(MediaStore.MediaColumns.DATA, (new AppUtils()).getFilePath(titleStr + ext));
					boolean successMediaStore = context.getContentResolver().update(
									MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values,
									MediaStore.MediaColumns.DATA + "='" + from.getPath() + "'", null) == 1;
					if (successMediaStore) {
						Log.d("media-store: ", "success");
					} else {
						Log.d("media-store: ", "failed");
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
							
							ContentValues m_values = new ContentValues();
							m_values.put(MediaStore.Images.Media.DISPLAY_NAME, titleStr + ext);
							m_values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + ext.replace(".", ""));
							m_values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
							//TODO: Unable to insert
							Uri result = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, m_values);
							Log.d("media-store: ", "trying saving via special method on android Q or higher");
							if (result != null) {
								Log.d("media-store: ", "successful. " + result);
							} else {
								Log.d("media-store: ", "successful");
							}
						}
					}
					Toast.makeText(DownloadActivity.this, "Download completed!", Toast.LENGTH_SHORT).show();
					primaryActionButton.setVisibility(View.VISIBLE);
					primaryActionButton.setImageResource(R.drawable.ic_wallpaper_black_24dp);
					//AppUtils.saveToMediaStore(context,titleStr + ext);
				} catch (Exception e) {
					Log.d("renaming after download: ", "failed");
					Toast.makeText(DownloadActivity.this, "Downloading error!", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
				findViewById(R.id.download).setEnabled(true);
				progressBar.setVisibility(View.INVISIBLE);
			}
			alertDialog.dismiss();
		}
	};
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		//delayedHide(100);
	}
	
	private void toggle() {
		if (mVisible) {
			hide();
		} else {
			show();
		}
	}
	
	private void hide() {
		// Hide UI first
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}
		mControlsView.animate()
						.translationY(mControlsView.getHeight())
						.alpha(0.0f)
						.setDuration(300)
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								super.onAnimationEnd(animation);
								mControlsView.setVisibility(View.GONE);
							}
						});
		mVisible = false;
		
		// Schedule a runnable to remove the status and navigation bar after a delay
		mHideHandler.removeCallbacks(mShowPart2Runnable);
		mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
	}
	
	private void show() {
		if (findViewById(R.id.commentsView).getVisibility() != View.VISIBLE) {
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.show();
			}
			// Show the system bar
			mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
			mVisible = true;
			
			// Schedule a runnable to display UI elements after a delay
			mHideHandler.removeCallbacks(mHidePart2Runnable);
			mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
		}
	}
	
	private String loadComments(String key) {
		String url =
						"https://www.reddit.com/r/Amoledbackgrounds/comments/" + key + "/" +
										".json";
		ArrayList<HashMap<String, String>> arrayList = (new FetchUtils()).grabPostComments(url.trim());
		
		String data = null;
		if (arrayList != null && !arrayList.isEmpty()) {
			// Fill up WebView with comments
			data = decodeHTML(parseComments(arrayList));
			
			data = "<!DOCTYPE html>\n" +
							"<html lang=\"en\">\n" +
							"<head>\n" +
							"    <meta charset=\"UTF-8\">\n" +
							"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
							"    <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">\n" +
							"    <title>Comments</title>\n" +
							"    <style type=\"text/css\">\n" +
							"        html, body {overflow-x: hidden; max-width:100%; }" +
							"        body {\n" +
							"            font-family: sans-serif; font-size:15px;\n" +
							"            word-wrap: break-word;\n" +
							"            color: #fff; margin:0;\n" +
							"        }\n" +
							"        ul {list-style: none;\n" +
							"             padding:0; margin:0;\n" +
							"             \n" +
							"        }\n " +
							"        .top-li { background:rgba(0,0,0,0.8); margin-bottom:6px; padding-bottom:8px;}" +
							"        ul ul {border-left:1px rgba(255,255,255,0.25) solid; margin-left:2px;} ul ul li {\n" +
							"            margin-left:8px; line-height:1.2;\n" +
							"        }\n" +
							"        hr {\n" +
							"            height: 1px;\n" +
							"            border: 0;\n" +
							"            margin: 0;\n" +
							"            border-bottom: 1px #343434 solid;\n" +
							"        }\n" +
							"        .author_text {\n" +
							"            font-size:14px\n" +
							"        }\n" +
							"        .red {\n" +
							"            color:red;\n" +
							"        }\n" +
							"        .username {\n" +
							"            color:#777;\n" +
							"            font-size:12px;\n" +
							"        }\n" +
							"        a {\n" +
							"            color:#f5c8d8;\n" +
							"            \n" +
							"        }\n" +
							"        .karma {text-align:right;" +
							"            font-size:14px; font-weight:bold; color:#777; padding-right:6px; margin-top:-4px;}" +
							"        .karma svg { \n" +
							"           vertical-align:middle; margin-top:-2px;} " +
							"        .karma-text {line-height:20px;}" +
							"        .author_flair {\n" +
							"            border:1px #666 solid;\n" +
							"            padding:2px;\n" +
							"        } .comment {\n" +
							"            padding: 8px 8px 8px 8px;\n" +
							"        }\n" +
							"    </style>\n" +
							"</head>\n" +
							"<body>\n" +
							data +
							"</body>\n" +
							"</html>";
		}
		return data;
	}
	
	private String decodeHTML(String data) {
		return data.replaceAll("&amp;", "&")
						.replaceAll("&lt;", "<")
						.replaceAll("&gt;", ">")
						.replaceAll("&quot;", "\"")
						.replaceAll("&#39;", "'")
						.replaceAll("&#10;", "");
	}
	
	private String parseComments(ArrayList<HashMap<String, String>> arrayList) {
		StringBuilder comments_html = new StringBuilder();
		int prev_depth = 0;
		for (int i = 0; i < arrayList.size(); i++) {
			Date date = new java.util.Date(Long.parseLong(Objects.requireNonNull(arrayList.get(i).get("utc"))) * 1000L);
			@SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
			sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT-4"));
			String formattedDate = sdf.format(date);
			CharSequence ago = null;
			try {
				ago = DateUtils.getRelativeTimeSpanString(Objects.requireNonNull(sdf.parse(formattedDate)).getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
			} catch (Exception e) {
				e.printStackTrace();
			}
			String upvoteSVG = "<svg version=\"1.1\" id=\"Layer_1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" x=\"0px\" y=\"0px\"\n" +
							"\t width=\"20px\" fill=\"#777777\" height=\"20px\" viewBox=\"0 0 20 20\" style=\"enable-background:new 0 0 20 20;\" xml:space=\"preserve\">\n" +
							"<path d=\"M9.951908,4.166667H9.951867l-5.36036,5.825781C4.593775,9.997617,4.592553,9.994831,4.594821,10h3.321361\n" +
							"\tc0.000668,0.000668,0.001042,0.001041,0.001709,0.001709V15h4.166667v-4.99648c0.001369-0.001369,0.002151-0.002151,0.00352-0.00352\n" +
							"\th3.329831L9.951908,4.166667z\"/>\n" +
							"</svg>";
			String downvoteSVG = "<svg version=\"1.1\" id=\"Layer_1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" x=\"0px\" y=\"0px\"\n" +
							"\t width=\"20px\" fill=\"red\" height=\"20px\" viewBox=\"0 0 20 20\" style=\"enable-background:new 0 0 20 20;\" xml:space=\"preserve\">\n" +
							"<path d=\"M10.048084,15.833334h0.00004l5.360361-5.825782c-0.002268-0.005168-0.001046-0.002383-0.003314-0.007551H12.08381\n" +
							"\tc-0.000668-0.000668-0.001041-0.001042-0.001709-0.001709V5H7.915434v4.99648c-0.001369,0.001369-0.002151,0.002151-0.00352,0.00352\n" +
							"\tH4.582084L10.048084,15.833334z\"/>\n" +
							"</svg>";
			String comment = "<div class=\"comment\">" +
							"<div class=\"username\">"
							+ "<span class=\"author_text\">" + " "
							+ arrayList.get(i).get("author") + " </span>"
							+ (!Objects.equals(arrayList.get(i).get("author_flair"), "null") ? " <span class=\"author_flair\">"
							+ arrayList.get(i).get("author_flair") + "</span>" : "")
							+ " • " + ago
							+ "</div>"
							+ arrayList.get(i).get("body")
							+ "<div class=\"karma\">"
							+ ((Integer.parseInt(Objects.requireNonNull(arrayList.get(i).get("score"))) < 0) ? downvoteSVG + "<span class=\"red karma-text\">"
							+ arrayList.get(i).get("score") + "</span>" : upvoteSVG + "<span class=\"karma-text\">" + arrayList.get(i).get("score")) + "</span>"
							+ "</div>"
							+ "</div>";
			int depth = Integer.parseInt(Objects.requireNonNull(arrayList.get(i).get("parent")));
			if (prev_depth > depth) {
				for (int j = depth; j < prev_depth; j++) {
					comments_html.append("</ul></li>");
				}
				prev_depth = depth;
				String CSS = "class=\"top-li\"";
				comments_html.append("<li ").append((depth == 0) ? CSS : "").append(">").append(comment);
			} else if (depth == prev_depth) {
				if (comments_html.toString().equals("")) {
					// top level item
					comments_html.append("<div class=\"list\"><ul class=\"top-ul\"><li class=\"top-li\">").append(comment);
				} else {
					String CSS = "class=\"top-li\"";
					comments_html.append("</li><li ").append((depth == 0) ? CSS : "").append(">").append(comment);
				}
			} else if (depth == prev_depth + 1) {
				prev_depth++;
				comments_html.append("<ul><li>").append(comment);
			}
		}
		if (prev_depth > 0) {
			for (int j = 0; j <= prev_depth; j++) {
				comments_html.append("</li></ul></li>");
			}
		}
		comments_html.append("</ul></div>");
		return comments_html.toString();
	}
	
}
