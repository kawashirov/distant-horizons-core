package com.seibel.lod.core.world;

import com.seibel.lod.core.level.DhClientServerLevel;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.file.structure.LocalSaveStructure;
import com.seibel.lod.core.logging.f3.F3Screen;
import com.seibel.lod.core.util.objects.EventLoop;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DhClientServerWorld extends AbstractDhWorld implements IDhClientWorld, IDhServerWorld
{
    private final HashMap<ILevelWrapper, DhClientServerLevel> levelObjMap;
    private final HashSet<DhClientServerLevel> dhLevels;
    public final LocalSaveStructure saveStructure;
	
    public ExecutorService dhTickerThread = LodUtil.makeSingleThreadPool("DHTickerThread", 2);
    public EventLoop eventLoop = new EventLoop(this.dhTickerThread, this::_clientTick); //TODO: Rate-limit the loop
	
    public F3Screen.DynamicMessage f3Message;
	
	
	
    public DhClientServerWorld()
	{
        super(EWorldEnvironment.Client_Server);
        this.saveStructure = new LocalSaveStructure();
		this.levelObjMap = new HashMap<>();
		this.dhLevels = new HashSet<>();
		
        LOGGER.info("Started DhWorld of type "+this.environment);
		
		this.f3Message = new F3Screen.DynamicMessage(() -> LodUtil.formatLog(this.environment+" World with "+this.dhLevels.size()+" levels"));
    }
	
	
	
    @Override
	public DhClientServerLevel getOrLoadLevel(ILevelWrapper wrapper)
	{
		if (wrapper instanceof IServerLevelWrapper)
		{
			return this.levelObjMap.computeIfAbsent(wrapper, (levelWrapper) -> 
			{
				File levelFile = this.saveStructure.tryGetOrCreateLevelFolder(levelWrapper);
				LodUtil.assertTrue(levelFile != null);
				DhClientServerLevel level = new DhClientServerLevel(this.saveStructure, (IServerLevelWrapper) levelWrapper);
				this.dhLevels.add(level);
				return level;
			});
		}
		else
		{
			return this.levelObjMap.computeIfAbsent(wrapper, (levelWrapper) ->
			{
				IClientLevelWrapper clientLevelWrapper = (IClientLevelWrapper) levelWrapper;
				IServerLevelWrapper serverLevelWrapper = clientLevelWrapper.tryGetServerSideWrapper();
				LodUtil.assertTrue(serverLevelWrapper != null);
				
				DhClientServerLevel level = this.levelObjMap.get(serverLevelWrapper);
				if (level == null)
				{
					return null;
				}
				
				level.startRenderer(clientLevelWrapper);
				return level;
			});
		}
	}

    @Override
    public DhClientServerLevel getLevel(ILevelWrapper wrapper) { return this.levelObjMap.get(wrapper); }
    
    @Override
    public Iterable<? extends IDhLevel> getAllLoadedLevels() { return this.dhLevels; }
    
    @Override
    public void unloadLevel(ILevelWrapper wrapper)
	{
		if (this.levelObjMap.containsKey(wrapper))
		{
			if (wrapper instanceof IServerLevelWrapper)
			{
				LOGGER.info("Unloading level "+this.levelObjMap.get(wrapper));
				DhClientServerLevel level = this.levelObjMap.remove(wrapper);
				this.dhLevels.remove(level);
				level.close();
			}
			else
			{
				// TODO why is this called here?
				this.levelObjMap.remove(wrapper).stopRenderer(); // Ignore resource warning. The level obj is referenced elsewhere.
			}
		}
	}

    private void _clientTick()
	{
        //LOGGER.info("Client world tick with {} levels", levels.size());
        this.dhLevels.forEach(DhClientServerLevel::clientTick);
    }

    public void clientTick()
	{
        //LOGGER.info("Client world tick");
		this.eventLoop.tick();
    }

    public void serverTick() { this.dhLevels.forEach(DhClientServerLevel::serverTick); }

    public void doWorldGen() { this.dhLevels.forEach(DhClientServerLevel::doWorldGen); }

    @Override
    public CompletableFuture<Void> saveAndFlush() 
	{ 
		return CompletableFuture.allOf(this.dhLevels.stream().map(DhClientServerLevel::saveAsync).toArray(CompletableFuture[]::new)); 
	}

    @Override
    public void close()
	{
		this.saveAndFlush().join();
		
		for (DhClientServerLevel level : this.dhLevels)
		{
			LOGGER.info("Unloading level " + level.serverLevel.getDimensionType().getDimensionName());
			level.close();
		}
		
		this.levelObjMap.clear();
		this.eventLoop.close();
		LOGGER.info("Closed DhWorld of type "+this.environment);
	}
	
}
