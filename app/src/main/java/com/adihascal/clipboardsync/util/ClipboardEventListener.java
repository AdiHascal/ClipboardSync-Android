package com.adihascal.clipboardsync.util;

import android.content.ClipData;
import android.content.ClipboardManager;

import com.adihascal.clipboardsync.network.SyncClient;
import com.adihascal.clipboardsync.reference.Reference;
import com.adihascal.clipboardsync.ui.AppDummy;

import static android.content.Context.CLIPBOARD_SERVICE;
import static com.adihascal.clipboardsync.service.NetworkThreadCreator.isBusy;
import static com.adihascal.clipboardsync.service.NetworkThreadCreator.isConnected;
import static com.adihascal.clipboardsync.service.NetworkThreadCreator.paused;

public class ClipboardEventListener implements ClipboardManager.OnPrimaryClipChangedListener
{
	public static final ClipboardEventListener INSTANCE = new ClipboardEventListener();

    @Override
    public void onPrimaryClipChanged()
    {
		if(!isBusy && !paused && isConnected)
		{
			ClipboardManager manager = (ClipboardManager) AppDummy.getContext().getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = manager.getPrimaryClip();
            if((clip.getDescription().getLabel() == null || !clip.getDescription().getLabel().equals(Reference.ORIGIN)))
            {
                new SyncClient("send", clip).start();
            }
        }
    }
}
