package com.droidheat.amoledbackgrounds;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Objects;

 class DailyWallpaper {

    private static HashMap<String, String> wallpaper;
    private static String ext, titleStr;

    DailyWallpaper() {
    }

    void apply(Context context) {
        if (new SharedPrefsUtils(context).readSharedPrefsBoolean("daily_wallpaper", false)) {
            (new GrabItemsAsyncTask(context)).execute();
        }
    }

    private static class GrabItemsAsyncTask extends AsyncTask<String, Integer, Context> {

        private WeakReference<Context> wContext;
        private HashMap<String, String> result;

        GrabItemsAsyncTask(Context context) {
            this.wContext = new WeakReference<>(context);
            Log.d("DailyService", "Service ran 2");
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
                context.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                wallpaper = this.result;
                titleStr = Objects.requireNonNull(wallpaper.get("title")).replaceAll("\\(.*?\\) ?", "").replaceAll("\\[.*?\\] ?", "")
                        .replaceAll("\\{[^}]*\\}", "");
                titleStr = titleStr.replaceAll("\\u00A0", " ").trim();
                titleStr = titleStr.replaceAll(" ", "_") + "_" + wallpaper.get("name");
                titleStr = titleStr.substring(0, Math.min(titleStr.length(), 50));
                titleStr = (new AppUtils()).validFileNameConvert(titleStr);
                ext = Objects.requireNonNull(wallpaper.get("image")).substring(Objects.requireNonNull(wallpaper.get("image")).lastIndexOf("."));
                ext.trim();

                // Checking if the folder in device exists
                File direct = new File(Environment.getExternalStorageDirectory()
                        + "/AmoledBackgrounds");
                if (!direct.exists()) {
                    direct.mkdirs();
                }

                // If file is already downloaded before then we simply apply it
                File file = new File(Environment.getExternalStorageDirectory()
                        + "/AmoledBackgrounds/" + titleStr + ext);
                if (file.exists()) {
                    (new SetWallpaperAsyncTask(context)).execute();
                    return;
                }
                Log.d("DailyWallpaper", "Wallpaper ran 1" + wallpaper.get("image"));

                DownloadManager mgr = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

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
                } catch (Exception e) {
                    Log.d("DailyWallpaper", e.getMessage());
                    Toast.makeText(context, "Unable to download Daily Wallpaper (AmoledBackgrounds)", Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

    private static long downloadID;

    private static BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                SetWallpaperAsyncTask setWallpaperAsyncTask = new SetWallpaperAsyncTask(context);
                setWallpaperAsyncTask.execute();
                context.unregisterReceiver(onDownloadComplete);
                Log.d("DailyWallpaper", "Service ran 3");
            }
        }
    };

    static class SetWallpaperAsyncTask extends AsyncTask<String, Integer, String> {

        private WeakReference<Context> weakReference;

        SetWallpaperAsyncTask(Context context) {
            this.weakReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... strings) {
            File direct = new File(Environment.getExternalStorageDirectory()
                    + "/AmoledBackgrounds/" + titleStr + ext);
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(weakReference.get());
            try {
                wallpaperManager.setBitmap(
                        decodeFile(
                                direct,
                                Integer.parseInt(Objects.requireNonNull(wallpaper.get("width"))),
                                Integer.parseInt(Objects.requireNonNull(wallpaper.get("height")))
                        )
                );
                Log.d("DailyWallpaper", "Wallpaper ran 2");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            weakReference.get().unregisterReceiver(onDownloadComplete);
        }
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

}