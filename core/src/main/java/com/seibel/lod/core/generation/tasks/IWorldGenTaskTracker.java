package com.seibel.lod.core.generation.tasks;

import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;

import java.util.function.Consumer;

/**
 * @author Leetom
 * @version 2022-11-25
 */
public interface IWorldGenTaskTracker
{
	/**  Returns true if the task hasn't been garbage collected. */
	boolean isMemoryAddressValid();
	
	Consumer<ChunkSizedFullDataAccessor> getOnGenTaskCompleteConsumer();
	
}
