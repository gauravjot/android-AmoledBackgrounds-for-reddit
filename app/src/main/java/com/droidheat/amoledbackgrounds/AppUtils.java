package com.droidheat.amoledbackgrounds;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class AppUtils {

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

        AlertDialog alertDialog = builder.create();


        return alertDialog;
    }
}
