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

    @Override
    public void run()
    {
        try
        {
            int nFiles = data.readInt();
            long len;

            for (int i = 0; i < nFiles; i++)
            {
                File f = new File(folder, data.readUTF());
                if (f.createNewFile())
                {
                    FileOutputStream out = new FileOutputStream(f);
                    len = data.readLong();
                    Utilities.copyStream(data, out, (int) len);
                    out.close();
                }
            }
            Reference.cacheFile.delete();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
