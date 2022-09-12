package com.seibel.lod.core.wrapperInterfaces.block;

import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.enums.config.EBlocksToAvoid;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

public interface IBlockStateWrapper {
    String serialize();

// TODO:
//    boolean hasFaceCullingFor(ELodDirection dir);
//    boolean hasNoCollision();
//    boolean noFaceIsFullFace();
}
