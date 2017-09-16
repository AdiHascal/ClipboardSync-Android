package com.adihascal.clipboardsync.util;

import java.io.Closeable;

public class ArrayStreamSupplier<T extends Closeable> implements IStreamSupplier<T>
{
	private final T[] streams;
	
	public ArrayStreamSupplier(T[] e)
	{
		this.streams = e;
	}
	
	@Override
	public T next(int prevIndex)
	{
		return streams[++prevIndex];
	}
	
	@Override
	public boolean canProvide(int index)
	{
		return index < streams.length;
	}
	
	@Override
	public void afterClose(int index)
	{
		
	}
}
