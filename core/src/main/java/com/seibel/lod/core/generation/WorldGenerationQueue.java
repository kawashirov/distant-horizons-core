package com.seibel.lod.core.generation;

import com.seibel.lod.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.lod.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.datatype.transform.LodDataBuilder;
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
	
	private final ConcurrentLinkedQueue<WorldGenTask> looseTasks = new ConcurrentLinkedQueue<>();
	// FIXME: Concurrency issue on close!
	// FIXME: This is using up a TONS of time to process!
	private final ConcurrentSkipListMap<DhLodPos, WorldGenTaskGroup> taskGroups = new ConcurrentSkipListMap<>(
			(a, b) -> {
				if (a.detailLevel != b.detailLevel)
					return a.detailLevel - b.detailLevel;
				int aDist = a.getCenter().toPos2D().chebyshevDist(Pos2D.ZERO);
				int bDist = b.getCenter().toPos2D().chebyshevDist(Pos2D.ZERO);
				if (aDist != bDist)
					return aDist - bDist;
				if (a.x != b.x)
					return a.x - b.x;
				return a.z - b.z;
			}
	); // Accessed by poller only
	
	private final ConcurrentHashMap<DhLodPos, InProgressWorldGenTaskGroup> inProgress = new ConcurrentHashMap<>();
	
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
			
			DhLodPos cornerSubPos = pos.getCorner(subDetail);
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
			
			this.looseTasks.addAll(subTasks);
			return splitTaskTracker.parentFuture;
		}
		else if (granularity < this.minGranularity)
		{
			// Too small of a chunk. We'll just over-size the generation.
			byte parentDetail = (byte) (this.minGranularity + requiredDataDetail);
			DhLodPos parentPos = pos.convertToDetailLevel(parentDetail);
			CompletableFuture<Boolean> future = new CompletableFuture<>();
			this.looseTasks.add(new WorldGenTask(parentPos, requiredDataDetail, tracker, future));
			
			return future;
		}
		else
		{
			// the requested granularity is within the min and max granularity provided by the world generator,
			// no additional task changes are necessary
			
			CompletableFuture<Boolean> future = new CompletableFuture<>();
			this.looseTasks.add(new WorldGenTask(pos, requiredDataDetail, tracker, future));
			return future;
		}
	}
	
	private void addAndCombineGroup(WorldGenTaskGroup target)
	{
		byte granularity = (byte) (target.pos.detailLevel - target.dataDetail);
		LodUtil.assertTrue(granularity <= this.maxGranularity && granularity >= this.minGranularity);
		LodUtil.assertTrue(!this.taskGroups.containsKey(target.pos));
		
		// Check and merge all those who has exactly the same dataDetail, and overlaps the position, but have lower granularity than us
		if (granularity > this.minGranularity)
		{
			// TODO: Optimize this check
			Iterator<WorldGenTaskGroup> groupIter = this.taskGroups.values().iterator();
			while (groupIter.hasNext())
			{
				WorldGenTaskGroup group = groupIter.next();
				if (group.dataDetail != target.dataDetail)
					continue;
				if (!group.pos.overlaps(target.pos))
					continue;
				
				// We should have already ALWAYS selected the higher granularity.
				LodUtil.assertTrue(group.pos.detailLevel < target.pos.detailLevel);
				groupIter.remove(); // Remove and consume all from that lower granularity request
				target.generatorTasks.addAll(group.generatorTasks);
			}
		}
		
		// Now, Check if we are the missing piece in the 4 quadrants, and if so, combine the four into a new higher granularity group
		if (granularity < this.maxGranularity)
		{ // Obviously, only do so if we aren't at the maxGranularity already
			// Check for merging and upping the granularity
			DhLodPos corePos = target.pos;
			DhLodPos parentPos = corePos.convertToDetailLevel((byte) (corePos.detailLevel + 1));
			int targetChildId = target.pos.getChildIndexOfParent();
			boolean allPassed = true;
			for (int i = 0; i < 4; i++)
			{
				if (i == targetChildId)
					continue;
				WorldGenTaskGroup group = this.taskGroups.get(parentPos.getChildByIndex(i));
				if (group == null || group.dataDetail != target.dataDetail)
				{
					allPassed = false;
					break;
				}
			}
			if (allPassed)
			{
				LodUtil.assertTrue(!this.taskGroups.containsKey(parentPos) || this.taskGroups.get(parentPos).dataDetail != target.dataDetail);
				WorldGenTaskGroup[] groups = new WorldGenTaskGroup[4];
				for (int i = 0; i < 4; i++)
				{
					if (i == targetChildId)
						groups[i] = target;
					else
						groups[i] = this.taskGroups.remove(parentPos.getChildByIndex(i));
					LodUtil.assertTrue(groups[i] != null && groups[i].dataDetail == target.dataDetail);
				}
				
				WorldGenTaskGroup newGroup = this.taskGroups.get(parentPos);
				if (newGroup != null)
				{
					LodUtil.assertTrue(newGroup.dataDetail != target.dataDetail); // if it is equal, we should have been merged ages ago
					if (newGroup.dataDetail < target.dataDetail)
					{
						// We can just append us into the existing list.
						for (WorldGenTaskGroup g : groups)
							newGroup.generatorTasks.addAll(g.generatorTasks);
					}
					else
					{
						// We need to upgrade the requested dataDetail of the group.
						newGroup.dataDetail = target.dataDetail;
						boolean worked = this.taskGroups.remove(parentPos, newGroup); // Pop it off for later proper merge check
						LodUtil.assertTrue(worked);
						for (WorldGenTaskGroup g : groups)
							newGroup.generatorTasks.addAll(g.generatorTasks);
						this.addAndCombineGroup(newGroup); // Recursive check the new group
					}
				}
				else
				{
					// There should not be any higher granularity to check, as otherwise we would have merged ages ago
					newGroup = new WorldGenTaskGroup(parentPos, target.dataDetail);
					for (WorldGenTaskGroup g : groups)
						newGroup.generatorTasks.addAll(g.generatorTasks);
					this.addAndCombineGroup(newGroup); // Recursive check the new group
				}
				return; // We have merged. So no need to add the target group
			}
		}
		
		// Finally, we should be safe to add the target group into the list
		WorldGenTaskGroup v = this.taskGroups.put(target.pos, target);
		LodUtil.assertTrue(v == null); // should never be replacing other things
	}
	
	private void processLooseTasks()
	{
		int taskProcessed = 0;
		
		WorldGenTask task = this.looseTasks.poll(); // using poll prevents concurrency issues where the list is cleared after asking if it was empty
		while (task != null && taskProcessed < MAX_TASKS_PROCESSED_PER_TICK)
		{
			taskProcessed++;
			byte taskDataDetail = task.dataDetailLevel;
			byte taskGranularity = (byte) (task.pos.detailLevel - taskDataDetail);
			LodUtil.assertTrue(taskGranularity >= LodUtil.CHUNK_DETAIL_LEVEL && taskGranularity >= this.minGranularity && taskGranularity <= this.maxGranularity);
			
			// Check existing one
			WorldGenTaskGroup group = this.taskGroups.get(task.pos);
			if (group != null)
			{
				if (group.dataDetail <= taskDataDetail)
				{
					// We can just append us into the existing list.
					group.generatorTasks.add(task);
				}
				else
				{
					// We need to upgrade the requested dataDetail of the group.
					group.dataDetail = taskDataDetail;
					boolean worked = this.taskGroups.remove(task.pos, group); // Pop it off for later proper merge check
					LodUtil.assertTrue(worked);
					group.generatorTasks.add(task);
					this.addAndCombineGroup(group);
				}
			}
			else
			{
				
				// Check higher granularity one
				byte granularity = taskGranularity;
				boolean didAnything = false;
				while (++granularity <= this.maxGranularity)
				{
					group = this.taskGroups.get(task.pos.convertToDetailLevel((byte) (taskDataDetail + granularity)));
					if (group != null && group.dataDetail == taskDataDetail)
					{
						// We can just append to the higher granularity group one
						group.generatorTasks.add(task);
						didAnything = true;
						break;
					}
				}
				
				if (!didAnything)
				{
					group = new WorldGenTaskGroup(task.pos, taskDataDetail);
					group.generatorTasks.add(task);
					this.addAndCombineGroup(group);
				}
			}
			
			// get the next task to process (will be null if the list is empty)
			task = this.looseTasks.poll();
		}
		
		if (taskProcessed != 0)
		{
			LOGGER.info("Processed " + taskProcessed + " loose tasks");
		}
		
	}
	
	private void removeOutdatedGroups()
	{
		// Remove all invalid genTasks and groups
		Iterator<WorldGenTaskGroup> groupIter = this.taskGroups.values().iterator();
		
		// go through each TaskGroup
		while (groupIter.hasNext())
		{
			// go through each WorldGenTask in the TaskGroup
			WorldGenTaskGroup group = groupIter.next();
			Iterator<WorldGenTask> taskIter = group.generatorTasks.iterator();
			while (taskIter.hasNext())
			{
				WorldGenTask task = taskIter.next();
				if (!task.taskTracker.isMemoryAddressValid())
				{
					taskIter.remove();
					task.future.complete(false);
				}
			}
			
			if (group.generatorTasks.isEmpty())
				groupIter.remove();
		}
	}
	
	private void pollAndStartNext(DhBlockPos2D targetPos)
	{
		// Select the one with the highest data detail level and closest to the target pos
		WorldGenTaskGroup best = null;
		long cachedDist = Long.MAX_VALUE;
		int lastChebDist = Integer.MIN_VALUE;
		boolean continueNextRound = true;
		byte currentDetailChecking = -1;
		
		for (WorldGenTaskGroup group : this.taskGroups.values())
		{
			if (currentDetailChecking == -1)
				currentDetailChecking = group.dataDetail;
			LodUtil.assertTrue(currentDetailChecking == group.dataDetail);
			int chebDistToOrigin = group.pos.getCenter().toPos2D().chebyshevDist(Pos2D.ZERO);
			if (chebDistToOrigin > lastChebDist)
			{
				if (!continueNextRound)
					break; // We have found the best one
				continueNextRound = false;
				lastChebDist = chebDistToOrigin;
			}
			long dist = group.pos.getCenter().distSquared(targetPos);
			if (best != null && dist >= cachedDist)
				continue;
			cachedDist = dist;
			best = group;
			continueNextRound = true;
		}
		
		if (best != null)
		{
			InProgressWorldGenTaskGroup startedTask = new InProgressWorldGenTaskGroup(best);
			InProgressWorldGenTaskGroup casTask = this.inProgress.putIfAbsent(best.pos, startedTask);
			boolean worked = this.taskGroups.remove(best.pos, best); // Remove the selected task from the group
			LodUtil.assertTrue(worked);
			if (casTask != null)
			{
				// Note: Due to concurrency reasons, even if the currently running task is compatible with selected task,
				//         we cannot use it, as some chunks may have already been written into.
				this.pollAndStartNext(targetPos); // Poll next one.
				WorldGenTaskGroup exchange = this.taskGroups.put(best.pos, best); // put back the task.
				LodUtil.assertTrue(exchange == null);
			}
			else
			{
				this.startTaskGroup(startedTask);
			}
		}
		
	}
	
	public void pollAndStartClosest(DhBlockPos2D targetPos)
	{
		if (this.generator == null)
		{
			throw new IllegalStateException("generator is null");
		}
		if (this.generator.isBusy())
		{
			// don't accept new requests if busy
			return;
		}
		
		
		// generate terrain until the generator is asked to stop (if the while loop wasn't done the world generator wouldn't have enough tasks`)
		while (!this.generator.isBusy())
		{
			this.removeOutdatedGroups();
			this.processLooseTasks();
			this.pollAndStartNext(targetPos);
		}
	}
	
	private void startTaskGroup(InProgressWorldGenTaskGroup task)
	{
		byte dataDetail = task.group.dataDetail;
		DhLodPos pos = task.group.pos;
		byte granularity = (byte) (pos.detailLevel - dataDetail);
		LodUtil.assertTrue(granularity >= this.minGranularity && granularity <= this.maxGranularity);
		LodUtil.assertTrue(dataDetail >= this.minDataDetail && dataDetail <= this.maxDataDetail);
		
		DhChunkPos chunkPosMin = new DhChunkPos(pos.getCorner());
		LOGGER.info("Generating section {} with granularity {} at {}", pos, granularity, chunkPosMin);
		task.genFuture = startGenerationEvent(this.generator, chunkPosMin, granularity, dataDetail, task.group::accept);
		task.genFuture.whenComplete((v, ex) -> {
			if (ex != null)
			{
				if (!UncheckedInterruptedException.isThrowableInterruption(ex))
					LOGGER.error("Error generating data for section {}", pos, ex);
				task.group.generatorTasks.forEach(m -> m.future.complete(false));
			}
			else
			{
				LOGGER.info("Section generation at {} complated", pos);
				task.group.generatorTasks.forEach(m -> m.future.complete(true));
			}
			boolean worked = inProgress.remove(pos, task);
			LodUtil.assertTrue(worked);
		});
	}
	
	
	
	//==========//
	// shutdown //
	//==========//
	
	public CompletableFuture<Void> startClosing(boolean cancelCurrentGeneration, boolean alsoInterruptRunning)
	{
		this.taskGroups.values().forEach(g -> g.generatorTasks.forEach(t -> t.future.complete(false)));
		this.taskGroups.clear();
		ArrayList<CompletableFuture<Void>> array = new ArrayList<>(this.inProgress.size());
		this.inProgress.values().forEach(runningTask ->
									{
										CompletableFuture<Void> genFuture = runningTask.genFuture; // Do this to prevent it getting swapped out
										if (cancelCurrentGeneration)
											genFuture.cancel(alsoInterruptRunning);
										array.add(genFuture.handle((v, ex) -> {
											if (ex instanceof CompletionException)
												ex = ex.getCause();
											if (!UncheckedInterruptedException.isThrowableInterruption(ex))
												LOGGER.error("Error when terminating data generation for section {}", runningTask.group.pos, ex);
											return null;
										}));
									});
		this.generatorClosingFuture = CompletableFuture.allOf(array.toArray(CompletableFuture[]::new)); //FIXME: Closer threading issues with pollAndStartClosest
		this.looseTasks.forEach(t -> t.future.complete(false));
		this.looseTasks.clear();
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
			this.generatorClosingFuture.orTimeout(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS).join();
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
			Consumer<ChunkSizedData> resultConsumer)
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
