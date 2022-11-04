package com.seibel.lod.core.jar;

import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.jar.wrapperInterfaces.config.LangWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILangWrapper;

public class JarDependencySetup {
    public static void createInitialBindings() {
        SingletonInjector.INSTANCE.bind(ILangWrapper.class, LangWrapper.INSTANCE);
        LangWrapper.init();
    }
}
