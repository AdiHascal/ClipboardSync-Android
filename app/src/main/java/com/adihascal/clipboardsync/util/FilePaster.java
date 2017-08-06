package com.adihascal.clipboardsync.util;

import com.adihascal.clipboardsync.reference.Reference;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FilePaster implements Runnable
{
    private final DataInputStream data;
    private final String folder;

    public FilePaster(String dir) throws FileNotFoundException
    {
        this.data = new DataInputStream(new FileInputStream(Reference.cacheFile));
        this.folder = dir;
    }

    private void receiveFile(DataInputStream in, String parent) throws IOException
    {
        File f;
        String path = folder;
        String thing = in.readUTF();
		if(thing.equals("file"))
		{
			if(parent != null)
			{
				path = parent;
            }
            path += "/" + in.readUTF();
            f = new File(path);
            f.createNewFile();

            FileOutputStream out = new FileOutputStream(f);
			long length = in.readLong();
			byte[] buffer = new byte[65536];
			long bytesRead;
			long totalBytesRead = 0;
			while(totalBytesRead < length && (bytesRead = in.read(buffer, 0, (int) Math.min(length - totalBytesRead, 65536))) != -1)
			{
				totalBytesRead += bytesRead;
				out.write(buffer, 0, (int) bytesRead);
				out.flush();
			}
			out.close();
		}
		else if(thing.equals("dir"))
		{
			if(parent != null)
			{
				path = parent;
            }
            path += "/" + in.readUTF();
            f = new File(path);
            f.mkdir();
            int nFiles = in.readInt();
			for(int i = 0; i < nFiles; i++)
			{
				receiveFile(in, f.getPath());
            }
        }
    }

    @Override
    public void run()
    {
        try
        {
            int nFiles = data.readInt();
			for(int i = 0; i < nFiles; i++)
			{
				receiveFile(data, null);
            }
            Reference.cacheFile.delete();
        }
		catch(IOException e)
		{
			e.printStackTrace();
        }
	}
	
	public void exec()
	{
		new Thread(this).start();
	}
}
