package com.adihascal.clipboardsync.handler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.reference.Reference;
import com.adihascal.clipboardsync.ui.AppDummy;
import com.adihascal.clipboardsync.ui.PasteActivity;
import com.adihascal.clipboardsync.util.Utilities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;

public class IntentHandler implements IClipHandler
{
    @Override
    public void sendClip(Socket s, ClipData clip) throws IOException
    {
        Intent intent = clip.getItemAt(0).getIntent();
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
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
            String path = Utilities.getPath(AppDummy.getContext(), uri);
            File f = new File(path);
            sendFile(out, f);
        }
        System.out.println("flushing...");
        s.close();
    }

    private void sendFile(DataOutputStream out, File f) throws IOException
    {
        if (!f.isDirectory())
        {
            FileInputStream in = new FileInputStream(f);
            out.writeUTF("file");
            out.writeUTF(f.getName());
            out.writeLong(f.length());
            Utilities.copyNoClose(in, out);
            in.close();
        }
        else
        {
            out.writeUTF("dir");
            File[] subs = f.listFiles();
            assert subs != null;
            out.writeUTF(f.getName());
            out.writeInt(subs.length);
            for (File sub : subs)
            {
                sendFile(out, sub);
            }
        }
    }

    @Override
    public void receiveClip(DataInputStream s, ClipboardManager manager) throws IOException
    {
        Intent pasteIntent = new Intent();
        pasteIntent.setClass(AppDummy.getContext(), PasteActivity.class);
        pasteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Utilities.copyStreamToFileWithProgressBar(s, Reference.cacheFile);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(AppDummy.getContext());
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Ready to paste")
                .setContentText("A clip containing file data was received by ClipboardSync and is available for pasting. tap to choose destination")
                .setContentIntent(PendingIntent.getActivity(AppDummy.getContext(), 1, pasteIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setLocalOnly(true)
                .setAutoCancel(true);
        ((NotificationManager) AppDummy.getContext().getSystemService(NOTIFICATION_SERVICE)).notify(1, builder.build());
    }
}
