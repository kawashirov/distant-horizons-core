package com.seibel.lod.core.level;

import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.util.math.Mat4f;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;

public interface IDhClientLevel extends IDhLevel
{
    /** Return whether the level needs to be reloaded */
    void clientTick();
	
    void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler);
	
    int computeBaseColor(DhBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper block);
	
    IClientLevelWrapper getClientLevelWrapper();
	
}
