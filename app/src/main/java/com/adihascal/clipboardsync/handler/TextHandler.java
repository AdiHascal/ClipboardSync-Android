package com.adihascal.clipboardsync.handler;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;

import com.adihascal.clipboardsync.reference.Reference;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;

import java.io.IOException;

import static com.adihascal.clipboardsync.network.SocketHolder.in;
import static com.adihascal.clipboardsync.network.SocketHolder.out;

public class TextHandler implements IClipHandler
{
	@Override
	public void sendClip(ClipData clip) throws IOException
	{
		if(clip.getItemAt(0).getIntent() != null)
		{
			ClipData.Item item = clip.getItemAt(0).getIntent().getClipData().getItemAt(0);
			if(item.getText() != null)
			{
				out().writeUTF("text/plain");
				out().writeUTF((String) item.getText());
			}
			else if(item.getUri() != null)
			{
				new IntentHandler().sendClip(ClipData.newIntent("ContentUri", new Intent().setType(ClipDescription.MIMETYPE_TEXT_INTENT).setAction(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, item.getUri())));
				return;
			}
		}
		else
		{
			out().writeUTF("text/plain");
			out().writeUTF((String) clip.getItemAt(0).getText());
		}
		NetworkThreadCreator.isBusy = false;
	}
	
	@Override
	public void receiveClip(ClipboardManager manager)
	{
		try
		{
			manager.setPrimaryClip(ClipData.newPlainText(Reference.ORIGIN, in().readUTF()));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		NetworkThreadCreator.isBusy = false;
	}
}
