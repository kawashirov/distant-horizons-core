package com.seibel.lod.core.util.objects;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DhUnclosableOutputStream extends FilterOutputStream { // TODO replace with BufferedInputStream and this should be usable again, otherwise this significantly slows down the file handling
    public DhUnclosableOutputStream(OutputStream it) {
        super(it);
    }
    @Override
    public void close() throws IOException {}
}
