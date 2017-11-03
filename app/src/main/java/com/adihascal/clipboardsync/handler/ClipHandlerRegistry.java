package com.adihascal.clipboardsync.handler;

import android.content.ClipDescription;

import java.util.HashMap;

public class ClipHandlerRegistry
{
	private static final HashMap<String, IClipHandler> handlers = new HashMap<>();
	
	static
	{
		handlers.put(ClipDescription.MIMETYPE_TEXT_PLAIN, new TextHandler());
		handlers.put(ClipDescription.MIMETYPE_TEXT_HTML, new TextHandler());
		handlers.put(ClipDescription.MIMETYPE_TEXT_INTENT, new IntentHandler());
		handlers.put("application/x-java-serialized-object", new IntentHandler());
	}
	
	public static IClipHandler getHandlerFor(String type)
	{
		return handlers.get(type);
	}
	
	public static boolean isMimeTypeSupported(String type)
	{
		return handlers.containsKey(type);
	}
}