package com.droidheat.amoledbackgrounds;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;

public class AppUtils {

    // schedule the start of the service every 10 - 30 seconds
    public static void scheduleJob(Context context) {
        if ((new SharedPrefsUtils(context))
                .readSharedPrefsBoolean("daily_wallpaper", false)) {
            ComponentName serviceComponent = new ComponentName(context, DailyJobService.class);
            JobInfo.Builder builder = new JobInfo.Builder(5799435, serviceComponent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setMinimumLatency(24 * 60 * 60 * 1000); // wait at least 1 day - 1440 mins
                builder.setOverrideDeadline(49 * 30 * 60 * 1000); // wait 1470 mins
            } else {
                builder.setPeriodic(24 * 60 * 60 * 1000); // wait at least 1 day
            }
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
        builder.setPositiveButton("DONE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        return builder.create();
    }
//
//
//    public static void saveToMediaStore(@NonNull final Context context, @NonNull final String displayName) {
//        final String relativeLocation = Environment.DIRECTORY_PICTURES;
//
//        final ContentValues contentValues = new ContentValues();
//        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
//        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
//        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation);
//
//        final ContentResolver resolver = context.getContentResolver();
//
//        try
//        {
//            final Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//            resolver.insert(contentUri, contentValues);
//        }
//        catch (Exception e)
//        {
//            Log.d("MediaStore","Unable to make an entry");
//        }
//
//    }

}
