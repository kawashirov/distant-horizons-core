package com.seibel.lod.core.dataObjects.fullData;

import com.seibel.lod.core.dataObjects.fullData.accessor.SingleFullDataAccessor;
import com.seibel.lod.core.dataObjects.fullData.sources.CompleteFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.IFullDataSource;
import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class FullDataDownSampler {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static CompletableFuture<IFullDataSource> createDownSamplingFuture(DhSectionPos newTarget, IFullDataSourceProvider provider) {
        // TODO: Make this future somehow run with lowest priority (to ensure ram usage stays low)
        return createDownSamplingFuture(CompleteFullDataSource.createEmpty(newTarget), provider);
    }

    public static CompletableFuture<IFullDataSource> createDownSamplingFuture(CompleteFullDataSource target, IFullDataSourceProvider provider) {
        int sectionSizeNeeded = 1 << target.getDataDetailLevel();

        ArrayList<CompletableFuture<IFullDataSource>> futures;
        DhLodPos basePos = target.getSectionPos().getSectionBBoxPos().getCornerLodPos(CompleteFullDataSource.SECTION_SIZE_OFFSET);
        if (sectionSizeNeeded <= CompleteFullDataSource.SECTION_SIZE_OFFSET) {
            futures = new ArrayList<>(sectionSizeNeeded * sectionSizeNeeded);
            for (int ox = 0; ox < sectionSizeNeeded; ox++) {
                for (int oz = 0; oz < sectionSizeNeeded; oz++) {
                    CompletableFuture<IFullDataSource> future = provider.read(new DhSectionPos(
                            CompleteFullDataSource.SECTION_SIZE_OFFSET, basePos.x + ox, basePos.z + oz));
                    future = future.whenComplete((source, ex) -> {
                        if (ex == null && source != null && source instanceof CompleteFullDataSource) {
                            downSample(target, (CompleteFullDataSource) source);
                        } else if (ex != null) {
                            LOGGER.error("Error while down sampling", ex);
                        }
                    });
                    futures.add(future);
                }
            }
        } else {
            futures = new ArrayList<>(CompleteFullDataSource.SECTION_SIZE * CompleteFullDataSource.SECTION_SIZE);
            int multiplier = sectionSizeNeeded / CompleteFullDataSource.SECTION_SIZE;
            for (int ox = 0; ox < CompleteFullDataSource.SECTION_SIZE; ox++) {
                for (int oz = 0; oz < CompleteFullDataSource.SECTION_SIZE; oz++) {
                    CompletableFuture<IFullDataSource> future = provider.read(new DhSectionPos(
                            CompleteFullDataSource.SECTION_SIZE_OFFSET, basePos.x + ox * multiplier, basePos.z + oz * multiplier));
                    future = future.whenComplete((source, ex) -> {
                        if (ex == null && source != null && source instanceof CompleteFullDataSource) {
                            downSample(target, (CompleteFullDataSource) source);
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

    public static void downSample(CompleteFullDataSource target, CompleteFullDataSource source) {
        LodUtil.assertTrue(target.getSectionPos().overlaps(source.getSectionPos()));
        LodUtil.assertTrue(target.getDataDetailLevel() > source.getDataDetailLevel());

        byte detailDiff = (byte) (target.getDataDetailLevel() - source.getDataDetailLevel());
        DhSectionPos trgPos = target.getSectionPos();
        DhSectionPos srcPos = source.getSectionPos();

        if (detailDiff >= CompleteFullDataSource.SECTION_SIZE_OFFSET) {
            // The source occupies only 1 datapoint in the target
            // FIXME: TEMP method for down-sampling: take only the corner column
            int sourceSectionPerTargetData = 1 << (detailDiff - CompleteFullDataSource.SECTION_SIZE_OFFSET);
            if (srcPos.sectionX % sourceSectionPerTargetData != 0 || srcPos.sectionZ % sourceSectionPerTargetData != 0) {
                return;
            }
            DhLodPos trgOffset = trgPos.getCorner(target.getDataDetailLevel());
            DhLodPos srcOffset = srcPos.getSectionBBoxPos().convertToDetailLevel(target.getDataDetailLevel());
            int offsetX = trgOffset.x - srcOffset.x;
            int offsetZ = trgOffset.z - srcOffset.z;
            LodUtil.assertTrue(offsetX >= 0 && offsetX < CompleteFullDataSource.SECTION_SIZE
                    && offsetZ >= 0 && offsetZ < CompleteFullDataSource.SECTION_SIZE);
            target.markNotEmpty();
            source.get(0,0).deepCopyTo(target.get(offsetX, offsetZ));

        } else if (detailDiff > 0) {
            // The source occupies multiple data-points in the target
            int srcDataPerTrgData = 1 << detailDiff;
            int overlappedTrgDataSize = CompleteFullDataSource.SECTION_SIZE / srcDataPerTrgData;

            DhLodPos trgOffset = trgPos.getCorner(target.getDataDetailLevel());
            DhLodPos srcOffset = srcPos.getSectionBBoxPos().getCornerLodPos(target.getDataDetailLevel());
            int offsetX = trgOffset.x - srcOffset.x;
            int offsetZ = trgOffset.z - srcOffset.z;
            LodUtil.assertTrue(offsetX >= 0 && offsetX < CompleteFullDataSource.SECTION_SIZE
                    && offsetZ >= 0 && offsetZ < CompleteFullDataSource.SECTION_SIZE);
            target.markNotEmpty();

            for (int ox = 0; ox < overlappedTrgDataSize; ox++) {
                for (int oz = 0; oz < overlappedTrgDataSize; oz++) {
                    SingleFullDataAccessor column = target.get(ox + offsetX, oz + offsetZ);
                    column.downsampleFrom(source.subView(srcDataPerTrgData, ox * srcDataPerTrgData, oz * srcDataPerTrgData));
                }
            }
        } else {
            LodUtil.assertNotReach();
        }
    }
}
