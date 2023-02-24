package com.seibel.lod.core.generation;

import com.seibel.lod.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.lod.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dataObjects.fullData.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.dataObjects.transformers.LodDataBuilder;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.generation.tasks.*;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.Pos2D;
import com.seibel.lod.core.util.objects.UncheckedInterruptedException;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * @author Leetom
 * @version 2022-11-25
 */
public class WorldGenerationQueue implements Closeable
{
	public static final int SHUTDOWN_TIMEOUT_SEC = 10;
	public static final int MAX_TASKS_PROCESSED_PER_TICK = 10000;
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private final IDhApiWorldGenerator generator;
	
	/**
	 * This list contains all of the {@link WorldGenTask}'s that haven't been processed yet. <br> 
	 * These tasks may or may not be necessary or valid. <br.
	 * All valid tasks in this list will eventually be added to 
	 * the {@link WorldGenerationQueue#waitingTaskGroupsByLodPos} list (provided they aren't garbage collected first).
	 */
	private final ConcurrentLinkedQueue<WorldGenTask> looseWoldGenTasks = new ConcurrentLinkedQueue<>();
	// FIXME: Concurrency issue on close!
	// FIXME: This is using up a TONS of time to process!
	private final ConcurrentSkipListMap<DhLodPos, WorldGenTaskGroup> waitingTaskGroupsByLodPos = new ConcurrentSkipListMap<>(
			(a, b) ->
			{
				// sort based on detail level, higher detailLevels first (less detailed sections first)
				if (a.detailLevel != b.detailLevel)
				{
					return a.detailLevel - b.detailLevel;
				}
				
				// sort into layers (or sqaures) around the world origin, closer positions first // (look at the definition of chebyshev distance for an example of what this looks like)
				// TODO shouldn't we sort based on the player's position, not the world center? Although doing that could potentially cause issues with having to constantly re-sort this list
				int aDist = a.getCenterBlockPos().toPos2D().chebyshevDist(Pos2D.ZERO);
				int bDist = b.getCenterBlockPos().toPos2D().chebyshevDist(Pos2D.ZERO);
				if (aDist != bDist)
				{
					return aDist - bDist;
				}
				else if (a.x != b.x)
				{
					return a.x - b.x;
				}
				else
				{
				return a.z - b.z;
				}
			}
	); // Accessed by poller only
	
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
	
	public CompletableFuture<Boolean> submitGenTask(DhLodPos pos, byte requiredDataDetail, AbstractWorldGenTaskTracker tracker)
	{
		// if the generator is shutting down, don't add new tasks
		if (this.generatorClosingFuture != null)
		{
			return CompletableFuture.completedFuture(false);
		}
		
		if (requiredDataDetail < this.minDataDetail)
		{
			throw new UnsupportedOperationException("Current generator does not meet requiredDataDetail level");
		}
		if (requiredDataDetail > this.maxDataDetail)
		{
			requiredDataDetail = this.maxDataDetail;
		}
		
		
		LodUtil.assertTrue(pos.detailLevel > requiredDataDetail + 4);
		byte granularity = (byte) (pos.detailLevel - requiredDataDetail);
		
		
		
		
		if (granularity > this.maxGranularity)
		{
			// The generation section is too big, split it up
			
			byte subDetail = (byte) (this.maxGranularity + requiredDataDetail);
			int subPosWidthCount = pos.getBlockWidth(subDetail);
			
			DhLodPos cornerSubPos = pos.getCornerLodPos(subDetail);
			CompletableFuture<Boolean>[] subFutures = new CompletableFuture[subPosWidthCount * subPosWidthCount];
			ArrayList<WorldGenTask> subTasks = new ArrayList<>(subPosWidthCount * subPosWidthCount);
			SplitTaskTracker splitTaskTracker = new SplitTaskTracker(tracker, new CompletableFuture<>());
			
			// create the new sub-futures
			int subFutureIndex = 0;
			for (int xOffset = 0; xOffset < subPosWidthCount; xOffset++)
			{
				for (int zOffset = 0; zOffset < subPosWidthCount; zOffset++)
				{
					CompletableFuture<Boolean> subFuture = new CompletableFuture<>();
					subFutures[subFutureIndex++] = subFuture;
					subTasks.add(new WorldGenTask(cornerSubPos.addOffset(xOffset, zOffset), requiredDataDetail, splitTaskTracker, subFuture));
				}
			}
			
			CompletableFuture.allOf(subFutures).whenComplete((v, ex) -> 
			{
				if (ex != null)
				{
					splitTaskTracker.parentFuture.completeExceptionally(ex);
				}
				
				if (!splitTaskTracker.recalculateIsValid())
				{
					return; // Auto join future
				}
				
				for (CompletableFuture<Boolean> subFuture : subFutures)
				{
					boolean successful = subFuture.join();
					if (!successful)
					{
						splitTaskTracker.parentFuture.complete(false);
						return;
					}
				}
				splitTaskTracker.parentFuture.complete(true);
			});
			
			this.looseWoldGenTasks.addAll(subTasks);
			return splitTaskTracker.parentFuture;
		}
		else if (granularity < this.minGranularity)
		{
			// Too small of a chunk. We'll just over-size the generation.
			byte parentDetail = (byte) (this.minGranularity + requiredDataDetail);
			DhLodPos parentPos = pos.convertToDetailLevel(parentDetail);
			CompletableFuture<Boolean> future = new CompletableFuture<>();
			this.looseWoldGenTasks.add(new WorldGenTask(parentPos, requiredDataDetail, tracker, future));
			
			return future;
		}
		else
		{
			// the requested granularity is within the min and max granularity provided by the world generator,
			// no additional task changes are necessary
			
			CompletableFuture<Boolean> future = new CompletableFuture<>();
			this.looseWoldGenTasks.add(new WorldGenTask(pos, requiredDataDetail, tracker, future));
			return future;
		}
	}
	
	public void runCurrentGenTasksUntilBusy(DhBlockPos2D targetPos)
	{
		if (this.generator == null)
		{
			throw new IllegalStateException("generator is null");
		}
		
		
		// generate terrain until the generator is asked to stop (if the while loop wasn't done the world generator would run out of tasks and will end up idle)
		while (!this.generator.isBusy())// && !this.waitingTaskGroupsByLodPos.isEmpty())
		{
			this.removeOutdatedTaskGroups();
			this.processLooseTasks();
			this.startNextWorldGenTask(targetPos);
		}
	}
	
	/** 
	 * Removes all invalid {@link WorldGenTask}'s and {@link WorldGenTaskGroup}'s <br> 
	 * This generally happens if a worldGenTask has been garbage collected.
	 */
	private void removeOutdatedTaskGroups()
	{
		Iterator<WorldGenTaskGroup> groupIter = this.waitingTaskGroupsByLodPos.values().iterator();
		
		// go through each TaskGroup
		while (groupIter.hasNext())
		{
			// go through each WorldGenTask in the TaskGroup
			WorldGenTaskGroup taskGroup = groupIter.next();
			Iterator<WorldGenTask> taskIter = taskGroup.generatorTasks.iterator();
			while (taskIter.hasNext())
			{
				// remove this task if it has been garbage collected
				WorldGenTask task = taskIter.next();
				if (!task.taskTracker.isMemoryAddressValid())
				{
					taskIter.remove();
					task.future.complete(false);
				}
			}
			
			// remove this group if it is now empty
			if (taskGroup.generatorTasks.isEmpty())
			{
				groupIter.remove();
			}
		}
	}
	
	/** 
	 * This processes the currently available loose tasks and prepares them 
	 * so, they can actually be used for world generation.
	 */
	private void processLooseTasks()
	{
		int taskProcessed = 0;
		
		WorldGenTask task = this.looseWoldGenTasks.poll(); // using poll prevents concurrency issues where the list is cleared after asking if it was empty
		while (task != null && taskProcessed < MAX_TASKS_PROCESSED_PER_TICK)
		{
			taskProcessed++;
			byte taskDataDetail = task.dataDetailLevel;
			byte taskGranularity = (byte) (task.pos.detailLevel - taskDataDetail);
			LodUtil.assertTrue(taskGranularity >= LodUtil.CHUNK_DETAIL_LEVEL && taskGranularity >= this.minGranularity && taskGranularity <= this.maxGranularity);
			
			// Check if a task already exists for this position
			WorldGenTaskGroup existingWorldGenGroup = this.waitingTaskGroupsByLodPos.get(task.pos);
			if (existingWorldGenGroup != null)
			{
				// a task already exists for this exact position
				
				if (existingWorldGenGroup.dataDetail <= taskDataDetail)
				{
					// the existing group has an equal or lower detail level,
					// we can just append the new task to its list.
					existingWorldGenGroup.generatorTasks.add(task);
				}
				else
				{
					// the existing group has a higher detail level than this one,
					// we need to increase the existing group's detail level.
					existingWorldGenGroup.dataDetail = taskDataDetail;
					
					// remove the existing task, so it can be re-added after the necessary modifications
					boolean taskRemoved = this.waitingTaskGroupsByLodPos.remove(task.pos, existingWorldGenGroup);
					LodUtil.assertTrue(taskRemoved);
					
					// re-add the task group
					existingWorldGenGroup.generatorTasks.add(task);
					this.addAndCombineTaskGroup(existingWorldGenGroup);
				}
			}
			else
			{
				// no task group exists for this position
				
				// Check if there is one with a higher detail level
				byte granularity = taskGranularity;
				boolean addedToHigherDetailGroup = false;
				while (++granularity <= this.maxGranularity)
				{
					existingWorldGenGroup = this.waitingTaskGroupsByLodPos.get(task.pos.convertToDetailLevel((byte) (taskDataDetail + granularity)));
					if (existingWorldGenGroup != null && existingWorldGenGroup.dataDetail == taskDataDetail)
					{
						// We can just append to the higher detail level group
						existingWorldGenGroup.generatorTasks.add(task);
						addedToHigherDetailGroup = true;
						break;
					}
				}
				
				if (!addedToHigherDetailGroup)
				{
					// no higher detail group exists that we can append to,
					// create a new task group
					existingWorldGenGroup = new WorldGenTaskGroup(task.pos, taskDataDetail);
					existingWorldGenGroup.generatorTasks.add(task);
					this.addAndCombineTaskGroup(existingWorldGenGroup);
				}
			}
			
			
			// get the next task to process (will be null if the list is empty)
			task = this.looseWoldGenTasks.poll();
		}
		
		if (taskProcessed != 0)
		{
			LOGGER.info("Processed " + taskProcessed + " loose tasks");
		}
	}
	/** adds the new TaskGroup either as a new group or combines it into an existing task group */
	private void addAndCombineTaskGroup(WorldGenTaskGroup newTaskGroup)
	{
		byte newGranularity = (byte) (newTaskGroup.pos.detailLevel - newTaskGroup.dataDetail);
		LodUtil.assertTrue(newGranularity <= this.maxGranularity && newGranularity >= this.minGranularity);
		LodUtil.assertTrue(!this.waitingTaskGroupsByLodPos.containsKey(newTaskGroup.pos));
		
		// Check and merge all those who have exactly the same dataDetail, and overlap the position; but have lower granularity than us
		if (newGranularity > this.minGranularity)
		{
			// TODO: Optimize this check
			Iterator<WorldGenTaskGroup> groupIter = this.waitingTaskGroupsByLodPos.values().iterator();
			while (groupIter.hasNext())
			{
				WorldGenTaskGroup group = groupIter.next();
				if (group.dataDetail != newTaskGroup.dataDetail
					|| !group.pos.overlaps(newTaskGroup.pos))
				{
					continue;
				}
				
				// We should have already ALWAYS selected the higher granularity.
				LodUtil.assertTrue(group.pos.detailLevel < newTaskGroup.pos.detailLevel);
				groupIter.remove(); // Remove and consume all from that lower granularity request
				newTaskGroup.generatorTasks.addAll(group.generatorTasks);
			}
		}
		
		// Now, Check if we are the missing piece in the 4 quadrants, and if so, combine the four into a new higher granularity group
		if (newGranularity < this.maxGranularity)
		{ 
			// Obviously, only do so if we aren't at the maxGranularity already
			// Check for merging and upping the granularity
			DhLodPos corePos = newTaskGroup.pos;
			DhLodPos parentPos = corePos.convertToDetailLevel((byte) (corePos.detailLevel + 1));
			int targetChildId = newTaskGroup.pos.getChildIndexOfParent();
			
			boolean allPassed = true;
			for (int i = 0; i < 4; i++)
			{
				if (i == targetChildId)
					continue;
				WorldGenTaskGroup group = this.waitingTaskGroupsByLodPos.get(parentPos.getChildPosByIndex(i));
				if (group == null || group.dataDetail != newTaskGroup.dataDetail)
				{
					allPassed = false;
					break;
				}
			}
			
			if (allPassed)
			{
				LodUtil.assertTrue(!this.waitingTaskGroupsByLodPos.containsKey(parentPos) || this.waitingTaskGroupsByLodPos.get(parentPos).dataDetail != newTaskGroup.dataDetail);
				WorldGenTaskGroup[] groups = new WorldGenTaskGroup[4];
				for (int i = 0; i < 4; i++)
				{
					if (i == targetChildId)
					{
						groups[i] = newTaskGroup;
					}
					else
					{
						groups[i] = this.waitingTaskGroupsByLodPos.remove(parentPos.getChildPosByIndex(i));
					}
					LodUtil.assertTrue(groups[i] != null && groups[i].dataDetail == newTaskGroup.dataDetail);
				}
				
				WorldGenTaskGroup newGroup = this.waitingTaskGroupsByLodPos.get(parentPos);
				if (newGroup != null)
				{
					LodUtil.assertTrue(newGroup.dataDetail != newTaskGroup.dataDetail); // if it is equal, we should have been merged ages ago
					if (newGroup.dataDetail < newTaskGroup.dataDetail)
					{
						// We can just append us into the existing list.
						for (WorldGenTaskGroup g : groups)
						{
							newGroup.generatorTasks.addAll(g.generatorTasks);
						}	
					}
					else
					{
						// We need to upgrade the requested dataDetail of the group.
						newGroup.dataDetail = newTaskGroup.dataDetail;
						boolean worked = this.waitingTaskGroupsByLodPos.remove(parentPos, newGroup); // Pop it off for later proper merge check
						LodUtil.assertTrue(worked);
						for (WorldGenTaskGroup g : groups)
						{
							newGroup.generatorTasks.addAll(g.generatorTasks);
						}
						this.addAndCombineTaskGroup(newGroup); // Recursive check the new group
					}
				}
				else
				{
					// There should not be any higher granularity to check, as otherwise we would have merged ages ago
					newGroup = new WorldGenTaskGroup(parentPos, newTaskGroup.dataDetail);
					for (WorldGenTaskGroup g : groups)
					{
						newGroup.generatorTasks.addAll(g.generatorTasks);
					}
					this.addAndCombineTaskGroup(newGroup); // Recursive check the new group
				}
				
				// We have merged. So no need to add the target group
				return;
			}
		}
		
		// Finally, we should be safe to add the target group into the list
		WorldGenTaskGroup existingTaskGroup = this.waitingTaskGroupsByLodPos.put(newTaskGroup.pos, newTaskGroup);
		LodUtil.assertTrue(existingTaskGroup == null); // should never be replacing other things
	}
	
	private void startNextWorldGenTask(DhBlockPos2D targetPos)
	{
		WorldGenTaskGroup closestTaskGroup = null;
		long closestGenGroupDist = Long.MAX_VALUE;
		int lastChebshevDistToOrigin = Integer.MIN_VALUE;
		boolean continueNextRound = true;
		byte currentDetailChecking = -1;
		
		
		// Select the TaskGroup closest to the target pos with the highest detail level
		for (WorldGenTaskGroup worldGenGroup : this.waitingTaskGroupsByLodPos.values())
		{
			// the list should be sorted detailLevel first,
			// so we should break before getting to a different detail level in the list
			if (currentDetailChecking == -1)
			{
				currentDetailChecking = worldGenGroup.dataDetail;
			}
			LodUtil.assertTrue(currentDetailChecking == worldGenGroup.dataDetail);
			
			
			// look for the closest position in each given layer around the world origin
			// TODO why are we looking around the world's origin?
			int chebDistToOrigin = worldGenGroup.pos.getCenterBlockPos().toPos2D().chebyshevDist(Pos2D.ZERO);
			if (chebDistToOrigin > lastChebshevDistToOrigin)
			{
				// this worldGenGroup is 1 layer farther from the world origin
				
				if (!continueNextRound)
				{
					// We have found the best worldGenGroup, stop looking
					break;
				}
				else
				{
					continueNextRound = false;
					lastChebshevDistToOrigin = chebDistToOrigin;
				}
			}


			// is this worldGenGroup closer to the targetPos than the previous closest?
			long dist = worldGenGroup.pos.getCenterBlockPos().distSquared(targetPos);
			if (closestTaskGroup != null && dist >= closestGenGroupDist)
			{
				// this worldGenGroup is farther away
				continue;
			}
			else
			{
				// this worldGenGroup is closer than the previous closest
				closestGenGroupDist = dist;
				closestTaskGroup = worldGenGroup;
				
				continueNextRound = true;
			}
		}
		
		
		// if a new worldGenGroup was found, try starting it
		if (closestTaskGroup != null)
		{
			InProgressWorldGenTaskGroup newInProgressTask = new InProgressWorldGenTaskGroup(closestTaskGroup);
			InProgressWorldGenTaskGroup previousInProgressTask = this.inProgressGenTasksByLodPos.putIfAbsent(closestTaskGroup.pos, newInProgressTask);
			
			// Remove the selected task from the waiting list
			boolean taskRemoved = this.waitingTaskGroupsByLodPos.remove(closestTaskGroup.pos, closestTaskGroup);
			LodUtil.assertTrue(taskRemoved);
			
			if (previousInProgressTask != null)
			{
				// There is already a worldGenTask running for this position
				
				// Note: Due to concurrency reasons, even if the currently running task is compatible with 
				// 		   the newly selected task, we cannot use it,
				//         as some chunks may have already been written into.
				
				// recursively look for a different worldGenTask to start
				this.startNextWorldGenTask(targetPos);
				
				// TODO why are we putting the task back? since a compatible task is already running, why would we re-add it to the list
				WorldGenTaskGroup exchange = this.waitingTaskGroupsByLodPos.put(closestTaskGroup.pos, closestTaskGroup); // put back the task.
				LodUtil.assertTrue(exchange == null);
			}
			else
			{
				// No worldGenTask is running for this position, start one
				this.startWorldGenTaskGroup(newInProgressTask);
			}
		}
	}
	private void startWorldGenTaskGroup(InProgressWorldGenTaskGroup task)
	{
		byte dataDetail = task.group.dataDetail;
		DhLodPos pos = task.group.pos;
		byte granularity = (byte) (pos.detailLevel - dataDetail);
		LodUtil.assertTrue(granularity >= this.minGranularity && granularity <= this.maxGranularity);
		LodUtil.assertTrue(dataDetail >= this.minDataDetail && dataDetail <= this.maxDataDetail);
		
		DhChunkPos chunkPosMin = new DhChunkPos(pos.getCornerBlockPos());
		LOGGER.info("Generating section {} with granularity {} at {}", pos, granularity, chunkPosMin);
		task.genFuture = startGenerationEvent(this.generator, chunkPosMin, granularity, dataDetail, task.group::accept);
		task.genFuture.whenComplete((voidObj, ex) ->
		{
			if (ex != null)
			{
				if (!UncheckedInterruptedException.isThrowableInterruption(ex))
				{
					LOGGER.error("Error generating data for section [{}]", pos, ex);
				}
				task.group.generatorTasks.forEach(m -> m.future.complete(false));
			}
			else
			{
				LOGGER.info("Section generation at [{}] completed", pos);
				task.group.generatorTasks.forEach(m -> m.future.complete(true));
			}
			boolean worked = this.inProgressGenTasksByLodPos.remove(pos, task);
			LodUtil.assertTrue(worked);
		});
	}
	
	
	
	//==========//
	// shutdown //
	//==========//
	
	public CompletableFuture<Void> startClosing(boolean cancelCurrentGeneration, boolean alsoInterruptRunning)
	{
		this.waitingTaskGroupsByLodPos.values().forEach(g -> g.generatorTasks.forEach(t -> t.future.complete(false)));
		this.waitingTaskGroupsByLodPos.clear();
		ArrayList<CompletableFuture<Void>> array = new ArrayList<>(this.inProgressGenTasksByLodPos.size());
		this.inProgressGenTasksByLodPos.values().forEach(runningTaskGroup ->
									{
										CompletableFuture<Void> genFuture = runningTaskGroup.genFuture; // Do this to prevent it getting swapped out
										
										if (cancelCurrentGeneration)
										{
											genFuture.cancel(alsoInterruptRunning);
										}
										
										array.add(genFuture.handle((voidObj, ex) ->
										{
											if (ex instanceof CompletionException)
											{
												ex = ex.getCause();
											}
											if (!UncheckedInterruptedException.isThrowableInterruption(ex))
											{
												LOGGER.error("Error when terminating data generation for section {}", runningTaskGroup.group.pos, ex);
											}
											return null;
										}));
									});
		this.generatorClosingFuture = CompletableFuture.allOf(array.toArray(new CompletableFuture[0])); //FIXME: Closer threading issues with runCurrentGenTasksUntilBusy
		this.looseWoldGenTasks.forEach(t -> t.future.complete(false));
		this.looseWoldGenTasks.clear();
		return this.generatorClosingFuture;
	}
	
	@Override
	public void close()
	{
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
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
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
			Consumer<ChunkSizedFullDataSource> resultConsumer)
	{
		EDhApiDistantGeneratorMode generatorMode = Config.Client.WorldGenerator.distantGeneratorMode.get();
		return worldGenerator.generateChunks(chunkPosMin.x, chunkPosMin.z, granularity, targetDataDetail, generatorMode, (objectArray) ->
		{
			try
			{
				IChunkWrapper chunk = SingletonInjector.INSTANCE.get(IWrapperFactory.class).createChunkWrapper(objectArray);
				resultConsumer.accept(LodDataBuilder.createChunkData(chunk));
			}
			catch (ClassCastException e)
			{
				DhLoggerBuilder.getLogger().error("World generator return type incorrect. Error: [" + e.getMessage() + "].", e);
				Config.Client.WorldGenerator.enableDistantGeneration.set(false);
			}
		});
	}
	
}
