package com.seibel.lod.core.dataObjects.fullData.loader;

import com.seibel.lod.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.HighDetailIncompleteFullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.util.objects.dataStreams.DhDataInputStream;

import java.io.IOException;

public class HighDetailIncompleteFullDataSourceLoader extends AbstractFullDataSourceLoader
{
	public HighDetailIncompleteFullDataSourceLoader()
	{
		super(HighDetailIncompleteFullDataSource.class, HighDetailIncompleteFullDataSource.TYPE_ID, new byte[] { HighDetailIncompleteFullDataSource.DATA_FORMAT_VERSION });
	}

    @Override
	public IFullDataSource loadData(FullDataMetaFile dataFile, DhDataInputStream inputStream, IDhLevel level) throws IOException, InterruptedException
	{
		HighDetailIncompleteFullDataSource dataSource = HighDetailIncompleteFullDataSource.createEmpty(dataFile.pos);
		dataSource.populateFromStream(dataFile, inputStream, level);
		return dataSource;
	}
}
