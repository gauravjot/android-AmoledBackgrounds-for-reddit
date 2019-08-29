package com.droidheat.amoledbackgrounds;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class DailyServiceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            AppUtils.scheduleJob(context);
            DailyWallpaper dailyWallpaper = new DailyWallpaper();
            dailyWallpaper.apply(context);
        }
    }
}
