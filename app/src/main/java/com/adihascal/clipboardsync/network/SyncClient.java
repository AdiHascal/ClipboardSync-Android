package com.adihascal.clipboardsync.network;

import android.content.ClipData;
import android.content.Context;
import android.os.Looper;

import com.adihascal.clipboardsync.handler.ClipHandlerRegistry;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;

import java.io.DataOutputStream;
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
        try
        {
            Socket s = new Socket(super.deviceAddress, 63708);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            if (clip != null && ClipHandlerRegistry.isMimeTypeSupported(this.clip.getDescription().getMimeType(0)))
            {
                NetworkThreadCreator.isBusy = true;
                Looper.prepare();
                out.writeUTF("receive");
                ClipHandlerRegistry.getHandlerFor(this.clip.getDescription().getMimeType(0)).sendClip(s, this.clip);
                NetworkThreadCreator.isBusy = false;
                s.close();
            }
            else if (clip == null)
            {
                out.writeUTF("reconnect");
                out.writeUTF(Handshake.getIPAddress());
                s.close();
            }
        }
        catch (IOException | InstantiationException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
}