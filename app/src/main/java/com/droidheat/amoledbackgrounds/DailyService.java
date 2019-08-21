package com.droidheat.amoledbackgrounds;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

public class DailyService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d("DailyService","Service ran 1");
        DailyWallpaper dailyWallpaper = new DailyWallpaper();
        dailyWallpaper.apply(this);
        AppUtils.scheduleJob(this); // reschedule the job
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }


}
