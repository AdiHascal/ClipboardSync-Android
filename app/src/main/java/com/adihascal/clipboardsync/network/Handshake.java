package com.adihascal.clipboardsync.network;

import android.net.wifi.WifiManager;

import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Locale;

import static android.content.Context.WIFI_SERVICE;

public class Handshake implements Runnable
{
    private final String address;

    public Handshake(String address)
    {
        this.address = address;
    }

    public static String getIPAddress()
    {
        int ipAddress = ((WifiManager) AppDummy.getContext().getApplicationContext().getSystemService(WIFI_SERVICE)).getConnectionInfo().getIpAddress();
        return String.format(Locale.ENGLISH, "%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    @Override
    public void run()
    {
        try
        {
            Socket s = new Socket(this.address, 63708);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            out.writeUTF(getIPAddress());
            s.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
