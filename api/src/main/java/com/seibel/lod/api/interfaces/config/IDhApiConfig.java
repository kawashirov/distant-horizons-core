package com.seibel.lod.api.interfaces.config;

import com.seibel.lod.api.interfaces.config.both.IDhApiWorldGenerationConfig;
import com.seibel.lod.api.interfaces.config.client.IDhApiBuffersConfig;
import com.seibel.lod.api.interfaces.config.client.IDhApiGraphicsConfig;
import com.seibel.lod.api.interfaces.config.client.IDhApiMultiplayerConfig;
import com.seibel.lod.api.interfaces.config.client.IDhApiThreadingConfig;

/**
 * This interfaces holds all of the config groups
 * the API has access to for easy access to all config values.
 * 
 * @author James Seibel
 * @version 9-15-2022
 */
public interface IDhApiConfig
{
	
	IDhApiWorldGenerationConfig getWorldGeneratorConfig();
	IDhApiBuffersConfig getBufferConfig();
	IDhApiGraphicsConfig getGraphicsConfig();
	IDhApiMultiplayerConfig getMultiplayerConfig();
	IDhApiThreadingConfig getThreadingConfig();
	
}
