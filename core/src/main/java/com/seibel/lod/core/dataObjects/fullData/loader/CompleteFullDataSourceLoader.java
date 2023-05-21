package com.seibel.lod.core.dataObjects.fullData.loader;

import com.seibel.lod.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.CompleteFullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.util.objects.dataStreams.DhDataInputStream;

import java.io.IOException;

public class CompleteFullDataSourceLoader extends AbstractFullDataSourceLoader
{
    public CompleteFullDataSourceLoader()
	{
        super(CompleteFullDataSource.class, CompleteFullDataSource.TYPE_ID, new byte[]{ CompleteFullDataSource.DATA_FORMAT_VERSION });
    }
	
    @Override
    public IFullDataSource loadData(FullDataMetaFile dataFile, DhDataInputStream inputStream, IDhLevel level) throws IOException, InterruptedException
	{
		CompleteFullDataSource dataSource = CompleteFullDataSource.createEmpty(dataFile.pos);
		dataSource.populateFromStream(dataFile, inputStream, level);
        return dataSource;
    }
	
}
