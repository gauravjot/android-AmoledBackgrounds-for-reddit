package com.droidheat.amoledbackgrounds;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.format.DateUtils;
import android.transition.Visibility;
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
import android.widget.ViewSwitcher;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@SuppressWarnings("unchecked")
public class DownloadActivity extends AppCompatActivity {

    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
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
    private boolean isDownloaded = false;
    private String ext, titleStr;
    private ImageSwitcher imageSwitcher;
    private ProgressBar progressBar;
    private AlertDialog alertDialog;
    private WebView commentsWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_download);

        Intent intent = getIntent();
        wallpaper = (HashMap<String, String>) intent.getSerializableExtra("map");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);


        progressBar = findViewById(R.id.progress_circular);
        progressBar.setVisibility(View.INVISIBLE);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setMessage("Downloading");
        alertDialog = alertDialogBuilder.create();

        commentsWebView = findViewById(R.id.webView);
        commentsWebView.getSettings();
        commentsWebView.setBackgroundColor(Color.TRANSPARENT);
        findViewById(R.id.commentsView).setVisibility(View.INVISIBLE);
        findViewById(R.id.button_Close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideCommentWebView();
            }
        });

        findViewById(R.id.button_browser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com" + wallpaper.get("postlink")));
                startActivity(browserIntent);
            }
        });

        (new GrabItemsAsyncTask()).execute(Objects.requireNonNull(wallpaper.get("name")).replace("t3_", ""));

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        ImageView preview = findViewById(R.id.fullscreen_image);
        Picasso.get().load(wallpaper.get("image")).resize(1440, 2560).centerCrop().into(preview);

        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        imageSwitcher = findViewById(R.id.download);
        imageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            public View makeView() {
                ImageView myView = new ImageView(getApplicationContext());
                myView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                myView.setLayoutParams(new
                        ImageSwitcher.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                        ActionBar.LayoutParams.MATCH_PARENT));
                return myView;
            }
        });
        imageSwitcher.setImageResource(R.drawable.ic_file_download_black_24dp);
        Animation in = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        imageSwitcher.setInAnimation(in);
        imageSwitcher.setOutAnimation(out);

        TextView title = findViewById(R.id.title);
        TextView author = findViewById(R.id.author);
        TextView post_flair = findViewById(R.id.post_flair);
        TextView user_flair = findViewById(R.id.user_flair);
        TextView resolution = findViewById(R.id.resolution);
        TextView score = findViewById(R.id.score);
        TextView comments = findViewById(R.id.comments);

        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCommentWebView();
            }
        });

        comments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCommentWebView();
            }
        });

        findViewById(R.id.imageView1908).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCommentWebView();
            }
        });

        titleStr = Objects.requireNonNull(wallpaper.get("title")).replaceAll("\\(.*?\\) ?", "").replaceAll("\\[.*?\\] ?", "")
                .replaceAll("\\{[^}]*\\}", "");
        titleStr = titleStr.replaceAll("\\u00A0", " ").trim();
        String post_title = titleStr;
        titleStr = titleStr.replaceAll(" ", "_") + "_" + wallpaper.get("name");
        titleStr = titleStr.substring(0, Math.min(titleStr.length(), 50));
        titleStr = (new AppUtils()).validFileNameConvert(titleStr);
        title.setText(post_title.replaceAll("-","").replaceAll("&amp;", "&").trim());
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
        comments.setText(String.format("%s • View Comments", wallpaper.get("comments")));

        findViewById(R.id.open_reddit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com" + wallpaper.get("postlink")));
                startActivity(browserIntent);
            }
        });

        ext = Objects.requireNonNull(wallpaper.get("image")).substring(Objects.requireNonNull(wallpaper.get("image")).lastIndexOf("."));
        ext.trim();

        File direct = new File(Environment.getExternalStorageDirectory()
                + "/AmoledBackgrounds/" + titleStr + ext);

        if (direct.exists()) {
            isDownloaded = true;
            imageSwitcher.setImageResource(R.drawable.ic_wallpaper_black_24dp);
        }

        imageSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDownloaded) {
                    if (ContextCompat.checkSelfPermission(DownloadActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(DownloadActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST);
                    } else {
                        file_download();
                    }
                } else {
                    //Set as Wallpaper
                    imageSwitcher.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    SetWallpaperAsyncTask setWallpaperAsyncTask = new SetWallpaperAsyncTask();
                    setWallpaperAsyncTask.execute();
                }
            }
        });

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
    }

    private void showCommentWebView() {
        hide();
        Animation in = AnimationUtils.makeInAnimation(this,false);
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

    public static Bitmap decodeFile(File f, int WIDTH, int HIGHT) {
        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            //The new size we want to scale to
            //Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= WIDTH && o.outHeight / scale / 2 >= HIGHT)
                scale *= 2;

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException ignored) {
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
                file_download();
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(getApplicationContext(), "Permission required to download", Toast.LENGTH_LONG).show();
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void file_download() {
        findViewById(R.id.download).setEnabled(false);
        File direct = new File(Environment.getExternalStorageDirectory()
                + "/AmoledBackgrounds");

        if (!direct.exists()) {
            direct.mkdirs();
        }

        DownloadManager mgr = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);

        Uri downloadUri = Uri.parse(wallpaper.get("image"));
        DownloadManager.Request request = new DownloadManager.Request(
                downloadUri);

        request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI
                        | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(titleStr)
                .setDescription("r/AmoledBackgrounds")
                .setDestinationInExternalPublicDir("/AmoledBackgrounds", titleStr + ext)
                .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        try {
            downloadID = mgr.enqueue(request);
            progressBar.setVisibility(View.VISIBLE);
            imageSwitcher.setVisibility(View.INVISIBLE);
            alertDialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to download: " + titleStr + ext, Toast.LENGTH_LONG).show();
            findViewById(R.id.download).setEnabled(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }

    private long downloadID;
    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                Toast.makeText(DownloadActivity.this, "Download completed!", Toast.LENGTH_SHORT).show();
                isDownloaded = true;
                findViewById(R.id.download).setEnabled(true);
                progressBar.setVisibility(View.INVISIBLE);
                imageSwitcher.setVisibility(View.VISIBLE);
                imageSwitcher.setImageResource(R.drawable.ic_wallpaper_black_24dp);
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

    @SuppressLint("InlinedApi")
    private void show() {
        if (findViewById(R.id.commentsView).getVisibility() != View.VISIBLE) {
            // Show the system bar
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            mVisible = true;

            // Schedule a runnable to display UI elements after a delay
            mHideHandler.removeCallbacks(mHidePart2Runnable);
            mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
        }
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.commentsView).getVisibility() == View.VISIBLE) {
            hideCommentWebView();
        } else {
            finish();
        }
    }

    @SuppressLint("StaticFieldLeak")
    class SetWallpaperAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            File direct = new File(Environment.getExternalStorageDirectory()
                    + "/AmoledBackgrounds/" + titleStr + ext);
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(DownloadActivity.this);
            try {
                wallpaperManager.setBitmap(
                        decodeFile(
                                direct,
                                Integer.parseInt(Objects.requireNonNull(wallpaper.get("width"))),
                                Integer.parseInt(Objects.requireNonNull(wallpaper.get("height")))
                        )
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(getApplicationContext(), "Wallpaper set!", Toast.LENGTH_SHORT).show();
            imageSwitcher.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            imageSwitcher.setImageResource(R.drawable.ic_check_black_24dp);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GrabItemsAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            String url = "https://www.reddit.com/r/Amoledbackgrounds/comments/" + params[0] + "/.json";
            ArrayList<HashMap<String, String>> arrayList = (new UtilsJSON()).grabPostComments(url.trim());

            String data = null;
            if (arrayList != null && !arrayList.isEmpty()) {
                // Fill up WebView with comments
                data = decodedHTMLComments(arrayList);
                data = decodeHTML(data);

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

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                commentsWebView.loadData(result, "text/html", "UTF-8");
            }
        }

        private String decodeHTML(String data) {
            data = data.replaceAll("&amp;", "&");
            data = data.replaceAll("&amp;", "&");
            data = data.replaceAll("&lt;", "<");
            data = data.replaceAll("&gt;", ">");
            data = data.replaceAll("&quot;", "\"");
            data = data.replaceAll("&#39;", "'");
            data = data.replaceAll("&#10;", "");
            return data;
        }

        private String decodedHTMLComments(ArrayList<HashMap<String, String>> arrayList) {
            StringBuilder comments_html = new StringBuilder();
            int prev_depth = 0;
            for (int i = 0; i < arrayList.size(); i++) {
                Date date = new java.util.Date(Long.parseLong(Objects.requireNonNull(arrayList.get(i).get("utc"))) * 1000L);
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT-4"));
                String formattedDate = sdf.format(date);
                CharSequence ago = null;
                try {
                    ago = DateUtils.getRelativeTimeSpanString(sdf.parse(formattedDate).getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                } catch (ParseException e) {
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
                        + arrayList.get(i).get("score") + "</span>" : upvoteSVG  + "<span class=\"karma-text\">" + arrayList.get(i).get("score")) + "</span>"
                        + "</div>"
                        + "</div>";
                int depth = Integer.parseInt(Objects.requireNonNull(arrayList.get(i).get("parent")));
                if (prev_depth > depth) {
                    for (int j = depth; j < prev_depth; j++) {
                        comments_html.append("</ul></li>");
                    }
                    prev_depth = depth;
                    String CSS = "class=\"top-li\"";
                    comments_html.append("<li " + ((depth == 0) ? CSS : "") + ">").append(comment);
                } else if (depth == prev_depth) {
                    if (comments_html.toString().equals("")) {
                        // top level item
                        comments_html.append("<div class=\"list\"><ul class=\"top-ul\"><li class=\"top-li\">").append(comment);
                    } else {
                        String CSS = "class=\"top-li\"";
                        comments_html.append("</li><li " + ((depth == 0) ? CSS : "") + ">").append(comment);
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

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }
}
