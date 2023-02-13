package com.dgrodt.phonegapMail;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MailBroadcast extends BroadcastReceiver {
	private final static String TAG = "MailBroadcast";

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Log.v(TAG, "trying to start service");
		Intent intent = new Intent(arg0, MailService.class);
		intent.putExtra("emails", arg1.getSerializableExtra("emails"));
		arg0.startService(intent);
	}

}
