package com.adihascal.clipboardsync.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketHolder
{
	private volatile static Socket socket;
	private volatile static DataInputStream socketIn;
	private volatile static DataOutputStream socketOut;
	
	public static DataInputStream in()
	{
		return socketIn;
	}
	
	public static DataOutputStream out()
	{
		return socketOut;
	}
	
	public static Socket getSocket()
	{
		return socket;
	}
	
	public static void setSocket(Socket sock)
	{
		try
		{
			if(socket != null && !socket.isClosed())
			{
				socket.close();
			}
			SocketHolder.socket = sock;
			socketIn = new DataInputStream(sock.getInputStream());
			socketOut = new DataOutputStream(sock.getOutputStream());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	static void terminate()
	{
		try
		{
			if(socket != null)
			{
				SyncClient.init = false;
				socket.close();
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static boolean valid()
	{
		return socket != null && !socket.isClosed();
	}
}
