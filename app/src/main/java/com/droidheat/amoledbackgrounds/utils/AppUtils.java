package com.droidheat.amoledbackgrounds.utils;

import android.Manifest;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.droidheat.amoledbackgrounds.services.DailyJobService;
import com.droidheat.amoledbackgrounds.R;

import java.io.IOException;
import java.util.Objects;

public class AppUtils {
	
	public Boolean changeWallpaper(Context context, String pathOfNewWallpaper) {
		
		if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
						!= PackageManager.PERMISSION_GRANTED) {
			Log.d("Permission error", "Read permission is not granted");
		} else {
			Log.d("Permission", "Read permission is granted");
		}
		
		WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
		Bitmap bitmap = getBitmap(context, pathOfNewWallpaper);
		if (bitmap != null) {
			try {
				wallpaperManager.setBitmap(bitmap);
				Log.d("wallpaper", "wallpaper changed");
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Log.d("error", "bitmap is null " + pathOfNewWallpaper);
		}
		return false;
	}
	
	public String getFilePath(String fileName) {
		String path = "/storage/emulated/0/Pictures";
		return path + "/" + fileName;
	}
	
	public String purifyRedditFileTitle(String rawFileTitle, String redditUniqueName) {
		String titleStr = Objects.requireNonNull(rawFileTitle)
						.replaceAll("\\(.*?\\) ?", "")
						.replaceAll("\\[.*?\\] ?", "")
						.replaceAll("\\{[^}]*\\}", "")
						.replaceAll("\\u00A0", " ").trim();
		titleStr = titleStr.replaceAll(" ", "_") + "_" + redditUniqueName;
		titleStr = titleStr.substring(0, Math.min(titleStr.length(), 50));
		titleStr = validFileNameConvert(titleStr);
		return titleStr;
	}
	
	public String purifyRedditFileExtension(String rawImageName) {
		return Objects.requireNonNull(rawImageName)
						.substring(Objects.requireNonNull(rawImageName).lastIndexOf("."))
						.trim();
		
	}
	
	private String validFileNameConvert(String string) {
		return string.replaceAll("[\\\\/:*?\"<>|]", "")
						.replace(".", "")
						.replaceAll("%", "")
						+ "_amoled_droidheat";
	}
	
	private Bitmap getBitmap(Context context, String pathOfNewWallpaper) {
		
		if (Build.VERSION.SDK_INT >= 29) {
			String[] projection = new String[]{
							MediaStore.Images.Media._ID,
							MediaStore.Images.Media.DISPLAY_NAME,
							MediaStore.Images.Media.RELATIVE_PATH
			};
			String selection = MediaStore.Images.Media.DISPLAY_NAME + " == ?";
			String[] selectionArgs = new String[]{
							pathOfNewWallpaper.substring(pathOfNewWallpaper.lastIndexOf("/")).replace("/", "")
											.trim()
			};
			try {
				final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
								projection, selection, selectionArgs, MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC");
				if (cursor != null && cursor.moveToFirst()) {
					// You can replace '0' by 'cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)'
					// Note that now, you read the column '_ID' and not the column 'DATA'
					Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getInt(0));
					
					// now that you have the media URI, you can decode it to a bitmap
					try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(imageUri, "r")) {
						if (pfd != null) {
							return BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
						}
					} catch (IOException ex) {
						return null;
					}
					cursor.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			return BitmapFactory.decodeFile(pathOfNewWallpaper, options);
		}
		return null;
	}
	
	
	// schedule the start of the service every 10 - 30 seconds
	public void scheduleJob(Context context) {
		if ((new SharedPrefsUtils(context))
						.readSharedPrefsBoolean("daily_wallpaper", false)) {
			ComponentName serviceComponent = new ComponentName(context, DailyJobService.class);
			JobInfo.Builder builder = new JobInfo.Builder(5799435, serviceComponent);
			builder.setMinimumLatency(24 * 60 * 60 * 1000); // wait at least 1 day - 1440 mins
			builder.setOverrideDeadline(49 * 30 * 60 * 1000); // wait 1470 mins
			
			builder.setRequiresCharging(false); // we don't care if the device is charging or not
			JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
			jobScheduler.schedule(builder.build());
		}
	}
	
	
	public AlertDialog changelog(Context context) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			builder.setTitle("Update Changelog: " + pInfo.versionName);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			builder.setTitle("Changelog");
		}
		builder.setMessage(context.getString(R.string.changelog_message));
		builder.setPositiveButton("DONE", (dialog, which) -> {
		
		});
		
		return builder.create();
	}
	
}
