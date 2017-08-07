package com.adihascal.clipboardsync.network.tasks;

import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.network.IReconnectListener;
import com.adihascal.clipboardsync.network.NetworkChangeReceiver;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import static com.adihascal.clipboardsync.network.SocketHolder.in;

public class ReceiveTask implements IReconnectListener
{
	private static final NotificationManager manager = (NotificationManager) AppDummy.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
	private static final NotificationCompat.Builder builder = new NotificationCompat.Builder(AppDummy.getContext())
			.setContentTitle("downloading")
			.setSmallIcon(R.drawable.ic_file_download_black_24dp);
	private final NotificationUpdater updater = new NotificationUpdater();
	private final String dest;
	private long totalBytesRead = 0L;
	private long size = 0L;
	private String sizeAsText;
	
	public ReceiveTask(Intent intent)
	{
		this.dest = intent.getStringExtra("folder");
		ClipData clip = intent.getClipData();
		if(clip != null)
		{
			((ClipboardManager) AppDummy.getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(intent.getClipData());
		}
	}
	
	@SuppressWarnings("SpellCheckingInspection")
	private String humanReadableByteCount(long bytes)
	{
		int unit = 1000;
		if(bytes < unit)
		{
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = String.valueOf("kMGTPE".charAt(exp - 1));
		return String.format(Locale.ENGLISH, "%.3f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	private void receiveFile(DataInputStream in, String parent) throws IOException
	{
		File f;
		String path = dest;
		String thing = in.readUTF();
		totalBytesRead += getUTFLength(thing);
		if(thing.equals("file"))
		{
			if(parent != null)
			{
				path = parent;
			}
			String name = in.readUTF();
			totalBytesRead += getUTFLength(name);
			path += "/" + name;
			f = new File(path);
			f.createNewFile();
			
			FileOutputStream out = new FileOutputStream(f);
			long length = in.readLong();
			totalBytesRead += 8;
			byte[] buffer = new byte[15360];
			int bytesRead;
			long fileBytesRead = 0;
			while(fileBytesRead < length)
			{
				bytesRead = in.read(buffer, 0, (int) Math.min(length - fileBytesRead, buffer.length));
				totalBytesRead += bytesRead;
				fileBytesRead += bytesRead;
				out.write(buffer, 0, bytesRead);
				out.flush();
			}
			out.close();
		}
		else if(thing.equals("dir"))
		{
			if(parent != null)
			{
				path = parent;
			}
			String name = in.readUTF();
			totalBytesRead += getUTFLength(name);
			path += "/" + name;
			f = new File(path);
			f.mkdir();
			int nFiles = in.readInt();
			totalBytesRead += 4;
			for(int i = 0; i < nFiles; i++)
			{
				receiveFile(in, f.getPath());
			}
		}
	}
	
	public void run()
	{
		try
		{
			NetworkChangeReceiver.INSTANCE.addListener(this);
			size = in().readLong();
			sizeAsText = humanReadableByteCount(size);
			updater.exec();
			int nFiles = in().readInt();
			for(int i = 0; i < nFiles; i++)
			{
				receiveFile(in(), null);
			}
			updater.stop();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private int getUTFLength(String str)
	{
		int len = 0;
		
		for(char c : str.toCharArray())
		{
			if((c >= 0x0001) && (c <= 0x007F))
			{
				len++;
			}
			else if(c > 0x07FF)
			{
				len += 3;
			}
			else
			{
				len += 2;
			}
		}
		return len + 2;
	}
	
	public void exec()
	{
		this.run();
	}
	
	@Override
	public void onReconnect()
	{
		//maybe won't work. if it doesn't notify directly from NetworkChangeReceiver
		synchronized(this)
		{
			notify();
		}
	}
	
	private class NotificationUpdater implements Runnable
	{
		private volatile boolean run = true;
		
		@Override
		public synchronized void run()
		{
			while(run)
			{
				try
				{
					builder.setProgress(100, (int) (100 * totalBytesRead / size), false)
							.setContentText(humanReadableByteCount(totalBytesRead) + "/" + sizeAsText);
					manager.notify(10, builder.build());
					Thread.sleep(250);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			manager.cancel(10);
		}
		
		private void exec()
		{
			new Thread(this).start();
		}
		
		private void stop()
		{
			this.run = false;
		}
	}
}
