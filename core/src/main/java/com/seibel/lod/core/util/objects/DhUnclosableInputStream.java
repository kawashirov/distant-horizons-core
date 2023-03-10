package com.seibel.lod.core.util.objects;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DhUnclosableInputStream extends FilterInputStream { // TODO replace with BufferedInputStream and this should be usable again, otherwise this significantly slows down the file handling
    public DhUnclosableInputStream(InputStream it) {
        super(it);
    }

    @Override
    public void close() throws IOException {
        // Do nothing.
    }
}
