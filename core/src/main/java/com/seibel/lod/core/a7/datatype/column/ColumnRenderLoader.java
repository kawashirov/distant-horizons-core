package com.seibel.lod.core.a7.datatype.column;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.FullDataSource;
import com.seibel.lod.core.a7.datatype.transform.FullToColumnTransformer;
import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.datatype.LodRenderSource;
import com.seibel.lod.core.a7.datatype.RenderSourceLoader;
import com.seibel.lod.core.a7.save.io.render.RenderMetaFile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ColumnRenderLoader extends RenderSourceLoader {
    public ColumnRenderLoader() {
        super(ColumnRenderSource.class, ColumnRenderSource.TYPE_ID, new byte[]{ColumnRenderSource.LATEST_VERSION}, ColumnRenderSource.SECTION_SIZE_OFFSET);
    }

    @Override
    public LodRenderSource loadRender(RenderMetaFile dataFile, InputStream data, IClientLevel level) throws IOException {
        try (
                //TODO: Add decompressor here
                DataInputStream dis = new DataInputStream(data);
        ) {
            return new ColumnRenderSource(dataFile.pos, dis, dataFile.loaderVersion, level);
        }
    }

    @Override
    public LodRenderSource createRender(LodDataSource dataSource, IClientLevel level) {
        if (dataSource instanceof FullDataSource) {
            return FullToColumnTransformer.transformFullDataToColumnData(level, (FullDataSource) dataSource);
        }
        return null;
    }


}
