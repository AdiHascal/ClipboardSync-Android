package com.adihascal.clipboardsync.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.network.NetworkChangeReceiver;
import com.adihascal.clipboardsync.network.SocketHolder;
import com.adihascal.clipboardsync.network.SyncClient;
import com.adihascal.clipboardsync.network.SyncServer;
import com.adihascal.clipboardsync.ui.AppDummy;
import com.adihascal.clipboardsync.ui.MainActivity;
import com.adihascal.clipboardsync.util.ClipboardEventListener;

import java.io.IOException;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

@SuppressWarnings("ConstantConditions")
public class NetworkThreadCreator extends Service
{
	public static final String ACTION_CONNECT = "com.adihascal.clipboardsync.action.CONNECT";
	public static final String ACTION_RECONNECT = "com.adihascal.clipboardsync.action.RECONNECT";
	private static final PendingIntent openMainIntent = PendingIntent.getActivity(AppDummy.getContext(), 1, new Intent().setClass(AppDummy.getContext(), MainActivity.class), 0);
	private static final PendingIntent pauseIntent = PendingIntent.getService(AppDummy.getContext(), 2, new Intent().setAction("pause").setClass(AppDummy.getContext(), WtfAndroid2.class), 0);
	private static final PendingIntent resumeIntent = PendingIntent.getService(AppDummy.getContext(), 2, new Intent().setAction("resume").setClass(AppDummy.getContext(), WtfAndroid2.class), 0);
	private static final Notification notificationNormal = new NotificationCompat.Builder(AppDummy.getContext(), "CSyncService")
			.setSmallIcon(R.mipmap.ic_launcher)
			.setContentTitle("ClipboardSync is running")
			.setContentIntent(openMainIntent)
			.setPriority(PRIORITY_MIN)
			.addAction(R.drawable.ic_play_arrow_black_24dp, "pause", pauseIntent).build();
	private static final Notification notificationPaused = new NotificationCompat.Builder(AppDummy.getContext(), "CSyncService")
			.setSmallIcon(R.mipmap.ic_launcher)
			.setContentTitle("ClipboardSync is running")
			.setContentIntent(openMainIntent)
			.setPriority(PRIORITY_MIN)
			.addAction(R.drawable.ic_pause_black_24dp, "resume", resumeIntent).build();
	public volatile static boolean paused = false, isConnected, isBusy = false;
	private SyncServer server = new SyncServer();
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if(!NetworkChangeReceiver.INSTANCE.init)
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			{
				NotificationManager manager = ((NotificationManager) AppDummy.getContext().getSystemService(NOTIFICATION_SERVICE));
				manager.createNotificationChannel(new NotificationChannel("CSyncService", "ClipboardSync Service", NotificationManager.IMPORTANCE_MIN));
				manager.createNotificationChannel(new NotificationChannel("CSyncTransfer", "ClipboardSync Data Transfers", NotificationManager.IMPORTANCE_DEFAULT));
			}
			AppDummy.getContext().registerReceiver(NetworkChangeReceiver.INSTANCE, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
			NetworkChangeReceiver.INSTANCE.init = true;
		}
		
		isConnected = NetworkChangeReceiver.INSTANCE.checkConnection(this);
		if(intent != null && isConnected)
		{
			if(intent.getAction().equals("refreshNotification"))
			{
				if(paused)
				{
					startForeground(3, notificationPaused);
				}
				else
				{
					startForeground(3, notificationNormal);
				}
			}
			else
			{
				String address = intent.getStringExtra("device_address");
				if(address != null)
				{
					startForeground(3, notificationNormal);
					SyncClient.address = address;
					SyncClient.init = false;
					((ClipboardManager) AppDummy.getContext().getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(ClipboardEventListener.INSTANCE);
					SyncClient.service = this;
					new SyncClient("connect", null).start();
				}
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy()
	{
		try
		{
			MainActivity.writeToSave();
			new SyncClient("disconnect", null).start();
			Toast.makeText(AppDummy.getContext(), "ClipboardSync service stopped", Toast.LENGTH_SHORT).show();
			if(SocketHolder.valid())
			{
				SocketHolder.getSocket().close();
			}
			stopForeground(true);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			LocalBroadcastManager.getInstance(AppDummy.getContext()).sendBroadcast(new Intent(Intent.ACTION_DEFAULT));
			super.onDestroy();
		}
	}
	
	public SyncServer getServer()
	{
		return server;
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		MainActivity.writeToSave();
		super.finalize();
	}
	
	@SuppressWarnings("unused")
	public static class WtfAndroid2 extends IntentService
	{
		public WtfAndroid2(String name)
		{
			super(name);
		}
		
		public WtfAndroid2()
		{
			super("WtfAndroid2");
		}
		
		@Override
		protected void onHandleIntent(@Nullable Intent intent)
		{
			paused = intent.getAction().equals("pause");
			AppDummy.getContext().startService(new Intent(AppDummy.getContext(), NetworkThreadCreator.class).setAction("refreshNotification"));
			new SyncClient(intent.getAction(), null).start();
		}
	}
}
