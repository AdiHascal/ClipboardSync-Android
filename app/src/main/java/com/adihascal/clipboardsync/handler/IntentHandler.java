package com.adihascal.clipboardsync.handler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.ui.AppDummy;
import com.adihascal.clipboardsync.ui.PasteActivity;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

import static android.content.Context.NOTIFICATION_SERVICE;

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

    private String parseUri(Uri u)
    {
        if (u.getScheme().equals("file"))
        {
            return u.getPath();
        }
        else if (u.getScheme().equals("content"))
        {
            return u.getPath().substring(5);
        }
        else
        {
            throw new UnsupportedOperationException(u.getScheme());
        }
    }

    @Override
    public void sendClip(Socket s, ClipData clip) throws IOException
    {
        Intent intent = clip.getItemAt(0).getIntent();

        if (intent.getAction().equals(Intent.ACTION_SEND))
        {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            File f = new File(parseUri(uri));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream(), 104857600));

            out.writeUTF("application/x-java-serialized-object");
            out.write(1);
            FileInputStream in = new FileInputStream(f);
            out.writeUTF(f.getName());
            out.writeLong(f.length());
            byte[] data = readFully(in, -1, true);
            out.write(data);
            System.out.println("flushing...");
            out.flush();
            in.close();
        }
        else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE))
        {
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out.writeUTF("application/x-java-serialized-object");
            out.write(uris.size());

            for (Uri uri : uris)
            {
                File f = new File(uri.getPath());
                FileInputStream in = new FileInputStream(f);
                out.writeUTF(f.getName());
                out.writeLong(f.length());
                byte[] data = readFully(in, -1, true);
                out.write(data);
                in.close();
            }
            out.flush();
        }
        s.close();
    }

    @Override
    public void receiveClip(DataInputStream s, ClipboardManager manager) throws IOException
    {
        Intent pasteIntent = new Intent();
        pasteIntent.setClass(AppDummy.getContext(), PasteActivity.class);
        pasteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pasteIntent.putExtra("data", readFully(s, -1, true));
        AppDummy.getContext().startActivity(pasteIntent);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(AppDummy.getContext());
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Ready to paste")
                .setContentText("A clip containing file data was received by ClipboardSync and is available for pasting. tap to choose destination")
                .setContentIntent(PendingIntent.getActivity(AppDummy.getContext(), 0, pasteIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setLocalOnly(true);
        ((NotificationManager) AppDummy.getContext().getSystemService(NOTIFICATION_SERVICE)).notify(0, builder.build());
    }
}
