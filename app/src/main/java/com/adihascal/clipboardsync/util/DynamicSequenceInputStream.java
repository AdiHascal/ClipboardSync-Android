package com.adihascal.clipboardsync.util;

import android.support.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DynamicSequenceInputStream extends InputStream implements DataInput
{
	private final IStreamSupplier supplier;
	private InputStream in;
	private int pos, count;
	private int streamIndex = -1;
	
	public DynamicSequenceInputStream(IStreamSupplier supp)
	{
		this.supplier = supp;
		next(false);
	}
	
	private void next(boolean close)
	{
		try
		{
			if(in != null)
			{
				in.close();
				supplier.afterClose(streamIndex);
			}
			
			if(!close)
			{
				while(true)
				{
					if(supplier.canProvide(streamIndex + 1))
					{
						FileInputStream fIn = (FileInputStream) supplier.next(streamIndex++);
						pos = 0;
						count = fIn.available();
						in = new BufferedInputStream(fIn, 15360);
						break;
					}
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void readFully(@NonNull byte[] b) throws IOException
	{
		readFully(b, 0, b.length);
	}
	
	public int read() throws IOException
	{
		if(pos < count)
		{
			pos++;
			return in.read();
		}
		else
		{
			next(false);
			pos++;
			return in.read();
		}
	}
	
	public int read(@NonNull byte[] b, int off, int len) throws IOException
	{
		if(off < 0 || len < 0 || len > b.length - off)
		{
			throw new IndexOutOfBoundsException();
		}
		else if(len == 0)
		{
			return 0;
		}
		
		int c = read();
		if(c == -1)
		{
			return -1;
		}
		b[off] = (byte) c;
		
		int i = 1;
		try
		{
			for(; i < len; i++)
			{
				c = read();
				if(c == -1)
				{
					break;
				}
				b[off + i] = (byte) c;
			}
		}
		catch(IOException ignored)
		{
		}
		return i;
	}
	
	@Override
	public int available() throws IOException
	{
		return count - pos;
	}
	
	@Override
	public void close() throws IOException
	{
		if(in != null)
		{
			next(true);
		}
	}
	
	@Override
	public void readFully(@NonNull byte[] b, int off, int len) throws IOException
	{
		int n = 0;
		do
		{
			int count = this.read(b, off + n, len - n);
			if(count < 0)
			{
				throw new EOFException();
			}
			n += count;
		} while(n < len);
	}
	
	@Override
	public int skipBytes(int n) throws IOException
	{
		int total = 0;
		int cur;
		
		while((total < n) && ((cur = (int) in.skip(n - total)) > 0))
		{
			total += cur;
		}
		
		pos += total;
		return total;
	}
	
	@Override
	public boolean readBoolean() throws IOException
	{
		int ch = this.read();
		if(ch < 0)
		{
			throw new EOFException();
		}
		return (ch != 0);
	}
	
	@Override
	public byte readByte() throws IOException
	{
		int ch = this.read();
		if(ch < 0)
		{
			throw new EOFException();
		}
		return (byte) (ch);
	}
	
	@Override
	public int readUnsignedByte() throws IOException
	{
		int ch = this.read();
		if(ch < 0)
		{
			throw new EOFException();
		}
		return ch;
	}
	
	public short readShort() throws IOException
	{
		int ch1 = this.read();
		int ch2 = this.read();
		if((ch1 | ch2) < 0)
		{
			throw new EOFException();
		}
		return (short) ((ch1 << 8) + (ch2));
	}
	
	public int readUnsignedShort() throws IOException
	{
		int ch1 = this.read();
		int ch2 = this.read();
		if((ch1 | ch2) < 0)
		{
			throw new EOFException();
		}
		return (ch1 << 8) + (ch2);
	}
	
	public char readChar() throws IOException
	{
		int ch1 = this.read();
		int ch2 = this.read();
		if((ch1 | ch2) < 0)
		{
			throw new EOFException();
		}
		return (char) ((ch1 << 8) + (ch2));
	}
	
	public int readInt() throws IOException
	{
		int ch1 = this.read();
		int ch2 = this.read();
		int ch3 = this.read();
		int ch4 = this.read();
		if((ch1 | ch2 | ch3 | ch4) < 0)
		{
			throw new EOFException();
		}
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
	}
	
	public long readLong() throws IOException
	{
		return ((long) (readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
	}
	
	public float readFloat() throws IOException
	{
		return Float.intBitsToFloat(readInt());
	}
	
	public double readDouble() throws IOException
	{
		return Double.longBitsToDouble(readLong());
	}
	
	public String readLine() throws IOException
	{
		return null;
	}
	
	@NonNull
	@Override
	public String readUTF() throws IOException
	{
		return DataInputStream.readUTF(this);
	}
}
