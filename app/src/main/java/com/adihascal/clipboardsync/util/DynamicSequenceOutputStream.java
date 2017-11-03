package com.adihascal.clipboardsync.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DynamicSequenceOutputStream extends OutputStream
{
	private final IStreamSupplier<OutputStream> supplier;
	private int streamIndex = 0;
	private int count, pos;
	private OutputStream out;
	
	public DynamicSequenceOutputStream(IStreamSupplier<OutputStream> supp)
	{
		this.supplier = supp;
	}
	
	@Override
	public void write(int b) throws IOException
	{
		if(pos < count)
		{
			out.write(b);
			pos++;
		}
		else
		{
			next(false);
			out.write(b);
			pos++;
		}
	}
	
	private void next(boolean close)
	{
		try
		{
			if(out != null)
			{
				out.close();
				supplier.afterClose(streamIndex - 1);
			}
			
			if(!close)
			{
				OutputStream next = supplier.next(streamIndex);
				pos = 0;
				count = (int) supplier.length(streamIndex++);
				out = new BufferedOutputStream(next, 61440);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void close() throws IOException
	{
		if(out != null)
		{
			next(true);
		}
	}
}
