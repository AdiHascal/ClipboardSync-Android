package com.adihascal.clipboardsync.ui;

import android.app.Application;
import android.content.Context;

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
    }
}
