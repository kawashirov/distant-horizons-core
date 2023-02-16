package com.seibel.lod.core.world;

import com.seibel.lod.core.level.DhClientLevel;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.file.structure.ClientOnlySaveStructure;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.objects.EventLoop;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DhClientWorld extends AbstractDhWorld implements IDhClientWorld
{
    private final HashMap<IClientLevelWrapper, DhClientLevel> levels;
    public final ClientOnlySaveStructure saveStructure;
	
    public ExecutorService dhTickerThread = LodUtil.makeSingleThreadPool("DHTickerThread", 2);
    public EventLoop eventLoop = new EventLoop(this.dhTickerThread, this::_clientTick);
	
	
	
    public DhClientWorld()
	{
		super(EWorldEnvironment.Client_Only);
		this.saveStructure = new ClientOnlySaveStructure();
		this.levels = new HashMap<>();
		LOGGER.info("Started DhWorld of type "+this.environment);
	}
	
	
	
    @Override
    public DhClientLevel getOrLoadLevel(ILevelWrapper wrapper)
	{
        if (!(wrapper instanceof IClientLevelWrapper))
		{
			return null;
		}

        return this.levels.computeIfAbsent((IClientLevelWrapper) wrapper, (clientLevelWrapper) ->
		{
            File level = this.saveStructure.tryGetOrCreateLevelFolder(wrapper);
            if (level == null)
			{
				return null;
			}
			
            return new DhClientLevel(this.saveStructure, clientLevelWrapper);
        });
    }

    @Override
    public DhClientLevel getLevel(ILevelWrapper wrapper)
	{
        if (!(wrapper instanceof IClientLevelWrapper))
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
        if (!(wrapper instanceof IClientLevelWrapper))
		{
			return;
		}
		
        if (this.levels.containsKey(wrapper))
		{
            LOGGER.info("Unloading level "+this.levels.get(wrapper));
			this.levels.remove(wrapper).close();
        }
    }

    private void _clientTick()
	{
		int newBlockRenderDistance = Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH;
		
		Iterator<DhClientLevel> iterator = this.levels.values().iterator();
		while (iterator.hasNext())
		{
			DhClientLevel level = iterator.next();
			if (level.tree.blockRenderDistance != newBlockRenderDistance)
			{
				level.close();
				iterator.remove();
			}
		}
		
		DetailDistanceUtil.updateSettings();
		this.levels.values().forEach(DhClientLevel::clientTick);
	}

    public void clientTick() { this.eventLoop.tick(); }

    @Override
    public CompletableFuture<Void> saveAndFlush()
	{
        return CompletableFuture.allOf(this.levels.values().stream().map(DhClientLevel::saveAsync).toArray(CompletableFuture[]::new));
    }

    @Override
    public void close()
	{
		this.saveAndFlush().join();
        for (DhClientLevel level : this.levels.values())
		{
            LOGGER.info("Unloading level " + level.level.getDimensionType().getDimensionName());
            level.close();
        }
		
		this.levels.clear();
		this.eventLoop.close();
        LOGGER.info("Closed DhWorld of type "+this.environment);
    }
	
}
