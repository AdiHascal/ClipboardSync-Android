package com.adihascal.clipboardsync.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.network.SyncClient;
import com.adihascal.clipboardsync.network.SyncServer;
import com.adihascal.clipboardsync.reference.Reference;
import com.adihascal.clipboardsync.ui.AppDummy;
import com.adihascal.clipboardsync.ui.MainActivity;

public class NetworkThreadCreator extends Service implements ClipboardManager.OnPrimaryClipChangedListener
{
    public static final String ACTION_CONNECT = "com.adihascal.clipboardsync.action.CONNECT";
    public static final String ACTION_RECONNECT = "com.adihascal.clipboardsync.action.RECONNECT";
    public volatile static boolean isBusy = false;
    private String address;
    private SyncServer server = new SyncServer();

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
                startForeground(3, new NotificationCompat.Builder(AppDummy.getContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("ClipboardSync is running")
                        .build());
                SyncClient.address = this.address;
                SyncClient.init = false;
                ((ClipboardManager) AppDummy.getContext().getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(this);
                SyncClient.service = this;
                new SyncClient("connect", null).start();
            }
        }
        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy()
    {
        MainActivity.writeToSave();
        new SyncClient("disconnect", null).start();
        server.interrupt();
        Toast.makeText(AppDummy.getContext(), "ClipboardSync service stopped", Toast.LENGTH_SHORT).show();
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
                new SyncClient("send", clip).start();
            }
        }
    }

    public SyncServer getServer()
    {
        return server;
    }

    @Override
    protected void finalize() throws Throwable
    {
        MainActivity.writeToSave();
        super.finalize();
    }
}
