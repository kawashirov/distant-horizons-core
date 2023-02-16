package com.seibel.lod.core.world;

import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

public interface IDhClientWorld extends IDhWorld
{
    void clientTick();
	
	default IDhClientLevel getOrLoadClientLevel(ILevelWrapper levelWrapper) { return (IDhClientLevel) this.getOrLoadLevel(levelWrapper); }
	
}
