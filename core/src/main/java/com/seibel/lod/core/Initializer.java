package com.seibel.lod.core;

import com.seibel.lod.core.api.external.methods.config.DhApiConfig;
import com.seibel.lod.core.datatype.column.ColumnRenderLoader;
import com.seibel.lod.core.datatype.full.FullDataLoader;
import com.seibel.lod.core.datatype.full.SparseDataLoader;
import com.seibel.lod.core.interfaces.dependencyInjection.ApiCoreInjectors;

/**
 * Handles first time Core setup.
 * 
 * @author Leetom
 * @version 2022-9-15
 */
public class Initializer
{
    public static void init()
	{
        ColumnRenderLoader unused = new ColumnRenderLoader(); // Auto register into the loader system
        FullDataLoader unused2 = new FullDataLoader(); // Auto register into the loader system
        SparseDataLoader unused3 = new SparseDataLoader(); // Auto register
		
		
		// link Core's config to the API
		ApiCoreInjectors.getInstance().configs = DhApiConfig.INSTANCE;
		
    }
}
