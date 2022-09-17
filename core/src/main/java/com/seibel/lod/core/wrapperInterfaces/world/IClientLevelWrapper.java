package com.seibel.lod.core.wrapperInterfaces.world;

import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;

import javax.annotation.Nullable;

/**
 * 
 * @version 2022-9-16
 */
public interface IClientLevelWrapper extends ILevelWrapper
{
	
    @Nullable
    IServerLevelWrapper tryGetServerSideWrapper();
	
    int computeBaseColor(DhBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper blockState);
	
}
