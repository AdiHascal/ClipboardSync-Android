package com.adihascal.clipboardsync.network;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.handler.TaskHandler;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.IOException;
import java.net.Socket;

import static android.net.ConnectivityManager.TYPE_WIFI;

public class NetworkChangeReceiver extends BroadcastReceiver
{
	public static final NetworkChangeReceiver INSTANCE = new NetworkChangeReceiver();
	private final Notification notification = new NotificationCompat.Builder(AppDummy.getContext(), "ClipboardSync")
			.setContentTitle("connection interrupted")
			.setSmallIcon(R.drawable.ic_error_black_24dp)
			.setProgress(0, 0, true).build();
	public boolean init = false;
	private boolean needNewSocket = false;
	
	@Override
	public synchronized void onReceive(final Context context, Intent intent)
	{
		boolean prev = NetworkThreadCreator.isConnected;
		final boolean now = checkConnection(context);
		
		if(!prev && now)
		{
			//because StrictMode
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						if(needNewSocket)
						{
							SocketHolder.setSocket(new Socket(SyncClient.address, 63708));
							new SyncClient("resume_transfer", null).start();
							needNewSocket = false;
						}
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
				}
			}).start();
			NetworkThreadCreator.isConnected = true;
			//MainActivity.reconnect();
		}
		else if(prev && !now)
		{
			try
			{
				NetworkThreadCreator.isConnected = false;
				SocketHolder.getSocket().close();
				needNewSocket = true;
				if(TaskHandler.INSTANCE.get() != null)
				{
					((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(12, notification);
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			
		}
	}
	
	public boolean checkConnection(Context context)
	{
		ConnectivityManager cm =
				(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnected() && activeNetwork.getType() == TYPE_WIFI;
	}
}
