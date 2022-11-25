package com.seibel.lod.core.generation.tasks;

import java.util.concurrent.CompletableFuture;

/**
 * @author Leetom
 * @version 2022-11-25
 */
public final class InProgressWorldGenTask
{
	public final TaskGroup group;
	public CompletableFuture<Void> genFuture = null;
	
	
	public InProgressWorldGenTask(TaskGroup group)
	{
		this.group = group;
	}
	
}
