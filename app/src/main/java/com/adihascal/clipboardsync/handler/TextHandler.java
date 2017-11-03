package com.adihascal.clipboardsync.handler;

import android.content.ClipData;
import android.content.ClipboardManager;

import com.adihascal.clipboardsync.reference.Reference;

import java.io.IOException;

import static com.adihascal.clipboardsync.network.SocketHolder.in;
import static com.adihascal.clipboardsync.network.SocketHolder.out;

public class TextHandler implements IClipHandler
{
	@Override
	public void sendClip(ClipData clip) throws IOException
	{
		out().writeUTF("text/plain");
		if(clip.getItemAt(0).getIntent() != null)
		{
			out().writeUTF((String) clip.getItemAt(0).getIntent().getClipData().getItemAt(0).getText());
		}
		else
		{
			out().writeUTF((String) clip.getItemAt(0).getText());
		}
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
	}
}
