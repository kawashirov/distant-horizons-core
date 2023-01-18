package com.seibel.lod.core.generation.tasks;

import com.seibel.lod.core.pos.DhLodPos;

import java.util.concurrent.CompletableFuture;

/**
 * @author Leetom
 * @version 2022-11-25
 */
public final class WorldGenTask
{
	public final DhLodPos pos;
	public final byte dataDetailLevel;
	public final AbstractWorldGenTaskTracker taskTracker;
	public final CompletableFuture<Boolean> future;
	
	
	public WorldGenTask(DhLodPos pos, byte dataDetail, AbstractWorldGenTaskTracker taskTracker, CompletableFuture<Boolean> future)
	{
		this.dataDetailLevel = dataDetail;
		this.pos = pos;
		this.taskTracker = taskTracker;
		this.future = future;
	}
	
}