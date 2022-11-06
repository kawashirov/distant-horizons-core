package com.seibel.lod.core.datatype.transform;

import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.datatype.ILodRenderSource;
import com.seibel.lod.core.datatype.column.ColumnRenderLoader;
import com.seibel.lod.core.datatype.column.ColumnRenderSource;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.util.LodUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

//TODO: Merge this with FullToColumnTransformer
public class DataRenderTransformer
{
    public static final ExecutorService TRANSFORMER_THREADS
            = LodUtil.makeThreadPool(4, "Data/Render Transformer");
	
    public static CompletableFuture<ILodRenderSource> transformDataSource(ILodDataSource data, IDhClientLevel level)
	{
        return CompletableFuture.supplyAsync(() -> transform(data, level), TRANSFORMER_THREADS);
    }
	
    public static CompletableFuture<ILodRenderSource> asyncTransformDataSource(CompletableFuture<ILodDataSource> data, IDhClientLevel level)
	{
        return data.thenApplyAsync((d) -> transform(d, level), TRANSFORMER_THREADS);
    }
	
    private static ILodRenderSource transform(ILodDataSource dataSource, IDhClientLevel level)
	{
        if (dataSource == null) return null;
        return ColumnRenderLoader.loaderRegistry.get(ColumnRenderSource.class)
                .stream().findFirst().get().createRender(dataSource, level);
    }
	
}
