package com.seibel.lod.core.util.objects;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DhUnclosableInputStream extends FilterInputStream {
    public DhUnclosableInputStream(InputStream it) {
        super(it);
    }

    @Override
    public void close() throws IOException {
        // Do nothing.
    }
}
