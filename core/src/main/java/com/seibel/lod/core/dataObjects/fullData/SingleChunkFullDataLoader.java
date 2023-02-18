package com.seibel.lod.core.dataObjects.fullData;

import com.seibel.lod.core.dataObjects.fullData.sources.SingleChunkFullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;

import java.io.IOException;
import java.io.InputStream;

public class SingleChunkFullDataLoader extends AbstractFullDataSourceLoader
{
    public SingleChunkFullDataLoader() {
        super(SingleChunkFullDataSource.class, SingleChunkFullDataSource.TYPE_ID, new byte[]{ SingleChunkFullDataSource.LATEST_VERSION});
    }

    @Override
    public IFullDataSource loadData(FullDataMetaFile dataFile, InputStream data, IDhLevel level) throws IOException {
        return SingleChunkFullDataSource.loadData(dataFile, data, level);
    }
}
