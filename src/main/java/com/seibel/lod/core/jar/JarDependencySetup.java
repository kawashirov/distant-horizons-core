package com.seibel.lod.core.jar;

import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.jar.wrapperInterfaces.config.ConfigWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.IConfigWrapper;

public class JarDependencySetup {
    public static void createInitialBindings() {
        SingletonInjector.INSTANCE.bind(IConfigWrapper.class, ConfigWrapper.INSTANCE);
        ConfigWrapper.init();
    }
}
