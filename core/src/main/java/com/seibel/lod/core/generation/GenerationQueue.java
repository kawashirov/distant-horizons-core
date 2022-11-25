package com.seibel.lod.core.generation;

import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.Pos2D;
import com.seibel.lod.core.util.objects.UncheckedInterruptedException;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * @version 2022-11-25
 */
public class GenerationQueue implements Closeable
{
	public static final int SHUTDOWN_TIMEOUT_SEC = 10;
	public static final int MAX_TASKS_PROCESSED_PER_TICK = 10000;
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
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
	
	public static abstract class GenTaskTracker
	{
		public abstract boolean isValid();
		
		public abstract Consumer<ChunkSizedData> getConsumer();
	}
	
	final IGenerator generator;
	
	static final class GenTask
	{
		final DhLodPos pos;
		final byte dataDetail;
		final GenTaskTracker taskTracker;
		final CompletableFuture<Boolean> future;
		
		GenTask(DhLodPos pos, byte dataDetail, GenTaskTracker taskTracker, CompletableFuture<Boolean> future)
		{
			this.dataDetail = dataDetail;
			this.pos = pos;
			this.taskTracker = taskTracker;
			this.future = future;
		}
	}
	
	static final class TaskGroup
	{
		final DhLodPos pos;
		byte dataDetail;
		final LinkedList<GenTask> members = new LinkedList<>(); // Accessed by gen poller thread only
		
		TaskGroup(DhLodPos pos, byte dataDetail)
		{
			this.pos = pos;
			this.dataDetail = dataDetail;
		}
		
		void accept(ChunkSizedData data)
		{
			Iterator<GenTask> iter = this.members.iterator();
			while (iter.hasNext())
			{
				GenTask task = iter.next();
				Consumer<ChunkSizedData> consumer = task.taskTracker.getConsumer();
				if (consumer == null)
				{
					iter.remove();
					task.future.complete(false);
				}
				else
				{
					consumer.accept(data);
				}
			}
		}
	}
	
	static final class InProgressTask
	{
		final TaskGroup group;
		CompletableFuture<Void> genFuture = null;
		
		InProgressTask(TaskGroup group)
		{
			this.group = group;
		}
	}
	
	static class SplitTask extends GenTaskTracker
	{
		final GenTaskTracker parentTracker;
		final CompletableFuture<Boolean> parentFuture;
		boolean cachedValid = true;
		
		SplitTask(GenTaskTracker parentTracker, CompletableFuture<Boolean> parentFuture)
		{
			this.parentTracker = parentTracker;
			this.parentFuture = parentFuture;
		}
		
		boolean recheckState()
		{
			if (!this.cachedValid)
				return false;
			
			this.cachedValid = this.parentTracker.isValid();
			if (!this.cachedValid)
				this.parentFuture.complete(false);
			
			return this.cachedValid;
		}
		
		@Override
		public boolean isValid() { return this.cachedValid; }
		
		@Override
		public Consumer<ChunkSizedData> getConsumer() { return this.parentTracker.getConsumer(); }
		
	}
	
	private final ConcurrentLinkedQueue<GenTask> looseTasks = new ConcurrentLinkedQueue<>();
	// FIXME: Concurrency issue on close!
	// FIXME: This is using up a TONS of time to process!
	private final ConcurrentSkipListMap<DhLodPos, TaskGroup> taskGroups = new ConcurrentSkipListMap<>(
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
	
	private final ConcurrentHashMap<DhLodPos, InProgressTask> inProgress = new ConcurrentHashMap<>();
	
	private final byte maxGranularity;
	private final byte minGranularity;
	private final byte maxDataDetail;
	private final byte minDataDetail;
	private volatile CompletableFuture<Void> closer = null;
	
	public GenerationQueue(IGenerator generator)
	{
		this.generator = generator;
		this.maxGranularity = generator.getMaxGenerationGranularity();
		this.minGranularity = generator.getMinGenerationGranularity();
		this.maxDataDetail = generator.getMaxDataDetail();
		this.minDataDetail = generator.getMinDataDetail();
		
		if (this.minGranularity < 4)
			throw new IllegalArgumentException("DH-IGenerator: min granularity must be at least 4!");
		if (this.maxGranularity < this.minGranularity)
			throw new IllegalArgumentException("DH-IGenerator: max granularity smaller than min granularity!");
	}
	
	public CompletableFuture<Boolean> submitGenTask(DhLodPos pos, byte requiredDataDetail, GenTaskTracker tracker)
	{
		if (this.closer != null)
			return CompletableFuture.completedFuture(false);
		
		if (requiredDataDetail < this.minDataDetail)
		{
			throw new UnsupportedOperationException("Current generator does not meet requiredDataDetail level");
		}
		if (requiredDataDetail > this.maxDataDetail)
			requiredDataDetail = this.maxDataDetail;
		
		LodUtil.assertTrue(pos.detailLevel > requiredDataDetail + 4);
		byte granularity = (byte) (pos.detailLevel - requiredDataDetail);
		
		if (granularity > this.maxGranularity)
		{
			// Too big of a chunk. We need to split it up
			byte subDetail = (byte) (this.maxGranularity + requiredDataDetail);
			int subPosCount = pos.getBlockWidth(subDetail);
			DhLodPos cornerSubPos = pos.getCorner(subDetail);
			CompletableFuture<Boolean>[] subFutures = new CompletableFuture[subPosCount * subPosCount];
			ArrayList<GenTask> subTasks = new ArrayList<>(subPosCount * subPosCount);
			SplitTask splitTask = new SplitTask(tracker, new CompletableFuture<>());
			{
				int i = 0;
				for (int ox = 0; ox < subPosCount; ox++)
				{
					for (int oz = 0; oz < subPosCount; oz++)
					{
						CompletableFuture<Boolean> subFuture = new CompletableFuture<>();
						subFutures[i++] = subFuture;
						subTasks.add(new GenTask(cornerSubPos.addOffset(ox, oz), requiredDataDetail, splitTask, subFuture));
					}
				}
			}
			CompletableFuture.allOf(subFutures).whenComplete((v, ex) -> {
				if (ex != null)
					splitTask.parentFuture.completeExceptionally(ex);
				if (!splitTask.recheckState())
					return; // Auto join future
				for (CompletableFuture<Boolean> subFuture : subFutures)
				{
					boolean successful = subFuture.join();
					if (!successful)
					{
						splitTask.parentFuture.complete(false);
						return;
					}
				}
				splitTask.parentFuture.complete(true);
			});
			this.looseTasks.addAll(subTasks);
			if (this.closer != null)
				return CompletableFuture.completedFuture(false);
			else
				return splitTask.parentFuture;
		}
		else if (granularity < this.minGranularity)
		{
			// Too small of a chunk. We'll just over-size the generation.
			byte parentDetail = (byte) (this.minGranularity + requiredDataDetail);
			DhLodPos parentPos = pos.convertUpwardsTo(parentDetail);
			CompletableFuture<Boolean> future = new CompletableFuture<>();
			this.looseTasks.add(new GenTask(parentPos, requiredDataDetail, tracker, future));
			if (this.closer != null)
				return CompletableFuture.completedFuture(false);
			else
				return future;
		}
		else
		{
			CompletableFuture<Boolean> future = new CompletableFuture<>();
			this.looseTasks.add(new GenTask(pos, requiredDataDetail, tracker, future));
			if (this.closer != null)
				return CompletableFuture.completedFuture(false);
			else
				return future;
		}
	}
	
	private void addAndCombineGroup(TaskGroup target)
	{
		byte granularity = (byte) (target.pos.detailLevel - target.dataDetail);
		LodUtil.assertTrue(granularity <= this.maxGranularity && granularity >= this.minGranularity);
		LodUtil.assertTrue(!this.taskGroups.containsKey(target.pos));
		
		// Check and merge all those who has exactly the same dataDetail, and overlaps the position, but have lower granularity than us
		if (granularity > this.minGranularity)
		{
			// TODO: Optimize this check
			Iterator<TaskGroup> groupIter = this.taskGroups.values().iterator();
			while (groupIter.hasNext())
			{
				TaskGroup group = groupIter.next();
				if (group.dataDetail != target.dataDetail)
					continue;
				if (!group.pos.overlaps(target.pos))
					continue;
				
				// We should have already ALWAYS selected the higher granularity.
				LodUtil.assertTrue(group.pos.detailLevel < target.pos.detailLevel);
				groupIter.remove(); // Remove and consume all from that lower granularity request
				target.members.addAll(group.members);
			}
		}
		
		// Now, Check if we are the missing piece in the 4 quadrants, and if so, combine the four into a new higher granularity group
		if (granularity < this.maxGranularity)
		{ // Obviously, only do so if we aren't at the maxGranularity already
			// Check for merging and upping the granularity
			DhLodPos corePos = target.pos;
			DhLodPos parentPos = corePos.convertUpwardsTo((byte) (corePos.detailLevel + 1));
			int targetChildId = target.pos.getChildIndexOfParent();
			boolean allPassed = true;
			for (int i = 0; i < 4; i++)
			{
				if (i == targetChildId)
					continue;
				TaskGroup group = this.taskGroups.get(parentPos.getChildByIndex(i));
				if (group == null || group.dataDetail != target.dataDetail)
				{
					allPassed = false;
					break;
				}
			}
			if (allPassed)
			{
				LodUtil.assertTrue(!this.taskGroups.containsKey(parentPos) || this.taskGroups.get(parentPos).dataDetail != target.dataDetail);
				TaskGroup[] groups = new TaskGroup[4];
				for (int i = 0; i < 4; i++)
				{
					if (i == targetChildId)
						groups[i] = target;
					else
						groups[i] = this.taskGroups.remove(parentPos.getChildByIndex(i));
					LodUtil.assertTrue(groups[i] != null && groups[i].dataDetail == target.dataDetail);
				}
				
				TaskGroup newGroup = this.taskGroups.get(parentPos);
				if (newGroup != null)
				{
					LodUtil.assertTrue(newGroup.dataDetail != target.dataDetail); // if it is equal, we should have been merged ages ago
					if (newGroup.dataDetail < target.dataDetail)
					{
						// We can just append us into the existing list.
						for (TaskGroup g : groups)
							newGroup.members.addAll(g.members);
					}
					else
					{
						// We need to upgrade the requested dataDetail of the group.
						newGroup.dataDetail = target.dataDetail;
						boolean worked = this.taskGroups.remove(parentPos, newGroup); // Pop it off for later proper merge check
						LodUtil.assertTrue(worked);
						for (TaskGroup g : groups)
							newGroup.members.addAll(g.members);
						this.addAndCombineGroup(newGroup); // Recursive check the new group
					}
				}
				else
				{
					// There should not be any higher granularity to check, as otherwise we would have merged ages ago
					newGroup = new TaskGroup(parentPos, target.dataDetail);
					for (TaskGroup g : groups)
						newGroup.members.addAll(g.members);
					this.addAndCombineGroup(newGroup); // Recursive check the new group
				}
				return; // We have merged. So no need to add the target group
			}
		}
		
		// Finally, we should be safe to add the target group into the list
		TaskGroup v = this.taskGroups.put(target.pos, target);
		LodUtil.assertTrue(v == null); // should never be replacing other things
	}
	
	private void processLooseTasks()
	{
		int taskProcessed = 0;
		while (!this.looseTasks.isEmpty() && taskProcessed < MAX_TASKS_PROCESSED_PER_TICK)
		{
			GenTask task = this.looseTasks.poll();
			taskProcessed++;
			byte taskDataDetail = task.dataDetail;
			byte taskGranularity = (byte) (task.pos.detailLevel - taskDataDetail);
			LodUtil.assertTrue(taskGranularity >= 4 && taskGranularity >= this.minGranularity && taskGranularity <= this.maxGranularity);
			
			// Check existing one
			TaskGroup group = this.taskGroups.get(task.pos);
			if (group != null)
			{
				if (group.dataDetail <= taskDataDetail)
				{
					// We can just append us into the existing list.
					group.members.add(task);
				}
				else
				{
					// We need to upgrade the requested dataDetail of the group.
					group.dataDetail = taskDataDetail;
					boolean worked = this.taskGroups.remove(task.pos, group); // Pop it off for later proper merge check
					LodUtil.assertTrue(worked);
					group.members.add(task);
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
					group = this.taskGroups.get(task.pos.convertUpwardsTo((byte) (taskDataDetail + granularity)));
					if (group != null && group.dataDetail == taskDataDetail)
					{
						// We can just append to the higher granularity group one
						group.members.add(task);
						didAnything = true;
						break;
					}
				}
				if (!didAnything)
				{
					group = new TaskGroup(task.pos, taskDataDetail);
					group.members.add(task);
					this.addAndCombineGroup(group);
				}
			}
		}
		
		if (taskProcessed != 0)
		{
			LOGGER.info("Processed " + taskProcessed + " loose tasks");
		}
		
	}
	
	private void removeOutdatedGroups()
	{
		// Remove all invalid genTasks and groups
		Iterator<TaskGroup> groupIter = this.taskGroups.values().iterator();
		while (groupIter.hasNext())
		{
			TaskGroup group = groupIter.next();
			Iterator<GenTask> taskIter = group.members.iterator();
			while (taskIter.hasNext())
			{
				GenTask task = taskIter.next();
				if (!task.taskTracker.isValid())
				{
					taskIter.remove();
					task.future.complete(false);
				}
			}
			
			if (group.members.isEmpty())
				groupIter.remove();
		}
	}
	
	private void pollAndStartNext(DhBlockPos2D targetPos)
	{
		// Select the one with the highest data detail level and closest to the target pos
		TaskGroup best = null;
		long cachedDist = Long.MAX_VALUE;
		int lastChebDist = Integer.MIN_VALUE;
		boolean continueNextRound = true;
		byte currentDetailChecking = -1;
		
		for (TaskGroup group : this.taskGroups.values())
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
			InProgressTask startedTask = new InProgressTask(best);
			InProgressTask casTask = this.inProgress.putIfAbsent(best.pos, startedTask);
			boolean worked = this.taskGroups.remove(best.pos, best); // Remove the selected task from the group
			LodUtil.assertTrue(worked);
			if (casTask != null)
			{
				// Note: Due to concurrency reasons, even if the currently running task is compatible with selected task,
				//         we cannot use it, as some chunks may have already been written into.
				this.pollAndStartNext(targetPos); // Poll next one.
				TaskGroup exchange = this.taskGroups.put(best.pos, best); // put back the task.
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
			throw new IllegalStateException("generator is null");
		if (this.generator.isBusy())
			return;
		this.removeOutdatedGroups();
		this.processLooseTasks();
		this.pollAndStartNext(targetPos);
	}
	
	private void startTaskGroup(InProgressTask task)
	{
		byte dataDetail = task.group.dataDetail;
		DhLodPos pos = task.group.pos;
		byte granularity = (byte) (pos.detailLevel - dataDetail);
		LodUtil.assertTrue(granularity >= this.minGranularity && granularity <= this.maxGranularity);
		LodUtil.assertTrue(dataDetail >= this.minDataDetail && dataDetail <= this.maxDataDetail);
		
		DhChunkPos chunkPosMin = new DhChunkPos(pos.getCorner());
		LOGGER.info("Generating section {} with granularity {} at {}", pos, granularity, chunkPosMin);
		task.genFuture = this.generator.generate(
				chunkPosMin, granularity, dataDetail, task.group::accept);
		task.genFuture.whenComplete((v, ex) -> {
			if (ex != null)
			{
				if (!UncheckedInterruptedException.isThrowableInterruption(ex))
					LOGGER.error("Error generating data for section {}", pos, ex);
				task.group.members.forEach(m -> m.future.complete(false));
			}
			else
			{
				LOGGER.info("Section generation at {} complated", pos);
				task.group.members.forEach(m -> m.future.complete(true));
			}
			boolean worked = inProgress.remove(pos, task);
			LodUtil.assertTrue(worked);
		});
	}
	
	public CompletableFuture<Void> startClosing(boolean cancelCurrentGeneration, boolean alsoInterruptRunning)
	{
		this.taskGroups.values().forEach(g -> g.members.forEach(t -> t.future.complete(false)));
		this.taskGroups.clear();
		ArrayList<CompletableFuture<Void>> array = new ArrayList<>(inProgress.size());
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
		this.closer = CompletableFuture.allOf(array.toArray(CompletableFuture[]::new)); //FIXME: Closer threading issues with pollAndStartClosest
		this.looseTasks.forEach(t -> t.future.complete(false));
		this.looseTasks.clear();
		return this.closer;
	}
	
	@Override
	public void close()
	{
		if (this.closer == null)
			this.startClosing(true, true);
		LodUtil.assertTrue(this.closer != null);
		try
		{
			this.closer.orTimeout(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS).join();
		}
		catch (Throwable e)
		{
			LOGGER.error("Failed to close generation queue: ", e);
		}
	}
}
