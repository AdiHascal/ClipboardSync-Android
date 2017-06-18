package com.adihascal.clipboardsync.handler;

import android.content.ClipData;
import android.content.ClipboardManager;

import java.io.IOException;
import java.net.Socket;

public interface IClipHandler
{
    void sendClip(Socket s, ClipData clip) throws IOException;

    void receiveClip(Socket s, ClipboardManager manager) throws IOException;
}
