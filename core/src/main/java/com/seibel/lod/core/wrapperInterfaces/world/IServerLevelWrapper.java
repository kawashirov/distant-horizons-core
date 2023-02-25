package com.seibel.lod.core.wrapperInterfaces.world;

import javax.annotation.Nullable;
import java.io.File;

public interface IServerLevelWrapper extends ILevelWrapper {
    @Nullable
    IClientLevelWrapper tryGetClientLevelWrapper();

    File getSaveFolder();
}
