package com.seibel.lod.core.a7.save.structure;

import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.io.File;

public class LocalSaveStructure extends SaveStructure {
    private static final IMinecraftSharedWrapper MC = SingletonHandler.INSTANCE.get(IMinecraftSharedWrapper.class);

    private File debugPath = new File("");

    // Fit for Client_Server & Server_Only environment
    public LocalSaveStructure() {
    }

    @Override
    public File tryGetLevelFolder(ILevelWrapper wrapper) {
        debugPath = new File(wrapper.getSaveFolder(), "Distant_Horizons");
        return new File(wrapper.getSaveFolder(), "Distant_Horizons");
    }

    @Override
    public File getRenderCacheFolder(ILevelWrapper level) {
        debugPath = new File(level.getSaveFolder(), "Distant_Horizons");
        return new File(new File(level.getSaveFolder(), "Distant_Horizons"), RENDER_CACHE_FOLDER);
    }

    @Override
    public File getDataFolder(ILevelWrapper level) {
        debugPath = new File(level.getSaveFolder(), "Distant_Horizons");
        return new File(new File(level.getSaveFolder(), "Distant_Horizons"), DATA_FOLDER);
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public String toString() {
        return "[LocalSave at ["+debugPath+"] ]";
    }
}
