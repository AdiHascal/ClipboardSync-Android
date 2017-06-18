package com.adihascal.clipboardsync.reference;

import com.adihascal.clipboardsync.R;

import butterknife.BindString;

public final class Reference
{
    public static final String ORIGIN = "com.adihascal.clipboardsync.extra.ORIGIN";
    @BindString(R.string.device_name_default)
    public String defaultDeviceName = "N/A";
    @BindString(R.string.device_name)
    public String deviceName = defaultDeviceName;
    public String currentDeviceAddress = "";
}
