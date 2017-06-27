package com.adihascal.clipboardsync.handler;

import android.content.ClipData;
import android.content.ClipboardManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public interface IClipHandler
{
    /**
     * Sends the current primary clip to the connected computer
     *
     * @param s    The socket connected to the computer
     * @param clip The clip object containing the data to be sent
     * @throws IOException
     */
    void sendClip(Socket s, ClipData clip) throws IOException;

    /**
     * Receives a clip from the connected computer and puts it on the clipboard (for text)/pasting it per the user'socket request (for files)
     *
     * @param s       The socket connected to the computer
     * @param manager The clipboard manager, for convenience
     * @throws IOException
     */
    void receiveClip(DataInputStream s, ClipboardManager manager) throws IOException;
}
