package com.adihascal.clipboardsync.ui;

import android.app.Application;
import android.content.Context;

import com.adihascal.clipboardsync.reference.Reference;

import java.io.File;

public class AppDummy extends Application
{
    private static Context context;

    public static Context getContext()
    {
        return context;
    }

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        context = base;
        Reference.cacheFile = new File(AppDummy.getContext().getCacheDir(), "temp.bin");
    }
}
