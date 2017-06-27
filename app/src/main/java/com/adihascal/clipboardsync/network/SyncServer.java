package com.adihascal.clipboardsync.network;

import android.content.ClipboardManager;
import android.content.Context;

import com.adihascal.clipboardsync.handler.ClipHandlerRegistry;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SyncServer extends SyncThread
{
    public volatile static ServerSocket serverSocket;

    public SyncServer(String address, Context ctx)
    {
        super(address, ctx);
    }

    @Override
    public void run()
    {
        try
        {
            serverSocket = new ServerSocket(63708);
            while (true)
            {

                Socket s = serverSocket.accept();
                NetworkThreadCreator.isBusy = true;
                DataInputStream socketIn = new DataInputStream(s.getInputStream());
                ByteArrayOutputStream baos = new ByteArrayOutputStream(104857600);
                int count;
                byte[] buffer = new byte[s.getReceiveBufferSize()];
                while ((count = socketIn.read(buffer)) > 0)
                {
                    baos.write(buffer, 0, count);
                }
                DataInputStream is = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
                String type = is.readUTF();
                ClipboardManager manager = (ClipboardManager) this.appContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipHandlerRegistry.getHandlerFor(type).receiveClip(is, manager);
                s.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
}
