package com.seibel.lod.core.datatype.full;

import com.seibel.lod.core.datatype.AbstractDataSourceLoader;
import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;

import java.io.IOException;
import java.io.InputStream;

public class SpottyDataLoader extends AbstractDataSourceLoader
{
    public SpottyDataLoader() {
        super(SpottyDataSource.class, SpottyDataSource.TYPE_ID, new byte[]{SpottyDataSource.LATEST_VERSION});
    }

    @Override
    public ILodDataSource loadData(FullDataMetaFile dataFile, InputStream data, IDhLevel level) throws IOException {
        return SpottyDataSource.loadData(dataFile, data, level);
    }
}
