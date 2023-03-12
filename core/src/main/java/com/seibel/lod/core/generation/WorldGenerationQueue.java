package com.seibel.lod.core.generation;

import com.seibel.lod.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.lod.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dataObjects.fullData.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.dataObjects.transformers.LodDataBuilder;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.generation.tasks.*;
import com.seibel.lod.core.pos.*;
import com.seibel.lod.core.util.gridList.MovableGridRingList;
import com.seibel.lod.core.util.objects.QuadTree;
import com.seibel.lod.core.util.objects.UncheckedInterruptedException;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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
	
	
	
	
	public WorldGenerationQueue(IDhApiWorldGenerator generator)
	{
		this.generator = generator;
		this.maxGranularity = generator.getMaxGenerationGranularity();
		this.minGranularity = generator.getMinGenerationGranularity();
		this.maxDataDetail = generator.getMaxDataDetailLevel();
		this.minDataDetail = generator.getMinDataDetailLevel();
		
		this.waitingTaskQuadTree = new QuadTree<>(Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH, DhBlockPos2D.ZERO /*the quad tree will be re-centered later*/);
		
		
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
	
	public CompletableFuture<Boolean> submitGenTask(DhLodPos pos, byte requiredDataDetail, IWorldGenTaskTracker tracker)
	{
		// TODO implement multiple detail level generation
		if (pos.detailLevel != 6)
//		if (!(pos.detailLevel >= this.minGranularity && pos.detailLevel <= this.maxGranularity))
		{
			return CompletableFuture.completedFuture(false);
		}
		
		
		// the generator is shutting down, don't add new tasks
		if (this.generatorClosingFuture != null)
		{
			return CompletableFuture.completedFuture(false);
		}
		
		// TODO what does these checks and the assert below mean?
		if (requiredDataDetail < this.minDataDetail)
		{
			throw new UnsupportedOperationException("Current generator does not meet requiredDataDetail level");
		}
		if (requiredDataDetail > this.maxDataDetail)
		{
			requiredDataDetail = this.maxDataDetail;
		}
		
		LodUtil.assertTrue(pos.detailLevel > requiredDataDetail + LodUtil.CHUNK_DETAIL_LEVEL/*TODO is chunkDetailLevel the correct replacement? otherwise the magic number was 4*/); 
		
		
		
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		this.waitingTaskQuadTree.set(new DhSectionPos(pos.detailLevel, pos.x, pos.z), new WorldGenTask(pos, requiredDataDetail, tracker, future));
		return future;
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
			
			
			// done to prevent generating chunks where the player isn't
			this.removeOutOfRangeTasks(targetPos);
			
			// generate terrain until the generator is asked to stop (if the while loop wasn't done the world generator would run out of tasks and will end up idle)
			boolean taskStarted = true;
			while (!this.generator.isBusy() && taskStarted)// && !this.waitingTaskGroupsByLodPos.isEmpty()) // TODO add !isEmpty()
			{
				this.removeGarbageCollectedTasks();
				taskStarted = this.startNextWorldGenTask(targetPos);
			}
		}
		catch (Exception e)
		{
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	private void removeOutOfRangeTasks(DhBlockPos2D targetBlockPos)
	{
		AtomicInteger numberOfTasksRemoved = new AtomicInteger();
		
		this.waitingTaskQuadTree.setCenterPos(targetBlockPos, (worldGenTask) -> { numberOfTasksRemoved.getAndIncrement(); });
		
//		if (numberOfTasksRemoved.get() != 0)
//		{
//			LOGGER.info(numberOfTasksRemoved.get()+" world gen tasks removed.");
//		}
	}
	
	/** Removes all {@link WorldGenTask}'s and {@link WorldGenTaskGroup}'s that have been garbage collected. */
	private void removeGarbageCollectedTasks() // TODO remove, potential mystery errors caused by garbage collection isn't worth it (and may not be necessary any more now that we are using a quad tree to hold the tasks)
	{
		for (byte detailLevel = QuadTree.TREE_LOWEST_DETAIL_LEVEL; detailLevel < this.waitingTaskQuadTree.treeMaxDetailLevel; detailLevel++)
		{
			MovableGridRingList<WorldGenTask> gridRingList = this.waitingTaskQuadTree.getRingList(detailLevel);
			Iterator<WorldGenTask> taskIterator = gridRingList.iterator();
			while (taskIterator.hasNext())
			{
				// go through each WorldGenTask in the TaskGroup
				WorldGenTask genTask = taskIterator.next();
				if (genTask != null && !genTask.taskTracker.isMemoryAddressValid())
				{
					taskIterator.remove();
					genTask.future.complete(false);
				}
			}
		}
	}
	
	/** @param targetPos the position to center the generation around */
	private boolean startNextWorldGenTask(DhBlockPos2D targetPos)
	{
		WorldGenTask closestTask = null;
		
		// look through the tree from lowest to highest detail level to find the next task to generate
		for (byte detailLevel = QuadTree.TREE_LOWEST_DETAIL_LEVEL; detailLevel < this.waitingTaskQuadTree.treeMaxDetailLevel; detailLevel++)
		{
			// look for the task that is closest to the targetPos
			long closestGenDist = Long.MAX_VALUE;
			
			MovableGridRingList<WorldGenTask> gridRingList = this.waitingTaskQuadTree.getRingList(detailLevel);
			for (WorldGenTask newGenTask : gridRingList)
			{
				if (newGenTask != null)
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
			
			// a task has been found, don't look at the next detail level,
			// everything there will be farther away
			if (closestTask != null)
			{
				break;
			}
		}
		
		
		
		if (closestTask == null)
		{
			// no task was found, this probably means there isn't anything left to generate
			return false;
		}
		
		
		
		// remove the task we found, we are going to start it and don't want to run it multiple times
		WorldGenTask removedWorldGenTask = this.waitingTaskQuadTree.set(closestTask.pos.detailLevel, closestTask.pos.x, closestTask.pos.z, null);
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
			// detail level is (probably) too high, split up the task
			LodUtil.assertTrue(closestTask == removedWorldGenTask); // should be the same memory address, removedWorldGenTask shouldn't be null // TODO why shouldn't it be null?
			
			
			// split up the task and add each one to the tree
			DhSectionPos sectionPos = new DhSectionPos(closestTask.pos.detailLevel, closestTask.pos.x, closestTask.pos.z);
			sectionPos.forEachChild((childDhSectionPos) -> 
			{
				WorldGenTask newGenTask = new WorldGenTask(new DhLodPos(childDhSectionPos.sectionDetailLevel, childDhSectionPos.sectionX, childDhSectionPos.sectionZ), childDhSectionPos.sectionDetailLevel, removedWorldGenTask.taskTracker, removedWorldGenTask.future /*TODO probably need to do something about the futures here*/);
				this.waitingTaskQuadTree.set(childDhSectionPos.sectionDetailLevel, childDhSectionPos.sectionX, childDhSectionPos.sectionZ, newGenTask);
			});
			
			// return true so we attempt to generate again
			return true;
		}
	}
	private void startWorldGenTaskGroup(InProgressWorldGenTaskGroup task)
	{
		byte taskDetailLevel = task.group.dataDetail;
		DhLodPos taskPos = task.group.pos;
		byte granularity = (byte) (taskPos.detailLevel - taskDetailLevel);
		LodUtil.assertTrue(granularity >= this.minGranularity && granularity <= this.maxGranularity);
		LodUtil.assertTrue(taskDetailLevel >= this.minDataDetail && taskDetailLevel <= this.maxDataDetail);
		
		DhChunkPos chunkPosMin = new DhChunkPos(taskPos.getCornerBlockPos());
		LOGGER.info("Generating section "+taskPos+" with granularity "+granularity+" at "+chunkPosMin);
		
		task.genFuture = startGenerationEvent(this.generator, chunkPosMin, granularity, taskDetailLevel, task.group::onGenerationComplete);
		task.genFuture.whenComplete((voidObj, exception) ->
		{
			if (exception != null)
			{
				// don't log the shutdown exceptions
				if (!UncheckedInterruptedException.isThrowableInterruption(exception) && !(exception instanceof CancellationException || exception.getCause() instanceof CancellationException))
				{
					LOGGER.error("Error generating data for section "+taskPos, exception);
				}
				
				task.group.worldGenTasks.forEach(worldGenTask -> worldGenTask.future.complete(false));
			}
			else
			{
				//LOGGER.info("Section generation at "+pos+" completed");
				task.group.worldGenTasks.forEach(worldGenTask -> worldGenTask.future.complete(true));
			}
			boolean worked = this.inProgressGenTasksByLodPos.remove(taskPos, task);
			LodUtil.assertTrue(worked);
		});
	}
	
	
	
	//==========//
	// shutdown //
	//==========//
	
	public CompletableFuture<Void> startClosing(boolean cancelCurrentGeneration, boolean alsoInterruptRunning)
	{
		for (byte detailLevel = QuadTree.TREE_LOWEST_DETAIL_LEVEL; detailLevel < this.waitingTaskQuadTree.treeMaxDetailLevel; detailLevel++)
		{
			// TODO remove
//			Iterator<WorldGenTask> ringListIterator = this.waitingTaskQuadTree.getRingList(detailLevel).iterator();
//			while (ringListIterator.hasNext())
//			{
//				WorldGenTask worldGenTask = ringListIterator.next();
//				if (worldGenTask != null)
//				{
//					try
//					{
//						worldGenTask.future.cancel(true);
//					}
//					catch (CancellationException ignored)
//					{ /* don't log shutdown exceptions */ }
//				}
//			}
			
			// TODO shouldn't I clear the list? not just cancel each item?
			MovableGridRingList<WorldGenTask> ringList = this.waitingTaskQuadTree.getRingList(detailLevel);
			ringList.clear((worldGenTask) ->
			{
				if (worldGenTask != null)
				{
					try
					{
						worldGenTask.future.cancel(true);
					}
					catch (CancellationException ignored)
					{ /* don't log shutdown exceptions */ }
				}
			});
		}
		
		
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
		this.generatorClosingFuture = CompletableFuture.allOf(inProgressTasksCancelingFutures.toArray(new CompletableFuture[0])); //FIXME: Closer threading issues with runCurrentGenTasksUntilBusy
		
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
