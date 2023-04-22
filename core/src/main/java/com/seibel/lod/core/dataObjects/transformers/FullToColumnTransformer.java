package com.seibel.lod.core.dataObjects.transformers;

import com.seibel.lod.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.lod.core.dataObjects.fullData.sources.CompleteFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.IIncompleteFullDataSource;
import com.seibel.lod.core.util.RenderDataPointUtil;
import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.dataObjects.render.columnViews.ColumnArrayView;
import com.seibel.lod.core.dataObjects.render.columnViews.ColumnQuadView;
import com.seibel.lod.core.dataObjects.fullData.accessor.SingleFullArrayView;
import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataView;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.util.FullDataPointUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

public class FullToColumnTransformer
{
    private static final IBlockStateWrapper AIR = SingletonInjector.INSTANCE.get(IWrapperFactory.class).getAirBlockStateWrapper();
	
	
	
	/**
	 * Called in loops that may run for an extended period of time. <br>
	 * This is necessary to allow canceling these transformers since running
	 * them after the client has left a given world will throw exceptions here.
	 */
	private static void throwIfThreadInterrupted() throws InterruptedException
	{
		if (Thread.interrupted())
		{
			throw new InterruptedException(FullToColumnTransformer.class.getSimpleName()+" task interrupted.");
		}
	}
	
	
	//==============//
	// transformers //
	//==============//
	
    /**
     * Creates a LodNode for a chunk in the given world.
     * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 * @throws InterruptedException Can be caused by interrupting the thread upstream.
	 * 								Generally thrown if the method is running after the client leaves the current world.
     */
    public static ColumnRenderSource transformFullDataToColumnData(IDhClientLevel level, CompleteFullDataSource fullDataSource) throws InterruptedException 
	{
        final DhSectionPos pos = fullDataSource.getSectionPos();
        final byte dataDetail = fullDataSource.getDataDetailLevel();
        final int vertSize = Config.Client.Graphics.Quality.verticalQuality.get().calculateMaxVerticalData(fullDataSource.getDataDetailLevel());
        final ColumnRenderSource columnSource = new ColumnRenderSource(pos, vertSize, level.getMinY());
        if (fullDataSource.isEmpty())
		{
			return columnSource;
		}
		
        columnSource.markNotEmpty();

        if (dataDetail == columnSource.getDataDetail())
		{
            int baseX = pos.getCorner().getCornerBlockPos().x;
            int baseZ = pos.getCorner().getCornerBlockPos().z;
			
            for (int x = 0; x < pos.getWidth(dataDetail).numberOfLodSectionsWide; x++)
			{
                for (int z = 0; z < pos.getWidth(dataDetail).numberOfLodSectionsWide; z++)
				{
					throwIfThreadInterrupted();
					
                    ColumnArrayView columnArrayView = columnSource.getVerticalDataPointView(x, z);
                    SingleFullArrayView fullArrayView = fullDataSource.get(x, z);
                    convertColumnData(level, baseX + x, baseZ + z, columnArrayView, fullArrayView, 1);
                    
					if (fullArrayView.doesItExist())
					{
						LodUtil.assertTrue(columnSource.doesDataPointExist(x, z));
					}
                }
            }
			
            columnSource.fillDebugFlag(0, 0, ColumnRenderSource.SECTION_SIZE, ColumnRenderSource.SECTION_SIZE, ColumnRenderSource.DebugSourceFlag.FULL);
			
//        } else if (dataDetail == 0 && columnSource.getDataDetail() > dataDetail) {
//            byte deltaDetail = (byte) (columnSource.getDataDetail() - dataDetail);
//            int perColumnWidth = 1 << deltaDetail;
//            int columnCount = pos.getWidth(dataDetail).value / perColumnWidth;
//
//
//            for (int x = 0; x < pos.getWidth(dataDetail).value; x++) {
//                for (int z = 0; z < pos.getWidth(dataDetail).value; z++) {
//                    ColumnArrayView columnArrayView = columnSource.getVerticalDataView(x, z);
//                    SingleFullArrayView fullArrayView = data.get(x, z);
//                    convertColumnData(level, columnArrayView, fullArrayView);
//                }
//            }
		}
		else
		{
			throw new UnsupportedOperationException("To be implemented");
			//FIXME: Implement different size creation of renderData
		}
		return columnSource;
    }
	
	/**
	 * @throws InterruptedException Can be caused by interrupting the thread upstream.
	 * 								Generally thrown if the method is running after the client leaves the current world.
	 */
    public static ColumnRenderSource transformIncompleteDataToColumnData(IDhClientLevel level, IIncompleteFullDataSource data) throws InterruptedException
	{
        final DhSectionPos pos = data.getSectionPos();
        final byte dataDetail = data.getDataDetailLevel();
        final int vertSize = Config.Client.Graphics.Quality.verticalQuality.get().calculateMaxVerticalData(data.getDataDetailLevel());
        final ColumnRenderSource columnSource = new ColumnRenderSource(pos, vertSize, level.getMinY());
        if (data.isEmpty())
		{
			return columnSource;
		}
		
        columnSource.markNotEmpty();
		
		if (dataDetail == columnSource.getDataDetail())
		{
			int baseX = pos.getCorner().getCornerBlockPos().x;
			int baseZ = pos.getCorner().getCornerBlockPos().z;
			for (int x = 0; x < pos.getWidth(dataDetail).numberOfLodSectionsWide; x++)
			{
				for (int z = 0; z < pos.getWidth(dataDetail).numberOfLodSectionsWide; z++)
				{
					throwIfThreadInterrupted();
					
					SingleFullArrayView fullArrayView = data.tryGet(x, z);
					if (fullArrayView == null)
					{
						continue;
					}
					
					ColumnArrayView columnArrayView = columnSource.getVerticalDataPointView(x, z);
					convertColumnData(level, baseX + x, baseZ + z, columnArrayView, fullArrayView, 1);
					columnSource.fillDebugFlag(x, z, 1, 1, ColumnRenderSource.DebugSourceFlag.SPARSE);
					if (fullArrayView.doesItExist())
						LodUtil.assertTrue(columnSource.doesDataPointExist(x, z));
				}
			}
		}
		else
		{
			throw new UnsupportedOperationException("To be implemented");
			//FIXME: Implement different size creation of renderData
		}
        return columnSource;
    }
	
	/**
	 * @throws InterruptedException Can be caused by interrupting the thread upstream.
	 * 								Generally thrown if the method is running after the client leaves the current world.
	 */
	public static void writeFullDataChunkToColumnData(ColumnRenderSource render, IDhClientLevel level, ChunkSizedFullDataView chunkDataView) throws InterruptedException
	{
		final DhSectionPos pos = render.getSectionPos();
		final int renderOffsetX = (chunkDataView.pos.x * LodUtil.CHUNK_WIDTH) - pos.getCorner().getCornerBlockPos().x;
		final int renderOffsetZ = (chunkDataView.pos.z * LodUtil.CHUNK_WIDTH) - pos.getCorner().getCornerBlockPos().z;
		final int blockX = pos.getCorner().getCornerBlockPos().x;
		final int blockZ = pos.getCorner().getCornerBlockPos().z;
		final int perRenderWidth = 1 << render.getDataDetail();
		final int perDataWidth = 1 << chunkDataView.detailLevel;
		render.markNotEmpty();
		
		if (chunkDataView.detailLevel == render.getDataDetail())
		{
			if (renderOffsetX < 0 || renderOffsetX + LodUtil.CHUNK_WIDTH > render.getDataSize() || renderOffsetZ < 0 || renderOffsetZ + LodUtil.CHUNK_WIDTH > render.getDataSize())
			{
				throw new IllegalArgumentException("Data offset is out of bounds");
			}
			
			
			for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
			{
				for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
				{
					throwIfThreadInterrupted();
					
					ColumnArrayView columnArrayView = render.getVerticalDataPointView(renderOffsetX + x, renderOffsetZ + z);
					SingleFullArrayView fullArrayView = chunkDataView.get(x, z);
					convertColumnData(level, blockX + perRenderWidth * (renderOffsetX + x),
							blockZ + perRenderWidth * (renderOffsetZ + z),
							columnArrayView, fullArrayView, 2);
					
					if (fullArrayView.doesItExist())
					{
						LodUtil.assertTrue(render.doesDataPointExist(renderOffsetX + x, renderOffsetZ + z));
					}
				}
			}
			render.fillDebugFlag(renderOffsetX, renderOffsetZ, LodUtil.CHUNK_WIDTH, LodUtil.CHUNK_WIDTH, ColumnRenderSource.DebugSourceFlag.DIRECT);
		}
		else
		{
			final int dataPerRender = 1 << (render.getDataDetail() - chunkDataView.detailLevel);
			final int dataSize = LodUtil.CHUNK_WIDTH / dataPerRender;
			final int vertSize = render.getVerticalSize();
			long[] tempRender = new long[dataPerRender * dataPerRender * vertSize];
			if (renderOffsetX < 0 || renderOffsetX + dataSize > render.getDataSize() || renderOffsetZ < 0 || renderOffsetZ + dataSize > render.getDataSize())
			{
				throw new IllegalArgumentException("Data offset is out of bounds");
			}
			
			for (int x = 0; x < dataSize; x++)
			{
				for (int z = 0; z < dataSize; z++)
				{
					
					ColumnQuadView tempQuadView = new ColumnQuadView(tempRender, dataPerRender, vertSize, 0, 0, dataPerRender, dataPerRender);
					for (int ox = 0; ox < dataPerRender; ox++)
					{
						for (int oz = 0; oz < dataPerRender; oz++)
						{
							throwIfThreadInterrupted();
							
							
							ColumnArrayView columnArrayView = tempQuadView.get(ox, oz);
							SingleFullArrayView fullArrayView = chunkDataView.get(x * dataPerRender + ox, z * dataPerRender + oz);
							convertColumnData(level, blockX + perRenderWidth * (renderOffsetX + x) + perDataWidth * ox,
									blockZ + perRenderWidth * (renderOffsetZ + z) + perDataWidth * oz,
									columnArrayView, fullArrayView, 2);
						}
					}
					ColumnArrayView downSampledArrayView = render.getVerticalDataPointView(renderOffsetX + x, renderOffsetZ + z);
					downSampledArrayView.mergeMultiDataFrom(tempQuadView);
				}
			}
			render.fillDebugFlag(renderOffsetX, renderOffsetZ, dataSize, dataSize, ColumnRenderSource.DebugSourceFlag.DIRECT);
		}
	}

    private static void convertColumnData(IDhClientLevel level, int blockX, int blockZ, ColumnArrayView columnArrayView, SingleFullArrayView fullArrayView, int genMode)
	{
        if (!fullArrayView.doesItExist())
		{
			return;
		}
        
		int dataTotalLength = fullArrayView.getSingleLength();
        if (dataTotalLength == 0)
		{
			return;
		}

        if (dataTotalLength > columnArrayView.verticalSize())
		{
            ColumnArrayView totalColumnData = new ColumnArrayView(new long[dataTotalLength], dataTotalLength, 0, dataTotalLength);
            iterateAndConvert(level, blockX, blockZ, genMode, totalColumnData, fullArrayView);
            columnArrayView.changeVerticalSizeFrom(totalColumnData);
		}
		else
		{
			iterateAndConvert(level, blockX, blockZ, genMode, columnArrayView, fullArrayView); //Directly use the arrayView since it fits.
		}
    }
	
	private static void iterateAndConvert(IDhClientLevel level, int blockX, int blockZ, int genMode, ColumnArrayView column, SingleFullArrayView data)
	{
		FullDataPointIdMap mapping = data.getMapping();
		boolean isVoid = true;
		int offset = 0;
		for (int i = 0; i < data.getSingleLength(); i++)
		{
			long fullData = data.getSingle(i);
			int bottomY = FullDataPointUtil.getBottomY(fullData);
			int blockHeight = FullDataPointUtil.getHeight(fullData);
			int id = FullDataPointUtil.getId(fullData);
			int light = FullDataPointUtil.getLight(fullData);
			IBiomeWrapper biome = mapping.getBiomeWrapper(id);
			IBlockStateWrapper block = mapping.getBlockStateWrapper(id);
			if (block.equals(AIR))
			{
				continue;
			}
			
			isVoid = false;
			int color = level.computeBaseColor(new DhBlockPos(blockX, bottomY + level.getMinY(), blockZ), biome, block);
			long columnData = RenderDataPointUtil.createDataPoint(bottomY + blockHeight, bottomY, color, light, genMode);
			column.set(offset, columnData);
			offset++;
		}
		
		if (isVoid)
		{
			column.set(0, RenderDataPointUtil.createVoidDataPoint((byte) genMode));
		}
	}
	
	
	
//    /** creates a vertical DataPoint */
//    private void writeVerticalData(long[] data, int dataOffset, int maxVerticalData,
//                                   IChunkWrapper chunk, LodBuilderConfig config, int chunkSubPosX, int chunkSubPosZ)
//    {
//
//        int totalVerticalData = (chunk.getHeight());
//        long[] dataToMerge = new long[totalVerticalData];
//
//        boolean hasCeiling = MC.getWrappedClientWorld().getDimensionType().hasCeiling();
//        boolean hasSkyLight = MC.getWrappedClientWorld().getDimensionType().hasSkyLight();
//        byte generation = config.distanceGenerationMode.complexity;
//        int count = 0;
//        // FIXME: This yAbs is just messy!
//        int x = chunk.getMinX() + chunkSubPosX;
//        int z = chunk.getMinZ() + chunkSubPosZ;
//        int y = chunk.getMaxY(x, z);
//
//        boolean topBlock = true;
//        if (y < chunk.getMinBuildHeight())
//            dataToMerge[0] = DataPointUtil.createVoidDataPoint(generation);
//        int maxConnectedLods = Config.Client.Graphics.Quality.verticalQuality.get().maxVerticalData[0];
//        while (y >= chunk.getMinBuildHeight()) {
//            int height = determineHeightPointFrom(chunk, config, x, y, z);
//            // If the lod is at the default height, it must be void data
//            if (height < chunk.getMinBuildHeight()) {
//                if (topBlock) dataToMerge[0] = DataPointUtil.createVoidDataPoint(generation);
//                break;
//            }
//            y = height - 1;
//            // We search light on above air block
//            int depth = determineBottomPointFrom(chunk, config, x, y, z,
//                    count < maxConnectedLods && (!hasCeiling || !topBlock));
//            if (hasCeiling && topBlock)
//                y = depth;
//            int light = getLightValue(chunk, x, y, z, hasCeiling, hasSkyLight, topBlock);
//            int color = generateLodColor(chunk, config, x, y, z);
//            int lightBlock = light & 0b1111;
//            int lightSky = (light >> 4) & 0b1111;
//            dataToMerge[count] = DataPointUtil.createDataPoint(height-chunk.getMinBuildHeight(), depth-chunk.getMinBuildHeight(),
//                    color, lightSky, lightBlock, generation);
//            topBlock = false;
//            y = depth - 1;
//            count++;
//        }
//        long[] result = DataPointUtil.mergeMultiData(dataToMerge, totalVerticalData, maxVerticalData);
//        if (result.length != maxVerticalData) throw new ArrayIndexOutOfBoundsException();
//        System.arraycopy(result, 0, data, dataOffset, maxVerticalData);
//    }
//
//    public static final ELodDirection[] DIRECTIONS = new ELodDirection[] {
//            ELodDirection.UP,
//            ELodDirection.DOWN,
//            ELodDirection.WEST,
//            ELodDirection.EAST,
//            ELodDirection.NORTH,
//            ELodDirection.SOUTH };
//
//    private boolean hasCliffFace(IChunkWrapper chunk, int x, int y, int z) {
//        for (ELodDirection dir : DIRECTIONS) {
//            IBlockDetailWrapper block = chunk.getBlockDetailAtFace(x, y, z, dir);
//            if (block == null || !block.hasFaceCullingFor(ELodDirection.OPPOSITE_DIRECTIONS[dir.ordinal()]))
//                return true;
//        }
//        return false;
//    }
//
//    /**
//     * Find the lowest valid point from the bottom.
//     * Used when creating a vertical LOD.
//     */
//    private int determineBottomPointFrom(IChunkWrapper chunk, LodBuilderConfig builderConfig, int xAbs, int yAbs, int zAbs, boolean strictEdge)
//    {
//        int depth = chunk.getMinBuildHeight();
//        IBlockDetailWrapper currentBlockDetail = null;
//        if (strictEdge)
//        {
//            IBlockDetailWrapper blockAbove = chunk.getBlockDetail(xAbs, yAbs + 1, zAbs);
//            if (blockAbove != null && Config.Client.WorldGenerator.tintWithAvoidedBlocks.get() && !blockAbove.shouldRender(Config.Client.WorldGenerator.blocksToAvoid.get()))
//            { // The above block is skipped. Lets use its skipped color for current block
//                currentBlockDetail = blockAbove;
//            }
//            if (currentBlockDetail == null) currentBlockDetail = chunk.getBlockDetail(xAbs, yAbs, zAbs);
//        }
//
//        for (int y = yAbs - 1; y >= chunk.getMinBuildHeight(); y--)
//        {
//            IBlockDetailWrapper nextBlock = chunk.getBlockDetail(xAbs, y, zAbs);
//            if (isLayerValidLodPoint(nextBlock)) {
//                if (!strictEdge) continue;
//                if (currentBlockDetail.equals(nextBlock)) continue;
//                if (!hasCliffFace(chunk, xAbs, y, zAbs)) continue;
//            }
//            depth = (y + 1);
//            break;
//        }
//        return depth;
//    }
//
//    /** Find the highest valid point from the Top */
//    private int determineHeightPointFrom(IChunkWrapper chunk, LodBuilderConfig config, int xAbs, int yAbs, int zAbs)
//    {
//        //TODO find a way to skip bottom of the world
//        int height = chunk.getMinBuildHeight()-1;
//        for (int y = yAbs; y >= chunk.getMinBuildHeight(); y--)
//        {
//            if (isLayerValidLodPoint(chunk, xAbs, y, zAbs))
//            {
//                height = (y + 1);
//                break;
//            }
//        }
//        return height;
//    }
//
//
//
//    // =====================//
//    // constructor helpers //
//    // =====================//
//
//    /**
//     * Generate the color for the given chunk using biome water color, foliage
//     * color, and grass color.
//     */
//    private int generateLodColor(IChunkWrapper chunk, LodBuilderConfig builderConfig, int x, int y, int z)
//    {
//        int colorInt;
//        if (builderConfig.useBiomeColors)
//        {
//            // I have no idea why I need to bit shift to the right, but
//            // if I don't the biomes don't show up correctly.
//            colorInt = chunk.getBiome(x, y, z).getColorForBiome(x, z);
//        }
//        else
//        {
//            // if we are skipping non-full and non-solid blocks that means we ignore
//            // snow, flowers, etc. Get the above block so we can still get the color
//            // of the snow, flower, etc. that may be above this block
//            colorInt = 0;
//            if (chunk.blockPosInsideChunk(x, y+1, z)) {
//                IBlockDetailWrapper blockAbove = chunk.getBlockDetail(x, y+1, z);
//                if (blockAbove != null && Config.Client.WorldGenerator.tintWithAvoidedBlocks.get() && !blockAbove.shouldRender(Config.Client.WorldGenerator.blocksToAvoid.get()))
//                {  // The above block is skipped. Lets use its skipped color for current block
//                    colorInt = blockAbove.getAndResolveFaceColor(null, chunk, new DHBlockPos(x, y+1, z));
//                }
//            }
//
//            // override this block's color if there was a block above this
//            // and we were avoiding non-full/non-solid blocks
//            if (colorInt == 0) {
//                IBlockDetailWrapper detail = chunk.getBlockDetail(x, y, z);
//                colorInt = detail.getAndResolveFaceColor(null, chunk, new DHBlockPos(x, y, z));
//            }
//        }
//
//        return colorInt;
//    }
//
//    /** Gets the light value for the given block position */
//    private int getLightValue(IChunkWrapper chunk, int x, int y, int z, boolean hasCeiling, boolean hasSkyLight, boolean topBlock)
//    {
//        int skyLight;
//        int blockLight;
//
//        int blockBrightness = chunk.getEmittedBrightness(x, y, z);
//        // get the air block above or below this block
//        if (hasCeiling && topBlock)
//            y--;
//        else
//            y++;
//
//        blockLight = chunk.getBlockLight(x, y, z);
//        skyLight = hasSkyLight ? chunk.getSkyLight(x, y, z) : 0;
//
//        if (blockLight == -1 || skyLight == -1)
//        {
//
//            ILevelWrapper world = MC.getWrappedServerWorld();
//
//            if (world != null)
//            {
//                // server world sky light (always accurate)
//                blockLight = world.getBlockLight(x, y, z);
//
//                if (topBlock && !hasCeiling && hasSkyLight)
//                    skyLight = DEFAULT_MAX_LIGHT;
//                else
//                    skyLight = hasSkyLight ? world.getSkyLight(x, y, z) : 0;
//
//                if (!topBlock && skyLight == 15)
//                {
//                    // we are on predicted terrain, and we don't know what the light here is,
//                    // lets just take a guess
//                    skyLight = 12;
//                }
//            }
//            else
//            {
//                world = MC.getWrappedClientWorld();
//                if (world == null)
//                {
//                    blockLight = 0;
//                    skyLight = 12;
//                }
//                else
//                {
//                    // client world sky light (almost never accurate)
//                    blockLight = world.getBlockLight(x, y, z);
//                    // estimate what the lighting should be
//                    if (hasSkyLight || !hasCeiling)
//                    {
//                        if (topBlock)
//                            skyLight = DEFAULT_MAX_LIGHT;
//                        else
//                        {
//                            if (hasSkyLight)
//                                skyLight = world.getSkyLight(x, y, z);
//                            //else
//                            //	skyLight = 0;
//                            if (!chunk.isLightCorrect() && (skyLight == 0 || skyLight == 15))
//                            {
//                                // we don't know what the light here is,
//                                // lets just take a guess
//                                skyLight = 12;
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        blockLight = LodUtil.clamp(0, Math.max(blockLight, blockBrightness), DEFAULT_MAX_LIGHT);
//        return blockLight + (skyLight << 4);
//    }
//
//    /** Is the block at the given blockPos a valid LOD point? */
//    private boolean isLayerValidLodPoint(IBlockDetailWrapper blockDetail)
//    {
//        EBlocksToAvoid avoid = Config.Client.WorldGenerator.blocksToAvoid.get();
//        return blockDetail != null && blockDetail.shouldRender(avoid);
//    }
//
//    /** Is the block at the given blockPos a valid LOD point? */
//    private boolean isLayerValidLodPoint(IChunkWrapper chunk, int x, int y, int z) {
//        EBlocksToAvoid avoid = Config.Client.WorldGenerator.blocksToAvoid.get();
//        IBlockDetailWrapper block = chunk.getBlockDetail(x, y, z);
//        return block != null && block.shouldRender(avoid);
//    }
}
