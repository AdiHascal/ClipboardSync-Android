package com.adihascal.clipboardsync.network;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.handler.ClipHandlerRegistry;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.adihascal.clipboardsync.ui.AppDummy;
import com.adihascal.clipboardsync.ui.DestinationChooserActivity;

import java.io.IOException;

import static com.adihascal.clipboardsync.network.SocketHolder.in;
import static com.adihascal.clipboardsync.network.SocketHolder.out;
import static com.adihascal.clipboardsync.network.SocketHolder.terminate;

@SuppressWarnings({"unused", "ConstantConditions"})
public class SyncServer extends Thread
{
	@Override
	public synchronized void run()
	{
		try
		{
			Looper.prepare();
			while(true)
			{
				try
				{
					int seconds = 0;
					if(!NetworkThreadCreator.isConnected)
					{
						while(seconds < 10)
						{
							if(!NetworkThreadCreator.isConnected)
							{
								wait(1000);
								seconds++;
							}
						}
					}
					
					if(!NetworkThreadCreator.isConnected)
					{
						throw new IOException("connection timeout");
					}
					
					while(NetworkThreadCreator.isBusy)
					{
						wait(1000);
					}
					String command = in().readUTF();
					switch(command)
					{
						case "receive":
							NetworkThreadCreator.isBusy = true;
							ClipboardManager manager = (ClipboardManager) AppDummy.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
							ClipHandlerRegistry.getHandlerFor(in().readUTF()).receiveClip(manager);
							break;
						case "disconnect":
							AppDummy.getContext().stopService(new Intent(AppDummy.getContext(), NetworkThreadCreator.class));
							return;
						case "announce":
							Intent acceptIntent = new Intent().setClass(AppDummy.getContext(), WtfAndroid.class).setAction("accept");
							Intent refuseIntent = new Intent().setClass(AppDummy.getContext(), WtfAndroid.class).setAction("refuse");
							acceptIntent.putExtra("files", in().readBoolean());
							NotificationCompat.Builder builder = new NotificationCompat.Builder(AppDummy.getContext(), "CSyncTransfer")
									.setSmallIcon(R.drawable.ic_action_create)
									.setContentText("remote data detected. tap to accept or swipe to ignore")
									.setContentIntent
											(
													PendingIntent.getService(AppDummy.getContext(), 12, acceptIntent, PendingIntent.FLAG_CANCEL_CURRENT)
											)
									.setDeleteIntent
											(
													PendingIntent.getService(AppDummy.getContext(), 13, refuseIntent, PendingIntent.FLAG_CANCEL_CURRENT)
											)
									.setAutoCancel(true);
							((NotificationManager) AppDummy.getContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(4, builder.build());
							break;
					}
				}
				catch(InterruptedException | IOException e)
				{
					e.printStackTrace();
					break;
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void interrupt()
	{
		terminate();
		super.interrupt();
	}
	
	public static class WtfAndroid extends IntentService
	{
		public WtfAndroid(String name)
		{
			super(name);
		}
		
		public WtfAndroid()
		{
			super("wtf_android");
		}
		
		@Override
		protected void onHandleIntent(@Nullable Intent intent)
		{
			try
			{
				switch(intent.getAction())
				{
					case "accept":
						if(intent.getBooleanExtra("files", false))
						{
							AppDummy.getContext().startActivity(new Intent().setClass(AppDummy.getContext(), DestinationChooserActivity.class));
						}
						else
						{
							out().writeUTF("accept");
						}
						break;
					case "refuse":
						out().writeUTF(intent.getAction());
						break;
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
