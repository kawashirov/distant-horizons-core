package com.seibel.lod.core;

import com.seibel.lod.core.api.external.methods.config.DhApiConfig;
import com.seibel.lod.core.api.external.methods.data.DhApiTerrainDataRepo;
import com.seibel.lod.core.datatype.full.FullDataLoader;
import com.seibel.lod.core.datatype.full.SparseFullDataLoader;
import com.seibel.lod.api.DhApiMain;
import com.seibel.lod.core.datatype.full.SingleChunkFullDataLoader;
import com.seibel.lod.core.render.DhApiRenderProxy;
import com.seibel.lod.core.world.DhApiWorldProxy;

/** Handles first time Core setup. */
public class Initializer
{
    public static void init()
	{
        FullDataLoader unused2 = new FullDataLoader(); // Auto register into the loader system
        SparseFullDataLoader unused3 = new SparseFullDataLoader(); // Auto register
        SingleChunkFullDataLoader unused4 = new SingleChunkFullDataLoader(); // Auto register
		
		// link Core's config to the API
		DhApiMain.Delayed.configs = DhApiConfig.INSTANCE;
		DhApiMain.Delayed.terrainRepo = DhApiTerrainDataRepo.INSTANCE;
		DhApiMain.Delayed.worldProxy = DhApiWorldProxy.INSTANCE;
		DhApiMain.Delayed.renderProxy = DhApiRenderProxy.INSTANCE;
		
    }
}
