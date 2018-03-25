package com.adihascal.clipboardsync.tasks;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.adihascal.clipboardsync.handler.TaskHandler;
import com.adihascal.clipboardsync.ui.ClipboardSync;
import com.adihascal.clipboardsync.util.UriUtils;

import java.io.File;
import java.io.FileInputStream;

import static com.adihascal.clipboardsync.network.SocketHolder.out;

public class SendTask implements ITask
{
	private Uri contentUri;
	
	public SendTask(Uri u)
	{
		this.contentUri = u;
	}
	
	@Override
	public void execute()
	{
		try
		{
			sendAsFile();
		}
		catch(Exception e)
		{
			try
			{
				sendAsUri();
			}
			catch(Exception e1)
			{
				e1.printStackTrace();
			}
		}
		finally
		{
			finish();
		}
	}
	
	private void sendAsFile() throws Exception
	{
		File f = new File(UriUtils.getPath(ClipboardSync.getContext(), contentUri));
		out().writeUTF("fucking retards");
		out().writeLong(14 + getUTFLength(f.getName()) + f.length());
		out().writeInt(1);
		out().writeUTF("file");
		out().writeUTF(f.getName());
		out().writeLong(f.length());
		
		FileInputStream fileIn = new FileInputStream(f);
		byte[] buffer = new byte[15360];
		int bytesRead;
		while((bytesRead = fileIn.read(buffer)) != -1)
		{
			out().write(buffer, 0, bytesRead);
		}
		fileIn.close();
	}
	
	private void sendAsUri() throws Exception
	{
		Cursor cursor = ClipboardSync.getContext().getContentResolver().query(contentUri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);
		int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
		int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
		cursor.moveToFirst();
		int fileSize = (int) cursor.getLong(sizeIndex);
		String fileName = cursor.getString(nameIndex);
		cursor.close();
		
		out().writeUTF("fucking retards");
		out().writeLong(14 + getUTFLength(fileName) + fileSize);
		out().writeInt(1);
		out().writeUTF("file");
		out().writeUTF(fileName);
		out().writeLong(fileSize);
		
		FileInputStream fileIn = (FileInputStream) ClipboardSync.getContext().getContentResolver().openInputStream(contentUri);
		byte[] buffer = new byte[fileSize];
		fileIn.read(buffer, 0, buffer.length);
		out().write(buffer, 0, buffer.length);
		fileIn.close();
	}
	
	@Override
	public void finish()
	{
		TaskHandler.INSTANCE.pop();
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
}
