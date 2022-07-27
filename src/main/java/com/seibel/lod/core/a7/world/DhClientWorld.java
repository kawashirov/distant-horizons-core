package com.seibel.lod.core.a7.world;

import com.seibel.lod.core.a7.level.DhClientLevel;
import com.seibel.lod.core.a7.level.DhClientServerLevel;
import com.seibel.lod.core.a7.save.structure.ClientOnlySaveStructure;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.EventLoop;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DhClientWorld extends DhWorld implements IClientWorld {
    private final HashMap<ILevelWrapper, DhClientLevel> levels;
    public final ClientOnlySaveStructure saveStructure;
    public ExecutorService dhTickerThread = LodUtil.makeSingleThreadPool("DHTickerThread", 2);
    public EventLoop eventLoop = new EventLoop(dhTickerThread, this::_clientTick);

    public DhClientWorld() {
        super(WorldEnvironment.Client_Only);
        saveStructure = new ClientOnlySaveStructure();
        levels = new HashMap<>();
        LOGGER.info("Started DhWorld of type {}", environment);
    }

    @Override
    public DhClientLevel getOrLoadLevel(ILevelWrapper wrapper) {
        return levels.computeIfAbsent(wrapper, (w) -> {
            File level = saveStructure.tryGetLevelFolder(wrapper);
            if (level == null) return null;
            return new DhClientLevel(saveStructure, w);
        });
    }

    @Override
    public DhClientLevel getLevel(ILevelWrapper wrapper) {
        return levels.get(wrapper);
    }

    @Override
    public void unloadLevel(ILevelWrapper wrapper) {
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
            LOGGER.info("Unloading level for world " + level.level.getDimensionType().getDimensionName());
            level.close();
        }
        levels.clear();
        LOGGER.info("Closed DhWorld of type {}", environment);
    }
}
