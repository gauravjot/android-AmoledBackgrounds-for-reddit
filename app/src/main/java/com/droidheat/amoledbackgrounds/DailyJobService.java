package com.droidheat.amoledbackgrounds;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.os.StrictMode;

public class DailyJobService extends JobService {

    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        AppUtils.scheduleJob(this);

        if ((new SharedPrefsUtils(this))
                .readSharedPrefsBoolean("daily_wallpaper", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(this, DailyWallpaperService.class));
            } else {
                startService(new Intent(this, DailyWallpaperService.class));
            }
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

}
