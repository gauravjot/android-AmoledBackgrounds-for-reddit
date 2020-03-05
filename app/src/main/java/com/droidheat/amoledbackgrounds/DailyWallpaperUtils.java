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
import androidx.core.app.NotificationCompat;
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

    static File EXTERNAL_FILES_DIR;

    void applyAsync(Context context) {
        if (new SharedPrefsUtils(context).readSharedPrefsBoolean("daily_wallpaper", false)) {
            EXTERNAL_FILES_DIR = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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
            Log.d("DailyJobService", "Util ran");
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
                HashMap<String, String> wallpaper = this.result;
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


                File file = new File(EXTERNAL_FILES_DIR, titleStr + ext);
                Log.d("Location",EXTERNAL_FILES_DIR.getAbsolutePath() + "/" + titleStr + ext);
                if (file.exists()) {
                    SetWallpaperAsyncTask setWallpaperAsyncTask = new SetWallpaperAsyncTask(context);
                    setWallpaperAsyncTask.execute(titleStr,ext,wWidth+"",wHeight+"");
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
                        .setDescription("AmoledBackgrounds")
                        .setDestinationInExternalFilesDir(context,Environment.DIRECTORY_PICTURES, titleStr + ext)
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
                                SetWallpaperAsyncTask setWallpaperAsyncTask = new SetWallpaperAsyncTask(context);
                                setWallpaperAsyncTask.execute(titleStr,ext,wWidth+"",wHeight+"");
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

    static class SetWallpaperAsyncTask extends AsyncTask<String, Integer, String> {

        Context context;
        SetWallpaperAsyncTask(Context context) {
            this.context = context;
        }
        @Override
        protected String doInBackground(String... strings) {
            File direct = new File(EXTERNAL_FILES_DIR,strings[0] + strings[1]);
            Log.d("Location/Service",Environment.DIRECTORY_PICTURES + "/" + strings[0] + strings[1]);
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            try {
                wallpaperManager.setBitmap(
                        decodeFile(
                                direct,
                                Integer.parseInt(Objects.requireNonNull(strings[2])),
                                Integer.parseInt(Objects.requireNonNull(strings[3]))
                        )
                );
            } catch (Exception e) {
                Log.d("DailyWallpaperUtils","unable to set wallpaper");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
        }
    }

}