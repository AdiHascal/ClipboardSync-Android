package com.adihascal.clipboardsync.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class SocketHolder
{
    private static Socket socket;

    static DataInputStream getInputStream() throws IOException
    {
        return new DataInputStream(socket.getInputStream());
    }

    static DataOutputStream getOutputStream() throws IOException
    {
        return new DataOutputStream(socket.getOutputStream());
    }

    static Socket getSocket()
    {
        return socket;
    }

    static void setSocket(Socket socket)
    {
        SocketHolder.socket = socket;
    }

    static void terminate() throws IOException
    {
        if(socket != null)
        {
            SyncClient.init = false;
            socket.close();
        }
    }
}
