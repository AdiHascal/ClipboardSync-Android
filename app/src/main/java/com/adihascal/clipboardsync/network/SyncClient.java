package com.adihascal.clipboardsync.network;

import android.content.ClipData;
import android.net.wifi.WifiManager;
import android.os.Looper;

import com.adihascal.clipboardsync.handler.ClipHandlerRegistry;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Locale;

import static android.content.Context.WIFI_SERVICE;

public class SyncClient extends Thread
{
    public static volatile String address;
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
            Socket s = new Socket(address, 63708);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            switch (command)
            {
                case "send":
                    if (ClipHandlerRegistry.isMimeTypeSupported(this.clip.getDescription().getMimeType(0)))
                    {
                        NetworkThreadCreator.isBusy = true;
                        Looper.prepare();
                        out.writeUTF("receive");
                        ClipHandlerRegistry.getHandlerFor(this.clip.getDescription().getMimeType(0)).sendClip(s, this.clip);
                        NetworkThreadCreator.isBusy = false;
                        s.close();
                    }
                    break;
                case "connect":
                    out.writeUTF("connect");
                    out.writeUTF(getIPAddress());
                    s.close();
                    break;
                case "disconnect":
                    out.writeUTF(this.command);
                    System.out.println("disconnected from " + address);
                    address = null;
                    break;
            }
        }
        catch (IOException | InstantiationException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
}