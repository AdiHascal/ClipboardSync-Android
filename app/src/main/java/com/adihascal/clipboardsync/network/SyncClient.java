package com.adihascal.clipboardsync.network;

import android.content.ClipData;
import android.content.Context;

import com.adihascal.clipboardsync.handler.ClipHandlerRegistry;

import java.io.IOException;
import java.net.Socket;

public class SyncClient extends SyncThread
{
    private final ClipData clip;

    public SyncClient(String address, Context ctx, ClipData c)
    {
        super(address, ctx);
        this.clip = c;
    }

    @Override
    public void run()
    {
        Socket s;
        try
        {
            if (ClipHandlerRegistry.isMimeTypeSupported(this.clip.getDescription().getMimeType(0)))
            {
                s = new Socket(super.deviceAddress, 63708);
                ClipHandlerRegistry.getHandlerFor(this.clip.getDescription().getMimeType(0)).sendClip(s, this.clip);
            }
        }
        catch (IOException | InstantiationException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
}