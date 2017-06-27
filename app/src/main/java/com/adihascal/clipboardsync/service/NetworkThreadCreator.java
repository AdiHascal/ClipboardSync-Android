package com.adihascal.clipboardsync.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.IBinder;

import com.adihascal.clipboardsync.network.Handshake;
import com.adihascal.clipboardsync.network.SyncClient;
import com.adihascal.clipboardsync.network.SyncServer;
import com.adihascal.clipboardsync.reference.Reference;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.IOException;

public class NetworkThreadCreator extends Service implements ClipboardManager.OnPrimaryClipChangedListener
{
    public volatile static boolean isBusy = false;
    private String address;

    public NetworkThreadCreator() throws IOException
    {
        ((ClipboardManager) AppDummy.getContext().getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(this);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        this.address = intent.getStringExtra("device_address");
        new Thread(new Handshake(this.address)).start();
        if (this.address != null)
        {
            SyncServer server = new SyncServer(this.address, AppDummy.getContext());
            server.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        try
        {
            SyncServer.serverSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onPrimaryClipChanged()
    {
        if (!isBusy)
        {
            ClipboardManager manager = (ClipboardManager) AppDummy.getContext().getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = manager.getPrimaryClip();
            if (this.address != null && (clip.getDescription().getLabel() == null || !clip.getDescription().getLabel().equals(Reference.ORIGIN)))
            {
                new SyncClient(this.address, AppDummy.getContext(), clip).start();
            }
            manager.removePrimaryClipChangedListener(this);
            try
            {
                Thread.sleep(100);
                manager.addPrimaryClipChangedListener(this);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}
