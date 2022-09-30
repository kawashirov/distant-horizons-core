package com.seibel.lod.core.world;

import com.seibel.lod.core.level.DhServerLevel;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.file.structure.LocalSaveStructure;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class DhServerWorld extends DhWorld implements IDhServerWorld
{
    private final HashMap<IServerLevelWrapper, DhServerLevel> levels;
    public final LocalSaveStructure saveStructure;

    public DhServerWorld() {
        super(WorldEnvironment.Server_Only);
        saveStructure = new LocalSaveStructure();
        levels = new HashMap<>();
        LOGGER.info("Started DhWorld of type {}", environment);
    }

    @Override
    public DhServerLevel getOrLoadLevel(ILevelWrapper wrapper) {
        if (!(wrapper instanceof IServerLevelWrapper)) return null;
        return levels.computeIfAbsent((IServerLevelWrapper) wrapper, (w) -> {
            File levelFile = saveStructure.tryGetLevelFolder(wrapper);
            LodUtil.assertTrue(levelFile != null);
            return new DhServerLevel(saveStructure, w);
        });
    }

    @Override
    public DhServerLevel getLevel(ILevelWrapper wrapper) {
        if (!(wrapper instanceof IServerLevelWrapper)) return null;
        return levels.get(wrapper);
    }
    
    @Override
    public Iterable<? extends IDhLevel> getAllLoadedLevels()
    {
        return levels.values();
    }

    @Override
    public void unloadLevel(ILevelWrapper wrapper) {
        if (!(wrapper instanceof IServerLevelWrapper)) return;
        if (levels.containsKey(wrapper)) {
            LOGGER.info("Unloading level {} ", levels.get(wrapper));
            levels.remove(wrapper).close();
        }
    }

    public void serverTick() {
        levels.values().forEach(DhServerLevel::serverTick);
    }

    public void doWorldGen() {
        levels.values().forEach(DhServerLevel::doWorldGen);

    }

    @Override
    public CompletableFuture<Void> saveAndFlush() {
        return CompletableFuture.allOf(levels.values().stream().map(DhServerLevel::save).toArray(CompletableFuture[]::new));
    }

    @Override
    public void close() {
        for (DhServerLevel level : levels.values()) {
            LOGGER.info("Unloading level " + level.level.getDimensionType().getDimensionName());
            level.close();
        }
        levels.clear();
        LOGGER.info("Closed DhWorld of type {}", environment);
    }



}
