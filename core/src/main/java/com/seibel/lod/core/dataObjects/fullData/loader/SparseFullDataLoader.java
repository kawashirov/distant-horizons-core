package com.seibel.lod.core.dataObjects.fullData.loader;

import com.seibel.lod.core.dataObjects.fullData.sources.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.SparseFullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;

import java.io.BufferedInputStream;
import java.io.IOException;

public class SparseFullDataLoader extends AbstractFullDataSourceLoader
{
	public SparseFullDataLoader()
	{
		super(SparseFullDataSource.class, SparseFullDataSource.TYPE_ID, new byte[] { SparseFullDataSource.LATEST_VERSION });
	}

    @Override
	public IFullDataSource loadData(FullDataMetaFile dataFile, BufferedInputStream bufferedInputStream, IDhLevel level) throws IOException, InterruptedException
	{
		return SparseFullDataSource.loadData(dataFile, bufferedInputStream, level);
	}
}
