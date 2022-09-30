package com.seibel.lod.core.datatype.full;

import com.seibel.lod.core.datatype.AbstractDataSourceLoader;
import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.file.datafile.DataMetaFile;

import java.io.IOException;
import java.io.InputStream;

public class FullDataLoader extends AbstractDataSourceLoader
{
    public FullDataLoader() {
        super(FullDataSource.class, FullDataSource.TYPE_ID, new byte[]{FullDataSource.LATEST_VERSION});
    }

    @Override
    public ILodDataSource loadData(DataMetaFile dataFile, InputStream data, IDhLevel level) throws IOException {
        //TODO: Add decompressor here
        return FullDataSource.loadData(dataFile, data, level);
    }
}
