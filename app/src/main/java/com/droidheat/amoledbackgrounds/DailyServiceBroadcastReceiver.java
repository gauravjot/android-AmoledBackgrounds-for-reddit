package com.droidheat.amoledbackgrounds;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class DailyServiceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AppUtils.scheduleJob(context);
    }
}
