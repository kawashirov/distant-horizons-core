package com.seibel.lod.core.datatype.transform;

import com.seibel.lod.core.datatype.full.IFullDataSource;
import com.seibel.lod.core.datatype.render.ColumnRenderLoader;
import com.seibel.lod.core.datatype.render.ColumnRenderSource;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.util.LodUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

//TODO: Merge this with FullToColumnTransformer
public class DataRenderTransformer
{
    public static final ExecutorService TRANSFORMER_THREADS
            = LodUtil.makeThreadPool(4, "Data/Render Transformer");
	
    public static CompletableFuture<ColumnRenderSource> transformDataSource(IFullDataSource data, IDhClientLevel level)
	{
        return CompletableFuture.supplyAsync(() -> transform(data, level), TRANSFORMER_THREADS);
    }
	
    public static CompletableFuture<ColumnRenderSource> asyncTransformDataSource(CompletableFuture<IFullDataSource> data, IDhClientLevel level)
	{
        return data.thenApplyAsync((d) -> transform(d, level), TRANSFORMER_THREADS);
    }
	
    private static ColumnRenderSource transform(IFullDataSource dataSource, IDhClientLevel level)
	{
        if (dataSource == null)
		{
			return null;
		}
		
        return ColumnRenderLoader.LOADER_BY_SOURCE_TYPE.get(ColumnRenderSource.class)
                .stream().findFirst().get().createRenderSource(dataSource, level);
    }
	
}
