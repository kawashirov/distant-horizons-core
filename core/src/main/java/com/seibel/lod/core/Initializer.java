package com.seibel.lod.core;

import com.seibel.lod.core.api.external.methods.config.DhApiConfig;
import com.seibel.lod.core.api.external.methods.data.DhApiTerrainDataRepo;
import com.seibel.lod.core.dataObjects.fullData.loader.CompleteFullDataSourceLoader;
import com.seibel.lod.core.dataObjects.fullData.loader.HighDetailIncompleteFullDataSourceLoader;
import com.seibel.lod.api.DhApiMain;
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
		DhApiMain.Delayed.configs = DhApiConfig.INSTANCE;
		DhApiMain.Delayed.terrainRepo = DhApiTerrainDataRepo.INSTANCE;
		DhApiMain.Delayed.worldProxy = DhApiWorldProxy.INSTANCE;
		DhApiMain.Delayed.renderProxy = DhApiRenderProxy.INSTANCE;
		
    }
}
