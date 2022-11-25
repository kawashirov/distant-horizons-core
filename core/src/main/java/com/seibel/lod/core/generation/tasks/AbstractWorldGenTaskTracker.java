package com.seibel.lod.core.generation.tasks;

import com.seibel.lod.core.datatype.full.ChunkSizedData;

import java.util.function.Consumer;

/**
 * @author Leetom
 * @version 2022-11-25
 */
public abstract class AbstractWorldGenTaskTracker
{
	public abstract boolean isValid();
	
	public abstract Consumer<ChunkSizedData> getConsumer();
	
}
