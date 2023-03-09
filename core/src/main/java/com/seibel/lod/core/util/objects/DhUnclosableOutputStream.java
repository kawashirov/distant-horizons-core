package com.seibel.lod.core.util.objects;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DhUnclosableOutputStream extends FilterOutputStream {
    public DhUnclosableOutputStream(OutputStream it) {
        super(it);
    }
    @Override
    public void close() throws IOException {}
}
