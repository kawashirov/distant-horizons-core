package com.seibel.lod.core.a7.datatype.full;

import com.seibel.lod.core.a7.datatype.DataSourceLoader;
import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.save.io.file.DataMetaFile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FullDataLoader extends DataSourceLoader {
    public FullDataLoader() {
        super(FullDataSource.class, FullDataSource.TYPE_ID, new byte[]{FullDataSource.LATEST_VERSION});
    }

    @Override
    public LodDataSource loadData(DataMetaFile dataFile, InputStream data, ILevel level) throws IOException {
        //TODO: Add decompressor here
        return FullDataSource.loadData(dataFile, data, level);
    }
}
