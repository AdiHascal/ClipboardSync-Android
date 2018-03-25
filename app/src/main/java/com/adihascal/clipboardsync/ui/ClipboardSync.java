package com.adihascal.clipboardsync.ui;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.adihascal.clipboardsync.network.ConnectionListener;

public class ClipboardSync extends Application
{
	@SuppressLint("StaticFieldLeak")
	private static Context context;
	public static boolean listenerInit = false;
	
	public static Context getContext()
	{
		return context;
	}
	
	@Override
	protected void attachBaseContext(Context base)
	{
		super.attachBaseContext(base);
		context = base;
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		if(!listenerInit)
		{
			startService(new Intent(this, ConnectionListener.class));
			listenerInit = true;
		}
	}
}
