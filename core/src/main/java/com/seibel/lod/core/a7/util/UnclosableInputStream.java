package com.seibel.lod.core.a7.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class UnclosableInputStream extends FilterInputStream {
    public UnclosableInputStream(InputStream it) {
        super(it);
    }

    @Override
    public void close() throws IOException {
        // Do nothing.
    }
}
