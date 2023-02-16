package com.seibel.lod.core.api.internal;

import com.seibel.lod.core.Initializer;
import com.seibel.lod.core.world.EWorldEnvironment;
import com.seibel.lod.core.world.AbstractDhWorld;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;

public class SharedApi
{
    public static IMinecraftSharedWrapper MC;
    public static AbstractDhWorld currentWorld;
	
	
	
    public static void init() { Initializer.init(); }
	
	
	
	public static EWorldEnvironment getEnvironment() { return (currentWorld == null) ? null : currentWorld.environment; }
	
}
