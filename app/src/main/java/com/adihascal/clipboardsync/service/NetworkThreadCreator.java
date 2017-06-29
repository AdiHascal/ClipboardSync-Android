package com.adihascal.clipboardsync.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
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

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent != null)
        {
            this.address = intent.getStringExtra("device_address");
            if (this.address != null)
            {
                new Thread(new Handshake(this.address)).start();
                SyncServer server = new SyncServer(this.address, AppDummy.getContext());
                server.start();
                ((ClipboardManager) AppDummy.getContext().getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(this);
                startForeground(3, new NotificationCompat.Builder(AppDummy.getContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("ClipboardSync is running")
                        .build());
            }
        }
        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy()
    {
        try
        {
            SyncServer.serverSocket.close();
            ((ClipboardManager) AppDummy.getContext().getSystemService(CLIPBOARD_SERVICE)).removePrimaryClipChangedListener(this);
            ((NotificationManager) AppDummy.getContext().getSystemService(NOTIFICATION_SERVICE)).notify(2, new NotificationCompat.Builder(AppDummy.getContext())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("ClipboardSync service stopped")
                    .build());
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
        }
    }
}
