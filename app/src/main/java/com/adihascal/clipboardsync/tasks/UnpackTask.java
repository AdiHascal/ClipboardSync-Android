package com.adihascal.clipboardsync.tasks;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;

public class UnpackTask
{
	private final DataInputStream src;
	private final String dest;
	
	public UnpackTask(File[] sourceFiles, Intent intent) throws IOException
	{
		this.dest = intent.getStringExtra("folder");
		ClipData clip = intent.getClipData();
		if(clip != null)
		{
			((ClipboardManager) AppDummy.getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);
		}
		
		final InputStream[] streams = new InputStream[sourceFiles.length];
		for(int i = 0; i < sourceFiles.length; i++)
		{
			streams[i] = new AutoDeleteFileInputStream(sourceFiles[i]);
		}
		Enumeration<InputStream> e = new Enumeration<InputStream>()
		{
			int index = 0;
			
			@Override
			public boolean hasMoreElements()
			{
				return index < streams.length;
			}
			
			@Override
			public InputStream nextElement()
			{
				return streams[index++];
			}
		};
		this.src = new DataInputStream(new SequenceInputStream(e));
	}
	
	public void exec() throws IOException
	{
		int nFiles = src.readInt();
		for(int i = 0; i < nFiles; i++)
		{
			read(null);
		}
		this.src.close();
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
			if(!f.delete())
			{
				throw new IOException("failed to delete file");
			}
		}
	}
}
