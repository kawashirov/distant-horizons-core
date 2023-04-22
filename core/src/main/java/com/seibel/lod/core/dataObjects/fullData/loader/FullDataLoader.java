package com.seibel.lod.core.dataObjects.fullData.loader;

import com.seibel.lod.core.dataObjects.fullData.sources.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.CompleteFullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;

import java.io.BufferedInputStream;
import java.io.IOException;

public class FullDataLoader extends AbstractFullDataSourceLoader
{
    public FullDataLoader()
	{
        super(CompleteFullDataSource.class, CompleteFullDataSource.TYPE_ID, new byte[]{ CompleteFullDataSource.LATEST_VERSION});
    }
	
    @Override
    public IFullDataSource loadData(FullDataMetaFile dataFile, BufferedInputStream bufferedInputStream, IDhLevel level) throws IOException, InterruptedException
	{
        //TODO: Add decompressor here
        return CompleteFullDataSource.loadData(dataFile, bufferedInputStream, level);
    }
	
}
