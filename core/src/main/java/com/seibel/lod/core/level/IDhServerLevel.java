package com.seibel.lod.core.level;

import com.seibel.lod.core.file.fullDatafile.GeneratedFullDataFileHandler;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;

public interface IDhServerLevel extends IDhLevel, GeneratedFullDataFileHandler.IOnWorldGenCompleteListener
        {
    void serverTick();
    void doWorldGen();
	
    IServerLevelWrapper getServerLevelWrapper();
	
}
