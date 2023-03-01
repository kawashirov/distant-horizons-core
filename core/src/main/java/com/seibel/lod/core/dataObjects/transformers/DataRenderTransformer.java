package com.seibel.lod.core.dataObjects.transformers;

import com.seibel.lod.core.dataObjects.fullData.IFullDataSource;
import com.seibel.lod.core.dataObjects.render.ColumnRenderLoader;
import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.util.LodUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/** TODO: Merge this with {@link FullToColumnTransformer} */
public class DataRenderTransformer
{
    public static final ExecutorService TRANSFORMER_THREADS
            = LodUtil.makeThreadPool(4, "Data/Render Transformer");
	
    public static CompletableFuture<ColumnRenderSource> transformDataSource(IFullDataSource data, IDhClientLevel level)
	{
        return CompletableFuture.supplyAsync(() -> transform(data, level), TRANSFORMER_THREADS);
    }
	
    public static CompletableFuture<ColumnRenderSource> asyncTransformDataSource(CompletableFuture<IFullDataSource> fullDataSourceFuture, IDhClientLevel level)
	{
        return fullDataSourceFuture.thenApplyAsync((fullDataSource) -> transform(fullDataSource, level), TRANSFORMER_THREADS);
    }
	
    private static ColumnRenderSource transform(IFullDataSource dataSource, IDhClientLevel level)
	{
        if (dataSource == null)
		{
			return null;
		}
		
        return ColumnRenderLoader.INSTANCE.createRenderSource(dataSource, level);
    }
	
}
