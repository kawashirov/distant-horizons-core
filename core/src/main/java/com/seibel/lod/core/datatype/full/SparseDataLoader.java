package com.seibel.lod.core.datatype.full;

import com.seibel.lod.core.datatype.DataSourceLoader;
import com.seibel.lod.core.datatype.LodDataSource;
import com.seibel.lod.core.level.ILevel;
import com.seibel.lod.core.io.datafile.DataMetaFile;

import java.io.IOException;
import java.io.InputStream;

public class SparseDataLoader extends DataSourceLoader {
    public SparseDataLoader() {
        super(SparseDataSource.class, SparseDataSource.TYPE_ID, new byte[]{SparseDataSource.LATEST_VERSION});
    }

    @Override
    public LodDataSource loadData(DataMetaFile dataFile, InputStream data, ILevel level) throws IOException {
        return SparseDataSource.loadData(dataFile, data, level);
    }
}
