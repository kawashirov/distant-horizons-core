package com.seibel.lod.core.wrapperInterfaces.world;

import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;

import javax.annotation.Nullable;

public interface IClientLevelWrapper extends ILevelWrapper {
    @Nullable
    IServerLevelWrapper tryGetServerSideWrapper();
    int computeBaseColor(DHBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper blockState);
}
