package com.adihascal.clipboardsync.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.IBinder;

import com.adihascal.clipboardsync.network.SyncClient;
import com.adihascal.clipboardsync.network.SyncServer;
import com.adihascal.clipboardsync.reference.Reference;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.IOException;

public class NetworkThreadCreator extends Service implements ClipboardManager.OnPrimaryClipChangedListener
{
    private SyncServer server;
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
        if (this.address != null)
        {
            this.server = new SyncServer(this.address, AppDummy.getContext());
            this.server.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        this.server.shouldRun = false;
        super.onDestroy();
    }

    @Override
    public void onPrimaryClipChanged()
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
