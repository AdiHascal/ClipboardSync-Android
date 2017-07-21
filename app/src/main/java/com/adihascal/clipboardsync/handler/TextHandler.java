package com.adihascal.clipboardsync.handler;

import android.content.ClipData;
import android.content.ClipboardManager;

import com.adihascal.clipboardsync.reference.Reference;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TextHandler implements IClipHandler
{
    @Override
    public void sendClip(Socket s, ClipData clip) throws IOException
    {
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        out.writeUTF("text/plain");
        if(clip.getItemAt(0).getIntent() != null)
        {
            out.writeUTF((String) clip.getItemAt(0).getIntent().getClipData().getItemAt(0).getText());
        }
        else
        {
            out.writeUTF((String) clip.getItemAt(0).getText());
        }
    }

    @Override
    public void receiveClip(DataInputStream s, ClipboardManager manager) throws IOException
    {
        manager.setPrimaryClip(ClipData.newPlainText(Reference.ORIGIN, s.readUTF()));
    }
}
