package com.adihascal.clipboardsync.tasks;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import com.adihascal.clipboardsync.handler.TaskHandler;
import com.adihascal.clipboardsync.ui.AppDummy;
import com.adihascal.clipboardsync.util.ArrayStreamSupplier;
import com.adihascal.clipboardsync.util.DynamicSequenceInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class UnpackTask implements ITask
{
	private final DynamicSequenceInputStream src;
	private final String dest;
	
	public UnpackTask(File[] sourceFiles, Intent intent) throws IOException
	{
		this.dest = intent.getStringExtra("folder");
		ClipData clip = intent.getClipData();
		if(clip != null)
		{
			((ClipboardManager) AppDummy.getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);
		}
		
		AutoDeleteFileInputStream[] streams = new AutoDeleteFileInputStream[sourceFiles.length];
		for(int i = 0; i < sourceFiles.length; i++)
		{
			streams[i] = new AutoDeleteFileInputStream(sourceFiles[i]);
		}
		this.src = new DynamicSequenceInputStream(new ArrayStreamSupplier<>(streams));
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
	public void execute()
	{
		try
		{
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
	
	@Override
	public void finish()
	{
		TaskHandler.pop();
	}
	
	private static class AutoDeleteFileInputStream extends FileInputStream
	{
		private final File f;
		
		AutoDeleteFileInputStream(File file) throws FileNotFoundException
		{
			super(file);
			this.f = file;
		}
		
		@Override
		public void close() throws IOException
		{
			super.close();
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						Thread.sleep(20);
						if(!f.delete())
						{
							throw new IOException("failed to delete file");
						}
					}
					catch(InterruptedException | IOException e)
					{
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
}
