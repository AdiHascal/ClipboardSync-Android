package com.adihascal.clipboardsync.network;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.adihascal.clipboardsync.ui.ClipboardSync;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionListener extends Service
{
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		new Thread()
		{
			public void run()
			{
				Socket sock = null;
				try
				{
					ServerSocket serverSocket = new ServerSocket(63709);
					sock = serverSocket.accept();
					serverSocket.close();
					sock.setSoTimeout(1000);
					DataInputStream in = new DataInputStream(sock.getInputStream());
					if(in.readUTF().equals("connect"))
					{
						Intent serviceIntent = new Intent(NetworkThreadCreator.ACTION_CONNECT, null, ClipboardSync.getContext(), NetworkThreadCreator.class);
						serviceIntent.putExtra("device_address", sock.getInetAddress().getHostAddress());
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
						{
							ClipboardSync.getContext().startForegroundService(serviceIntent);
						}
						else
						{
							ClipboardSync.getContext().startService(serviceIntent);
						}
					}
					else
					{
						throw new IllegalArgumentException("invalid connection message");
					}
				}
				catch(IOException | IllegalArgumentException e)
				{
					e.printStackTrace();
				}
				finally
				{
					try
					{
						if(sock != null)
						{
							sock.close();
						}
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}.start();
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Nullable
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
}
