package com.adihascal.clipboardsync.network.tasks;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.network.IReconnectListener;
import com.adihascal.clipboardsync.network.NetworkChangeReceiver;
import com.adihascal.clipboardsync.reference.Reference;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.adihascal.clipboardsync.ui.AppDummy;
import com.adihascal.clipboardsync.ui.PasteActivity;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.adihascal.clipboardsync.network.SocketHolder.in;

public class ReceiveTask implements IReconnectListener
{
	private static final NotificationManager manager = (NotificationManager) AppDummy.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
	private static final NotificationCompat.Builder builder = new NotificationCompat.Builder(AppDummy.getContext())
			.setContentTitle("copying stream to file")
			.setSmallIcon(R.drawable.ic_file_download_black_24dp);
	private final NotificationUpdater updater = new NotificationUpdater();
	private long totalBytesRead = 0L;
	private long size = 0L;
	private String sizeAsText;
	private boolean finished = false;
	
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
	
	private void copyStreamWithProgressBar(OutputStream output, boolean resumed, long alreadyRead)
	{
		if(resumed)
		{
			totalBytesRead = alreadyRead;
		}
		else
		{
			try
			{
				size = in().readLong();
				sizeAsText = humanReadableByteCount(size);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		
		byte[] buffer = new byte[15360];
		int bytesRead;
		
		try
		{
			updater.exec();
			while(totalBytesRead < size)
			{
				bytesRead = in().read(buffer, 0, (int) Math.min(buffer.length, size - totalBytesRead));
				output.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
			}
			
			if(totalBytesRead == size && !finished)
			{
				finish();
			}
			
		}
		catch(IOException e)
		{
			try
			{
				e.printStackTrace();
				System.out.println("bleh");
				synchronized(this)
				{
					wait(3000);
				}
				
				if(NetworkThreadCreator.isConnected)
				{
					copyStreamWithProgressBar(new FileOutputStream(Reference.cacheFile), true, totalBytesRead);
				}
				else
				{
					Toast.makeText(AppDummy.getContext(), "connection timeout", Toast.LENGTH_SHORT).show();
					return;
				}
			}
			catch(InterruptedException | FileNotFoundException e1)
			{
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	private void finish()
	{
		updater.stop();
		NetworkChangeReceiver.INSTANCE.removeListener(this);
		manager.cancel(10);
		
		Intent pasteIntent = new Intent()
				.setClass(AppDummy.getContext(), PasteActivity.class)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(AppDummy.getContext());
		builder.setSmallIcon(R.drawable.ic_content_paste_black_24dp)
				.setContentTitle("Ready to paste")
				.setContentText("A clip containing file data was received by ClipboardSync and is available for pasting. tap to choose destination")
				.setContentIntent(PendingIntent.getActivity(AppDummy.getContext(), 1, pasteIntent, PendingIntent.FLAG_UPDATE_CURRENT))
				.setLocalOnly(true)
				.setAutoCancel(true);
		
		((NotificationManager) AppDummy.getContext().getSystemService(NOTIFICATION_SERVICE)).notify(2, builder.build());
		
		finished = true;
	}
	
	public void run()
	{
		try
		{
			NetworkChangeReceiver.INSTANCE.addListener(this);
			copyStreamWithProgressBar(new FileOutputStream(Reference.cacheFile), false, 0);
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
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
