package com.seibel.lod.core.datatype.full;

import com.seibel.lod.core.datatype.full.sources.FullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;

import java.io.IOException;
import java.io.InputStream;

public class FullDataLoader extends AbstractFullDataSourceLoader
{
    public FullDataLoader()
	{
        super(FullDataSource.class, FullDataSource.TYPE_ID, new byte[]{FullDataSource.LATEST_VERSION});
    }
	
    @Override
    public IFullDataSource loadData(FullDataMetaFile dataFile, InputStream data, IDhLevel level) throws IOException
	{
        //TODO: Add decompressor here
        return FullDataSource.loadData(dataFile, data, level);
    }
	
}
