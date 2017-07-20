package com.adihascal.clipboardsync.handler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.Pair;

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

        //I'm looking at you, Discord 9gag and Twitter
        if(intent.getType().equals("text/plain"))
        {
            new TextHandler().sendClip(s, clip);
            return;
        }

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
            try
            {
                String path = Utilities.getPath(AppDummy.getContext(), uri);
                File f = new File(path);
                sendFile(out, f);
            }
            catch(Exception e)
            {
                doThe(uri, getFileNameAndSize(uri), out);
            }
        }
    }

    private Pair<String, Long> getFileNameAndSize(Uri u)
    {
        Cursor cursor = AppDummy.getContext().getContentResolver().query(u, null, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        cursor.moveToFirst();
        Pair<String, Long> ret = Pair.create(cursor.getString(nameIndex), cursor.getLong(sizeIndex));
        cursor.close();
        return ret;
    }

    /**
     * "Do the thingy with the stuff"
     * I'm looking at you, apps with a locked content provider
     *
     * @param thingy the thingy we're doing
     * @param stuff  the stuff with which we are doing the thingy
     * @param out    a streamy boi
     * @throws IOException o shit waddup
     */
    private void doThe(Uri thingy, Pair<String, Long> stuff, DataOutputStream out) throws IOException
    {
        FileInputStream fileIn = AppDummy.getContext().getContentResolver().openAssetFileDescriptor(thingy, "r").createInputStream();
        out.writeUTF("file");
        out.writeUTF(stuff.first);
        out.writeLong(stuff.second);
        Utilities.copyNoClose(fileIn, out);
        fileIn.close();
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
        builder.setSmallIcon(R.drawable.ic_content_paste_black_24dp)
                .setContentTitle("Ready to paste")
                .setContentText("A clip containing file data was received by ClipboardSync and is available for pasting. tap to choose destination")
                .setContentIntent(PendingIntent.getActivity(AppDummy.getContext(), 1, pasteIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setLocalOnly(true)
                .setAutoCancel(true);
        ((NotificationManager) AppDummy.getContext().getSystemService(NOTIFICATION_SERVICE)).notify(1, builder.build());
    }
}
