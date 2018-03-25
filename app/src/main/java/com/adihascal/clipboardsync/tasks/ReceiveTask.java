package com.adihascal.clipboardsync.tasks;

import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.handler.TaskHandler;
import com.adihascal.clipboardsync.network.SocketHolder;
import com.adihascal.clipboardsync.ui.ClipboardSync;
import com.adihascal.clipboardsync.util.DynamicSequenceInputStream;
import com.adihascal.clipboardsync.util.IStreamSupplier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.SocketException;
import java.util.Locale;

import static com.adihascal.clipboardsync.network.SocketHolder.getSocket;
import static com.adihascal.clipboardsync.network.SocketHolder.in;
import static com.adihascal.clipboardsync.network.SocketHolder.out;

public class ReceiveTask implements ITask, IStreamSupplier<InputStream>
{
	private static final NotificationManager manager = (NotificationManager) ClipboardSync.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
	private static final NotificationCompat.Builder builder = new NotificationCompat.Builder(ClipboardSync.getContext(), "CSyncTransfer")
			.setContentTitle("downloading")
			.setSmallIcon(R.drawable.ic_file_download_black_24dp)
			.setSound(null);
	private static final int chunkSize = 15728640;
	private final NotificationUpdater updater = new NotificationUpdater();
	private long totalBytesWritten = 0L;
	private long size = 0L;
	private String sizeAsText;
	private int nChunks;
	private int currentChunk = 0;
	
	@SuppressWarnings("SpellCheckingInspection")
	private String humanReadableByteCount(long bytes)
	{
		final int unit = 1000;
		if(bytes < unit)
		{
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = String.valueOf("kMGTPE".charAt(exp - 1));
		return String.format(Locale.ENGLISH, "%.2f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	private void getChunk(RandomAccessFile raf, long length)
	{
		int totalBytesRead = 0;
		
		try
		{
			byte[] buffer = new byte[chunkSize / 1024];
			int bytesRead;
			totalBytesRead = (int) raf.getFilePointer();
			while(totalBytesRead < length && (bytesRead = in().read(buffer, 0, Math.min(chunkSize - totalBytesRead, buffer.length))) != -1)
			{
				raf.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
				totalBytesWritten += bytesRead;
			}
		}
		catch(IOException e)
		{
			try
			{
				e.printStackTrace();
				System.out.println("network error. waiting.");
				TaskHandler.INSTANCE.pause();
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
	public void execute()
	{
		try
		{
			size = in().readLong();
			sizeAsText = humanReadableByteCount(size);
			updater.exec();
			nChunks = (int) Math.ceil((double) size / chunkSize);
			getSocket().setSoTimeout(5000);
			new Thread(new Unpacker(this)).start();
			
			for(int i = 0; i < nChunks; i++)
			{
				File f = new File(ClipboardSync.getContext().getCacheDir(), Integer.toString(i) + ".bin");
				f.createNewFile();
				RandomAccessFile raf = new RandomAccessFile(f, "rw");
				System.out.println("receiving chunk " + i);
				getChunk(raf, length(i));
				raf.close();
				System.out.println("incrementing currentChunk to " + (currentChunk + 1));
				currentChunk++;
			}
			updater.stop();
			finish();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void finish()
	{
		try
		{
			SocketHolder.getSocket().setSoTimeout(0);
			TaskHandler.INSTANCE.pop();
		}
		catch(SocketException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public InputStream next(int index)
	{
		try
		{
			return new FileInputStream(new File(ClipboardSync.getContext().getCacheDir(), Integer.toString(index) + ".bin"));
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public boolean canProvide(int index)
	{
		return index < currentChunk;
	}
	
	@Override
	public void afterClose(int index)
	{
		new File(ClipboardSync.getContext().getCacheDir(), Integer.toString(index) + ".bin").delete();
	}
	
	@Override
	public long length(int index)
	{
		if(index == nChunks - 1 && size % chunkSize != 0)
		{
			return size % chunkSize;
		}
		else
		{
			return chunkSize;
		}
	}
	
	private static class Unpacker implements Runnable
	{
		private final String dest;
		private final IStreamSupplier<InputStream> supplier;
		private DynamicSequenceInputStream src;
		
		public Unpacker(IStreamSupplier<InputStream> supplier)
		{
			this.supplier = supplier;
			ClipboardManager manager = (ClipboardManager) ClipboardSync.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
			assert manager != null;
			Intent intent = manager.getPrimaryClip().getItemAt(0).getIntent();
			this.dest = intent.getStringExtra("folder");
			ClipData clip = intent.getClipData();
			if(clip != null)
			{
				manager.setPrimaryClip(clip);
			}
		}
		
		private void read(String parent) throws IOException
		{
			File f;
			String path = dest;
			String thing = src.readUTF();
			if(thing.equals("file"))
			{
				if(parent != null)
				{
					path = parent;
				}
				path += "/" + src.readUTF();
				f = new File(path);
				f.createNewFile();
				byte[] buffer = new byte[15360];
				int bytesRead;
				int totalBytesRead = 0;
				long length = src.readLong();
				FileOutputStream output = new FileOutputStream(f);
				while(totalBytesRead < length)
				{
					bytesRead = src.read(buffer, 0, (int) Math.min(length - totalBytesRead, 15360));
					totalBytesRead += bytesRead;
					output.write(buffer, 0, bytesRead);
				}
				output.close();
			}
			else
			{
				if(parent != null)
				{
					path = parent;
				}
				path += "/" + src.readUTF();
				f = new File(path);
				f.mkdir();
				int nFiles = src.readInt();
				for(int i = 0; i < nFiles; i++)
				{
					read(f.getPath());
				}
			}
		}
		
		@Override
		public void run()
		{
			try
			{
				this.src = new DynamicSequenceInputStream(this.supplier);
				int nFiles = src.readInt();
				for(int i = 0; i < nFiles; i++)
				{
					read(null);
				}
				this.src.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
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
