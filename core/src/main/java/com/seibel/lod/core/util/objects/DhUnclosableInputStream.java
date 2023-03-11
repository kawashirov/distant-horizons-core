package com.seibel.lod.core.util.objects;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DhUnclosableInputStream extends BufferedInputStream
{
    public DhUnclosableInputStream(InputStream it) { super(it); }
	
    @Override
    public void close() throws IOException { /* Do nothing. */ }
	
}
