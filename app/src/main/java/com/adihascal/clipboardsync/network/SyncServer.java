package com.adihascal.clipboardsync.network;

import android.content.ClipboardManager;
import android.content.Context;

import com.adihascal.clipboardsync.handler.ClipHandlerRegistry;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SyncServer extends SyncThread
{
    public volatile boolean shouldRun = true;
    private ServerSocket serverSocket;

    public SyncServer(String address, Context ctx)
    {
        super(address, ctx);
    }

    @Override
    public void run()
    {
        while (this.shouldRun)
        {
            try
            {
                this.serverSocket = new ServerSocket(63708);
                Socket s = this.serverSocket.accept();
                DataInputStream is = new DataInputStream(s.getInputStream());
                String type = is.readUTF();
                ClipboardManager manager = (ClipboardManager) this.appContext.getSystemService(Context.CLIPBOARD_SERVICE);
                if (ClipHandlerRegistry.isMimeTypeSupported(type))
                {
                    ClipHandlerRegistry.getHandlerFor(type).receiveClip(s, manager);
                }
                s.close();
            }
            catch (IOException | InstantiationException | IllegalAccessException e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    serverSocket.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void interrupt()
    {
        super.interrupt();
        try
        {
            serverSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
