package com.droidheat.amoledbackgrounds;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.droidheat.amoledbackgrounds.Utils.FunctionUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.HashMap;

class DailyWallpaperUtils {

  private final static String SERVICE_NAME = "DailyWallpaperUtils";

  DailyWallpaperUtils() {
  }


  void applyAsync(Context context) {
    if (new SharedPrefsUtils(context).readSharedPrefsBoolean("daily_wallpaper", false)) {
      (new GrabItemsAsyncTask(context)).execute();
    }
  }

  void apply(Context context) {

  }


  private static class GrabItemsAsyncTask extends AsyncTask<String, Integer, Context> {

    private WeakReference<Context> wContext;
    private HashMap<String, String> result;
    private String ext, titleStr;
    private long downloadID;

    GrabItemsAsyncTask(Context context) {
      this.wContext = new WeakReference<>(context);
    }

    @Override
    protected Context doInBackground(String... params) {
      int sort_i = new SharedPrefsUtils(wContext.get()).readSharedPrefsInt("auto_sort", 0);
      String sort = "hot.json";
      if (sort_i == 1) {
        sort = "top.json?t=day";
      } else if (sort_i == 2) {
        sort = "top.json?t=week";
      }
      String url = "https://www.reddit.com/r/Amoledbackgrounds/" + sort;
      Context context = wContext.get();
      result = (new UtilsJSON()).grabPostsAsArrayList(context, url.trim()).get(0);
      return context;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onPostExecute(Context context) {
      if (this.result != null && !this.result.isEmpty()) {
        HashMap<String, String> wallpaper = this.result;

        titleStr = (new FunctionUtils()).purifyRedditFileTitle(
                wallpaper.get("title"),
                wallpaper.get("name"));
        ext = (new FunctionUtils()).purifyRedditFileExtension(wallpaper.get("image"));


        String filePath = (new FunctionUtils()).getFilePath(titleStr + ext);
        File file = new File(filePath);
        if (file.exists()) {
          SetWallpaperAsyncTask setWallpaperAsyncTask = new SetWallpaperAsyncTask(context);
          setWallpaperAsyncTask.execute(titleStr, ext);
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

        final BroadcastReceiver receiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
              long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
              if (downloadID == id) {
                context.unregisterReceiver(this);
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
                    SetWallpaperAsyncTask setWallpaperAsyncTask = new SetWallpaperAsyncTask(context);
                    setWallpaperAsyncTask.execute(titleStr, ext);
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

        try {
          context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
          downloadID = mgr.enqueue(request);
          Log.d(SERVICE_NAME, "Requested service to start a new download");
        } catch (Exception e) {
          Log.d(SERVICE_NAME, "Problem making download request. Ending with failure...");
          Toast.makeText(context, "Error: Unable to download daily wallpaper from Reddit", Toast.LENGTH_LONG).show();
        }
      }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
      super.onProgressUpdate(values);
    }
  }

  static class SetWallpaperAsyncTask extends AsyncTask<String, Integer, String> {


    private WeakReference<Context> wContext;

    SetWallpaperAsyncTask(Context context) {
      this.wContext = new WeakReference<>(context);
    }

    @Override
    protected String doInBackground(String... strings) {
      return (new FunctionUtils())
              .changeWallpaper(
                      wContext.get(),
                      (new FunctionUtils()).getFilePath(strings[0] + strings[1])
              );
    }

    @Override
    protected void onPostExecute(String s) {
      if (s == "success") {
        Log.d(SERVICE_NAME, "Wallpaper is set with success.");
      } else {
        Log.d(SERVICE_NAME, "A failure has occurred while setting wallpaper.");
      }
    }
  }

}