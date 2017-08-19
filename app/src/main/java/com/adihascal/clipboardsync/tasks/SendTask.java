package com.adihascal.clipboardsync.tasks;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v4.util.Pair;

import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.adihascal.clipboardsync.network.SocketHolder.out;

public class SendTask implements Runnable
{
	private Uri contentUri;
	
	public SendTask(Uri u)
	{
		this.contentUri = u;
	}
	
	private Pair<String, Long> getFileNameAndSize(Uri u)
	{
		Cursor cursor = AppDummy.getContext().getContentResolver().query(u, null, null, null, null);
		int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
		int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
		cursor.moveToFirst();
		Pair<String, Long> ret = Pair.create(cursor.getString(nameIndex), cursor.getLong(sizeIndex));
		cursor.close();
		return ret;
	}
	
	/**
	 * "Do the thingy with the stuff"
	 * I'm looking at you, apps with a locked content provider
	 *
	 * @param thingy the thingy we're doing
	 * @param stuff  the stuff with which we are doing the thingy
	 */
	private void doThe(Uri thingy, Pair<String, Long> stuff)
	{
		try
		{
			FileInputStream fileIn = AppDummy.getContext().getContentResolver().openAssetFileDescriptor(thingy, "r").createInputStream();
			out().writeUTF("file");
			out().writeUTF(stuff.first);
			out().writeLong(stuff.second);
			copyNoClose(fileIn, out());
			fileIn.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void doSend()
	{
		try
		{
			out().writeUTF("fucking retards");
			out().writeInt(1);
			doThe(contentUri, getFileNameAndSize(contentUri));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void copyNoClose(InputStream input, OutputStream output)
	{
		try
		{
			byte[] buffer = new byte[65536];
			int bytesRead;
			while((bytesRead = input.read(buffer)) != -1)
			{
				output.write(buffer, 0, bytesRead);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void run()
	{
		doSend();
	}
	
	public void exec()
	{
		new Thread(this).start();
	}
}
