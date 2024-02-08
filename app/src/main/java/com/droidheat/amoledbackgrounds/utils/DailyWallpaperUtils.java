package com.droidheat.amoledbackgrounds.utils;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.concurrent.Executors;

public class DailyWallpaperUtils {
	
	private final static String SERVICE_NAME = "DailyWallpaperUtils";
	private long downloadID;
	private HashMap<String, String> wallpaper;
	
	
	public void applyAsync(Context context) {
		if (new SharedPrefsUtils(context).readSharedPrefsBoolean("daily_wallpaper", false)) {
			downloadLatestWallpaper(context);
		}
	}
	
	private void setWallpaper(Context context, String filePath) {
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				(new FunctionUtils()).changeWallpaper(
								context,
								filePath
				);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	private void downloadLatestWallpaper(Context context) {
		Executors.newSingleThreadExecutor().execute(() -> {
			int sort_i = new SharedPrefsUtils(context).readSharedPrefsInt(
							"auto_sort", 0);
			String sort = "hot.json";
			if (sort_i == 1) {
				sort = "top.json?t=day";
			} else if (sort_i == 2) {
				sort = "top.json?t=week";
			}
			String url = "https://www.reddit.com/r/Amoledbackgrounds/" + sort;
			
			wallpaper =
							(new FetchUtils()).grabPostsAsArrayList(context, url.trim()).get(0);
			String titleStr = (new FunctionUtils()).purifyRedditFileTitle(
							wallpaper.get("title"),
							wallpaper.get("name"));
			String ext =
							(new FunctionUtils()).purifyRedditFileExtension(wallpaper.get("image"));
			
			
			String filePath = (new FunctionUtils()).getFilePath(titleStr + ext);
			File file = new File(filePath);
			if (file.exists()) {
				setWallpaper(context, filePath);
				return;
			}
			
			final DownloadManager mgr = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
			
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
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
				} else {
					context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
				}
				downloadID = mgr.enqueue(request);
				Log.d(SERVICE_NAME, "Requested service to start a new download");
			} catch (Exception e) {
				Log.d(SERVICE_NAME, "Problem making download request. Ending with failure...");
				Toast.makeText(context, "Error: Unable to download daily wallpaper from Reddit", Toast.LENGTH_LONG).show();
			}
			
		});
	}
	
	
	final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
				long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
				if (downloadID == id) {
					context.unregisterReceiver(this);
					String titleStr = (new FunctionUtils()).purifyRedditFileTitle(
									wallpaper.get("title"),
									wallpaper.get("name"));
					String ext =
									(new FunctionUtils()).purifyRedditFileExtension(wallpaper.get("image"));
					File from = new File((new FunctionUtils()).getFilePath(titleStr + ".download"));
					try {
						Files.move(from.toPath(), from.toPath().resolveSibling(titleStr + ext));
						Log.d("renaming after download: ", "success");
						ContentValues values = new ContentValues();
						values.put(MediaStore.MediaColumns.DATA, (new FunctionUtils()).getFilePath(titleStr + ext));
						boolean successMediaStore = context.getContentResolver().update(
										MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values,
										MediaStore.MediaColumns.DATA + "='" + from.getPath() + "'", null) == 1;
						if (successMediaStore) {
							setWallpaper(context,
											(new FunctionUtils()).getFilePath(titleStr + ext));
						} else {
							Log.d("mediastore: ", "failed");
						}
					} catch (IOException e) {
						Log.d("renaming after download: ", "failed");
						e.printStackTrace();
					}
					//AppUtils.saveToMediaStore(context,titleStr + ext);
				}
			}
		}
	};
	
}