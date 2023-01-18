package com.seibel.lod.core.generation.tasks;

import com.seibel.lod.core.datatype.full.ChunkSizedData;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import com.seibel.lod.core.generation.WorldGenerationQueue;

/**
 * Used to synchronize {@link WorldGenerationQueue} {@link WorldGenTask}'s
 * if the {@link WorldGenTask} needs to be split up.
 * 
 * @author Leetom
 * @version 2022-11-25
 */
public class SplitTaskTracker extends AbstractWorldGenTaskTracker
{
	public final AbstractWorldGenTaskTracker parentTracker;
	public final CompletableFuture<Boolean> parentFuture;
	
	/** cached value to allow for quicker checking */
	public boolean isValid = true;
	
	
	
	public SplitTaskTracker(AbstractWorldGenTaskTracker parentTracker, CompletableFuture<Boolean> parentFuture)
	{
		this.parentTracker = parentTracker;
		this.parentFuture = parentFuture;
	}
	
	
	
	/** Recalculates and returns the new {@link SplitTaskTracker#isValid} value */
	public boolean recalculateIsValid()
	{
		if (!this.isValid)
		{
			return false;
		}
		
		this.isValid = this.parentTracker.isValid();
		if (!this.isValid)
		{
			this.parentFuture.complete(false);
		}
		
		return this.isValid;
	}
	
	@Override
	public boolean isValid() { return this.isValid; }
	
	@Override
	public Consumer<ChunkSizedData> getConsumer() { return this.parentTracker.getConsumer(); }
	
}