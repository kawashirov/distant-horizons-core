package com.seibel.lod.core.api.internal;

import com.seibel.lod.core.Initializer;
import com.seibel.lod.core.dataObjects.transformers.DataRenderTransformer;
import com.seibel.lod.core.world.*;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;

/** Contains code and variables used by both {@link ClientApi} and {@link ServerApi} */
public class SharedApi
{
	public static IMinecraftSharedWrapper MC; // TODO remove if possible, it is only used in one odd spot
    private static AbstractDhWorld currentWorld;
	
	
	
    public static void init() { Initializer.init(); }
	
	
	
	public static EWorldEnvironment getEnvironment() { return (currentWorld == null) ? null : currentWorld.environment; }
	
	
	public static void setDhWorld(AbstractDhWorld newWorld) 
	{
		currentWorld = newWorld; 
		
		// starting and stopping the DataRenderTransformer is necessary to prevent attempting to
		// access the MC level at inappropriate times, which can cause exceptions
		if (currentWorld == null)
		{
			DataRenderTransformer.shutdownExecutorService();
		}
		else
		{
			DataRenderTransformer.setupExecutorService();
		}
	}
	
	public static AbstractDhWorld getAbstractDhWorld() { return currentWorld; }
	/** returns null if the {@link SharedApi#currentWorld} isn't a {@link DhClientServerWorld} */
	public static DhClientServerWorld getDhClientServerWorld() { return (currentWorld != null && DhClientServerWorld.class.isInstance(currentWorld)) ? (DhClientServerWorld) currentWorld : null; }
	/** returns null if the {@link SharedApi#currentWorld} isn't a {@link DhClientWorld} or {@link DhClientServerWorld} */
	public static IDhClientWorld getIDhClientWorld() { return (currentWorld != null && IDhClientWorld.class.isInstance(currentWorld)) ? (IDhClientWorld) currentWorld : null; }
	/** returns null if the {@link SharedApi#currentWorld} isn't a {@link DhServerWorld} or {@link DhClientServerWorld} */
	public static IDhServerWorld getIDhServerWorld() { return (currentWorld != null && IDhServerWorld.class.isInstance(currentWorld)) ? (IDhServerWorld) currentWorld : null; }
	
}
