package com.adihascal.clipboardsync.handler;

import com.adihascal.clipboardsync.tasks.ITask;

public class TaskHandler
{
	private static final TaskHandler INSTANCE = new TaskHandler();
	private static ITask current;
	
	static void setAndRun(ITask task) throws Exception
	{
		set(task);
		run();
	}
	
	private static void set(ITask task) throws Exception
	{
		if(current == null)
		{
			current = task;
		}
		else
		{
			throw new Exception("cannot set task while another one is already running");
		}
	}
	
	private static void run()
	{
		current.execute();
	}
	
	public static ITask get()
	{
		return current;
	}
	
	public static void pop()
	{
		current = null;
	}
	
	public static void pause() throws InterruptedException
	{
		synchronized(current)
		{
			current.wait();
		}
	}
	
	public static void resume()
	{
		synchronized(current)
		{
			INSTANCE.notify();
		}
	}
}
