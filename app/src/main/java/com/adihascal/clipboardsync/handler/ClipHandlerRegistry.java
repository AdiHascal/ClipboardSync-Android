package com.adihascal.clipboardsync.handler;

import android.content.ClipDescription;
import android.os.Build;

import java.util.HashMap;

public class ClipHandlerRegistry
{
    private static final HashMap<String, IClipHandler> handlers = new HashMap<>();
    private static TextHandler textHandler = new TextHandler();
    private static IntentHandler intentHandler = new IntentHandler();

    static
    {
        handlers.put(ClipDescription.MIMETYPE_TEXT_PLAIN, textHandler);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            handlers.put(ClipDescription.MIMETYPE_TEXT_HTML, textHandler);
        }
        handlers.put(ClipDescription.MIMETYPE_TEXT_INTENT, intentHandler);
        handlers.put("application/x-java-serialized-object", intentHandler);
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