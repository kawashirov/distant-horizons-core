package com.seibel.lod.core.world;

import com.seibel.lod.core.level.IDhServerLevel;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

/**  Used both for dedicated server and singleplayer worlds */
public interface IDhServerWorld extends IDhWorld
{
    void serverTick();
    void doWorldGen();
	
	default IDhServerLevel getOrLoadServerLevel(ILevelWrapper levelWrapper) { return (IDhServerLevel) this.getOrLoadLevel(levelWrapper); }
	
}
