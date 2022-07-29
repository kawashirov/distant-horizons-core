package com.seibel.lod.core.a7.generation;

import com.seibel.lod.core.a7.PlaceHolderQueue;
import com.seibel.lod.core.a7.datatype.PlaceHolderRenderSource;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.full.FullDataSource;
import com.seibel.lod.core.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.util.UncheckedInterruptedException;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.ArrayGridList;
import org.apache.logging.log4j.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

public class GenerationQueue implements PlaceHolderQueue {
    private final Logger logger = DhLoggerBuilder.getLogger();
    DhBlockPos2D lastPlayerPos = new DhBlockPos2D(0, 0);
    final HashMap<DhSectionPos, WeakReference<PlaceHolderRenderSource>> trackers = new HashMap<>();
    final BiConsumer<DhSectionPos, ChunkSizedData> writeConsumer;
    final HashSet<DhSectionPos> inProgressSections = new HashSet<>();

    public GenerationQueue(BiConsumer<DhSectionPos, ChunkSizedData> writeConsumer) {
        this.writeConsumer = writeConsumer;
    }

    public void track(PlaceHolderRenderSource source) {
        //logger.info("Tracking source {} at {}", source, source.getSectionPos());
        trackers.put(source.getSectionPos(), new WeakReference<>(source));
    }

    private void update() {
        LinkedList<DhSectionPos> toRemove = new LinkedList<>();
        for (DhSectionPos pos : trackers.keySet()) {
            WeakReference<PlaceHolderRenderSource> ref = trackers.get(pos);
            if (ref.get() == null) {
                toRemove.add(pos);
            }
        }
        for (DhSectionPos pos : toRemove) {
            trackers.remove(pos);
        }
    }

    //FIXME: Do optimizations on polling closest to player. (Currently its a O(n) search!)
    //FIXME: Do not return sections that is already being generated.
    //FIXME: Optimize the checks for inProgressSections.
    private DhSectionPos pollClosest(DhBlockPos2D playerPos) {
        update();
        DhSectionPos closest = null;
        long closestDist = Long.MAX_VALUE;
        for (DhSectionPos pos : trackers.keySet()) {
            if (inProgressSections.contains(pos)) {
                continue;
            }
            long distSqr = pos.getCenter().getCenter().distSquared(playerPos);
            if (distSqr < closestDist) {
                closest = pos;
                closestDist = distSqr;
            }
        }
        if (closest != null) inProgressSections.add(closest);
        return closest;
    }

    public void doGeneration(IGenerator generator) {
        if (generator == null) return;
        if (generator.isBusy()) return;

        DhSectionPos pos = pollClosest(lastPlayerPos);
        if (pos == null) return;

        byte dataDetail = generator.getDataDetail();
        byte minGenGranularity = generator.getMinGenerationGranularity();
        byte maxGenGranularity = generator.getMaxGenerationGranularity();
        if (minGenGranularity < 4 || maxGenGranularity < 4) {
            throw new IllegalStateException("Generation granularity must be at least 4!");
        }

        byte minUnitDetail = (byte) (dataDetail + minGenGranularity);
        byte maxUnitDetail = (byte) (dataDetail + maxGenGranularity);

        byte granularity;
        int count;
        DHChunkPos chunkPosMin;
        if (pos.sectionDetail < minUnitDetail) {
            granularity = minGenGranularity;
            count = 1;
            chunkPosMin = new DHChunkPos(pos.getSectionBBoxPos().convertUpwardsTo(minUnitDetail).getCorner());
        } else if (pos.sectionDetail > maxUnitDetail) {
            granularity = maxGenGranularity;
            count = 1 << (pos.sectionDetail - maxUnitDetail);
            chunkPosMin = new DHChunkPos(pos.getCorner().getCorner());
        } else {
            granularity = (byte) (pos.sectionDetail - dataDetail);
            count = 1;
            chunkPosMin = new DHChunkPos(pos.getCorner().getCorner());
        }
        assert granularity >= minGenGranularity && granularity <= maxGenGranularity;
        assert count > 0;
        assert granularity >= 4; // Thanks compiler. Guess having a 'always true' warning means I did it right.
        logger.info("Generating section {} of size {} with granularity {} at {}", pos, count, granularity, chunkPosMin);
        int perCallChunksWidth = 1 << (granularity - 4);
        final byte sectionDetail = (byte) (dataDetail + FullDataSource.SECTION_SIZE_OFFSET);

        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>(count*count);
        for (int dx = 0; dx < count; dx++) {
            for (int dz = 0; dz < count; dz++) { // TODO: Unroll this loop to yield when generator is busy.
                DHChunkPos subCallChunkPosMin = new DHChunkPos(chunkPosMin.x + dx * perCallChunksWidth, chunkPosMin.z + dz * perCallChunksWidth);
                CompletableFuture<ArrayGridList<ChunkSizedData>> dataFuture = generator.generate(subCallChunkPosMin, granularity);
                futures.add(dataFuture.whenComplete((data, ex) -> {
                    if (ex != null) {
                        if (ex instanceof CompletionException) {
                            ex = ex.getCause();
                        }
                        if (ex instanceof InterruptedException) return; // Ignore interrupted exceptions.
                        if (ex instanceof UncheckedInterruptedException) return; // Ignore unchecked interrupted exceptions.
                        logger.error("Error generating data for section {}", pos, ex);
                        return;
                    }
                    assert data != null;
                    if (data.gridSize < (1 << (granularity-4))) {
                        logger.error(
                                "Generator at {} returned {} by {} chunks but requested granularity was {}, which expect at least {} by {} chunks! ",
                                pos, data.gridSize, data.gridSize, granularity, perCallChunksWidth, perCallChunksWidth);
                        return;
                    }

                    DhLodPos minSectPos = new DhLodPos((byte)(dataDetail+4), data.getFirst().x, data.getFirst().z).convertUpwardsTo(sectionDetail);
                    DhLodPos maxSectPos = new DhLodPos((byte)(dataDetail+4), data.getLast().x, data.getLast().z).convertUpwardsTo(sectionDetail);

                    int sectionCount = (maxSectPos.x - minSectPos.x) + 1;
                    LodUtil.assertTrue(sectionCount > 0 && sectionCount == (maxSectPos.z - minSectPos.z) + 1);

                    logger.info("Writing {} by {} chunks (at {}) with data detail {} to {} by {} sections (at {})",
                            data.gridSize, data.gridSize, subCallChunkPosMin, dataDetail,
                            sectionCount, sectionCount, minSectPos);

                    data.forEachPos((x,z) -> {
                        ChunkSizedData chunkData = data.get(x,z);
                        DhLodPos chunkDataPos = new DhLodPos((byte)(chunkData.dataDetail + 4), chunkData.x, chunkData.z).convertUpwardsTo(sectionDetail);
                        DhSectionPos sectionPos = new DhSectionPos(chunkDataPos.detail, chunkDataPos.x, chunkDataPos.z);
                        //logger.info("Writing chunk {} with data detail {} to section {}",
                        //        new DhLodPos((byte)(chunkData.dataDetail + 4), chunkData.x, chunkData.z),
                        //        dataDetail, sectionPos);
                        writeConsumer.accept(sectionPos, chunkData);
                    });
//
//                    for (int dsx = 0; dsx < sectionCount; dsx++) {
//                        for (int dsz = 0; dsz < sectionCount; dsz++) {
//                            WeakReference<PlaceHolderRenderSource> ref = trackers.remove(new DhSectionPos(
//                                    sectionDetail, minSectPos.x + dsx, minSectPos.z + dsz));
//                            if (ref == null) return; // No placeholder there, so no need to trigger a refresh on it.
//                            PlaceHolderRenderSource source = ref.get();
//                            if (source == null) return; // Same as above.
//                            source.markInvalid(); // Mark the placeholder as invalid, so it will be refreshed on next lodTree update.
//                        }
//                    }
                }).exceptionally(ex -> {
                    if (ex instanceof CompletionException) {
                        ex = ex.getCause();
                    }
                    if (ex instanceof InterruptedException) return null; // Ignore interrupted exceptions.
                    if (ex instanceof UncheckedInterruptedException) return null; // Ignore unchecked interrupted exceptions.
                    logger.error("Error generating data for {} by {} chunks (at {}) with data detail {}",
                            perCallChunksWidth, perCallChunksWidth, subCallChunkPosMin, dataDetail, ex);
                    return null;
                }).thenRun(()->{})); // Convert to a CompletableFuture<Void>.
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            //try {
                //Thread.sleep(10000); // FIXME: Only for current debug testing. REMOVE THIS!
            //} catch (InterruptedException ignored) {}
            WeakReference<PlaceHolderRenderSource> ref = trackers.remove(pos);
            if (ref == null) return; // No placeholder there, so no need to trigger a refresh on it.
            PlaceHolderRenderSource source = ref.get();
            if (source == null) return; // Same as above.
            source.markInvalid(); // Mark the placeholder as invalid, so it will be refreshed on next lodTree update.
        });
    }
}
