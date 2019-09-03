package com.droidheat.amoledbackgrounds;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.os.Build;

public class AppUtils {

    // schedule the start of the service every 10 - 30 seconds
    public static void scheduleJob(Context context) {
        ComponentName serviceComponent = new ComponentName(context, DailyJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(5799435, serviceComponent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setMinimumLatency(60 * 60 * 1000); // wait at least 1 day
            builder.setOverrideDeadline(61 * 60 * 1000);
        } else {
            builder.setPeriodic(60 * 60 * 1000); // wait at least 1 day
        }
        builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }

    public String validFileNameConvert(String string) {
        String newS = string.replaceAll("[\\\\/:*?\"<>|]", "");
        return newS.replace(".","").replaceAll("%","");
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
        builder.setPositiveButton("DONE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        return builder.create();
    }

}
