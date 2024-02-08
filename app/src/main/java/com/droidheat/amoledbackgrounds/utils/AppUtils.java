package com.droidheat.amoledbackgrounds.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;

import com.droidheat.amoledbackgrounds.DailyJobService;
import com.droidheat.amoledbackgrounds.R;

public class AppUtils {
	
	// schedule the start of the service every 10 - 30 seconds
	public static void scheduleJob(Context context) {
		if ((new SharedPrefsUtils(context))
						.readSharedPrefsBoolean("daily_wallpaper", false)) {
			ComponentName serviceComponent = new ComponentName(context, DailyJobService.class);
			JobInfo.Builder builder = new JobInfo.Builder(5799435, serviceComponent);
			builder.setMinimumLatency(24 * 60 * 60 * 1000); // wait at least 1 day - 1440 mins
			builder.setOverrideDeadline(49 * 30 * 60 * 1000); // wait 1470 mins
			
			builder.setRequiresCharging(false); // we don't care if the device is charging or not
			JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
			jobScheduler.schedule(builder.build());
		}
	}
	
	
	public AlertDialog changelog(Context context) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			builder.setTitle("Update Changelog: " + pInfo.versionName);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			builder.setTitle("Changelog");
		}
		builder.setMessage(context.getString(R.string.changelog_message));
		builder.setPositiveButton("DONE", (dialog, which) -> {
		
		});
		
		return builder.create();
	}
	
}
