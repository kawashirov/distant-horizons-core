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
    public EventLoop eventLoop = new EventLoop(dhTickerThread, this::_clientTick); //TODO: Rate-limit the loop
    public F3Screen.DynamicMessage f3Msg;

    public DhClientServerWorld() {
        super(EWorldEnvironment.Client_Server);
        saveStructure = new LocalSaveStructure();
        levelObjMap = new HashMap<>();
        dhLevels = new HashSet<>();
        LOGGER.info("Started DhWorld of type {}", environment);
        f3Msg = new F3Screen.DynamicMessage(() ->
                LodUtil.formatLog("{} World with {} levels", environment, dhLevels.size()));
    }

    @Override
    public DhClientServerLevel getOrLoadLevel(ILevelWrapper wrapper) {
        if (wrapper instanceof IServerLevelWrapper) {
            return levelObjMap.computeIfAbsent(wrapper, (w) -> {
                File levelFile = saveStructure.tryGetOrCreateLevelFolder(w);
                LodUtil.assertTrue(levelFile != null);
                DhClientServerLevel level = new DhClientServerLevel(saveStructure, (IServerLevelWrapper) w);
                dhLevels.add(level);
                return level;
            });
        } else {
            return levelObjMap.computeIfAbsent(wrapper, (w) -> {
                IClientLevelWrapper clientSide = (IClientLevelWrapper) w;
                IServerLevelWrapper serverSide = clientSide.tryGetServerSideWrapper();
                LodUtil.assertTrue(serverSide != null);
                DhClientServerLevel level = levelObjMap.get(serverSide);
                if (level==null) return null;
                level.startRenderer(clientSide);
                return level;
            });
        }
    }

    @Override
    public DhClientServerLevel getLevel(ILevelWrapper wrapper) {
        return levelObjMap.get(wrapper);
    }
    
    @Override
    public Iterable<? extends IDhLevel> getAllLoadedLevels()
    {
        return dhLevels;
    }
    
    @Override
    public void unloadLevel(ILevelWrapper wrapper) {
        if (levelObjMap.containsKey(wrapper)) {
            if (wrapper instanceof IServerLevelWrapper) {
                LOGGER.info("Unloading level {} ", levelObjMap.get(wrapper));
                DhClientServerLevel level = levelObjMap.remove(wrapper);
                dhLevels.remove(level);
                level.close();
            } else {
                levelObjMap.remove(wrapper).stopRenderer(); // Ignore resource warning. The level obj is referenced elsewhere.
            }
        }
    }

    private void _clientTick() {
        //LOGGER.info("Client world tick with {} levels", levels.size());
        dhLevels.forEach(DhClientServerLevel::clientTick);
    }

    public void clientTick() {
        //LOGGER.info("Client world tick");
        eventLoop.tick();
    }

    public void serverTick() {
        dhLevels.forEach(DhClientServerLevel::serverTick);
    }

    public void doWorldGen() {
        dhLevels.forEach(DhClientServerLevel::doWorldGen);
    }

    @Override
    public CompletableFuture<Void> saveAndFlush() {
        return CompletableFuture.allOf(dhLevels.stream().map(DhClientServerLevel::save).toArray(CompletableFuture[]::new));
    }

    @Override
    public void close() {
        saveAndFlush().join();
        for (DhClientServerLevel level : dhLevels) {
            LOGGER.info("Unloading level " + level.serverLevel.getDimensionType().getDimensionName());
            level.close();
        }
        levelObjMap.clear();
        eventLoop.close();
        LOGGER.info("Closed DhWorld of type {}", environment);
    }
}
