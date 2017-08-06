package com.adihascal.clipboardsync.network.tasks;

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.ui.AppDummy;
import com.adihascal.clipboardsync.util.UriUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static com.adihascal.clipboardsync.network.SocketHolder.out;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MultiSendTask
{
	private static final NotificationManager manager = (NotificationManager) AppDummy.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
	private static final NotificationCompat.Builder builder = new NotificationCompat.Builder(AppDummy.getContext())
			.setContentTitle("uploading")
			.setSmallIcon(R.drawable.ic_file_upload_black_24dp);
	private static final File dataDir = AppDummy.getContext().getCacheDir();
	private static final int chunkSize = 15728640;
	private final NotificationUpdater updater = new NotificationUpdater();
	private List<File> files;
	private List<File> binFiles;
	private List<Object> objectsToSend = new LinkedList<>();
	private DataOutputStream currentStream;
	private int nChunks;
	private int currentChunk = 0;
	private long size;
	private String sizeAsText;
	private long totalBytesSent = 0L;
	
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
		return String.format(Locale.ENGLISH, "%.3f %sB", bytes / Math.pow(unit, exp), pre);
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
			if(subs.length > 0)
			{
				objectsToSend.add(f.getName());
				objectsToSend.add(subs.length);
				for(File sub : subs)
				{
					addToList(sub);
				}
			}
		}
	}
	
	private void convertToFiles(List<Uri> uris)
	{
		files = new ArrayList<>(uris.size());
		
		for(int i = 0; i < uris.size(); i++)
		{
			Uri u = uris.get(i);
			files.add(i, new File(UriUtils.getPath(AppDummy.getContext(), u))); //order is important
		}
	}
	
	private long getObjectLength(Object o)
	{
		if(o instanceof String)
		{
			return getUTFLength((String) o);
		}
		else if(o instanceof Long)
		{
			return 8;
		}
		else if(o instanceof Integer)
		{
			return 4;
		}
		else if(o instanceof File)
		{
			return ((File) o).length();
		}
		else
		{
			return 0;
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
		return len;
	}
	
	private void writeObject(Object o) throws IOException
	{
		int bytesWritten;
		long bytesRemaining;
		byte[] tempBuffer;
		
		if(!(o instanceof File))
		{
			tempBuffer = convertToBytes(o);
			bytesRemaining = tempBuffer.length;
			bytesWritten = writeBytes(tempBuffer, 0, tempBuffer.length);
			bytesRemaining -= bytesWritten;
			
			if(bytesRemaining > 0)
			{
				nextStream();
				writeBytes(tempBuffer, bytesWritten, (int) Math.min(tempBuffer.length - bytesWritten, bytesRemaining));
			}
		}
		else
		{
			bytesRemaining = ((File) o).length();
			FileInputStream in = new FileInputStream((File) o);
			
			tempBuffer = new byte[chunkSize / 1024];
			int bytesRead;
			
			while((bytesRead = in.read(tempBuffer, 0, tempBuffer.length)) != -1)
			{
				bytesWritten = writeBytes(tempBuffer, 0, bytesRead);
				bytesRemaining -= bytesWritten;
				
				if(getFreeSpace() == 0 && bytesRemaining > 0)
				{
					nextStream();
					writeBytes(tempBuffer, bytesWritten, (int) Math.min(tempBuffer.length - bytesWritten, bytesRemaining));
				}
			}
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
	
	private int writeBytes(byte[] b, int off, int len) throws IOException
	{
		int ret = Math.min(getFreeSpace(), len);
		currentStream.write(b, off, ret);
		return ret;
	}
	
	private int getFreeSpace()
	{
		return chunkSize - currentStream.size();
	}
	
	private void nextStream()
	{
		try
		{
			currentStream.flush();
			currentStream.close();
			currentChunk++;
			if(currentChunk < nChunks)
			{
				currentStream = newStream(currentChunk);
			}
			byte[] buffer = new byte[chunkSize / 1024];
			int bytesRead;
			FileInputStream input = new FileInputStream(binFiles.get(currentChunk - 1));
			while((bytesRead = input.read(buffer)) != -1)
			{
				out().write(buffer, 0, bytesRead);
				totalBytesSent += bytesRead;
			}
			input.close();
			onChunkSent();
		}
		catch(IOException e)
		{
			manager.cancel(12);
			e.printStackTrace();
		}
	}
	
	private DataOutputStream newStream(int i) throws FileNotFoundException
	{
		return new DataOutputStream((new FileOutputStream(binFiles.get(i))));
	}
	
	private void onChunkSent()
	{
		if(currentChunk > 1)
		{
			File f = binFiles.get(currentChunk - 2);
			if(f.exists())
			{
				f.delete();
			}
		}
	}
	
	public void run()
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
			
			binFiles = new ArrayList<>(files.size());
			for(int i = 0; i < nChunks; i++)
			{
				File f = new File(dataDir, Integer.toString(i) + ".bin");
				f.createNewFile();
				binFiles.add(i, f);
			}
			
			out().writeUTF("application/x-java-serialized-object");
			out().writeInt(files.size());
			
			
			currentStream = newStream(0);
			updater.exec();
			for(Object o : objectsToSend)
			{
				writeObject(o);
			}
			nextStream();
			updater.stop();
			manager.cancel(12);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void exec()
	{
		this.run();
	}
	
	private class NotificationUpdater implements Runnable
	{
		private boolean run = true;
		
		@Override
		public synchronized void run()
		{
			while(run)
			{
				try
				{
					builder.setProgress(100, (int) (100 * totalBytesSent / size), false)
							.setContentText(humanReadableByteCount(totalBytesSent) + "/" + sizeAsText);
					manager.notify(12, builder.build());
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
