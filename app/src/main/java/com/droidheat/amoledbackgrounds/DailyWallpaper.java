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
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Objects;

 class DailyWallpaper {

    private static HashMap<String, String> wallpaper;
    private static String ext, titleStr;

    private String NOTIFICATION_CHANNEL = "daily_wallpaper";

    private int NOTIFICATION_ID = 456653;

    DailyWallpaper() {
    }

    void applyAsync(Context context) {
        if (new SharedPrefsUtils(context).readSharedPrefsBoolean("daily_wallpaper", false)) {
            (new GrabItemsAsyncTask(context)).execute();
        }
    }

    void apply(Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        pushNotification(context, "Looking up the wallpaper to Reddit...", notificationManager);
        int sort_i = new SharedPrefsUtils(context).readSharedPrefsInt("auto_sort", 0);
        String sort = "hot.json";
        if (sort_i == 1) {
            sort = "top.json?t=day";
        } else if (sort_i == 2) {
            sort = "top.json?t=week";
        }
        String url = "https://www.reddit.com/r/amoledbackgrounds/" + sort;

        try {
            wallpaper = (new UtilsJSON()).grabPostsAsArrayList(url.trim()).get(0);
        } catch (Exception e) {
            pushNotification(context, "Error: Unable to reach Reddit for daily wallpaper.",
                    notificationManager);
        }

        if (wallpaper != null && !wallpaper.isEmpty()) {
            context.registerReceiver(onDownloadComplete1, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
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

            // If file is already downloaded before then we simply applyAsync it
            File file = new File(Environment.getExternalStorageDirectory()
                    + "/AmoledBackgrounds/" + titleStr + ext);
            if (file.exists()) {
                pushNotification(context, "Setting the wallpaper...",
                        notificationManager);
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                try {
                    wallpaperManager.setBitmap(
                            decodeFile(
                                    file,
                                    Integer.parseInt(Objects.requireNonNull(wallpaper.get("width"))),
                                    Integer.parseInt(Objects.requireNonNull(wallpaper.get("height")))
                            )
                    );
                    Log.d("DailyWallpaper", "Wallpaper ran 2");
                    Log.d("DailyWallpaper", "Service ran 3");
                } catch (Exception ignored) {
                }
                notificationManager.cancel(NOTIFICATION_ID);
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
                pushNotification(context, "Downloading the wallpaper...",notificationManager);
            } catch (Exception e) {
                Log.d("DailyWallpaper", e.getMessage());
                pushNotification(context, "Error: Unable to download the wallpaper from Reddit.",notificationManager);
            }
        }
    }

    private BroadcastReceiver onDownloadComplete1 = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             //Fetching the download id received with the broadcast
             long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
             //Checking if the received broadcast is for our enqueued download by matching download id
             if (downloadID == id) {

                 NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                 pushNotification(context, "Setting the wallpaper...", notificationManager);
                 File direct = new File(Environment.getExternalStorageDirectory()
                         + "/AmoledBackgrounds/" + titleStr + ext);
                 WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                 try {
                     wallpaperManager.setBitmap(
                             decodeFile(
                                     direct,
                                     Integer.parseInt(Objects.requireNonNull(wallpaper.get("width"))),
                                     Integer.parseInt(Objects.requireNonNull(wallpaper.get("height")))
                             )
                     );
                     Log.d("DailyWallpaper", "Wallpaper ran 2");
                     Log.d("DailyWallpaper", "Service ran 3");
                     pushNotification(context, "Wallpaper is applied.",notificationManager);
                 } catch (Exception ignored) {
                 }
             }
             wallpaper = null;
             context.unregisterReceiver(onDownloadComplete1);
         }
     };

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

                // If file is already downloaded before then we simply applyAsync it
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
                Log.d("DailyWallpaper", "Service ran 3");
            }
            context.unregisterReceiver(onDownloadComplete);
        }
    };



    private static class SetWallpaperAsyncTask extends AsyncTask<String, Integer, String> {

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
            wallpaper = null;
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
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

    private void pushNotification(Context context, String message, NotificationManager notificationManager) {
        createNotificationChannel(notificationManager);

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Daily Wallpaper")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

     private void createNotificationChannel(NotificationManager notificationManager) {
         // Create the NotificationChannel, but only on API 26+ because
         // the NotificationChannel class is new and not in the support library
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             CharSequence name = "Daily Wallpaper";
             String description = "Notification runs when daily wallpaper updates.";
             int importance = NotificationManager.IMPORTANCE_LOW;
             NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, name, importance);
             channel.setDescription(description);
             // Register the channel with the system; you can't change the importance
             // or other notification behaviors after this
             notificationManager.createNotificationChannel(channel);
         }
     }


 }