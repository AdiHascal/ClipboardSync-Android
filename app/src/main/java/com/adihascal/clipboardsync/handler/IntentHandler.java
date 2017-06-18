package com.adihascal.clipboardsync.handler;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class IntentHandler implements IClipHandler
{
    //shamelessly copied from sun.misc.IOUtils
    private static byte[] readFully(InputStream stream, int length, boolean readAll) throws IOException
    {
        byte[] var3 = new byte[0];
        if (length == -1)
        {
            length = Integer.MAX_VALUE;
        }

        int var6;
        for (int var4 = 0; var4 < length; var4 += var6)
        {
            int var5;
            if (var4 >= var3.length)
            {
                var5 = Math.min(length - var4, var3.length + 1024);
                if (var3.length < var4 + var5)
                {
                    var3 = Arrays.copyOf(var3, var4 + var5);
                }
            }
            else
            {
                var5 = var3.length - var4;
            }

            var6 = stream.read(var3, var4, var5);
            if (var6 < 0)
            {
                if (readAll && length != Integer.MAX_VALUE)
                {
                    throw new EOFException("Detect premature EOF");
                }

                if (var3.length != var4)
                {
                    var3 = Arrays.copyOf(var3, var4);
                }
                break;
            }
        }
        return var3;
    }

    @Override
    public void sendClip(Socket s, ClipData clip) throws IOException
    {
        Intent intent = clip.getItemAt(0).getIntent();

        if (intent.getAction().equals(Intent.ACTION_SEND))
        {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            File f = new File(uri.getPath());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            FileInputStream in = new FileInputStream(f);
            out.writeUTF("application/x-java-serialized-object");
            out.write(1);
            out.writeUTF(f.getName());
            out.write((int) f.length());
            byte[] data = readFully(in, -1, true);
            out.write(data);
            in.close();
        }
        else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE))
        {
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            out.writeUTF("application/x-java-serialized-object");
            out.write(uris.size());

            for (Uri uri : uris)
            {
                File f = new File(uri.getPath());
                FileInputStream in = new FileInputStream(f);
                out.writeUTF(f.getName());
                out.write((int) f.length());
                byte[] data = readFully(in, -1, true);
                out.write(data);
                in.close();
            }
        }
    }

    @Override
    public void receiveClip(Socket s, ClipboardManager manager) throws IOException
    {
        //TODO write a file explorer activity and start it from here
        //DataInputStream in = new DataInputStream(s.getInputStream());
    }
}
