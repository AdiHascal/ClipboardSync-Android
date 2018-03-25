package com.adihascal.clipboardsync.tasks;

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.handler.TaskHandler;
import com.adihascal.clipboardsync.ui.ClipboardSync;
import com.adihascal.clipboardsync.util.DynamicSequenceOutputStream;
import com.adihascal.clipboardsync.util.IStreamSupplier;
import com.adihascal.clipboardsync.util.UriUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static com.adihascal.clipboardsync.network.SocketHolder.in;
import static com.adihascal.clipboardsync.network.SocketHolder.out;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MultiSendTask implements IStreamSupplier<OutputStream>, ITask
{
	private static final NotificationManager manager = (NotificationManager) ClipboardSync.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
	private static final NotificationCompat.Builder builder = new NotificationCompat.Builder(ClipboardSync.getContext(), "CSyncTransfer")
			.setContentTitle("uploading")
			.setContentText("")
			.setSmallIcon(R.drawable.ic_file_upload_black_24dp)
			.setSound(null);
	private static final File dataDir = ClipboardSync.getContext().getCacheDir();
	private static final int chunkSize = 15728640;
	private final NotificationUpdater updater = new NotificationUpdater();
	private List<File> files;
	private List<File> binFiles;
	private List<Object> objectsToSend = new LinkedList<>();
	private DynamicSequenceOutputStream stream;
	private int currentChunk = 0;
	private long size = 4L; //accounting for files.size()
	private String sizeAsText;
	private long totalBytesSent = 0L;
	private int nChunks;
	
	public MultiSendTask(List<Uri> uList)
	{
		convertToFiles(uList);
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
		return String.format(Locale.ENGLISH, "%.2f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	private void addToList(File f)
	{
		if(!f.isDirectory())
		{
			objectsToSend.add("file");
			objectsToSend.add(f.getName());
			objectsToSend.add(f.length());
			objectsToSend.add(f);
		}
		else
		{
			objectsToSend.add("dir");
			File[] subs = f.listFiles();
			assert subs != null;
			objectsToSend.add(f.getName());
			objectsToSend.add(subs.length);
			for(File sub : subs)
			{
				addToList(sub);
			}
		}
	}
	
	private void convertToFiles(List<Uri> uris)
	{
		files = new ArrayList<>(uris.size());
		
		for(int i = 0; i < uris.size(); i++)
		{
			Uri u = uris.get(i);
			files.add(i, new File(UriUtils.getPath(ClipboardSync.getContext(), u))); //order is important
		}
	}
	
	private long getObjectLength(Object o)
	{
		switch(o.getClass().getSimpleName())
		{
			case "String":
				return getUTFLength((String) o) + 2; //for the 2 null terminators
			case "Integer":
				return 4;
			case "Long":
				return 8;
			case "File":
				return ((File) o).length();
		}
		throw new IllegalArgumentException("Invalid type");
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
		return len;
	}
	
	private void writeObject(Object o) throws IOException
	{
		if(!(o instanceof File))
		{
			stream.write(convertToBytes(o));
		}
		else
		{
			FileInputStream in = new FileInputStream((File) o);
			byte[] tempBuffer = new byte[chunkSize / 1024];
			int bytesRead;
			while((bytesRead = in.read(tempBuffer)) != -1)
			{
				stream.write(tempBuffer, 0, bytesRead);
			}
			in.close();
		}
	}
	
	private byte[] convertToBytes(Object o)
	{
		switch(o.getClass().getSimpleName())
		{
			case "String":
				return convertStringToUTFBytes((String) o);
			case "Integer":
				return convertIntToBytes((Integer) o);
			case "Long":
				return convertLongToBytes(((Long) o));
		}
		return new byte[0];
	}
	
	private byte[] convertStringToUTFBytes(String str)
	{
		int strlen = str.length();
		int utflen = getUTFLength(str);
		int c, count = 0;
		
		byte[] bytearr = new byte[utflen + 2];
		bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
		bytearr[count++] = (byte) (utflen & 0xFF);
		
		int i;
		for(i = 0; i < strlen; i++)
		{
			c = str.charAt(i);
			if(!((c >= 0x0001) && (c <= 0x007F)))
			{
				break;
			}
			bytearr[count++] = (byte) c;
		}
		
		for(; i < strlen; i++)
		{
			c = str.charAt(i);
			if((c >= 0x0001) && (c <= 0x007F))
			{
				bytearr[count++] = (byte) c;
				
			}
			else if(c > 0x07FF)
			{
				bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				bytearr[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				bytearr[count++] = (byte) (0x80 | (c & 0x3F));
			}
			else
			{
				bytearr[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
				bytearr[count++] = (byte) (0x80 | (c & 0x3F));
			}
		}
		return bytearr;
	}
	
	private byte[] convertLongToBytes(long val)
	{
		byte[] writeBuffer = new byte[8];
		writeBuffer[0] = (byte) (val >>> 56);
		writeBuffer[1] = (byte) (val >>> 48);
		writeBuffer[2] = (byte) (val >>> 40);
		writeBuffer[3] = (byte) (val >>> 32);
		writeBuffer[4] = (byte) (val >>> 24);
		writeBuffer[5] = (byte) (val >>> 16);
		writeBuffer[6] = (byte) (val >>> 8);
		writeBuffer[7] = (byte) val;
		
		return writeBuffer;
	}
	
	private byte[] convertIntToBytes(int val)
	{
		byte[] buf = new byte[4];
		buf[0] = (byte) ((val >>> 24) & 0xFF);
		buf[1] = (byte) ((val >>> 16) & 0xFF);
		buf[2] = (byte) ((val >>> 8) & 0xFF);
		buf[3] = (byte) (val & 0xFF);
		
		return buf;
	}
	
	@Override
	public OutputStream next(int index)
	{
		try
		{
			System.out.println("loading chunk " + index);
			return new FileOutputStream(binFiles.get(index));
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
		afterClose(index, false);
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
	
	private void afterClose(int index, boolean recursive)
	{
		RandomAccessFile input = null;
		try
		{
			System.out.println("sending chunk " + index + (recursive ? " recursively" : ""));
			byte[] buffer = new byte[chunkSize / 1024];
			int bytesRead;
			input = new RandomAccessFile(binFiles.get(index), "rw");
			if(!recursive)
			{
				System.out.println("incrementing currentChunk to " + (currentChunk + 1));
				currentChunk++;
			}
			else
			{
				long p = in().readLong();
				System.out.println("seeking to position " + p);
				input.seek(p);
			}
			
			while((bytesRead = input.read(buffer)) != -1)
			{
				out().write(buffer, 0, bytesRead);
				totalBytesSent += bytesRead;
			}
			input.close();
			binFiles.get(index).delete();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			try
			{
				assert input != null : "wut";
				input.close();
				updater.stop();
				TaskHandler.INSTANCE.pause();
				updater.exec();
				afterClose(index, true);
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
			for(File bin : dataDir.listFiles())
			{
				if(bin.getName().endsWith(".bin"))
				{
					bin.delete();
				}
			}
			
			for(File f : files)
			{
				addToList(f);
			}
			
			for(Object o : objectsToSend)
			{
				size += getObjectLength(o);
			}
			sizeAsText = humanReadableByteCount(size);
			
			nChunks = (int) Math.ceil((double) size / chunkSize);
			
			binFiles = new ArrayList<>(nChunks);
			for(int i = 0; i < nChunks; i++)
			{
				File f = new File(dataDir, Integer.toString(i) + ".bin");
				f.createNewFile();
				binFiles.add(i, f);
			}
			
			out().writeUTF("application/x-java-serialized-object");
			out().writeLong(size);
			
			updater.exec();
			stream = new DynamicSequenceOutputStream(this);
			stream.write(convertIntToBytes(files.size()));
			for(Object o : objectsToSend)
			{
				writeObject(o);
			}
			stream.close();
			updater.stop();
			manager.cancel(12);
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
		TaskHandler.INSTANCE.pop();
	}
	
	private class NotificationUpdater implements Runnable
	{
		private boolean run = true;
		
		@Override
		public void run()
		{
			while(run)
			{
				try
				{
					builder.setProgress(100, (int) (100 * totalBytesSent / size), false)
							.setContentText(humanReadableByteCount(totalBytesSent) + "/" + sizeAsText);
					manager.notify(12, builder.build());
					Thread.sleep(200);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		private void exec()
		{
			run = true;
			new Thread(this).start();
		}
		
		private void stop()
		{
			this.run = false;
		}
	}
}
