package com.seibel.lod.core.datatype.full;

import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.datatype.full.accessor.SingleFullArrayView;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.file.datafile.IDataSourceProvider;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class FullDataDownSampler {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static CompletableFuture<ILodDataSource> createDownSamplingFuture(DhSectionPos newTarget, IDataSourceProvider provider) {
        // TODO: Make this future somehow run with lowest priority (to ensure ram usage stays low)
        return createDownSamplingFuture(FullDataSource.createEmpty(newTarget), provider);
    }

    public static CompletableFuture<ILodDataSource> createDownSamplingFuture(FullDataSource target, IDataSourceProvider provider) {
        int sectionSizeNeeded = 1 << target.getDataDetail();

        ArrayList<CompletableFuture<ILodDataSource>> futures;
        DhLodPos basePos = target.getSectionPos().getSectionBBoxPos().getCorner(FullDataSource.SECTION_SIZE_OFFSET);
        if (sectionSizeNeeded <= FullDataSource.SECTION_SIZE_OFFSET) {
            futures = new ArrayList<>(sectionSizeNeeded * sectionSizeNeeded);
            for (int ox = 0; ox < sectionSizeNeeded; ox++) {
                for (int oz = 0; oz < sectionSizeNeeded; oz++) {
                    CompletableFuture<ILodDataSource> future = provider.read(new DhSectionPos(
                            FullDataSource.SECTION_SIZE_OFFSET, basePos.x + ox, basePos.z + oz));
                    future = future.whenComplete((source, ex) -> {
                        if (ex == null && source != null && source instanceof FullDataSource) {
                            downSample(target, (FullDataSource) source);
                        } else if (ex != null) {
                            LOGGER.error("Error while down sampling", ex);
                        }
                    });
                    futures.add(future);
                }
            }
        } else {
            futures = new ArrayList<>(FullDataSource.SECTION_SIZE * FullDataSource.SECTION_SIZE);
            int multiplier = sectionSizeNeeded / FullDataSource.SECTION_SIZE;
            for (int ox = 0; ox < FullDataSource.SECTION_SIZE; ox++) {
                for (int oz = 0; oz < FullDataSource.SECTION_SIZE; oz++) {
                    CompletableFuture<ILodDataSource> future = provider.read(new DhSectionPos(
                            FullDataSource.SECTION_SIZE_OFFSET, basePos.x + ox * multiplier, basePos.z + oz * multiplier));
                    future = future.whenComplete((source, ex) -> {
                        if (ex == null && source != null && source instanceof FullDataSource) {
                            downSample(target, (FullDataSource) source);
                        } else if (ex != null) {
                            LOGGER.error("Error while down sampling", ex);
                        }
                    });
                    futures.add(future);
                }
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> target);
    }

    public static void downSample(FullDataSource target, FullDataSource source) {
        LodUtil.assertTrue(target.getSectionPos().overlaps(source.getSectionPos()));
        LodUtil.assertTrue(target.getDataDetail() > source.getDataDetail());

        byte detailDiff = (byte) (target.getDataDetail() - source.getDataDetail());
        DhSectionPos trgPos = target.getSectionPos();
        DhSectionPos srcPos = source.getSectionPos();

        if (detailDiff >= FullDataSource.SECTION_SIZE_OFFSET) {
            // The source occupies only 1 datapoint in the target
            // FIXME: TEMP method for down-sampling: take only the corner column
            int sourceSectionPerTargetData = 1 << (detailDiff - FullDataSource.SECTION_SIZE_OFFSET);
            if (srcPos.sectionX % sourceSectionPerTargetData != 0 || srcPos.sectionZ % sourceSectionPerTargetData != 0) {
                return;
            }
            DhLodPos trgOffset = trgPos.getCorner(target.getDataDetail());
            DhLodPos srcOffset = srcPos.getSectionBBoxPos().convertUpwardsTo(target.getDataDetail());
            int offsetX = trgOffset.x - srcOffset.x;
            int offsetZ = trgOffset.z - srcOffset.z;
            LodUtil.assertTrue(offsetX >= 0 && offsetX < FullDataSource.SECTION_SIZE
                    && offsetZ >= 0 && offsetZ < FullDataSource.SECTION_SIZE);
            target.markNotEmpty();
            source.get(0,0).deepCopyTo(target.get(offsetX, offsetZ));

        } else if (detailDiff > 0) {
            // The source occupies multiple data-points in the target
            int srcDataPerTrgData = 1 << detailDiff;
            int overlappedTrgDataSize = FullDataSource.SECTION_SIZE / srcDataPerTrgData;

            DhLodPos trgOffset = trgPos.getCorner(target.getDataDetail());
            DhLodPos srcOffset = srcPos.getSectionBBoxPos().getCorner(target.getDataDetail());
            int offsetX = trgOffset.x - srcOffset.x;
            int offsetZ = trgOffset.z - srcOffset.z;
            LodUtil.assertTrue(offsetX >= 0 && offsetX < FullDataSource.SECTION_SIZE
                    && offsetZ >= 0 && offsetZ < FullDataSource.SECTION_SIZE);
            target.markNotEmpty();

            for (int ox = 0; ox < overlappedTrgDataSize; ox++) {
                for (int oz = 0; oz < overlappedTrgDataSize; oz++) {
                    SingleFullArrayView column = target.get(ox + offsetX, oz + offsetZ);
                    column.downsampleFrom(source.subView(srcDataPerTrgData, ox * srcDataPerTrgData, oz * srcDataPerTrgData));
                }
            }
        } else {
            LodUtil.assertNotReach();
        }
    }
}
