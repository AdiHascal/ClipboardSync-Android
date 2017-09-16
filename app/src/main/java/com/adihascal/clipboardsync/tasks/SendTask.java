package com.adihascal.clipboardsync.tasks;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.adihascal.clipboardsync.handler.TaskHandler;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.FileInputStream;
import java.io.IOException;

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
			Cursor cursor = AppDummy.getContext().getContentResolver().query(contentUri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);
			int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
			int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
			cursor.moveToFirst();
			
			out().writeUTF("fucking retards");
			out().writeLong(cursor.getLong(sizeIndex));
			out().writeInt(1);
			out().writeUTF("file");
			out().writeUTF(cursor.getString(nameIndex));
			out().writeLong(cursor.getLong(sizeIndex));
			cursor.close();
			
			
			FileInputStream fileIn = (FileInputStream) AppDummy.getContext().getContentResolver().openInputStream(contentUri);
			byte[] buffer = new byte[65536];
			int bytesRead;
			while((bytesRead = fileIn.read(buffer)) != -1)
			{
				out().write(buffer, 0, bytesRead);
			}
			fileIn.close();
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
		TaskHandler.pop();
	}
}
