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
        if (thing.equals("file"))
        {
            if (parent != null)
            {
                path = parent;
            }
            path += "/" + in.readUTF();
            f = new File(path);
            f.createNewFile();
            FileOutputStream out = new FileOutputStream(f);
            Utilities.copyStream(in, out, (int) in.readLong());
        }
        else
        {
            if (parent != null)
            {
                path = parent;
            }
            path += "/" + in.readUTF();
            f = new File(path);
            f.mkdir();
            int nFiles = in.readInt();
            for (int i = 0; i < nFiles; i++)
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
            for (int i = 0; i < nFiles; i++)
            {
                receiveFile(data, null);
            }
            Reference.cacheFile.delete();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
