package com.adihascal.clipboardsync.network;

import android.content.ClipboardManager;
import android.content.Context;

import com.adihascal.clipboardsync.handler.ClipHandlerRegistry;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;

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
                String type = socketIn.readUTF();
                ClipboardManager manager = (ClipboardManager) this.appContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipHandlerRegistry.getHandlerFor(type).receiveClip(socketIn, manager);
                NetworkThreadCreator.isBusy = false;
                s.close();
            }
        }
        catch (IOException | IllegalAccessException | InstantiationException e)
        {
            e.printStackTrace();
        }
    }
}
