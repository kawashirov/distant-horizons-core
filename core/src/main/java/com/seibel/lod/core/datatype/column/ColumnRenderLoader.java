package com.seibel.lod.core.datatype.column;

import com.seibel.lod.core.datatype.LodDataSource;
import com.seibel.lod.core.datatype.full.FullDataSource;
import com.seibel.lod.core.datatype.full.SparseDataSource;
import com.seibel.lod.core.datatype.transform.FullToColumnTransformer;
import com.seibel.lod.core.level.IClientLevel;
import com.seibel.lod.core.datatype.LodRenderSource;
import com.seibel.lod.core.datatype.RenderSourceLoader;
import com.seibel.lod.core.level.ILevel;
import com.seibel.lod.core.io.renderfile.RenderMetaFile;
import com.seibel.lod.core.util.LodUtil;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ColumnRenderLoader extends RenderSourceLoader {
    public ColumnRenderLoader() {
        super(ColumnRenderSource.class, ColumnRenderSource.TYPE_ID, new byte[]{ColumnRenderSource.LATEST_VERSION}, ColumnRenderSource.SECTION_SIZE_OFFSET);
    }

    @Override
    public LodRenderSource loadRender(RenderMetaFile dataFile, InputStream data, ILevel level) throws IOException {
        DataInputStream dis = new DataInputStream(data); // DO NOT CLOSE
        return new ColumnRenderSource(dataFile.pos, dis, dataFile.metaData.loaderVersion, level);
    }

    @Override
    public LodRenderSource createRender(LodDataSource dataSource, IClientLevel level) {
        if (dataSource instanceof FullDataSource) {
            return FullToColumnTransformer.transformFullDataToColumnData(level, (FullDataSource) dataSource);
        } else if (dataSource instanceof SparseDataSource) {
            return FullToColumnTransformer.transformSparseDataToColumnData(level, (SparseDataSource) dataSource);
        }
        LodUtil.assertNotReach();
        return null;
    }


}
