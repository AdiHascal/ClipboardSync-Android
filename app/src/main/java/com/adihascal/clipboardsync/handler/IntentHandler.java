package com.adihascal.clipboardsync.handler;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

import com.adihascal.clipboardsync.tasks.MultiSendTask;
import com.adihascal.clipboardsync.tasks.ReceiveTask;
import com.adihascal.clipboardsync.tasks.SendTask;
import com.adihascal.clipboardsync.ui.ClipboardSync;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class IntentHandler implements IClipHandler
{
	@Override
	public void sendClip(ClipData clip) throws IOException
	{
		Intent intent = clip.getItemAt(0).getIntent();
		
		//I'm looking at you, Discord 9fag and Twitter
		if(intent.getType() != null && intent.getType().equals("text/plain"))
		{
			new TextHandler().sendClip(clip);
			return;
		}
		
		try
		{
			if(intent.getAction().equals(Intent.ACTION_SEND))
			{
				Uri u = intent.getParcelableExtra(Intent.EXTRA_STREAM);
				if(!intent.getType().equals("folder"))
				{
					AssetFileDescriptor fd = ClipboardSync.getContext().getContentResolver().openAssetFileDescriptor(u, "r");
					long length = fd.getLength();
					fd.close();
					
					if(length <= 15728370)
					{
						TaskHandler.INSTANCE.setAndRun(new SendTask(u));
					}
					else
					{
						TaskHandler.INSTANCE.setAndRun(new MultiSendTask(Collections.singletonList(u)));
					}
				}
				else
				{
					TaskHandler.INSTANCE.setAndRun(new MultiSendTask(Collections.singletonList(u)));
				}
			}
			else
			{
				List<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
				TaskHandler.INSTANCE.setAndRun(new MultiSendTask(uris));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void receiveClip(ClipboardManager manager)
	{
		TaskHandler.INSTANCE.setAndRun(new ReceiveTask());
	}
}
