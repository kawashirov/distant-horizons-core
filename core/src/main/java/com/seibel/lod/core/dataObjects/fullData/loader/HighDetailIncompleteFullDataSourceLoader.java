package com.seibel.lod.core.dataObjects.fullData.loader;

import com.seibel.lod.core.dataObjects.fullData.sources.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.HighDetailIncompleteFullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;

import java.io.BufferedInputStream;
import java.io.IOException;

public class HighDetailIncompleteFullDataSourceLoader extends AbstractFullDataSourceLoader
{
	public HighDetailIncompleteFullDataSourceLoader()
	{
		super(HighDetailIncompleteFullDataSource.class, HighDetailIncompleteFullDataSource.TYPE_ID, new byte[] { HighDetailIncompleteFullDataSource.DATA_FORMAT_VERSION });
	}

    @Override
	public IFullDataSource loadData(FullDataMetaFile dataFile, BufferedInputStream bufferedInputStream, IDhLevel level) throws IOException, InterruptedException
	{
		//TODO: Add decompressor here
		
		HighDetailIncompleteFullDataSource dataSource = HighDetailIncompleteFullDataSource.createEmpty(dataFile.pos);
		dataSource.populateFromStream(dataFile, bufferedInputStream, level);
		return dataSource;
	}
}
