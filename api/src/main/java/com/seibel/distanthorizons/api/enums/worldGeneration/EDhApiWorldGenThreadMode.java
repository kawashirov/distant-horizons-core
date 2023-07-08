package com.seibel.distanthorizons.api.enums.worldGeneration;

/**
 * MULTI_THREADED, <br>
 * SINGLE_THREADED, <br>
 * SERVER_THREAD, <br>
 *
 * @author James Seibel
 * @version 7-14-2022
 */
@Deprecated // not currently in use, if the config this enum represents is re-implemented, the deprecated flag can be removed
public enum EDhApiWorldGenThreadMode
{
	/**
	 * This world generator can be run on an unlimited number
	 * of concurrent threads.
	 */
	MULTI_THREADED,
	
	/**
	 * This world generator can only be run on one thread at
	 * a time, however that thread can run concurrently
	 * to Minecraft's server thread.
	 */
	SINGLE_THREADED,
	
	/**
	 * This world generator can only be run on Minecraft's
	 * server thread.
	 */
	SERVER_THREAD,
}
