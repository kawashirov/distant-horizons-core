package com.seibel.lod.core.a7.generation;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.full.FullDataSource;
import com.seibel.lod.core.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.util.ConcurrentQuadCombinableProviderTree;
import com.seibel.lod.core.a7.util.UncheckedInterruptedException;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.ArrayGridList;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

public class GenerationQueue implements AutoCloseable {
    final ConcurrentQuadCombinableProviderTree<GenerationResult> cqcpTree = new ConcurrentQuadCombinableProviderTree<>();
    IGenerator generator = null;
    private final Logger logger = DhLoggerBuilder.getLogger();
    private final ConcurrentHashMap<DhLodPos, CompletableFuture<GenerationResult>> taskMap = new ConcurrentHashMap<>();
    private final LinkedList<CompletableFuture<GenerationResult>> inProgress = new LinkedList<>();

    public GenerationQueue() {}
    public void pollAndStartClosest(DhBlockPos2D targetPos) {
        if (generator == null) throw new IllegalStateException("generator is null");
        if (generator.isBusy()) return;
        DhLodPos closest = null;
        long closestDist = Long.MAX_VALUE;
        int smallestDetail = Integer.MAX_VALUE;
        for (DhLodPos key : taskMap.keySet()) {
            if (key.detail > smallestDetail) continue;
            long dist = key.getCenter().distSquared(targetPos);
            if (key.detail == smallestDetail && dist >= closestDist) continue;
            closest = key;
            closestDist = dist;
            smallestDetail = key.detail;
        }
        if (closest != null) {
            CompletableFuture<GenerationResult> future = taskMap.remove(closest);
            startFuture(closest, future);
        }
    }

    public void setGenerator(IGenerator generator) {
        LodUtil.assertTrue(generator != null);
        LodUtil.assertTrue(this.generator == null);
        this.generator = generator;
    }
    public void removeGenerator() {
        LodUtil.assertTrue(generator != null);
        this.generator = null;
        inProgress.forEach(f -> f.cancel(true));
        inProgress.clear();
    }

    private CompletableFuture<GenerationResult> createFuture(DhLodPos pos) {
        CompletableFuture<GenerationResult> future = new CompletableFuture<>();
        CompletableFuture<GenerationResult> swapped = taskMap.put(pos, future);
        LodUtil.assertTrue(swapped == null);
        return future;
    }

    private void startFuture(DhLodPos pos, CompletableFuture<GenerationResult> resultFuture) {
        byte dataDetail = generator.getDataDetail();
        byte minGenGranularity = generator.getMinGenerationGranularity();
        byte maxGenGranularity = generator.getMaxGenerationGranularity();
        if (minGenGranularity < 4 || maxGenGranularity < 4) {
            throw new IllegalStateException("Generation granularity must be at least 4!");
        }
        byte minUnitDetail = (byte) (dataDetail + minGenGranularity);
        byte maxUnitDetail = (byte) (dataDetail + maxGenGranularity);
        LodUtil.assertTrue(pos.detail >= minUnitDetail && pos.detail <= maxUnitDetail);
        byte genGranularity = (byte) (pos.detail - dataDetail);
        DHChunkPos chunkPosMin = new DHChunkPos(pos.getCorner());
        logger.info("Generating section {} with granularity {} at {}", pos, genGranularity, chunkPosMin);
        int perCallChunksWidth = 1 << (genGranularity - 4);

        CompletableFuture<ArrayGridList<ChunkSizedData>> dataFuture = generator.generate(chunkPosMin, genGranularity);
        inProgress.add(
            dataFuture.handle((data, ex) -> {
                if (ex != null) {
                    if (ex instanceof CompletionException) {
                        ex = ex.getCause();
                    }
                    if (ex instanceof InterruptedException) return null; // Ignore interrupted exceptions.
                    if (ex instanceof UncheckedInterruptedException) return null; // Ignore unchecked interrupted exceptions.
                    logger.error("Error generating data for section {}", pos, ex);
                    throw new CompletionException("Generation failed", ex);
                }
                LodUtil.assertTrue(data != null);
                if (data.gridSize < (1 << (genGranularity-4))) {
                    logger.error(
                            "Generator at {} returned {} by {} chunks but requested granularity was {}, which expect at least {} by {} chunks! ",
                            pos, data.gridSize, data.gridSize, genGranularity, perCallChunksWidth, perCallChunksWidth);
                    throw new RuntimeException("Generation failed. Generator returned less data than requested!");
                }
                logger.info("Completed generating {} by {} chunks to sections that overlaps {}",
                        data.gridSize, data.gridSize, pos);
                return data;
            }).thenApply((list) -> {
                GenerationResult result = new GenerationResult();
                result.dataList.addAll(list);
                return result;
            }).handle((r, e) -> {
                if (e!=null) resultFuture.completeExceptionally(e); else resultFuture.complete(r);
                return null;
            })
        );
    }

//    private void startFuture(DhLodPos pos, CompletableFuture<GenerationResult> resultFuture) {
//        byte dataDetail = generator.getDataDetail();
//        byte minGenGranularity = generator.getMinGenerationGranularity();
//        byte maxGenGranularity = generator.getMaxGenerationGranularity();
//        if (minGenGranularity < 4 || maxGenGranularity < 4) {
//            throw new IllegalStateException("Generation granularity must be at least 4!");
//        }
//
//        byte minUnitDetail = (byte) (dataDetail + minGenGranularity);
//        byte maxUnitDetail = (byte) (dataDetail + maxGenGranularity);
//
//        byte granularity;
//        int count;
//        DHChunkPos chunkPosMin;
//        if (pos.detail < minUnitDetail) {
//            granularity = minGenGranularity;
//            count = 1;
//            chunkPosMin = new DHChunkPos(pos.convertUpwardsTo(minUnitDetail).getCorner());
//        } else if (pos.detail > maxUnitDetail) {
//            granularity = maxGenGranularity;
//            count = 1 << (pos.detail - maxUnitDetail);
//            chunkPosMin = new DHChunkPos(pos.getCorner());
//        } else {
//            granularity = (byte) (pos.detail - dataDetail);
//            count = 1;
//            chunkPosMin = new DHChunkPos(pos.getCorner());
//        }
//        assert granularity >= minGenGranularity && granularity <= maxGenGranularity;
//        assert count > 0;
//        assert granularity >= 4; // Thanks compiler. Guess having a 'always true' warning means I did it right.
//        logger.info("Generating section {} of size {} with granularity {} at {}", pos, count, granularity, chunkPosMin);
//        int perCallChunksWidth = 1 << (granularity - 4);
//        final byte sectionDetail = (byte) (dataDetail + FullDataSource.SECTION_SIZE_OFFSET);
//
//        ArrayList<CompletableFuture<Collection<ChunkSizedData>>> futures = new ArrayList<>(count*count);
//        for (int dx = 0; dx < count; dx++) {
//            for (int dz = 0; dz < count; dz++) { // TODO: Unroll this loop to yield when generator is busy.
//                DHChunkPos subCallChunkPosMin = new DHChunkPos(chunkPosMin.x + dx * perCallChunksWidth, chunkPosMin.z + dz * perCallChunksWidth);
//                CompletableFuture<ArrayGridList<ChunkSizedData>> dataFuture = generator.generate(subCallChunkPosMin, granularity);
//                futures.add(dataFuture.handle((data, ex) -> {
//                    if (ex != null) {
//                        if (ex instanceof CompletionException) {
//                            ex = ex.getCause();
//                        }
//                        if (ex instanceof InterruptedException) return null; // Ignore interrupted exceptions.
//                        if (ex instanceof UncheckedInterruptedException) return null; // Ignore unchecked interrupted exceptions.
//                        logger.error("Error generating data for section {}", pos, ex);
//                        return null;
//                    }
//                    LodUtil.assertTrue(data != null);
//                    if (data.gridSize < (1 << (granularity-4))) {
//                        logger.error(
//                                "Generator at {} returned {} by {} chunks but requested granularity was {}, which expect at least {} by {} chunks! ",
//                                pos, data.gridSize, data.gridSize, granularity, perCallChunksWidth, perCallChunksWidth);
//                        return null;
//                    }
//
//                    DhLodPos minSectPos = new DhLodPos((byte)(dataDetail+4), data.getFirst().x, data.getFirst().z).convertUpwardsTo(sectionDetail);
//                    DhLodPos maxSectPos = new DhLodPos((byte)(dataDetail+4), data.getLast().x, data.getLast().z).convertUpwardsTo(sectionDetail);
//
//                    int sectionCount = (maxSectPos.x - minSectPos.x) + 1;
//                    LodUtil.assertTrue(sectionCount > 0 && sectionCount == (maxSectPos.z - minSectPos.z) + 1);
//
//                    logger.info("Writing {} by {} chunks (at {}) with data detail {} to {} by {} sections (at {})",
//                            data.gridSize, data.gridSize, subCallChunkPosMin, dataDetail,
//                            sectionCount, sectionCount, minSectPos);
//                    return data;
//                }));
//            }
//        }
//        inProgress.add(
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply((v) -> {
//                GenerationResult result = new GenerationResult();
//                for (CompletableFuture<Collection<ChunkSizedData>> future : futures) {
//                    Collection<ChunkSizedData> data = future.join();
//                    if (data == null) continue;
//                    result.dataList.addAll(data);
//                }
//                return result;
//            }).handle((r, e) -> {
//                if (e!=null) resultFuture.completeExceptionally(e); else resultFuture.complete(r);
//                return null;
//            })
//        );
//    }

    public CompletableFuture<LodDataSource> generate(DhSectionPos sectionPos) {
        byte maxGen = (byte) (generator.getMaxGenerationGranularity() + generator.getDataDetail());
        if (sectionPos.sectionDetail > maxGen) {
            int count = 1 << (sectionPos.sectionDetail - maxGen);
            DhLodPos minPos = sectionPos.getCorner(maxGen);
            ArrayList<CompletableFuture<GenerationResult>> futures = new ArrayList<>(count*count);
            for (int x = 0; x < count; x++) {
                for (int z = 0; z < count; z++) {
                    DhLodPos subPos = new DhLodPos(maxGen, minPos.x + x, minPos.z + z);
                    futures.add(cqcpTree.createOrUseExisting(subPos, this::createFuture));
                }
            }
            // FIXME: Does `allOf` have correct behaviour when one of the futures fails?
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply((v) -> {
                        FullDataSource newSource = FullDataSource.createEmpty(sectionPos);
                        for (CompletableFuture<GenerationResult> future : futures) {
                            try {
                                GenerationResult result = future.join();
                                for (ChunkSizedData data : result.dataList) {
                                    if (data.getBBoxLodPos().overlaps(sectionPos.getSectionBBoxPos())) newSource.update(data);
                                }
                            } catch (Exception e) {
                                // continue
                            }
                        }
                        return newSource;
            });
        } else {
            DhLodPos lodPos = sectionPos.getSectionBBoxPos();
            return cqcpTree.createOrUseExisting(lodPos, this::createFuture).thenApply(
                    (result) -> {
                        if (result == null || result.dataList.isEmpty()) return FullDataSource.createEmpty(sectionPos);
                        FullDataSource newSource = FullDataSource.createEmpty(sectionPos);
                        for (ChunkSizedData data : result.dataList) {
                            if (data.getBBoxLodPos().overlaps(sectionPos.getSectionBBoxPos())) newSource.update(data);
                        }
                        return newSource;
                    });
        }
    }

    @Override
    public void close() {
        //TODO

    }

//
//    DhBlockPos2D lastPlayerPos = new DhBlockPos2D(0, 0);
//    final ConcurrentHashMap<DhSectionPos, WeakReference<PlaceHolderRenderSource>> trackers = new ConcurrentHashMap<>();
//    final BiConsumer<DhSectionPos, ChunkSizedData> writeConsumer;
//    final HashSet<Request, CompletableFuture<?>> inProgressSections = new HashSet<>();

//
//    public void track(PlaceHolderRenderSource source) {
//        //logger.info("Tracking source {} at {}", source, source.getSectionPos());
//        trackers.put(source.getSectionPos(), new WeakReference<>(source));
//    }
//
//    private void update() {
//        LinkedList<DhSectionPos> toRemove = new LinkedList<>();
//        for (DhSectionPos pos : trackers.keySet()) {
//            WeakReference<PlaceHolderRenderSource> ref = trackers.get(pos);
//            if (ref.get() == null) {
//                toRemove.add(pos);
//            }
//        }
//        for (DhSectionPos pos : toRemove) {
//            trackers.remove(pos);
//        }
//    }

//    //FIXME: Do optimizations on polling closest to player. (Currently its a O(n) search!)
//    //FIXME: Do not return sections that is already being generated.
//    //FIXME: Optimize the checks for inProgressSections.
//    private DhSectionPos pollClosest(DhBlockPos2D playerPos) {
//        update();
//        DhSectionPos closest = null;
//        long closestDist = Long.MAX_VALUE;
//        for (DhSectionPos pos : trackers.keySet()) {
//            if (inProgressSections.contains(pos)) {
//                continue;
//            }
//            long distSqr = pos.getCenter().getCenter().distSquared(playerPos);
//            if (distSqr < closestDist) {
//                closest = pos;
//                closestDist = distSqr;
//            }
//        }
//        if (closest != null) inProgressSections.add(closest);
//        return closest;
//    }
}
