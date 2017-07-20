package com.adihascal.clipboardsync.network;

import android.content.ClipData;
import android.net.wifi.WifiManager;
import android.os.Looper;

import com.adihascal.clipboardsync.handler.ClipHandlerRegistry;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.IOException;
import java.net.Socket;
import java.util.Locale;

import static android.content.Context.WIFI_SERVICE;
import static com.adihascal.clipboardsync.network.SocketHolder.getOutputStream;
import static com.adihascal.clipboardsync.network.SocketHolder.getSocket;
import static com.adihascal.clipboardsync.network.SocketHolder.setSocket;

public class SyncClient extends Thread
{
    public static volatile String address;
    public static NetworkThreadCreator service;
    public static boolean init = false;
    private final ClipData clip;
    private final String command;

    public SyncClient(String comm, ClipData c)
    {
        this.clip = c;
        this.command = comm;
    }

    private static String getIPAddress()
    {
        int ipAddress = ((WifiManager) AppDummy.getContext().getApplicationContext().getSystemService(WIFI_SERVICE)).getConnectionInfo().getIpAddress();
        return String.format(Locale.ENGLISH, "%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    @Override
    public void run()
    {
        try
        {
            if(!init)
            {
                init();
            }

            switch (command)
            {
                case "send":
                    if (ClipHandlerRegistry.isMimeTypeSupported(this.clip.getDescription().getMimeType(0)))
                    {
                        NetworkThreadCreator.isBusy = true;
                        getOutputStream().writeUTF("receive");
                        ClipHandlerRegistry.getHandlerFor(this.clip.getDescription().getMimeType(0)).sendClip(getSocket(), this.clip);
                        NetworkThreadCreator.isBusy = false;
                    }
                    break;
                case "connect":
                    getOutputStream().writeUTF("connect");
                    getOutputStream().writeUTF(getIPAddress());

                    break;
                case "disconnect":
                    getOutputStream().writeUTF(this.command);
                    System.out.println("disconnected from " + address);
                    address = null;
                    break;
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private void init() throws IOException
    {
        Looper.prepare();
        setSocket(new Socket(address, 63708));
        service.getServer().start();
        init = true;
    }
}