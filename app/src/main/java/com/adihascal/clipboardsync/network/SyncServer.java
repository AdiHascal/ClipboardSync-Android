package com.adihascal.clipboardsync.network;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.handler.ClipHandlerRegistry;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.DataOutputStream;
import java.io.IOException;

import static com.adihascal.clipboardsync.network.SocketHolder.getInputStream;
import static com.adihascal.clipboardsync.network.SocketHolder.getOutputStream;
import static com.adihascal.clipboardsync.network.SocketHolder.terminate;

public class SyncServer extends Thread
{
    private static final PendingIntent acceptIntent = PendingIntent.getService(AppDummy.getContext(), 12, new Intent().setClass(AppDummy.getContext(), WtfAndroid.class).setAction("accept"), 0);
    private static final PendingIntent refuseIntent = PendingIntent.getService(AppDummy.getContext(), 13, new Intent().setClass(AppDummy.getContext(), WtfAndroid.class).setAction("refuse"), 0);

    @Override
    public void run()
    {
        try
        {
            Looper.prepare();
            while (true)
            {
                String command = getInputStream().readUTF();
                switch (command)
                {
                    case "receive":
                        NetworkThreadCreator.isBusy = true;
                        ClipboardManager manager = (ClipboardManager) AppDummy.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipHandlerRegistry.getHandlerFor(getInputStream().readUTF()).receiveClip(getInputStream(), manager);
                        NetworkThreadCreator.isBusy = false;
                        break;
                    case "disconnect":
                        AppDummy.getContext().stopService(new Intent(AppDummy.getContext(), NetworkThreadCreator.class));
                        break;
                    case "announce":
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(AppDummy.getContext())
                                .setSmallIcon(R.drawable.ic_action_create)
                                .setContentText("remote data detected. tap to accept or swipe to ignore")
                                .setContentIntent
                                        (
                                                acceptIntent
                                        )
                                .setDeleteIntent
                                        (
                                                refuseIntent
                                        )
                                .setAutoCancel(true);
                        ((NotificationManager) AppDummy.getContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(14, builder.build());
                        break;
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void interrupt()
    {
        try
        {
            terminate();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        super.interrupt();
    }

    public static class WtfAndroid extends IntentService
    {
        public WtfAndroid(String name)
        {
            super(name);
        }

        public WtfAndroid()
        {
            super("wtf_android");
        }

        @Override
        protected void onHandleIntent(@Nullable Intent intent)
        {
            try
            {
                new DataOutputStream(getOutputStream()).writeUTF(intent.getAction());
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
