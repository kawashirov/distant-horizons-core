package com.seibel.lod.core.a7.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class UnclosableOutputStream extends FilterOutputStream {
    public UnclosableOutputStream(OutputStream it) {
        super(it);
    }
    @Override
    public void close() throws IOException {}
}
