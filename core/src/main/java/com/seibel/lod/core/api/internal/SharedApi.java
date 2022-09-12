package com.seibel.lod.core.api.internal;

import com.seibel.lod.core.Initializer;
import com.seibel.lod.core.world.WorldEnvironment;
import com.seibel.lod.core.world.DhWorld;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;

public class SharedApi
{
    public static IMinecraftSharedWrapper MC;
    public static DhWorld currentWorld;
    public static WorldEnvironment getEnvironment() { return currentWorld==null ? null : currentWorld.environment; }

    public static void init() { Initializer.init(); }
	
}
