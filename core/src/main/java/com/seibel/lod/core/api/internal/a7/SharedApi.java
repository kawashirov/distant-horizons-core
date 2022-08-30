package com.seibel.lod.core.api.internal.a7;

import com.seibel.lod.core.a7.Initializer;
import com.seibel.lod.core.a7.world.WorldEnvironment;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.a7.world.DhWorld;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import org.apache.logging.log4j.Logger;

public class SharedApi {
    public static final Logger LOGGER = DhLoggerBuilder.getLogger("DH Events");
    public static IMinecraftSharedWrapper MC;
    public static DhWorld currentWorld;
    public static WorldEnvironment getEnvironment() {
        return currentWorld==null ? null : currentWorld.environment;
    }

    public static void init() {
        Initializer.init();
    }
}
