package com.seibel.lod.core.dataObjects.fullData;

import com.seibel.lod.core.dataObjects.fullData.sources.FullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FullDataLoader extends AbstractFullDataSourceLoader
{
    public FullDataLoader()
	{
        super(FullDataSource.class, FullDataSource.TYPE_ID, new byte[]{FullDataSource.LATEST_VERSION});
    }
	
    @Override
    public IFullDataSource loadData(FullDataMetaFile dataFile, BufferedInputStream bufferedInputStream, IDhLevel level) throws IOException, InterruptedException
	{
        //TODO: Add decompressor here
        return FullDataSource.loadData(dataFile, bufferedInputStream, level);
    }
	
}
