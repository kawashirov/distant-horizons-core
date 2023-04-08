package com.seibel.lod.core.generation;

import com.seibel.lod.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.lod.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dataObjects.fullData.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.dataObjects.transformers.LodDataBuilder;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.generation.tasks.*;
import com.seibel.lod.core.pos.*;
import com.seibel.lod.core.util.ThreadUtil;
import com.seibel.lod.core.util.objects.quadTree.QuadNode;
import com.seibel.lod.core.util.objects.quadTree.QuadTree;
import com.seibel.lod.core.util.objects.UncheckedInterruptedException;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class WorldGenerationQueue implements Closeable
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private final IDhApiWorldGenerator generator;
	
	/** contains the positions that need to be generated */
	private final QuadTree<WorldGenTask> waitingTaskQuadTree;
	
	private final ConcurrentHashMap<DhLodPos, InProgressWorldGenTaskGroup> inProgressGenTasksByLodPos = new ConcurrentHashMap<>();
	
	// granularity is the detail level for batching world generator requests together
	private final byte maxGranularity;
	private final byte minGranularity;
	
	private final byte maxDataDetail;
	private final byte minDataDetail;
	
	/** If not null this generator is in the process of shutting down */
	private volatile CompletableFuture<Void> generatorClosingFuture = null;
	
	// TODO this logic isn't great and can cause a limit to how many threads could be used for world generation, 
	//  however it won't cause duplicate requests or concurrency issues, so it will be good enough for now.
	//  A good long term fix may be to either:
	//  1. allow the generator to deal with larger sections (let the generator threads split up larger tasks into smaller one
	//  2. batch requests better. instead of sending 4 individual tasks of detail level N, send 1 task of detail level n+1
	private final ExecutorService queueingThread = ThreadUtil.makeSingleThreadPool("World Gen Queue");
	private boolean generationQueueStarted = false;
	private DhBlockPos2D generationTargetPos = DhBlockPos2D.ZERO;
	/** can be used for debugging how many tasks are currently in the queue */
	private int numberOfTasksQueued = 0;
	/** 
	 * Settings this to true will cause the system to queue the first generation request it can find, 
	 * this improves generation thread feeding efficiency, but can potentially cause generation requests to queue out of order (IE they may not be closest to farthest). <br><br>
	 * 
	 * Setting this to false will cause the system to queue the closest generation request it can find. <br>
	 * This will reduce generation queuing efficiency.
	 */
	private boolean queueFirstGenerationRequestFound = true; 
	
	
	
	public WorldGenerationQueue(IDhApiWorldGenerator generator)
	{
		this.generator = generator;
		this.maxGranularity = generator.getMaxGenerationGranularity();
		this.minGranularity = generator.getMinGenerationGranularity();
		this.maxDataDetail = generator.getMaxDataDetailLevel();
		this.minDataDetail = generator.getMinDataDetailLevel();
		
		int treeWidth = Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH * 2; // TODO the *2 is to allow for generation edge cases, and should probably be removed at some point
		byte treeMinDetailLevel = LodUtil.BLOCK_DETAIL_LEVEL; // the tree shouldn't need to go this low, but just in case
		this.waitingTaskQuadTree = new QuadTree<>(treeWidth, DhBlockPos2D.ZERO /*the quad tree will be re-centered later*/, treeMinDetailLevel);
		
		
		if (this.minGranularity < LodUtil.CHUNK_DETAIL_LEVEL)
		{
			throw new IllegalArgumentException(IDhApiWorldGenerator.class.getSimpleName() + ": min granularity must be at least 4 (Chunk sized)!");
		}
		if (this.maxGranularity < this.minGranularity)
		{
			throw new IllegalArgumentException(IDhApiWorldGenerator.class.getSimpleName() + ": max granularity smaller than min granularity!");
		}
	}
	
	
	
	//=================//
	// world generator //
	// task handling   //
	//=================//
	
	public CompletableFuture<WorldGenResult> submitGenTask(DhLodPos pos, byte requiredDataDetail, IWorldGenTaskTracker tracker)
	{
		// the generator is shutting down, don't add new tasks
		if (this.generatorClosingFuture != null)
		{
			return CompletableFuture.completedFuture(WorldGenResult.CreateFail());
		}
		
		
		// make sure the generator can provide the requested position
		if (requiredDataDetail < this.minDataDetail)
		{
			throw new UnsupportedOperationException("Current generator does not meet requiredDataDetail level");
		}
		if (requiredDataDetail > this.maxDataDetail)
		{
			requiredDataDetail = this.maxDataDetail;
		}
		
		// TODO what does this assert mean?
		LodUtil.assertTrue(pos.detailLevel > requiredDataDetail + LodUtil.CHUNK_DETAIL_LEVEL/*TODO is chunkDetailLevel the correct replacement? otherwise the magic number was 4*/); 
		
		
		DhSectionPos requestPos = new DhSectionPos(pos.detailLevel, pos.x, pos.z);
		if (this.waitingTaskQuadTree.isSectionPosInBounds(requestPos))
		{
			CompletableFuture<WorldGenResult> future = new CompletableFuture<>();
			this.waitingTaskQuadTree.setValue(requestPos, new WorldGenTask(pos, requiredDataDetail, tracker, future));
			return future;
		}
		else
		{
			return CompletableFuture.completedFuture(WorldGenResult.CreateFail());
		}
	}
	
	
	//===============//
	// running tasks //
	//===============//
	
	public void runCurrentGenTasksUntilBusy(DhBlockPos2D targetPos)
	{
		try
		{
			if (this.generator == null)
			{
				throw new IllegalStateException("generator is null");
			}
			
			// the generator is shutting down, don't attempt to generate anything
			if (this.generatorClosingFuture != null)
			{
				return;
			}
			
			
			// update the target pos
			this.generationTargetPos = targetPos;
			
			// only start the queuing thread once
			if (!generationQueueStarted)
			{
				startWorldGenQueuingThread();
			}
		}
		catch (Exception e)
		{
			LOGGER.error(e.getMessage(), e);
		}
	}
	private void startWorldGenQueuingThread()
	{
		this.generationQueueStarted = true;
		
		// queue world generation tasks on its own thread since this process is very slow and would lag the server thread
		this.queueingThread.execute(() ->
		{
			try
			{
				// loop until the generator is shutdown
				while (!Thread.interrupted())
				{
//					LOGGER.info("pre task count: " + this.numberOfTasksQueued);
					
					// recenter the generator tasks, this is done to prevent generating chunks where the player isn't
					this.waitingTaskQuadTree.setCenterBlockPos(this.generationTargetPos);
					
					// queue generation tasks until the generator is full, or there are no more tasks to generate
					boolean taskStarted = true;
					while (!this.generator.isBusy() && taskStarted)
					{
						//this.removeGarbageCollectedTasks(); // TODO this is extremely slow
						taskStarted = this.startNextWorldGenTask(this.generationTargetPos);
						if (!taskStarted)
						{
							int debugPointOne = 0;
						}
					}
					
					
//					LOGGER.info("after task count: " + this.numberOfTasksQueued);
					
					// if there aren't any new tasks, wait a second before checking again // TODO replace with a listener instead
					Thread.sleep(1000);
				}
			}
			catch (InterruptedException e)
			{
				/* do nothing, this means the thread is being shut down */
			}
			catch (Exception e)
			{
				LOGGER.error("queueing exception: "+e.getMessage(), e);
				generationQueueStarted = false;
			}
		});
	}
	
//	/** Removes all {@link WorldGenTask}'s and {@link WorldGenTaskGroup}'s that have been garbage collected. */
//	private void removeGarbageCollectedTasks() // TODO remove, potential mystery errors caused by garbage collection isn't worth it (and may not be necessary any more now that we are using a quad tree to hold the tasks). // also this is very slow with the curent quad tree impelmentation
//	{
//		for (byte detailLevel = QuadTree.TREE_LOWEST_DETAIL_LEVEL; detailLevel < this.waitingTaskQuadTree.treeMaxDetailLevel; detailLevel++)
//		{
//			MovableGridRingList<WorldGenTask> gridRingList = this.waitingTaskQuadTree.getRingList(detailLevel);
//			Iterator<WorldGenTask> taskIterator = gridRingList.iterator();
//			while (taskIterator.hasNext())
//			{
//				// go through each WorldGenTask in the TaskGroup
//				WorldGenTask genTask = taskIterator.next();
//				if (genTask != null && !genTask.taskTracker.isMemoryAddressValid())
//				{
//					taskIterator.remove();
//					genTask.future.complete(WorldGenResult.CreateFail());
//				}
//			}
//		}
//	}
	
	/** 
	 * @param targetPos the position to center the generation around 
	 * @return false if no tasks were found to generate
	 */
	private boolean startNextWorldGenTask(DhBlockPos2D targetPos)
	{
		long closestGenDist = Long.MAX_VALUE;
		
		WorldGenTask closestTask = null;
		
		// TODO improve, having to go over every item isn't super efficient
		Iterator<QuadNode<WorldGenTask>> leafNodeIterator = this.waitingTaskQuadTree.leafNodeIterator();
		while (leafNodeIterator.hasNext())
		{
			WorldGenTask newGenTask = leafNodeIterator.next().value;
			if (newGenTask != null) // TODO add an option to skip leaves with null values and potentially auto-prune them
			{
				
				// use chebyShev distance in order to generate in rings around the target pos (also because it is a fast distance calculation)
				int chebDistToTargetPos = newGenTask.pos.getCenterBlockPos().toPos2D().chebyshevDist(targetPos.toPos2D());
				if (chebDistToTargetPos < closestGenDist)
				{
					// this task is closer than the last one
					closestTask = newGenTask;
					closestGenDist = chebDistToTargetPos;
				}
			}
		}
		
		if (closestTask == null)
		{
			// no task was found, this probably means there isn't anything left to generate
			return false;
		}
		
		
		
		// remove the task we found, we are going to start it and don't want to run it multiple times
		WorldGenTask removedWorldGenTask = this.waitingTaskQuadTree.setValue(new DhSectionPos(closestTask.pos.detailLevel, closestTask.pos.x, closestTask.pos.z), null);
		// removedWorldGenTask can be null // TODO when? 
		
		
		// do we need to modify this task to generate it?
		if(canGeneratePos((byte) 0, closestTask.pos)) // TODO should 0 be replaced?
		{
			// detail level is correct for generation, start generation
			
			WorldGenTaskGroup closestTaskGroup = new WorldGenTaskGroup(closestTask.pos, (byte) 0);  // TODO should 0 be replaced?
			closestTaskGroup.worldGenTasks.add(closestTask); // TODO
			
			InProgressWorldGenTaskGroup newInProgressTask = new InProgressWorldGenTaskGroup(closestTaskGroup);
			InProgressWorldGenTaskGroup previousInProgressTask = this.inProgressGenTasksByLodPos.putIfAbsent(closestTask.pos, newInProgressTask);
			if (previousInProgressTask == null)
			{
				// no task exists for this position, start one
				this.startWorldGenTaskGroup(newInProgressTask);	
			}
			else
			{
				// TODO replace the previous inProgress task if one exists
				// Note: Due to concurrency reasons, even if the currently running task is compatible with 
				// 		   the newly selected task, we cannot use it,
				//         as some chunks may have already been written into.
			}
			
			// a task has been started
			return true;
		}
		else
		{
			// detail level is too high (if the detail level was too low, the generator would've ignored the request),
			// split up the task
			
			// make sure that we have a task to split up
			LodUtil.assertTrue(closestTask == removedWorldGenTask);
			
			
			// split up the task and add each one to the tree
			LinkedList<CompletableFuture<WorldGenResult>> childFutures = new LinkedList<>();
			DhSectionPos sectionPos = new DhSectionPos(closestTask.pos.detailLevel, closestTask.pos.x, closestTask.pos.z);
			sectionPos.forEachChild((childDhSectionPos) -> 
			{
				CompletableFuture<WorldGenResult> newFuture = new CompletableFuture<>();
				childFutures.add(newFuture);
						
				WorldGenTask newGenTask = new WorldGenTask(new DhLodPos(childDhSectionPos.sectionDetailLevel, childDhSectionPos.sectionX, childDhSectionPos.sectionZ), childDhSectionPos.sectionDetailLevel, removedWorldGenTask.taskTracker, newFuture);
				this.waitingTaskQuadTree.setValue(new DhSectionPos(childDhSectionPos.sectionDetailLevel, childDhSectionPos.sectionX, childDhSectionPos.sectionZ), newGenTask);
				
				boolean valueAdded = this.waitingTaskQuadTree.getValue(new DhSectionPos(childDhSectionPos.sectionDetailLevel, childDhSectionPos.sectionX, childDhSectionPos.sectionZ)) != null;
				LodUtil.assertTrue(valueAdded); // failed to add world gen task to quad tree, this means the quad tree was the wrong size
				
//				LOGGER.info("split feature "+sectionPos+" into "+childDhSectionPos+" "+(valueAdded ? "added" : "notAdded"));
			});
			
			// send the child futures to the future recipient, to notify them of the new tasks
			removedWorldGenTask.future.complete(WorldGenResult.CreateSplit(childFutures));
			
			
			// return true so we attempt to generate again
			return true;
		}
	}
	private void startWorldGenTaskGroup(InProgressWorldGenTaskGroup inProgressTaskGroup)
	{
		byte taskDetailLevel = inProgressTaskGroup.group.dataDetail;
		DhLodPos taskPos = inProgressTaskGroup.group.pos;
		byte granularity = (byte) (taskPos.detailLevel - taskDetailLevel);
		LodUtil.assertTrue(granularity >= this.minGranularity && granularity <= this.maxGranularity);
		LodUtil.assertTrue(taskDetailLevel >= this.minDataDetail && taskDetailLevel <= this.maxDataDetail);
		
		DhChunkPos chunkPosMin = new DhChunkPos(taskPos.getCornerBlockPos());
		LOGGER.info("Generating section "+taskPos+" with granularity "+granularity+" at "+chunkPosMin);
		
		this.numberOfTasksQueued++;
		inProgressTaskGroup.genFuture = startGenerationEvent(this.generator, chunkPosMin, granularity, taskDetailLevel, inProgressTaskGroup.group::onGenerationComplete);
		inProgressTaskGroup.genFuture.whenComplete((voidObj, exception) ->
		{
			this.numberOfTasksQueued--;
			if (exception != null)
			{
				// don't log the shutdown exceptions
				if (!UncheckedInterruptedException.isThrowableInterruption(exception) && !(exception instanceof CancellationException || exception.getCause() instanceof CancellationException))
				{
					LOGGER.error("Error generating data for section "+taskPos, exception);
				}
				
				inProgressTaskGroup.group.worldGenTasks.forEach(worldGenTask -> worldGenTask.future.complete(WorldGenResult.CreateFail()));
			}
			else
			{
				//LOGGER.info("Section generation at "+pos+" completed");
				inProgressTaskGroup.group.worldGenTasks.forEach(worldGenTask -> worldGenTask.future.complete(WorldGenResult.CreateSuccess(new DhSectionPos(granularity, taskPos))));
			}
			boolean worked = this.inProgressGenTasksByLodPos.remove(taskPos, inProgressTaskGroup);
			LodUtil.assertTrue(worked);
		});
	}
	
	
	
	//==========//
	// shutdown //
	//==========//
	
	public CompletableFuture<Void> startClosing(boolean cancelCurrentGeneration, boolean alsoInterruptRunning)
	{
		queueingThread.shutdownNow();
		
		// remove any incomplete generation tasks
//		for (byte detailLevel = QuadTree.TREE_LOWEST_DETAIL_LEVEL; detailLevel < this.waitingTaskQuadTree.treeMaxDetailLevel; detailLevel++)
//		{
//			MovableGridRingList<WorldGenTask> ringList = this.waitingTaskQuadTree.getRingList(detailLevel);
//			ringList.clear((worldGenTask) ->
//			{
//				if (worldGenTask != null)
//				{
//					try
//					{
//						worldGenTask.future.cancel(true);
//					}
//					catch (CancellationException ignored)
//					{ /* don't log shutdown exceptions */ }
//				}
//			});
//		}
		
		
		// stop and remove any in progress tasks
		ArrayList<CompletableFuture<Void>> inProgressTasksCancelingFutures = new ArrayList<>(this.inProgressGenTasksByLodPos.size());
		this.inProgressGenTasksByLodPos.values().forEach(runningTaskGroup ->
			{
				CompletableFuture<Void> genFuture = runningTaskGroup.genFuture; // Do this to prevent it getting swapped out
				
				if (cancelCurrentGeneration)
				{
					genFuture.cancel(alsoInterruptRunning);
				}
				
				inProgressTasksCancelingFutures.add(genFuture.handle((voidObj, exception) ->
				{
					if (exception instanceof CompletionException)
					{
						exception = exception.getCause();
					}
					
					if (!UncheckedInterruptedException.isThrowableInterruption(exception) && !(exception instanceof CancellationException))
					{
						LOGGER.error("Error when terminating data generation for section "+runningTaskGroup.group.pos, exception);
					}
					
					return null;
				}));
			});
		this.generatorClosingFuture = CompletableFuture.allOf(inProgressTasksCancelingFutures.toArray(new CompletableFuture[0]));
		
		return this.generatorClosingFuture;
	}
	
	@Override
	public void close()
	{
		LOGGER.info("Closing "+WorldGenerationQueue.class.getSimpleName()+"...");
		
		if (this.generatorClosingFuture == null)
		{
			this.startClosing(true, true);
		}
		LodUtil.assertTrue(this.generatorClosingFuture != null);
		
		try
		{
			this.generatorClosingFuture.cancel(true);
		}
		catch (Throwable e)
		{
			LOGGER.error("Failed to close generation queue: ", e);
		}
		
		LOGGER.info("Successfully closed "+WorldGenerationQueue.class.getSimpleName());
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	private boolean canGeneratePos(byte worldGenTaskGroupDetailLevel /*when in doubt use 0*/, DhLodPos taskPos)
	{
		byte granularity = (byte) (taskPos.detailLevel - worldGenTaskGroupDetailLevel);
		return (granularity >= this.minGranularity && granularity <= this.maxGranularity);
	}
	
	/**
	 * Source: <a href="https://stackoverflow.com/questions/3706219/algorithm-for-iterating-over-an-outward-spiral-on-a-discrete-2d-grid-from-the-or">...</a>
	 * Description: Left-upper semi-diagonal (0-4-16-36-64) contains squared layer number (4 * layer^2).
	 * External if-statement defines layer and finds (pre-)result for position in corresponding row or
	 * column of left-upper semi-plane, and internal if-statement corrects result for mirror position.
	 */
	private static int gridSpiralIndexing(int X, int Y)
	{
		int index = 0;
		if (X * X >= Y * Y)
		{
			index = 4 * X * X - X - Y;
			if (X < Y)
				index = index - 2 * (X - Y);
		}
		else
		{
			index = 4 * Y * Y - X - Y;
			if (X < Y)
				index = index + 2 * (X - Y);
		}
		
		return index;
	}
	
	
	/**
	 * The chunkPos is always aligned to the granularity.
	 * For example: if the granularity is 4 (chunk sized) with a data detail level of 0 (block sized), the chunkPos will be aligned to 16x16 blocks.
	 */
	private static CompletableFuture<Void> startGenerationEvent(IDhApiWorldGenerator worldGenerator,
			DhChunkPos chunkPosMin,
			byte granularity, byte targetDataDetail,
			Consumer<ChunkSizedFullDataSource> generationCompleteConsumer)
	{
		EDhApiDistantGeneratorMode generatorMode = Config.Client.WorldGenerator.distantGeneratorMode.get();
		return worldGenerator.generateChunks(chunkPosMin.x, chunkPosMin.z, granularity, targetDataDetail, generatorMode, (objectArray) ->
		{
			try
			{
				IChunkWrapper chunk = SingletonInjector.INSTANCE.get(IWrapperFactory.class).createChunkWrapper(objectArray);
				generationCompleteConsumer.accept(LodDataBuilder.createChunkData(chunk));
			}
			catch (ClassCastException e)
			{
				DhLoggerBuilder.getLogger().error("World generator return type incorrect. Error: [" + e.getMessage() + "].", e);
				Config.Client.WorldGenerator.enableDistantGeneration.set(false);
			}
		});
	}
	
}
