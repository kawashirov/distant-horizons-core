package com.seibel.distanthorizons.core.world;

import com.seibel.distanthorizons.core.file.structure.ClientOnlySaveStructure;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.level.DhClientLevel;
import com.seibel.distanthorizons.core.network.NetworkClient;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.util.objects.EventLoop;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class DhClientWorld extends AbstractDhWorld implements IDhClientWorld
{
    private final ConcurrentHashMap<IClientLevelWrapper, DhClientLevel> levels;
    public final ClientOnlySaveStructure saveStructure;

    private final NetworkClient networkClient;

	// TODO why does this executor have 2 threads?
    public ExecutorService dhTickerThread = ThreadUtil.makeSingleThreadPool("DH Client World Ticker Thread", 2);
    public EventLoop eventLoop = new EventLoop(this.dhTickerThread, this::_clientTick);



    public DhClientWorld()
	{
		super(EWorldEnvironment.Client_Only);

        this.saveStructure = new ClientOnlySaveStructure();
		this.levels = new ConcurrentHashMap<>();
        this.networkClient = new NetworkClient("127.0.0.1", 25049);

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
            File file = this.saveStructure.getLevelFolder(wrapper);
            if (file == null)
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
        for (DhClientLevel dhClientLevel : this.levels.values())
		{
            LOGGER.info("Unloading level " + dhClientLevel.getLevelWrapper().getDimensionType().getDimensionName());
            dhClientLevel.close();
        }

		this.levels.clear();
		this.eventLoop.close();
        LOGGER.info("Closed DhWorld of type "+this.environment);
    }

}
