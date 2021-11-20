package com.seibel.lod.wrappers;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.wrapperAdapters.IWrapperFactory;
import com.seibel.lod.core.wrapperAdapters.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperAdapters.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperAdapters.world.IWorldWrapper;
import com.seibel.lod.core.wrapperAdapters.worldGeneration.AbstractWorldGeneratorWrapper;
import com.seibel.lod.wrappers.block.BlockPosWrapper;
import com.seibel.lod.wrappers.chunk.ChunkPosWrapper;
import com.seibel.lod.wrappers.worldGeneration.WorldGeneratorWrapper;

/**
 * This handles creating abstract wrapper objects.
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public class WrapperFactory implements IWrapperFactory
{
	public static final WrapperFactory INSTANCE = new WrapperFactory();
	
	
	@Override
	public AbstractBlockPosWrapper createBlockPos()
	{
		return new BlockPosWrapper();
	}
	
	@Override
	public AbstractBlockPosWrapper createBlockPos(int x, int y, int z)
	{
		return new BlockPosWrapper(x,y,z);
	}
	
	
	
	
	@Override
	public AbstractChunkPosWrapper createChunkPos()
	{
		return new ChunkPosWrapper();
	}

	@Override
	public AbstractChunkPosWrapper createChunkPos(int x, int z)
	{
		return new ChunkPosWrapper(x, z);
	}

	@Override
	public AbstractChunkPosWrapper createChunkPos(AbstractChunkPosWrapper newChunkPos)
	{
		return new ChunkPosWrapper(newChunkPos);
	}

	@Override
	public AbstractChunkPosWrapper createChunkPos(AbstractBlockPosWrapper blockPos)
	{
		return new ChunkPosWrapper(blockPos);
	}
	
	
	
	@Override
	public AbstractWorldGeneratorWrapper createWorldGenerator(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper)
	{
		return new WorldGeneratorWrapper(newLodBuilder, newLodDimension, worldWrapper);
	}
	
}
