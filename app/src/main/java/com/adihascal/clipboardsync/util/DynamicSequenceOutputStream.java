package com.adihascal.clipboardsync.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DynamicSequenceOutputStream extends OutputStream
{
	private final IStreamSupplier<OutputStream> supplier;
	private int streamIndex = 0;
	private int count, pos, written;
	private OutputStream out;
	
	public DynamicSequenceOutputStream(IStreamSupplier<OutputStream> supp)
	{
		this.supplier = supp;
		next(false);
	}
	
	@Override
	public void write(int b) throws IOException
	{
		if(pos < count)
		{
			out.write(b);
			pos++;
			written++;
		}
		else
		{
			System.out.println(written + " bytes written to stream " + (streamIndex - 1));
			next(false);
			write(b);
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
				written = 0;
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
			System.out.println(written + " bytes written to stream " + (streamIndex - 1));
			next(true);
		}
	}
}
