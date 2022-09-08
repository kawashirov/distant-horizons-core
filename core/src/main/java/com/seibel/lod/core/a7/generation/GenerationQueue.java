package com.seibel.lod.core.a7.generation;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.a7.util.UncheckedInterruptedException;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class GenerationQueue implements Closeable {
    public static final int SHUTDOWN_TIMEOUT_SEC = 10;


    private final Logger logger = DhLoggerBuilder.getLogger();
    public static abstract class GenTaskTracker {
        public abstract boolean isValid();
        public abstract Consumer<ChunkSizedData> getConsumer();
    }

    final IGenerator generator;
    static final class GenTask {
        final DhLodPos pos;
        final byte dataDetail;
        final GenTaskTracker taskTracker;
        final CompletableFuture<Boolean> future;
        GenTask(DhLodPos pos, byte dataDetail, GenTaskTracker taskTracker, CompletableFuture<Boolean> future) {
            this.dataDetail = dataDetail;
            this.pos = pos;
            this.taskTracker = taskTracker;
            this.future = future;
        }
    }
    static final class TaskGroup {
        final DhLodPos pos;
        byte dataDetail;
        final LinkedList<GenTask> members = new LinkedList<>(); // Accessed by gen poller thread only
        TaskGroup(DhLodPos pos, byte dataDetail) {
            this.pos = pos;
            this.dataDetail = dataDetail;
        }

        void accept(ChunkSizedData data) {
            Iterator<GenTask> iter = members.iterator();
            while (iter.hasNext()) {
                GenTask task = iter.next();
                Consumer<ChunkSizedData> consumer = task.taskTracker.getConsumer();
                if (consumer == null) {
                    iter.remove();
                    task.future.complete(false);
                } else {
                    consumer.accept(data);
                }
            }
        }
    }
    static final class InProgressTask {
        final TaskGroup group;
        CompletableFuture<Void> genFuture = null;
        InProgressTask(TaskGroup group) {
            this.group = group;
        }
    }

    static class SplitTask extends GenTaskTracker {
        final GenTaskTracker parentTracker;
        final CompletableFuture<Boolean> parentFuture;
        boolean cachedValid = true;
        SplitTask(GenTaskTracker parentTracker, CompletableFuture<Boolean> parentFuture) {
            this.parentTracker = parentTracker;
            this.parentFuture = parentFuture;
        }
        boolean recheckState() {
            if (!cachedValid) return false;
            cachedValid = parentTracker.isValid();
            if (!cachedValid) parentFuture.complete(false);
            return cachedValid;
        }
        @Override
        public boolean isValid() {
            return cachedValid;
        }
        @Override
        public Consumer<ChunkSizedData> getConsumer() {
            return parentTracker.getConsumer();
        }
    }

    private final ConcurrentLinkedQueue<GenTask> looseTasks = new ConcurrentLinkedQueue<>();
    private final HashMap<DhLodPos, TaskGroup> taskGroups = new HashMap<>(); // Accessed by poller only
    private final ConcurrentHashMap<DhLodPos, InProgressTask> inProgress = new ConcurrentHashMap<>();

    private final byte maxGranularity;
    private final byte minGranularity;
    private final byte maxDataDetail;
    private final byte minDataDetail;
    private volatile CompletableFuture<Void> closer = null;

    public GenerationQueue(IGenerator generator) {
        this.generator = generator;
        maxGranularity = generator.getMaxGenerationGranularity();
        minGranularity = generator.getMinGenerationGranularity();
        maxDataDetail = generator.getMaxDataDetail();
        minDataDetail = generator.getMinDataDetail();
        if (minGranularity < 4) throw new IllegalArgumentException("DH-IGenerator: min granularity must be at least 4!");
        if (maxGranularity < minGranularity) throw new IllegalArgumentException("DH-IGenerator: max granularity smaller than min granularity!");
    }

    public CompletableFuture<Boolean> submitGenTask(DhLodPos pos, byte requiredDataDetail, GenTaskTracker tracker) {
        if (closer != null) return CompletableFuture.completedFuture(false);
        if (requiredDataDetail < minDataDetail) {
            throw new UnsupportedOperationException("Current generator does not meet requiredDataDetail level");
        }
        if (requiredDataDetail > maxDataDetail) requiredDataDetail = maxDataDetail;

        LodUtil.assertTrue(pos.detail > requiredDataDetail+4);
        byte granularity = (byte) (pos.detail - requiredDataDetail);

        if (granularity > maxGranularity) {
            // Too big of a chunk. We need to split it up
            byte subDetail = (byte) (maxGranularity + requiredDataDetail);
            int subPosCount = pos.getWidth(subDetail);
            DhLodPos cornerSubPos = pos.getCorner(subDetail);
            CompletableFuture<Boolean>[] subFutures = new CompletableFuture[subPosCount*subPosCount];
            ArrayList<GenTask> subTasks = new ArrayList<>(subPosCount*subPosCount);
            SplitTask splitTask = new SplitTask(tracker, new CompletableFuture<>());
            {
                int i = 0;
                for (int ox = 0; ox < subPosCount; ox++) {
                    for (int oz = 0; oz < subPosCount; oz++) {
                        CompletableFuture<Boolean> subFuture = new CompletableFuture<>();
                        subFutures[i++] = subFuture;
                        subTasks.add(new GenTask(cornerSubPos.offset(ox, oz), requiredDataDetail, splitTask, subFuture));
                    }
                }
            }
            CompletableFuture.allOf(subFutures).whenComplete((v,ex) -> {
                if (ex != null) splitTask.parentFuture.completeExceptionally(ex);
                if (!splitTask.recheckState()) return; // Auto join future
                for (CompletableFuture<Boolean> subFuture: subFutures) {
                    boolean successful = subFuture.join();
                    if (!successful) {
                        splitTask.parentFuture.complete(false);
                        return;
                    }
                }
                splitTask.parentFuture.complete(true);
            });
            looseTasks.addAll(subTasks);
            if (closer != null) return CompletableFuture.completedFuture(false);
            else return splitTask.parentFuture;
        } else if (granularity < minGranularity) {
            // Too small of a chunk. We'll just over-size the generation.
            byte parentDetail = (byte) (minGranularity + requiredDataDetail);
            DhLodPos parentPos = pos.convertUpwardsTo(parentDetail);
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            looseTasks.add(new GenTask(parentPos, requiredDataDetail, tracker, future));
            if (closer != null) return CompletableFuture.completedFuture(false);
            else return future;
        } else {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            looseTasks.add(new GenTask(pos, requiredDataDetail, tracker, future));
            if (closer != null) return CompletableFuture.completedFuture(false);
            else return future;
        }
    }

    private void addAndCombineGroup(TaskGroup target) {
        byte granularity = (byte) (target.pos.detail - target.dataDetail);
        LodUtil.assertTrue(granularity <= maxGranularity && granularity >= minGranularity);
        LodUtil.assertTrue(!taskGroups.containsKey(target.pos));

        // Check and merge all those who has exactly the same dataDetail, and overlaps the position, but have lower granularity than us
        if (granularity > minGranularity) {
            // TODO: Optimize this check
            Iterator<TaskGroup> groupIter = taskGroups.values().iterator();
            while (groupIter.hasNext()) {
                TaskGroup group = groupIter.next();
                if (group.dataDetail != target.dataDetail) continue;
                if (!group.pos.overlaps(target.pos)) continue;

                // We should have already ALWAYS selected the higher granularity.
                LodUtil.assertTrue(group.pos.detail < target.pos.detail);
                groupIter.remove(); // Remove and consume all from that lower granularity request
                target.members.addAll(group.members);
            }
        }

        // Now, Check if we are the missing piece in the 4 quadrants, and if so, combine the four into a new higher granularity group
        if (granularity < maxGranularity) { // Obviously, only do so if we aren't at the maxGranularity already
            // Check for merging and upping the granularity
            DhLodPos corePos = target.pos;
            DhLodPos parentPos = corePos.convertUpwardsTo((byte) (corePos.detail+1));
            int targetChildId = target.pos.getChildIndexOfParent();
            boolean allPassed = true;
            for (int i = 0; i < 4; i++) {
                if (i == targetChildId) continue;
                TaskGroup group = taskGroups.get(parentPos.getChild(i));
                if (group == null || group.dataDetail != target.dataDetail) {
                    allPassed = false;
                    break;
                }
            }
            if (allPassed) {
                LodUtil.assertTrue(!taskGroups.containsKey(parentPos) || taskGroups.get(parentPos).dataDetail != target.dataDetail);
                TaskGroup[] groups = new TaskGroup[4];
                for (int i = 0; i < 4; i++) {
                    if (i==targetChildId) groups[i] = target;
                    else groups[i] = taskGroups.remove(parentPos.getChild(i));
                    LodUtil.assertTrue(groups[i] != null && groups[i].dataDetail == target.dataDetail);
                }

                TaskGroup newGroup = taskGroups.get(parentPos);
                if (newGroup != null) {
                    LodUtil.assertTrue(newGroup.dataDetail != target.dataDetail); // if it is equal, we should have been merged ages ago
                    if (newGroup.dataDetail < target.dataDetail) {
                        // We can just append us into the existing list.
                        for (TaskGroup g : groups) newGroup.members.addAll(g.members);
                    } else {
                        // We need to upgrade the requested dataDetail of the group.
                        newGroup.dataDetail = target.dataDetail;
                        boolean worked = taskGroups.remove(parentPos, newGroup); // Pop it off for later proper merge check
                        LodUtil.assertTrue(worked);
                        for (TaskGroup g : groups) newGroup.members.addAll(g.members);
                        addAndCombineGroup(newGroup); // Recursive check the new group
                    }
                } else {
                    // There should not be any higher granularity to check, as otherwise we would have merged ages ago
                    newGroup = new TaskGroup(parentPos, target.dataDetail);
                    for (TaskGroup g : groups) newGroup.members.addAll(g.members);
                    addAndCombineGroup(newGroup); // Recursive check the new group
                }
                return; // We have merged. So no need to add the target group
            }
        }

        // Finally, we should be safe to add the target group into the list
        TaskGroup v = taskGroups.put(target.pos, target);
        LodUtil.assertTrue(v == null); // should never be replacing other things
    }

    private void processLooseTasks() {
        while (!looseTasks.isEmpty()) {
            GenTask task = looseTasks.poll();
            byte taskDataDetail = task.dataDetail;
            byte taskGranularity = (byte) (task.pos.detail - taskDataDetail);
            LodUtil.assertTrue(taskGranularity >= 4 && taskGranularity >= minGranularity && taskGranularity <= maxGranularity);

            // Check existing one
            TaskGroup group = taskGroups.get(task.pos);
            if (group != null) {
                if (group.dataDetail <= taskDataDetail) {
                    // We can just append us into the existing list.
                    group.members.add(task);
                } else {
                    // We need to upgrade the requested dataDetail of the group.
                    group.dataDetail = taskDataDetail;
                    boolean worked = taskGroups.remove(task.pos, group); // Pop it off for later proper merge check
                    LodUtil.assertTrue(worked);
                    group.members.add(task);
                    addAndCombineGroup(group);
                }
            } else {

                // Check higher granularity one
                byte granularity = taskGranularity;
                boolean didAnything = false;
                while (++granularity <= maxGranularity) {
                    group = taskGroups.get(task.pos.convertUpwardsTo((byte) (taskDataDetail + granularity)));
                    if (group != null && group.dataDetail == taskDataDetail) {
                        // We can just append to the higher granularity group one
                        group.members.add(task);
                        didAnything = true;
                        break;
                    }
                }
                if (!didAnything) {
                    group = new TaskGroup(task.pos, taskDataDetail);
                    group.members.add(task);
                    addAndCombineGroup(group);
                }
            }

        }


    }

    private void removeOutdatedGroups() {
        // Remove all invalid genTasks and groups
        Iterator<TaskGroup> groupIter = taskGroups.values().iterator();
        while (groupIter.hasNext()) {
            TaskGroup group = groupIter.next();
            Iterator<GenTask> taskIter = group.members.iterator();
            while (taskIter.hasNext()) {
                GenTask task = taskIter.next();
                if (!task.taskTracker.isValid()) {
                    taskIter.remove();
                    task.future.complete(false);
                }
            }
            if (group.members.isEmpty()) groupIter.remove();
        }
    }

    private void pollAndStartNext(DhBlockPos2D targetPos) {
        // Select the one with the highest data detail level and closest to the target pos
        TaskGroup best = null;
        long cachedDist = Long.MAX_VALUE;
        for (TaskGroup group : taskGroups.values()) {
            if (best != null) {
                if (group.dataDetail < best.dataDetail) continue;
                long dist = group.pos.getCenter().distSquared(targetPos);
                if (cachedDist <= dist) continue;
                cachedDist = dist;
            }
            best = group;
        }
        if (best != null) {
            InProgressTask startedTask = new InProgressTask(best);
            InProgressTask casTask = inProgress.putIfAbsent(best.pos, startedTask);
            boolean worked = taskGroups.remove(best.pos, best); // Remove the selected task from the group
            LodUtil.assertTrue(worked);
            if (casTask != null) {
                // Note: Due to concurrency reasons, even if the currently running task is compatible with selected task,
                //         we cannot use it, as some chunks may have already been written into.
                pollAndStartNext(targetPos); // Poll next one.
                TaskGroup exchange = taskGroups.put(best.pos, best); // put back the task.
                LodUtil.assertTrue(exchange == null);
            } else {
                startTaskGroup(startedTask);
            }
        }

    }

    public void pollAndStartClosest(DhBlockPos2D targetPos) {
        if (generator == null) throw new IllegalStateException("generator is null");
        if (generator.isBusy()) return;
        removeOutdatedGroups();
        processLooseTasks();
        pollAndStartNext(targetPos);
    }

    private void startTaskGroup(InProgressTask task) {
        byte dataDetail = task.group.dataDetail;
        DhLodPos pos = task.group.pos;
        byte granularity = (byte) (pos.detail - dataDetail);
        LodUtil.assertTrue(granularity >= minGranularity && granularity <= maxGranularity);
        LodUtil.assertTrue(dataDetail >= minDataDetail && dataDetail <= maxDataDetail);

        DHChunkPos chunkPosMin = new DHChunkPos(pos.getCorner());
        logger.info("Generating section {} with granularity {} at {}", pos, granularity, chunkPosMin);
        task.genFuture = generator.generate(
                chunkPosMin, granularity, dataDetail, task.group::accept);
        task.genFuture.whenComplete((v, ex) -> {
           if (ex != null) {
               if (!UncheckedInterruptedException.isThrowableInterruption(ex))
                   logger.error("Error generating data for section {}", pos, ex);
               task.group.members.forEach(m -> m.future.complete(false));
           } else {
               logger.info("Section generation at {} complated", pos);
               task.group.members.forEach(m -> m.future.complete(true));
           }
           boolean worked = inProgress.remove(pos, task);
           LodUtil.assertTrue(worked);
        });
    }

    public CompletableFuture<Void> startClosing(boolean cancelCurrentGeneration, boolean alsoInterruptRunning) {
        taskGroups.values().forEach(g -> g.members.forEach(t -> t.future.complete(false)));
        taskGroups.clear();
        ArrayList<CompletableFuture<Void>> array = new ArrayList<>(inProgress.size());
        inProgress.values().forEach(runningTask -> array.add(
                runningTask.genFuture.exceptionally((ex) -> {
                    if (ex instanceof CompletionException) ex = ex.getCause();
                    if (!UncheckedInterruptedException.isThrowableInterruption(ex))
                        logger.error("Error when terminating data generation for section {}", runningTask.group.pos, ex);
                    return null;
                })));
        closer = CompletableFuture.allOf(array.toArray(CompletableFuture[]::new));
        if (cancelCurrentGeneration) {
            array.forEach(f -> f.cancel(alsoInterruptRunning));
        }
        looseTasks.forEach(t -> t.future.complete(false));
        looseTasks.clear();
        return closer;
    }

    @Override
    public void close() {
        if (closer == null) startClosing(true, true);
        LodUtil.assertTrue(closer != null);
        try {
            closer.orTimeout(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS).join();
        } catch (Throwable e) {
            logger.error("Failed to close generation queue: ", e);
        }
    }
}
