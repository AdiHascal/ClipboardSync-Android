package com.adihascal.clipboardsync.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class SocketHolder
{
    private static Socket socket;
    private static DataInputStream socketIn;
    private static DataOutputStream socketOut;

    static void setSocket(Socket socket) throws IOException
    {
        SocketHolder.socket = socket;
        socketIn = new DataInputStream(socket.getInputStream());
        socketOut = new DataOutputStream(socket.getOutputStream());
    }

    static DataInputStream getInputStream() throws IOException
    {
        return socketIn;
    }

    static DataOutputStream getOutputStream() throws IOException
    {
        return socketOut;
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
