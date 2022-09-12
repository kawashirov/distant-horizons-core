package com.seibel.lod.core.io.structure;

import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;

import java.io.File;

public class LocalSaveStructure extends SaveStructure {
    private File debugPath = new File("");

    // Fit for Client_Server & Server_Only environment
    public LocalSaveStructure() {
    }

    @Override
    public File tryGetLevelFolder(ILevelWrapper wrapper) {
        IServerLevelWrapper serverSide = (IServerLevelWrapper) wrapper;
        debugPath = new File(serverSide.getSaveFolder(), "Distant_Horizons");
        return new File(serverSide.getSaveFolder(), "Distant_Horizons");
    }

    @Override
    public File getRenderCacheFolder(ILevelWrapper level) {
        IServerLevelWrapper serverSide = (IServerLevelWrapper) level;
        debugPath = new File(serverSide.getSaveFolder(), "Distant_Horizons");
        return new File(new File(serverSide.getSaveFolder(), "Distant_Horizons"), RENDER_CACHE_FOLDER);
    }

    @Override
    public File getDataFolder(ILevelWrapper level) {
        IServerLevelWrapper serverSide = (IServerLevelWrapper) level;
        debugPath = new File(serverSide.getSaveFolder(), "Distant_Horizons");
        return new File(new File(serverSide.getSaveFolder(), "Distant_Horizons"), DATA_FOLDER);
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public String toString() {
        return "[LocalSave at ["+debugPath+"] ]";
    }
}
