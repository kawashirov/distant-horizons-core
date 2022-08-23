package com.seibel.lod.core.a7.datatype.full;

import com.seibel.lod.core.a7.datatype.DataSourceLoader;
import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.save.io.file.DataMetaFile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SparseDataLoader extends DataSourceLoader {
    public SparseDataLoader() {
        super(SparseDataSource.class, SparseDataSource.TYPE_ID, new byte[]{SparseDataSource.LATEST_VERSION});
    }

    @Override
    public LodDataSource loadData(DataMetaFile dataFile, InputStream data, ILevel level) throws IOException {
        try (
                //TODO: Add decompressor here
                DataInputStream dis = new DataInputStream(data);
        ) {
            return SparseDataSource.loadData(dataFile, dis, level);
        }
    }
}
