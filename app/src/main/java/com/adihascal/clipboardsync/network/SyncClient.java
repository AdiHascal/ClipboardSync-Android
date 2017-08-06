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
import static com.adihascal.clipboardsync.network.SocketHolder.out;
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

    private String getIPAddress()
    {
        int ipAddress = ((WifiManager) AppDummy.getContext().getApplicationContext().getSystemService(WIFI_SERVICE)).getConnectionInfo().getIpAddress();
        return String.format(Locale.ENGLISH, "%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    @Override
    public void run()
    {
        try
        {
			Looper.prepare();

            if(!init)
            {
                init();
            }
	
			while(!NetworkThreadCreator.isConnected)
			{
				wait(1000);
			}
	
			switch(command)
			{
				case "send":
					if(ClipHandlerRegistry.isMimeTypeSupported(this.clip.getDescription().getMimeType(0)))
					{
						NetworkThreadCreator.isBusy = true;
						out().writeUTF("receive");
						ClipHandlerRegistry.getHandlerFor(this.clip.getDescription().getMimeType(0)).sendClip(this.clip);
						NetworkThreadCreator.isBusy = false;
					}
                    break;
                case "connect":
					out().writeUTF("connect");
					out().writeUTF(getIPAddress());

                    break;
                case "disconnect":
					out().writeUTF(this.command);
					System.out.println("disconnected from " + address);
					address = null;
					service.getServer().interrupt();
					break;
                case "pause":
					out().writeUTF("pause");
					break;
				case "resume":
					out().writeUTF("resume");
					break;
			}
        }
		catch(IOException | InterruptedException e)
		{
			e.printStackTrace();
        }
    }

    private void init() throws IOException
    {
        setSocket(new Socket(address, 63708));
        service.getServer().start();
        init = true;
    }
}