package com.seibel.lod.api.interfaces.config;

import com.seibel.lod.api.interfaces.config.both.IDhApiWorldGenerationConfig;
import com.seibel.lod.api.interfaces.config.client.*;

/**
 * This interfaces holds all config groups
 * the API has access to for easy access.
 * 
 * @author James Seibel
 * @version 2023-6-14
 */
public interface IDhApiConfig
{
	IDhApiGraphicsConfig graphics();
	IDhApiWorldGenerationConfig worldGenerator();
	IDhApiMultiplayerConfig multiplayer();
	IDhApiMultiThreadingConfig multiThreading();
	IDhApiGpuBuffersConfig gpuBuffers();
	// note: DON'T add the Auto Updater to this API. We only want the user's to have the ability to control when things are downloaded to their machines.
	//IDhApiLoggingConfig logging(); // TODO implement
	IDhApiDebuggingConfig debugging();
	
}
