package com.adihascal.clipboardsync.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.adihascal.clipboardsync.ui.MainActivity;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import static android.net.ConnectivityManager.TYPE_WIFI;

public class NetworkChangeReceiver extends BroadcastReceiver
{
	public static final NetworkChangeReceiver INSTANCE = new NetworkChangeReceiver();
	private final ArrayList<IReconnectListener> listeners = new ArrayList<>();
	public boolean init = false;
	private boolean needNewSocket = false;
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		boolean prev = NetworkThreadCreator.isConnected;
		boolean now = checkConnection(context);
		
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
			MainActivity.reconnect();
			if(listeners.size() > 0)
			{
				new SyncClient("resume_transfer", null).start();
				
				for(IReconnectListener listener : listeners)
				{
					listener.onReconnect();
				}
			}
		}
		else if(prev && !now)
		{
			try
			{
				NetworkThreadCreator.isConnected = false;
				SocketHolder.getSocket().close();
				needNewSocket = true;
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
	
	public void addListener(IReconnectListener obj)
	{
		listeners.add(obj);
	}
	
	public void removeListener(IReconnectListener obj)
	{
		listeners.remove(obj);
	}
}
