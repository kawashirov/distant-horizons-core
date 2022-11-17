package com.seibel.lod.api.interfaces.block;

import com.seibel.lod.api.interfaces.IDhApiUnsafeWrapper;

/**
 * A Minecraft version independent way of handling Blocks.
 * 
 * @author James Seibel
 * @version 2022-11-12
 */
public interface IDhApiBlockStateWrapper extends IDhApiUnsafeWrapper
{
    boolean isAir();
	
}
