package com.seibel.lod.core.level;

import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;

public interface IDhServerLevel extends IDhLevel
{
    void serverTick();
    void doWorldGen();
	
    IServerLevelWrapper getServerLevelWrapper();
	
}
