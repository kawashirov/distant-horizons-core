package com.seibel.lod.core.dataObjects.fullData.loader;

import com.seibel.lod.core.dataObjects.fullData.sources.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.SpottyFullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;

import java.io.BufferedInputStream;
import java.io.IOException;

public class SingleChunkFullDataLoader extends AbstractFullDataSourceLoader
{
    public SingleChunkFullDataLoader() {
        super(SpottyFullDataSource.class, SpottyFullDataSource.TYPE_ID, new byte[]{ SpottyFullDataSource.LATEST_VERSION});
    }

    @Override
    public IFullDataSource loadData(FullDataMetaFile dataFile, BufferedInputStream bufferedInputStream, IDhLevel level) throws IOException, InterruptedException
	{
        return SpottyFullDataSource.loadData(dataFile, bufferedInputStream, level);
    }
}
