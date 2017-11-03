package com.adihascal.clipboardsync.handler;

import android.content.ClipData;
import android.content.ClipboardManager;

import java.io.IOException;

public interface IClipHandler
{
	/**
	 * Sends the current primary clip to the connected computer
	 *
	 * @param clip The clip object containing the data to be sent
	 * @throws IOException if something explodes
	 */
	void sendClip(ClipData clip) throws IOException;
	
	/**
	 * Receives a clip from the connected computer and puts it on the clipboard (for text)/pasting it per the user'socket request (for files)
	 *
	 * @param manager The clipboard manager, for convenience
	 */
	void receiveClip(ClipboardManager manager) throws IOException;
}
