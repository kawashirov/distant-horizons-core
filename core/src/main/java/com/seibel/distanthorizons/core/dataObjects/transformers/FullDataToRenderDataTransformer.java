/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.core.dataObjects.transformers;

import com.seibel.distanthorizons.api.enums.config.EBlocksToAvoid;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.SingleColumnFullDataAccessor;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.CompleteFullDataSource;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.interfaces.IIncompleteFullDataSource;
import com.seibel.distanthorizons.core.dataObjects.render.ColumnRenderSource;
import com.seibel.distanthorizons.core.dataObjects.render.columnViews.ColumnArrayView;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhLodPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.util.FullDataPointUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.RenderDataPointUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.coreapi.util.BitShiftUtil;

import java.util.HashSet;

/**
 * Handles converting {@link ChunkSizedFullDataAccessor}, {@link IIncompleteFullDataSource},
 * and {@link IFullDataSource}'s to {@link ColumnRenderSource}.
 */
public class FullDataToRenderDataTransformer
{
	private static final IWrapperFactory WRAPPER_FACTORY = SingletonInjector.INSTANCE.get(IWrapperFactory.class);
	
	
	
	/**
	 * Called in loops that may run for an extended period of time. <br>
	 * This is necessary to allow canceling these transformers since running
	 * them after the client has left a given world will throw exceptions here.
	 */
	private static void throwIfThreadInterrupted() throws InterruptedException
	{
		if (Thread.interrupted())
		{
			throw new InterruptedException(FullDataToRenderDataTransformer.class.getSimpleName() + " task interrupted.");
		}
	}
	
	
	//==============//
	// transformers //
	//==============//
	
	/**
	 * Creates a LodNode for a chunk in the given world.
	 *
	 * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 * @throws InterruptedException Can be caused by interrupting the thread upstream.
	 * Generally thrown if the method is running after the client leaves the current world.
	 */
	public static ColumnRenderSource transformFullDataToColumnData(IDhClientLevel level, CompleteFullDataSource fullDataSource) throws InterruptedException
	{
		final DhSectionPos pos = fullDataSource.getSectionPos();
		final byte dataDetail = fullDataSource.getDataDetailLevel();
		final int vertSize = Config.Client.Advanced.Graphics.Quality.verticalQuality.get().calculateMaxVerticalData(fullDataSource.getDataDetailLevel());
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
					SingleColumnFullDataAccessor fullArrayView = fullDataSource.get(x, z);
					convertColumnData(level, baseX + x, baseZ + z, columnArrayView, fullArrayView, 1);
					
					if (fullArrayView.doesColumnExist())
					{
						LodUtil.assertTrue(columnSource.doesDataPointExist(x, z));
					}
				}
			}
			
			columnSource.fillDebugFlag(0, 0, ColumnRenderSource.SECTION_SIZE, ColumnRenderSource.SECTION_SIZE, ColumnRenderSource.DebugSourceFlag.FULL);
			
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
	 * Generally thrown if the method is running after the client leaves the current world.
	 */
	public static ColumnRenderSource transformIncompleteDataToColumnData(IDhClientLevel level, IIncompleteFullDataSource data) throws InterruptedException
	{
		final DhSectionPos pos = data.getSectionPos();
		final byte dataDetail = data.getDataDetailLevel();
		final int vertSize = Config.Client.Advanced.Graphics.Quality.verticalQuality.get().calculateMaxVerticalData(data.getDataDetailLevel());
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
					
					SingleColumnFullDataAccessor fullArrayView = data.tryGet(x, z);
					if (fullArrayView == null)
					{
						continue;
					}
					
					ColumnArrayView columnArrayView = columnSource.getVerticalDataPointView(x, z);
					convertColumnData(level, baseX + x, baseZ + z, columnArrayView, fullArrayView, 1);
					
					columnSource.fillDebugFlag(x, z, 1, 1, ColumnRenderSource.DebugSourceFlag.SPARSE);
					if (fullArrayView.doesColumnExist())
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
	 * Generally thrown if the method is running after the client leaves the current world.
	 * 
	 * @return true if any data was changed, false otherwise
	 */
	public static boolean writeFullDataChunkToColumnData(ColumnRenderSource renderSource, IDhClientLevel level, ChunkSizedFullDataAccessor chunkDataView) throws InterruptedException, IllegalArgumentException
	{
		final DhSectionPos renderSourcePos = renderSource.getSectionPos();
		
		final int sourceBlockX = renderSourcePos.getCorner().getCornerBlockPos().x;
		final int sourceBlockZ = renderSourcePos.getCorner().getCornerBlockPos().z;
		
		// offset between the incoming chunk data and this render source
		final int blockOffsetX = (chunkDataView.pos.x * LodUtil.CHUNK_WIDTH) - sourceBlockX;
		final int blockOffsetZ = (chunkDataView.pos.z * LodUtil.CHUNK_WIDTH) - sourceBlockZ;
		
		final int sourceDataPointBlockWidth = BitShiftUtil.powerOfTwo(renderSource.getDataDetail());
		
		boolean changed = false;
		
		if (chunkDataView.detailLevel == renderSource.getDataDetail())
		{
			renderSource.markNotEmpty();
			// confirm the render source contains this chunk
			if (blockOffsetX < 0
					|| blockOffsetX + LodUtil.CHUNK_WIDTH > renderSource.getWidthInDataPoints()
					|| blockOffsetZ < 0
					|| blockOffsetZ + LodUtil.CHUNK_WIDTH > renderSource.getWidthInDataPoints())
			{
				throw new IllegalArgumentException("Data offset is out of bounds");
			}
			
			throwIfThreadInterrupted();
			
			for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
			{
				for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
				{
					ColumnArrayView columnArrayView = renderSource.getVerticalDataPointView(blockOffsetX + x, blockOffsetZ + z);
					int hash = columnArrayView.getDataHash();
					SingleColumnFullDataAccessor fullArrayView = chunkDataView.get(x, z);
					convertColumnData(level,
							sourceBlockX + sourceDataPointBlockWidth * (blockOffsetX + x),
							sourceBlockZ + sourceDataPointBlockWidth * (blockOffsetZ + z),
							columnArrayView, fullArrayView, 2);
					changed |= hash != columnArrayView.getDataHash();
				}
			}
			renderSource.fillDebugFlag(blockOffsetX, blockOffsetZ, LodUtil.CHUNK_WIDTH, LodUtil.CHUNK_WIDTH, ColumnRenderSource.DebugSourceFlag.DIRECT);
		}
		else if (chunkDataView.detailLevel < renderSource.getDataDetail() && renderSource.getDataDetail() <= chunkDataView.getLodPos().detailLevel)
		{
			renderSource.markNotEmpty();
			// multiple chunk data points converting to 1 column data point
			DhLodPos dataCornerPos = chunkDataView.getLodPos().getCornerLodPos(chunkDataView.detailLevel);
			DhLodPos sourceCornerPos = renderSourcePos.getCorner(renderSource.getDataDetail());
			DhLodPos sourceStartingChangePos = dataCornerPos.convertToDetailLevel(renderSource.getDataDetail());
			int relStartX = Math.floorMod(sourceStartingChangePos.x, renderSource.getWidthInDataPoints());
			int relStartZ = Math.floorMod(sourceStartingChangePos.z, renderSource.getWidthInDataPoints());
			int dataToSourceScale = sourceCornerPos.getWidthAtDetail(chunkDataView.detailLevel);
			int columnsInChunk = chunkDataView.getLodPos().getWidthAtDetail(renderSource.getDataDetail());
			
			for (int ox = 0; ox < columnsInChunk; ox++)
			{
				for (int oz = 0; oz < columnsInChunk; oz++)
				{
					int relSourceX = relStartX + ox;
					int relSourceZ = relStartZ + oz;
					ColumnArrayView columnArrayView = renderSource.getVerticalDataPointView(relSourceX, relSourceZ);
					int hash = columnArrayView.getDataHash();
					SingleColumnFullDataAccessor fullArrayView = chunkDataView.get(ox * dataToSourceScale, oz * dataToSourceScale);
					convertColumnData(level,
							sourceBlockX + sourceDataPointBlockWidth * relSourceX,
							sourceBlockZ + sourceDataPointBlockWidth * relSourceZ,
							columnArrayView, fullArrayView, 2);
					changed |= hash != columnArrayView.getDataHash();
				}
			}
			renderSource.fillDebugFlag(relStartX, relStartZ, columnsInChunk, columnsInChunk, ColumnRenderSource.DebugSourceFlag.DIRECT);
		}
		else if (chunkDataView.getLodPos().detailLevel < renderSource.getDataDetail())
		{
			// The entire chunk is being converted to a single column data point, possibly.
			DhLodPos dataCornerPos = chunkDataView.getLodPos().getCornerLodPos(chunkDataView.detailLevel);
			DhLodPos sourceCornerPos = renderSourcePos.getCorner(renderSource.getDataDetail());
			DhLodPos sourceStartingChangePos = dataCornerPos.convertToDetailLevel(renderSource.getDataDetail());
			int chunksPerColumn = sourceStartingChangePos.getWidthAtDetail(chunkDataView.getLodPos().detailLevel);
			if (chunkDataView.getLodPos().x % chunksPerColumn != 0 || chunkDataView.getLodPos().z % chunksPerColumn != 0)
			{
				return false; // not a multiple of the column size, so no change
			}
			int relStartX = Math.floorMod(sourceStartingChangePos.x, renderSource.getWidthInDataPoints());
			int relStartZ = Math.floorMod(sourceStartingChangePos.z, renderSource.getWidthInDataPoints());
			ColumnArrayView columnArrayView = renderSource.getVerticalDataPointView(relStartX, relStartZ);
			int hash = columnArrayView.getDataHash();
			SingleColumnFullDataAccessor fullArrayView = chunkDataView.get(0, 0);
			convertColumnData(level, dataCornerPos.x * sourceDataPointBlockWidth,
					dataCornerPos.z * sourceDataPointBlockWidth,
					columnArrayView, fullArrayView, 2);
			changed = hash != columnArrayView.getDataHash();
			renderSource.fillDebugFlag(relStartX, relStartZ, 1, 1, ColumnRenderSource.DebugSourceFlag.DIRECT);
		}
		return changed;
	}
	
	private static void convertColumnData(IDhClientLevel level, int blockX, int blockZ, ColumnArrayView columnArrayView, SingleColumnFullDataAccessor fullArrayView, int genMode)
	{
		if (!fullArrayView.doesColumnExist())
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
	
	private static void iterateAndConvert(IDhClientLevel level, int blockX, int blockZ, int genMode, ColumnArrayView column, SingleColumnFullDataAccessor data)
	{
		boolean avoidSolidBlocks = (Config.Client.Advanced.Graphics.Quality.blocksToIgnore.get() == EBlocksToAvoid.NON_COLLIDING);
		boolean colorBelowWithAvoidedBlocks = Config.Client.Advanced.Graphics.Quality.tintWithAvoidedBlocks.get();
		
		FullDataPointIdMap fullDataMapping = data.getMapping();
		HashSet<IBlockStateWrapper> blockStatesToIgnore = WRAPPER_FACTORY.getRendererIgnoredBlocks(level.getLevelWrapper());
		
		boolean isVoid = true;
		int colorToApplyToNextBlock = -1;
		int columnOffset = 0;
		
		// goes from the top down
		for (int i = 0; i < data.getSingleLength(); i++)
		{
			long fullData = data.getSingle(i);
			int bottomY = FullDataPointUtil.getBottomY(fullData);
			int blockHeight = FullDataPointUtil.getHeight(fullData);
			int id = FullDataPointUtil.getId(fullData);
			int light = FullDataPointUtil.getLight(fullData);
			IBiomeWrapper biome = fullDataMapping.getBiomeWrapper(id);
			IBlockStateWrapper block = fullDataMapping.getBlockStateWrapper(id);
			
			if (blockStatesToIgnore.contains(block))
			{
				// Don't render: air, barriers, light blocks, etc.
				continue;
			}
			
			
			// solid block check
			if (avoidSolidBlocks && !block.isSolid() && !block.isLiquid())
			{
				if (colorBelowWithAvoidedBlocks)
				{
					colorToApplyToNextBlock = level.computeBaseColor(new DhBlockPos(blockX, bottomY + level.getMinY(), blockZ), biome, block);
				}
				
				// don't add this block
				continue;
			}
			
			
			int color;
			if (colorToApplyToNextBlock == -1)
			{
				// use this block's color
				color = level.computeBaseColor(new DhBlockPos(blockX, bottomY + level.getMinY(), blockZ), biome, block);
			}
			else
			{
				// use the previous block's color
				color = colorToApplyToNextBlock;
				colorToApplyToNextBlock = -1;
			}
			
			
			// add the block
			isVoid = false;
			long columnData = RenderDataPointUtil.createDataPoint(bottomY + blockHeight, bottomY, color, light, genMode);
			column.set(columnOffset, columnData);
			columnOffset++;
		}
		
		
		if (isVoid)
		{
			column.set(0, RenderDataPointUtil.createVoidDataPoint((byte) genMode));
		}
	}


}
