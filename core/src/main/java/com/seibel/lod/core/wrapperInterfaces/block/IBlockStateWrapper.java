package com.seibel.lod.core.wrapperInterfaces.block;

import com.seibel.lod.api.interfaces.block.IDhApiBlockStateWrapper;

/**
 * A Minecraft version independent way of handling Blocks.
 *
 * @author James Seibel
 * @version 2022-11-12
 */
public interface IBlockStateWrapper extends IDhApiBlockStateWrapper
{
    String serialize();
	
}
