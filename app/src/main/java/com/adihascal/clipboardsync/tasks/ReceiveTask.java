package com.adihascal.clipboardsync.tasks;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.handler.TaskHandler;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;

import static com.adihascal.clipboardsync.network.SocketHolder.in;
import static com.adihascal.clipboardsync.network.SocketHolder.out;

public class ReceiveTask implements ITask
{
	private static final NotificationManager manager = (NotificationManager) AppDummy.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
	private static final NotificationCompat.Builder builder = new NotificationCompat.Builder(AppDummy.getContext(), "ClipboardSync")
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
	
	private void getChunk(RandomAccessFile raf, long length)
	{
		int totalBytesRead = 0;
		
		try
		{
			byte[] buffer = new byte[15360];
			int bytesRead;
			totalBytesRead = (int) raf.getFilePointer();
			while(totalBytesRead < length)
			{
				bytesRead = in().read(buffer, 0, Math.min(chunkSize - totalBytesRead, buffer.length));
				totalBytesRead += bytesRead;
				totalBytesWritten += bytesRead;
				raf.write(buffer, 0, bytesRead);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			try
			{
				System.out.println("network error. waiting.");
				TaskHandler.pause();
				out().writeLong(totalBytesRead);
				raf.seek(totalBytesRead);
				getChunk(raf, length);
			}
			catch(InterruptedException | IOException e1)
			{
				e1.printStackTrace();
			}
		}
	}
	
	@Override
	public synchronized void execute()
	{
		try
		{
			size = in().readLong();
			sizeAsText = humanReadableByteCount(size);
			updater.exec();
			int nChunks = (int) Math.ceil((double) size / chunkSize);
			RandomAccessFile[] packedFiles = new RandomAccessFile[nChunks];
			
			for(int i = 0; i < nChunks; i++)
			{
				File p = new File(AppDummy.getContext().getCacheDir(), Integer.toString(i) + ".bin");
				p.createNewFile();
				packedFiles[i] = new RandomAccessFile(p, "rw");
			}
			
			for(int i = 0; i < packedFiles.length; i++)
			{
				RandomAccessFile raf = packedFiles[i];
				long length;
				if(i == packedFiles.length - 1 && size % chunkSize != 0)
				{
					length = size % chunkSize;
				}
				else
				{
					length = chunkSize;
				}
				getChunk(raf, length);
				raf.close();
			}
			updater.stop();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void finish()
	{
		TaskHandler.pop();
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
					manager.notify(12, builder.build());
					Thread.sleep(200);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			manager.cancel(12);
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
