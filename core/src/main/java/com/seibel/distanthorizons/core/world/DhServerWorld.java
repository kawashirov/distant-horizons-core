package com.seibel.distanthorizons.core.world;

import com.seibel.distanthorizons.core.file.structure.LocalSaveStructure;
import com.seibel.distanthorizons.core.level.DhServerLevel;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.network.NetworkServer;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class DhServerWorld extends AbstractDhWorld implements IDhServerWorld
{
	private final HashMap<IServerLevelWrapper, DhServerLevel> levels;
	public final LocalSaveStructure saveStructure;
	private final NetworkServer networkServer;
	
	
	public DhServerWorld()
	{
		super(EWorldEnvironment.Server_Only);
		
		this.saveStructure = new LocalSaveStructure();
		this.levels = new HashMap<>();

		// TODO move to global config once server specific configs are implemented
		this.networkServer = new NetworkServer(25049);
		
		LOGGER.info("Started "+DhServerWorld.class.getSimpleName()+" of type "+this.environment);
	}
	
	
	
	@Override
	public DhServerLevel getOrLoadLevel(ILevelWrapper wrapper)
	{
		if (!(wrapper instanceof IServerLevelWrapper))
		{
			return null;
		}
		
		return this.levels.computeIfAbsent((IServerLevelWrapper) wrapper, (w) ->
		{
			File levelFile = this.saveStructure.getLevelFolder(wrapper);
			LodUtil.assertTrue(levelFile != null);
			return new DhServerLevel(this.saveStructure, w);
		});
	}
	
	@Override
	public DhServerLevel getLevel(ILevelWrapper wrapper)
	{
		if (!(wrapper instanceof IServerLevelWrapper))
		{
			return null;
		}
		
		return this.levels.get(wrapper);
	}
	
	@Override
	public Iterable<? extends IDhLevel> getAllLoadedLevels() { return this.levels.values(); }
	
	@Override
	public void unloadLevel(ILevelWrapper wrapper)
	{
		if (!(wrapper instanceof IServerLevelWrapper))
		{
			return;
		}
		
		if (this.levels.containsKey(wrapper))
		{
			LOGGER.info("Unloading level {} ", this.levels.get(wrapper));
			this.levels.remove(wrapper).close();
		}
	}
	
	public void serverTick() { this.levels.values().forEach(DhServerLevel::serverTick); }
	
	public void doWorldGen() { this.levels.values().forEach(DhServerLevel::doWorldGen); }
	
	@Override
	public CompletableFuture<Void> saveAndFlush()
	{
		return CompletableFuture.allOf(this.levels.values().stream().map(DhServerLevel::saveAsync).toArray(CompletableFuture[]::new));
	}
	
	@Override
	public void close()
	{
		this.networkServer.close();

		for (DhServerLevel level : this.levels.values())
		{
			LOGGER.info("Unloading level " + level.getLevelWrapper().getDimensionType().getDimensionName());
			level.close();
		}
		
		this.levels.clear();
		LOGGER.info("Closed DhWorld of type "+this.environment);
	}
	
	
	
}
