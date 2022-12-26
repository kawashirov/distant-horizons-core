package com.seibel.lod.core.datatype.column;

import com.seibel.lod.core.datatype.IIncompleteDataSource;
import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.datatype.full.FullDataSource;
import com.seibel.lod.core.datatype.transform.FullToColumnTransformer;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.datatype.ILodRenderSource;
import com.seibel.lod.core.datatype.AbstractRenderSourceLoader;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.file.renderfile.RenderMetaDataFile;
import com.seibel.lod.core.util.LodUtil;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ColumnRenderLoader extends AbstractRenderSourceLoader
{
    public ColumnRenderLoader() {
        super(ColumnRenderSource.class, ColumnRenderSource.TYPE_ID, new byte[]{ColumnRenderSource.LATEST_VERSION}, ColumnRenderSource.SECTION_SIZE_OFFSET);
    }

    @Override
    public ILodRenderSource loadRender(RenderMetaDataFile dataFile, InputStream data, IDhLevel level) throws IOException {
        DataInputStream dis = new DataInputStream(data); // DO NOT CLOSE
        return new ColumnRenderSource(dataFile.pos, dis, dataFile.metaData.loaderVersion, level);
    }

    @Override
    public ILodRenderSource createRender(ILodDataSource dataSource, IDhClientLevel level) {
        if (dataSource instanceof FullDataSource) {
            return FullToColumnTransformer.transformFullDataToColumnData(level, (FullDataSource) dataSource);
        } else if (dataSource instanceof IIncompleteDataSource) {
            return FullToColumnTransformer.transformIncompleteDataToColumnData(level, (IIncompleteDataSource) dataSource);
        }
        LodUtil.assertNotReach();
        return null;
    }


}
