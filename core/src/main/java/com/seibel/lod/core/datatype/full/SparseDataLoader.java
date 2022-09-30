package com.seibel.lod.core.datatype.full;

import com.seibel.lod.core.datatype.AbstractDataSourceLoader;
import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.file.datafile.DataMetaFile;

import java.io.IOException;
import java.io.InputStream;

public class SparseDataLoader extends AbstractDataSourceLoader
{
    public SparseDataLoader() {
        super(SparseDataSource.class, SparseDataSource.TYPE_ID, new byte[]{SparseDataSource.LATEST_VERSION});
    }

    @Override
    public ILodDataSource loadData(DataMetaFile dataFile, InputStream data, IDhLevel level) throws IOException {
        return SparseDataSource.loadData(dataFile, data, level);
    }
}
