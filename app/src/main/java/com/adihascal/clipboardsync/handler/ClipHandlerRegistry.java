package com.adihascal.clipboardsync.handler;

import android.content.ClipDescription;
import android.os.Build;

import java.util.HashMap;

public class ClipHandlerRegistry
{
    private static final HashMap<String, IClipHandler> handlers = new HashMap<String, IClipHandler>();

    static
    {
        handlers.put(ClipDescription.MIMETYPE_TEXT_PLAIN, new TextHandler());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            handlers.put(ClipDescription.MIMETYPE_TEXT_HTML, new TextHandler());
        }
        handlers.put(ClipDescription.MIMETYPE_TEXT_INTENT, new IntentHandler());
        handlers.put("application/x-java-serialized-object", new IntentHandler());
    }

    public static IClipHandler getHandlerFor(String type) throws IllegalAccessException, InstantiationException
    {
        return handlers.get(type);
    }

    public static boolean isMimeTypeSupported(String type)
    {
        return handlers.containsKey(type);
    }
}