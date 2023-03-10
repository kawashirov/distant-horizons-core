package com.seibel.lod.core.dataObjects.fullData;

import com.seibel.lod.core.dataObjects.fullData.sources.SingleChunkFullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SingleChunkFullDataLoader extends AbstractFullDataSourceLoader
{
    public SingleChunkFullDataLoader() {
        super(SingleChunkFullDataSource.class, SingleChunkFullDataSource.TYPE_ID, new byte[]{ SingleChunkFullDataSource.LATEST_VERSION});
    }

    @Override
    public IFullDataSource loadData(FullDataMetaFile dataFile, BufferedInputStream bufferedInputStream, IDhLevel level) throws IOException, InterruptedException
	{
        return SingleChunkFullDataSource.loadData(dataFile, bufferedInputStream, level);
    }
}
