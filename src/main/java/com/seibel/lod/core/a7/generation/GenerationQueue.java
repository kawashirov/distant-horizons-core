package com.seibel.lod.core.a7.generation;

import com.seibel.lod.core.a7.PlaceHolderQueue;
import com.seibel.lod.core.a7.datatype.PlaceHolderRenderSource;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.full.FullDataSource;
import com.seibel.lod.core.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.util.gridList.ArrayGridList;
import org.apache.logging.log4j.Logger;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

public class GenerationQueue implements PlaceHolderQueue {
    private final Logger logger = DhLoggerBuilder.getLogger();
    DhBlockPos2D lastPlayerPos = new DhBlockPos2D(0, 0);
    final HashMap<DhSectionPos, WeakReference<PlaceHolderRenderSource>> trackers = new HashMap<>();
    final BiConsumer<DhSectionPos, ChunkSizedData> writeConsumer;

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
    private DhSectionPos pollClosest(DhBlockPos2D playerPos) {
        update();
        DhSectionPos closest = null;
        long closestDist = Long.MAX_VALUE;
        for (DhSectionPos pos : trackers.keySet()) {
            long distSqr = pos.getCenter().getCenter().distSquared(playerPos);
            if (distSqr < closestDist) {
                closest = pos;
                closestDist = distSqr;
            }
        }
        return closest;
    }

    private void write(DhSectionPos pos, ChunkSizedData data) {
        writeConsumer.accept(pos, data);
        WeakReference<PlaceHolderRenderSource> ref = trackers.get(pos);
        if (ref == null) return; // No placeholder there, so no need to trigger a refresh on it.
        PlaceHolderRenderSource source = ref.get();
        if (source == null) return; // Same as above.
        source.markInvalid(); // Mark the placeholder as invalid, so it will be refreshed on next lodTree update.
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
//FIXME: Handle size != 1 case
        CompletableFuture<ArrayGridList<ChunkSizedData>> dataFuture = generator.generate(chunkPosMin, granularity);

        dataFuture.whenComplete((data, ex) -> {
            if (ex != null) {
                if (ex instanceof CompletionException) {
                    ex = ex.getCause();
                }
                logger.error("Error generating data for section {}", pos, ex);
                return;
            }
            assert data != null;
            if (data.gridSize < (1 << (granularity-4)))
                throw new IllegalStateException("Generator returned chunks of size "
                        + data.gridSize + " but requested granularity was " + granularity
                        + " (equals to chunks of : " + (1 << (granularity-4)) + ") @ " + chunkPosMin);

            logger.info("Writing chunk {} to {} with data detail {}",
                    chunkPosMin, new DHChunkPos(chunkPosMin.x + (1 << (granularity-4)), chunkPosMin.z + (1 << (granularity-4))),
                    dataDetail);

            final byte sectionDetail = (byte) (dataDetail + FullDataSource.SECTION_SIZE_OFFSET);
            data.forEachPos((x,z) -> {
                ChunkSizedData chunkData = data.get(x,z);
                DhLodPos chunkDataPos = new DhLodPos((byte) (dataDetail + 4), x, z).convertUpwardsTo(sectionDetail);
                DhSectionPos sectionPos = new DhSectionPos(chunkDataPos.detail, chunkDataPos.x, chunkDataPos.z);
                //logger.info("Writing chunk {} with data detail {} to section {}",
                //        new DHChunkPos(x+chunkPosMin.x,z+chunkPosMin.z),
                //        dataDetail, sectionPos);
                write(sectionPos, chunkData);
            });
        });

    }
}
