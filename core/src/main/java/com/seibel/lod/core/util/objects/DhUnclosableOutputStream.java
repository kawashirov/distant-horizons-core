package com.seibel.lod.core.util.objects;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DhUnclosableOutputStream extends BufferedOutputStream
{
    public DhUnclosableOutputStream(OutputStream outputStream) { super(outputStream); }
	
	@Override
	public void close() throws IOException { /* Do nothing. */ }
	
}
