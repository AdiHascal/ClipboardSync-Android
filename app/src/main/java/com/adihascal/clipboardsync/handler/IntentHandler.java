package com.adihascal.clipboardsync.handler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.CursorLoader;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.adihascal.clipboardsync.ui.AppDummy;
import com.adihascal.clipboardsync.ui.PasteActivity;
import com.adihascal.clipboardsync.util.PasteDataHolder;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;

public class IntentHandler implements IClipHandler
{
    public static Socket socket;

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

    private static String getRealPathFromURI(Uri uri)
    {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(AppDummy.getContext(), uri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    @Override
    public void sendClip(Socket s, ClipData clip) throws IOException
    {
        Intent intent = clip.getItemAt(0).getIntent();
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
        List<Uri> uris;

        if (intent.getAction().equals(Intent.ACTION_SEND))
        {
            uris = Collections.singletonList((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
        }
        else
        {
            uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }

        out.writeUTF("application/x-java-serialized-object");
        out.writeInt(uris.size());

        for (Uri uri : uris)
        {
            File f = new File(getRealPathFromURI(uri));
            FileInputStream in = new FileInputStream(f);
            out.writeUTF(f.getName());
            out.writeLong(f.length());
            byte[] data = readFully(in, -1, true);
            out.write(data);
            in.close();
        }
        out.flush();
        System.out.println("flushing...");
        socket.close();
        NetworkThreadCreator.isBusy = false;
    }

    @Override
    public void receiveClip(DataInputStream s, ClipboardManager manager) throws IOException
    {
        Intent pasteIntent = new Intent();
        pasteIntent.setClass(AppDummy.getContext(), PasteActivity.class);
        pasteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PasteDataHolder.setBytes(readFully(s, -1, true));
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
