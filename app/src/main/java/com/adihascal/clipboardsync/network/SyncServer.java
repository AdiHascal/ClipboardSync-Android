package com.adihascal.clipboardsync.network;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import com.adihascal.clipboardsync.handler.ClipHandlerRegistry;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.adihascal.clipboardsync.ui.AppDummy;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SyncServer extends Thread
{
    private ServerSocket serverSocket;

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
                String command = socketIn.readUTF();
                switch (command)
                {
                    case "receive":
                        ClipboardManager manager = (ClipboardManager) AppDummy.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipHandlerRegistry.getHandlerFor(socketIn.readUTF()).receiveClip(socketIn, manager);
                        NetworkThreadCreator.isBusy = false;
                        break;
                    case "disconnect":
                        AppDummy.getContext().stopService(new Intent(AppDummy.getContext(), NetworkThreadCreator.class));
                }
                s.close();
            }
        }
        catch (IOException | IllegalAccessException | InstantiationException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void interrupt()
    {
        try
        {
            serverSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        super.interrupt();
    }
}
