package com.adihascal.clipboardsync.network;

import android.content.Context;

class SyncThread extends Thread
{
    String deviceAddress;
    Context appContext;

    SyncThread(String address, Context ctx)
    {
        this.deviceAddress = address;
        this.appContext = ctx;
    }
}
