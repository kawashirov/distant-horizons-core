package com.seibel.distanthorizons.core.world;

import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import java.util.concurrent.CompletableFuture;

public interface IDhWorld
{
	
	IDhLevel getOrLoadLevel(ILevelWrapper levelWrapper);
	IDhLevel getLevel(ILevelWrapper wrapper);
	Iterable<? extends IDhLevel> getAllLoadedLevels();
	
	void unloadLevel(ILevelWrapper levelWrapper);
	
	CompletableFuture<Void> saveAndFlush();
	
}
