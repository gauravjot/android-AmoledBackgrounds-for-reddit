package com.droidheat.amoledbackgrounds.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.StrictMode;

import com.droidheat.amoledbackgrounds.utils.AppUtils;
import com.droidheat.amoledbackgrounds.utils.SharedPrefsUtils;

public class DailyJobService extends JobService {
	
	@Override
	public void onCreate() {
		super.onCreate();
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
	}
	
	@Override
	public boolean onStartJob(JobParameters params) {
		(new AppUtils()).scheduleJob(this);
		
		if ((new SharedPrefsUtils(this))
						.readSharedPrefsBoolean("daily_wallpaper", false)) {
			startForegroundService(new Intent(this, DailyWallpaperService.class));
			
		}
		return false;
	}
	
	@Override
	public boolean onStopJob(JobParameters params) {
		return false;
	}
	
}
