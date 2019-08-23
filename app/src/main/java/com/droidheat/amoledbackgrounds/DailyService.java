package com.droidheat.amoledbackgrounds;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.StrictMode;
import android.util.Log;

public class DailyService extends JobService {

    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d("DailyService","Service ran 1");
        DailyWallpaper dailyWallpaper = new DailyWallpaper();
        dailyWallpaper.apply(this);
        //AppUtils.scheduleJob(this); // reschedule the job
        jobFinished(params,false);
        AppUtils.scheduleJob(this);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }


}
