package com.seibel.lod.core.dataObjects.fullData.loader;

import com.seibel.lod.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.LowDetailIncompleteFullDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.util.objects.dataStreams.DhDataInputStream;

import java.io.IOException;

public class LowDetailIncompleteFullDataSourceLoader extends AbstractFullDataSourceLoader
{
    public LowDetailIncompleteFullDataSourceLoader() {
        super(LowDetailIncompleteFullDataSource.class, LowDetailIncompleteFullDataSource.TYPE_ID, new byte[]{ LowDetailIncompleteFullDataSource.DATA_FORMAT_VERSION });
    }

    @Override
    public IFullDataSource loadData(FullDataMetaFile dataFile, DhDataInputStream inputStream, IDhLevel level) throws IOException, InterruptedException
	{
		LowDetailIncompleteFullDataSource dataSource = LowDetailIncompleteFullDataSource.createEmpty(dataFile.pos);
		dataSource.populateFromStream(dataFile, inputStream, level);
		return dataSource;
    }
}
