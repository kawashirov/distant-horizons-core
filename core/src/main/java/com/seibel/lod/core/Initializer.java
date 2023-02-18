package com.seibel.lod.core;

import com.seibel.lod.core.api.external.methods.config.DhApiConfig;
import com.seibel.lod.core.api.external.methods.data.DhApiTerrainDataRepo;
import com.seibel.lod.core.datatype.column.ColumnRenderLoader;
import com.seibel.lod.core.datatype.full.FullDataLoader;
import com.seibel.lod.core.datatype.full.SparseDataLoader;
import com.seibel.lod.api.DhApiMain;
import com.seibel.lod.core.datatype.full.SingleChunkFullDataLoader;
import com.seibel.lod.core.render.DhApiRenderProxy;
import com.seibel.lod.core.world.DhApiWorldProxy;

/**
 * Handles first time Core setup.
 * 
 * @author Leetom
 * @version 2023-2-8
 */
public class Initializer
{
    public static void init()
	{
        ColumnRenderLoader unused = new ColumnRenderLoader(); // Auto register into the loader system
        FullDataLoader unused2 = new FullDataLoader(); // Auto register into the loader system
        SparseDataLoader unused3 = new SparseDataLoader(); // Auto register
        SingleChunkFullDataLoader unused4 = new SingleChunkFullDataLoader(); // Auto register
		
		// link Core's config to the API
		DhApiMain.Delayed.configs = DhApiConfig.INSTANCE;
		DhApiMain.Delayed.terrainRepo = DhApiTerrainDataRepo.INSTANCE;
		DhApiMain.Delayed.worldProxy = DhApiWorldProxy.INSTANCE;
		DhApiMain.Delayed.renderProxy = DhApiRenderProxy.INSTANCE;
		
    }
}
