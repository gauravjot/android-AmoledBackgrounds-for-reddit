package com.droidheat.amoledbackgrounds;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Objects;

class DailyWallpaperUtils {

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
        private HashMap<String, String> wallpaper;
        private String ext, titleStr;
        private long downloadID;

        GrabItemsAsyncTask(Context context) {
            this.wContext = new WeakReference<>(context);
            Log.d("DailyJobService", "Service ran 2");
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
            result = (new UtilsJSON()).grabPostsAsArrayList(url.trim()).get(0);
            return context;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Context context) {
            if (this.result != null && !this.result.isEmpty()) {
                wallpaper = this.result;
                titleStr = Objects.requireNonNull(wallpaper.get("title")).replaceAll("\\(.*?\\) ?", "").replaceAll("\\[.*?\\] ?", "")
                        .replaceAll("\\{[^}]*\\}", "");
                titleStr = titleStr.replaceAll("\\u00A0", " ").trim();
                titleStr = titleStr.replaceAll(" ", "_") + "_" + wallpaper.get("name");
                titleStr = titleStr.substring(0, Math.min(titleStr.length(), 50));
                titleStr = (new AppUtils()).validFileNameConvert(titleStr);
                ext = Objects.requireNonNull(wallpaper.get("image")).substring(Objects.requireNonNull(wallpaper.get("image")).lastIndexOf("."));
                ext.trim();
                final int wWidth = Integer.parseInt(Objects.requireNonNull(wallpaper.get("width")));
                final int wHeight = Integer.parseInt(Objects.requireNonNull(wallpaper.get("height")));

                // Checking if the folder in device exists
                File direct = new File(Environment.getExternalStorageDirectory()
                        + "/AmoledBackgrounds");
                if (!direct.exists()) {
                    direct.mkdirs();
                }

                // If file is already downloaded before then we simply applyAsync it
                File file = new File(Environment.getExternalStorageDirectory()
                        + "/AmoledBackgrounds/" + titleStr + ext);
                if (file.exists()) {
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                    try {
                        wallpaperManager.setBitmap(
                                decodeFile(
                                        file, wWidth, wHeight
                                )
                        );
                        Log.d("DailyWallpaperUtils", "Wallpaper ran 2");
                        Log.d("DailyWallpaperUtils", "Service ran 3");
                    } catch (Exception ignored) {
                    }
                    return;
                }
                Log.d("DailyWallpaperUtils", "Wallpaper ran 1" + wallpaper.get("image"));

                final DownloadManager mgr = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

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

                final String finalTitleStr = titleStr;
                final String finalExt = ext;
                final BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                            if (downloadID == id) {
                                File direct = new File(Environment.getExternalStorageDirectory()
                                        + "/AmoledBackgrounds/" + finalTitleStr + finalExt);
                                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                                try {
                                    wallpaperManager.setBitmap(
                                            decodeFile(
                                                    direct, wWidth, wHeight
                                            )
                                    );
                                    Log.d("DailyWallpaperUtils", "Wallpaper ran 2");
                                    Log.d("DailyWallpaperUtils", "Service ran 3");
                                } catch (Exception ignored) {
                                }
                                context.unregisterReceiver(this);
                            }
                        }
                    }
                };

                try {
                    downloadID = mgr.enqueue(request);
                    context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                } catch (Exception e) {
                    Log.d("DailyWallpaperUtils", e.getMessage());
                    Toast.makeText(context, "Unable to download Daily Wallpaper (AmoledBackgrounds)", Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }


    private static Bitmap decodeFile(File f, int WIDTH, int HIGHT) {
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


}