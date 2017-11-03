package com.adihascal.clipboardsync.handler;

import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.adihascal.clipboardsync.tasks.ITask;

public class TaskHandler
{
	public static final TaskHandler INSTANCE = new TaskHandler();
	private Thread current;
	
	void setAndRun(ITask task)
	{
		if(current == null)
		{
			set(task);
			run();
		}
	}
	
	private void set(final ITask task)
	{
		if(current == null)
		{
			current = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					task.execute();
				}
			});
		}
	}
	
	private void run()
	{
		current.start();
	}
	
	public Thread get()
	{
		return current;
	}
	
	public void pop()
	{
		current = null;
		NetworkThreadCreator.isBusy = false;
	}
	
	public void pause() throws InterruptedException
	{
		synchronized(current)
		{
			current.wait();
		}
	}
	
	public void resume()
	{
		synchronized(current)
		{
			current.notify();
		}
	}
}
