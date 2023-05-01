package com.seibel.lod.core.dataObjects.fullData.loader;

import com.seibel.lod.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.LowDetailIncompleteFullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;

import java.io.BufferedInputStream;
import java.io.IOException;

public class LowDetailIncompleteFullDataSourceLoader extends AbstractFullDataSourceLoader
{
    public LowDetailIncompleteFullDataSourceLoader() {
        super(LowDetailIncompleteFullDataSource.class, LowDetailIncompleteFullDataSource.TYPE_ID, new byte[]{ LowDetailIncompleteFullDataSource.DATA_FORMAT_VERSION });
    }

    @Override
    public IFullDataSource loadData(FullDataMetaFile dataFile, BufferedInputStream bufferedInputStream, IDhLevel level) throws IOException, InterruptedException
	{
		//TODO: Add decompressor here
		
		LowDetailIncompleteFullDataSource dataSource = LowDetailIncompleteFullDataSource.createEmpty(dataFile.pos);
		dataSource.populateFromStream(dataFile, bufferedInputStream, level);
		return dataSource;
    }
}
