package com.seibel.lod.core.api.external.coreImplementations.methods.config;

import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.api.items.interfaces.config.both.IDhApiWorldGenerationConfig;
import com.seibel.lod.api.items.interfaces.config.client.IDhApiBuffersConfig;
import com.seibel.lod.api.items.interfaces.config.client.IDhApiGraphicsConfig;
import com.seibel.lod.api.items.interfaces.config.client.IDhApiMultiplayerConfig;
import com.seibel.lod.api.items.interfaces.config.client.IDhApiThreadingConfig;
import com.seibel.lod.core.api.external.coreImplementations.methods.config.both.DhApiWorldGenerationConfig;
import com.seibel.lod.core.api.external.coreImplementations.methods.config.client.DhApiBuffersConfig;
import com.seibel.lod.core.api.external.coreImplementations.methods.config.client.DhApiGraphicsConfig;
import com.seibel.lod.core.api.external.coreImplementations.methods.config.client.DhApiMultiplayerConfig;
import com.seibel.lod.core.api.external.coreImplementations.methods.config.client.DhApiThreadingConfig;

/**
 * A singleton that holds all of the config groups for the API.
 * 
 * @author James Seibel
 * @version 9-15-2022
 */
public class DhApiConfig implements IDhApiConfig
{
	public static final DhApiConfig INSTANCE = new DhApiConfig();
	
	private DhApiConfig() {  }
	
	
	@Override 
	public IDhApiWorldGenerationConfig getWorldGeneratorConfig() { return DhApiWorldGenerationConfig.INSTANCE; }
	@Override 
	public IDhApiBuffersConfig getBufferConfig() { return DhApiBuffersConfig.INSTANCE; }
	@Override 
	public IDhApiGraphicsConfig getGraphicsConfig() { return DhApiGraphicsConfig.INSTANCE; }
	@Override 
	public IDhApiMultiplayerConfig getMultiplayerConfig() { return DhApiMultiplayerConfig.INSTANCE; }
	@Override 
	public IDhApiThreadingConfig getThreadingConfig() { return DhApiThreadingConfig.INSTANCE; }
	
}
