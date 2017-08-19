package com.adihascal.clipboardsync.handler;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.adihascal.clipboardsync.tasks.MultiSendTask;
import com.adihascal.clipboardsync.tasks.ReceiveTask;
import com.adihascal.clipboardsync.tasks.SendTask;
import com.adihascal.clipboardsync.tasks.UnpackTask;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class IntentHandler implements IClipHandler
{
    @Override
	public void sendClip(ClipData clip) throws IOException
	{
		Intent intent = clip.getItemAt(0).getIntent();

        //I'm looking at you, Discord 9gag and Twitter
		if(intent.getType() != null && intent.getType().equals("text/plain"))
		{
			new TextHandler().sendClip(clip);
			return;
		}
	
		if(intent.getAction().equals(Intent.ACTION_SEND))
		{
			Uri u = intent.getParcelableExtra(Intent.EXTRA_STREAM);
			if(!intent.getType().equals("folder"))
			{
				Cursor cursor = AppDummy.getContext().getContentResolver().query(u, null, null, null, null);
				int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
				cursor.moveToFirst();
				long length = cursor.getLong(sizeIndex);
				cursor.close();
				
				if(length <= 15728640)
				{
					new SendTask(u).exec();
				}
				else
				{
					new MultiSendTask(Collections.singletonList(u)).exec();
				}
			}
			else
			{
				new MultiSendTask(Collections.singletonList(u)).exec();
			}
		}
        else
        {
			List<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			new MultiSendTask(uris).exec();
		}
	}

    @Override
	public void receiveClip(ClipboardManager manager) throws IOException
	{
		new ReceiveTask().exec();
		new UnpackTask(AppDummy.getContext().getCacheDir().listFiles(), manager.getPrimaryClip().getItemAt(0).getIntent()).exec();
	}
}
