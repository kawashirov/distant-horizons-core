package com.seibel.lod.core.a7.level;

import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;

public interface IServerLevel extends ILevel {
    void serverTick();
    void doWorldGen();

    IServerLevelWrapper getServerLevelWrapper();
}
