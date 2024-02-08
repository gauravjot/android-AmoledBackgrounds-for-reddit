package com.droidheat.amoledbackgrounds.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.droidheat.amoledbackgrounds.utils.AppUtils;

import java.util.Objects;

public class RebootBroadcastReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
			(new AppUtils()).scheduleJob(context);
		}
	}
}
