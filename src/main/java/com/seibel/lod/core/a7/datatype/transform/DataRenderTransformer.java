package com.seibel.lod.core.a7.datatype.transform;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.LodRenderSource;
import com.seibel.lod.core.a7.datatype.column.ColumnRenderLoader;
import com.seibel.lod.core.a7.datatype.column.ColumnRenderSource;
import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.util.LodUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

//TODO: Merge this with FullToColumnTransformer
public class DataRenderTransformer {
    public static final ExecutorService TRANSFORMER_THREADS
            = LodUtil.makeSingleThreadPool("Data/Render Transformer");

    public static CompletableFuture<LodRenderSource> transformDataSource(LodDataSource data, IClientLevel level) {
        return CompletableFuture.supplyAsync(() -> transform(data, level), TRANSFORMER_THREADS);
    }

    public static CompletableFuture<LodRenderSource> asyncTransformDataSource(CompletableFuture<LodDataSource> data, IClientLevel level) {
        return data.thenApplyAsync((d) -> transform(d, level), TRANSFORMER_THREADS);
    }

    private static LodRenderSource transform(LodDataSource dataSource, IClientLevel level) {
        if (dataSource == null) return null;
        return ColumnRenderLoader.loaderRegistry.get(ColumnRenderSource.class)
                .stream().findFirst().get().createRender(dataSource, level);
    }
}
