package com.seibel.lod.core;

import com.seibel.lod.core.api.external.methods.config.DhApiConfig;
import com.seibel.lod.core.api.external.methods.data.DhApiTerrainDataRepo;
import com.seibel.lod.core.dataObjects.fullData.loader.CompleteFullDataSourceLoader;
import com.seibel.lod.core.dataObjects.fullData.loader.HighDetailIncompleteFullDataSourceLoader;
import com.seibel.lod.api.DhApi;
import com.seibel.lod.core.dataObjects.fullData.loader.LowDetailIncompleteFullDataSourceLoader;
import com.seibel.lod.core.render.DhApiRenderProxy;
import com.seibel.lod.core.world.DhApiWorldProxy;

/** Handles first time Core setup. */
public class Initializer
{
    public static void init()
	{
        CompleteFullDataSourceLoader unused2 = new CompleteFullDataSourceLoader(); // Auto register into the loader system
        HighDetailIncompleteFullDataSourceLoader unused3 = new HighDetailIncompleteFullDataSourceLoader(); // Auto register
        LowDetailIncompleteFullDataSourceLoader unused4 = new LowDetailIncompleteFullDataSourceLoader(); // Auto register
		
		// link Core's config to the API
		DhApi.Delayed.configs = DhApiConfig.INSTANCE;
		DhApi.Delayed.terrainRepo = DhApiTerrainDataRepo.INSTANCE;
		DhApi.Delayed.worldProxy = DhApiWorldProxy.INSTANCE;
		DhApi.Delayed.renderProxy = DhApiRenderProxy.INSTANCE;
		
    }
}
