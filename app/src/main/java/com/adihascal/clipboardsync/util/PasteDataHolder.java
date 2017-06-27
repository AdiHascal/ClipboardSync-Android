package com.adihascal.clipboardsync.util;

public class PasteDataHolder
{
    private volatile static byte[] bytes;

    public static byte[] getBytes()
    {
        return bytes;
    }

    public static void setBytes(byte[] b)
    {
        bytes = b;
    }
}
