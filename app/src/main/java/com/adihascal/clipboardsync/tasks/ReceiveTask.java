package com.adihascal.clipboardsync.tasks;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.network.IReconnectListener;
import com.adihascal.clipboardsync.network.NetworkChangeReceiver;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Locale;

import static com.adihascal.clipboardsync.network.SocketHolder.in;

public class ReceiveTask implements IReconnectListener
{
	private static final NotificationManager manager = (NotificationManager) AppDummy.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
	private static final NotificationCompat.Builder builder = new NotificationCompat.Builder(AppDummy.getContext())
			.setContentTitle("downloading")
			.setSmallIcon(R.drawable.ic_file_download_black_24dp);
	private static final int chunkSize = 15728640;
	private final NotificationUpdater updater = new NotificationUpdater();
	private long totalBytesWritten = 0L;
	private long size = 0L;
	private String sizeAsText;
	
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
	
	public void run()
	{
		try
		{
			NetworkChangeReceiver.INSTANCE.addListener(this);
			size = in().readLong();
			sizeAsText = humanReadableByteCount(size);
			updater.exec();
			int nChunks = (int) Math.ceil((double) size / chunkSize);
			ArrayList<RandomAccessFile> packedFiles = new ArrayList<>(nChunks);
			
			for(int i = 0; i < nChunks; i++)
			{
				File p = new File(AppDummy.getContext().getCacheDir(), Integer.toString(i) + ".bin");
				p.createNewFile();
				packedFiles.add(i, new RandomAccessFile(p, "rw"));
			}
			
			for(int i = 0; i < packedFiles.size(); i++)
			{
				RandomAccessFile raf = packedFiles.get(i);
				getChunk(raf, i == packedFiles.size() - 1 ? size % chunkSize : chunkSize);
				raf.close();
			}
			updater.stop();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void getChunk(RandomAccessFile raf, long length) throws IOException
	{
		byte[] buffer = new byte[15360];
		int bytesRead;
		int totalBytesRead = (int) raf.getFilePointer();
		while(totalBytesRead < length)
		{
			bytesRead = in().read(buffer, 0, Math.min(chunkSize - totalBytesRead, buffer.length));
			totalBytesRead += bytesRead;
			totalBytesWritten += bytesRead;
			raf.write(buffer, 0, bytesRead);
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
					builder.setProgress(100, (int) (100 * totalBytesWritten / size), false)
							.setContentText(humanReadableByteCount(totalBytesWritten) + "/" + sizeAsText);
					manager.notify(10, builder.build());
					Thread.sleep(200);
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
