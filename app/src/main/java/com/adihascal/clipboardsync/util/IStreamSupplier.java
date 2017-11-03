package com.adihascal.clipboardsync.util;

import java.io.Closeable;

public interface IStreamSupplier<T extends Closeable>
{
	T next(int index);
	
	boolean canProvide(int index);
	
	void afterClose(int index);
	
	long length(int index);
}
