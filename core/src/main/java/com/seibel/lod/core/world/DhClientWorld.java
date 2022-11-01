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
    public EventLoop eventLoop = new EventLoop(dhTickerThread, this::_clientTick);

    public DhClientWorld() {
        super(EWorldEnvironment.Client_Only);
        saveStructure = new ClientOnlySaveStructure();
        levels = new HashMap<>();
        LOGGER.info("Started DhWorld of type {}", environment);
    }

    @Override
    public DhClientLevel getOrLoadLevel(ILevelWrapper wrapper) {
        if (!(wrapper instanceof IClientLevelWrapper)) return null;

        return levels.computeIfAbsent((IClientLevelWrapper) wrapper, (w) -> {
            File level = saveStructure.tryGetLevelFolder(wrapper);
            if (level == null) return null;
            return new DhClientLevel(saveStructure, w);
        });
    }

    @Override
    public DhClientLevel getLevel(ILevelWrapper wrapper) {
        if (!(wrapper instanceof IClientLevelWrapper)) return null;
        return levels.get(wrapper);
    }
    
    @Override
    public Iterable<? extends IDhLevel> getAllLoadedLevels()
    {
        return levels.values();
    }

    @Override
    public void unloadLevel(ILevelWrapper wrapper) {
        if (!(wrapper instanceof IClientLevelWrapper)) return;
        if (levels.containsKey(wrapper)) {
            LOGGER.info("Unloading level {} ", levels.get(wrapper));
            levels.remove(wrapper).close();
        }
    }

    private void _clientTick() {
        int newViewDistance = Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * 16;
        Iterator<DhClientLevel> iterator = levels.values().iterator();
        while (iterator.hasNext()) {
            DhClientLevel level = iterator.next();
            if (level.tree.viewDistance != newViewDistance) {
                level.close();
                iterator.remove();
            }
        }
        DetailDistanceUtil.updateSettings();
        levels.values().forEach(DhClientLevel::clientTick);
    }

    public void clientTick() {
        eventLoop.tick();
    }

    @Override
    public CompletableFuture<Void> saveAndFlush() {
        return CompletableFuture.allOf(levels.values().stream().map(DhClientLevel::save).toArray(CompletableFuture[]::new));
    }

    @Override
    public void close() {
        saveAndFlush().join();
        for (DhClientLevel level : levels.values()) {
            LOGGER.info("Unloading level " + level.level.getDimensionType().getDimensionName());
            level.close();
        }
        levels.clear();
        eventLoop.close();
        LOGGER.info("Closed DhWorld of type {}", environment);
    }
}
