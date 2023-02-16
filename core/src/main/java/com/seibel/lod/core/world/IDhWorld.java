package com.seibel.lod.core.world;

import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.util.concurrent.CompletableFuture;

public interface IDhWorld
{
	
	IDhLevel getOrLoadLevel(ILevelWrapper levelWrapper);
	IDhLevel getLevel(ILevelWrapper wrapper);
	Iterable<? extends IDhLevel> getAllLoadedLevels();
	
	void unloadLevel(ILevelWrapper levelWrapper);
	
	CompletableFuture<Void> saveAndFlush();
	
}
