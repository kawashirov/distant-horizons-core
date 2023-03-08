package com.seibel.lod.core.dataObjects.fullData;

import com.seibel.lod.core.dataObjects.fullData.sources.SparseFullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;

import java.io.IOException;
import java.io.InputStream;

public class SparseFullDataLoader extends AbstractFullDataSourceLoader
{
	public SparseFullDataLoader()
	{
		super(SparseFullDataSource.class, SparseFullDataSource.TYPE_ID, new byte[] { SparseFullDataSource.LATEST_VERSION });
	}

    @Override
	public IFullDataSource loadData(FullDataMetaFile dataFile, InputStream data, IDhLevel level) throws IOException, InterruptedException
	{
		return SparseFullDataSource.loadData(dataFile, data, level);
	}
}
