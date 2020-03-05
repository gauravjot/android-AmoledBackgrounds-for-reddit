package com.droidheat.amoledbackgrounds;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class DailyWallpaperService extends Service {

    private final String NOTIFICATION_CHANNEL = "daily_wallpaper";
    private long downloadID;
    private HashMap<String, String> item;
    private NotificationManager notificationManager;
    static File EXTERNAL_FILES_DIR;

    public DailyWallpaperService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        HashMap<String, String> wallpaper;
        String ext, titleStr;
        notificationManager = getSystemService(NotificationManager.class);
        EXTERNAL_FILES_DIR = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        pushNotification("Looking up the wallpaper to Reddit...", notificationManager);
        int sort_i = new SharedPrefsUtils(this).readSharedPrefsInt("auto_sort", 0);
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
            pushNotification("Error: Unable to reach Reddit for daily wallpaper.",
                    notificationManager);
            stopForeground(false);
            return START_NOT_STICKY;
        }

        if (wallpaper != null && !wallpaper.isEmpty()) {

            titleStr = Objects.requireNonNull(wallpaper.get("title")).replaceAll("\\(.*?\\) ?", "").replaceAll("\\[.*?\\] ?", "")
                    .replaceAll("\\{[^}]*\\}", "");
            titleStr = titleStr.replaceAll("\\u00A0", " ").trim();
            titleStr = titleStr.replaceAll(" ", "_") + "_" + wallpaper.get("name");
            titleStr = titleStr.substring(0, Math.min(titleStr.length(), 50));
            titleStr = (new AppUtils()).validFileNameConvert(titleStr);
            ext = Objects.requireNonNull(wallpaper.get("image")).substring(Objects.requireNonNull(wallpaper.get("image")).lastIndexOf("."));
            ext.trim();

            item = new HashMap<>();
            item.put("title", titleStr);
            item.put("ext", ext);
            item.put("width", Objects.requireNonNull(wallpaper.get("width")));
            item.put("height", Objects.requireNonNull(wallpaper.get("height")));

            // If file is already downloaded before then we simply applyAsync it

            File file = new File(EXTERNAL_FILES_DIR, titleStr + ext);
            Log.d("Location",Environment.DIRECTORY_PICTURES + "/" + item.get("title") + item.get("ext"));
            if (file.exists()) {
                pushNotification("Setting the wallpaper...",
                        notificationManager);
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
                try {
                    wallpaperManager.setBitmap(
                            decodeFile(
                                    file,
                                    Integer.parseInt(Objects.requireNonNull(wallpaper.get("width"))),
                                    Integer.parseInt(Objects.requireNonNull(wallpaper.get("height")))
                            )
                    );
                    Log.d("DailyWallpaperUtils", "Wallpaper ran 2");
                    Log.d("DailyWallpaperUtils", "Service ran 3");
                } catch (Exception ignored) {
                }
                stopForeground(true);
                return START_NOT_STICKY;
            }
            Log.d("DailyWallpaperUtils", "Wallpaper ran 1" + wallpaper.get("image"));

            final DownloadManager mgr = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

            Uri downloadUri = Uri.parse(wallpaper.get("image"));
            DownloadManager.Request request = new DownloadManager.Request(
                    downloadUri);

            request.setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI
                            | DownloadManager.Request.NETWORK_MOBILE)
                    .setTitle(titleStr)
                    .setDescription("AmoledBackgrounds")
                    .setDestinationInExternalFilesDir(this,Environment.DIRECTORY_PICTURES, titleStr + ext)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

            try {
                registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                downloadID = mgr.enqueue(request);
                Log.d("DailyWallpaperUtils", "download queued");
                pushNotification("Downloading the wallpaper...", notificationManager);
            } catch (Exception e) {
                Log.d("DailyWallpaperUtils", e.getMessage());
                pushNotification("Error: Unable to download the wallpaper from Reddit.", notificationManager);
                stopForeground(false);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this,"Service Destroyed",Toast.LENGTH_SHORT).show();
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadID == id) {
                    pushNotification("Setting the wallpaper...", notificationManager);
                    SetWallpaperAsyncTask setWallpaperAsyncTask = new SetWallpaperAsyncTask(context,this);
                    setWallpaperAsyncTask.execute();
                }
            }
        }
    };

    private void pushNotification(String message, NotificationManager notificationManager) {
        createNotificationChannel(notificationManager);

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Daily Wallpaper")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        int NOTIFICATION_ID = 456653;
        startForeground(NOTIFICATION_ID, builder.build());
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

    private Bitmap decodeFile(File f, int WIDTH, int HEIGHT) {
        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            //The new size we want to scale to
            //Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= WIDTH && o.outHeight / scale / 2 >= HEIGHT)
                scale *= 2;

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException ignored) {
        }
        return null;
    }

    @SuppressLint("StaticFieldLeak")
    class SetWallpaperAsyncTask extends AsyncTask<String, Integer, String> {

        Context context;
        BroadcastReceiver broadcastReceiver;
        SetWallpaperAsyncTask(Context context, BroadcastReceiver broadcastReceiver) {
            this.context = context;
            this.broadcastReceiver = broadcastReceiver;
        }
        @Override
        protected String doInBackground(String... strings) {
            File direct = new File(EXTERNAL_FILES_DIR,item.get("title") + item.get("ext"));
            Log.d("Location/Service",Environment.DIRECTORY_PICTURES + "/" + item.get("title") + item.get("ext"));
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            try {
                wallpaperManager.setBitmap(
                        decodeFile(
                                direct,
                                Integer.parseInt(Objects.requireNonNull(item.get("width"))),
                                Integer.parseInt(Objects.requireNonNull(item.get("height")))
                        )
                );
                Log.d("DailyWallpaperUtils", "Wallpaper ran 2");
                Log.d("DailyWallpaperUtils", "Service ran 3");
                pushNotification("Wallpaper is applied.", notificationManager);
            } catch (Exception ignored) {
                pushNotification("Failed to apply the wallpaper.", notificationManager);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            context.unregisterReceiver(broadcastReceiver);
            stopForeground(false);
        }
    }
}
