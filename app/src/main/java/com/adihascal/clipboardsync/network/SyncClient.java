package com.adihascal.clipboardsync.network;

import android.content.ClipData;
import android.content.Context;
import android.os.Looper;

import com.adihascal.clipboardsync.handler.ClipHandlerRegistry;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;

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
        NetworkThreadCreator.isBusy = true;
        try
        {
            if (ClipHandlerRegistry.isMimeTypeSupported(this.clip.getDescription().getMimeType(0)))
            {
                Looper.prepare();
                Socket s = new Socket(super.deviceAddress, 63708);
                ClipHandlerRegistry.getHandlerFor(this.clip.getDescription().getMimeType(0)).sendClip(s, this.clip);
                NetworkThreadCreator.isBusy = false;
                s.close();
            }
        }
        catch (IOException | InstantiationException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
}