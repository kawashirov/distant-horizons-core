package com.seibel.lod.core.api.external.methods.data;

import com.seibel.lod.api.objects.DhApiResult;
import com.seibel.lod.api.objects.data.DhApiTerrainDataPoint;
import com.seibel.lod.api.interfaces.data.IDhApiTerrainDataRepo;
import com.seibel.lod.core.api.internal.SharedApi;
import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.datatype.full.FullDataPoint;
import com.seibel.lod.core.datatype.full.FullDataPointIdMap;
import com.seibel.lod.core.datatype.full.accessor.SingleFullArrayView;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.level.DhClientServerLevel;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Allows getting and setting any terrain data Distant Horizons has stored.
 *
 * @author James Seibel
 * @version 2022-11-12
 */
public class DhApiTerrainDataRepo implements IDhApiTerrainDataRepo
{
	public static DhApiTerrainDataRepo INSTANCE = new DhApiTerrainDataRepo();
	
	private static final Logger LOGGER = LogManager.getLogger(DhApiTerrainDataRepo.class.getSimpleName());
	
	// debugging values
	private static volatile boolean debugThreadRunning = false;
	private static String currentDebugBiomeName = "";
	private static int currentDebugBlockColorInt = -1;
	
	
	
	private DhApiTerrainDataRepo() 
	{
		
	}
	
	
	
	@Override
	public DhApiTerrainDataPoint getDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ) { throw new UnsupportedOperationException(); }
	@Override
	public DhApiResult setDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	@Override
	public DhApiTerrainDataPoint getDataAtChunkPos(int chunkPosX, int chunkPosZ) { throw new UnsupportedOperationException(); }
	@Override
	public DhApiResult setDataAtChunkPos(int chunkPosX, int chunkPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	@Override
	public DhApiTerrainDataPoint getDataAtRegionPos(int regionPosX, int regionPosZ) { throw new UnsupportedOperationException(); }
	@Override
	public DhApiResult setDataAtRegionPos(int regionPosX, int regionPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	
	@Override
	public DhApiTerrainDataPoint getDataAtDetailLevelAndPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ) { throw new UnsupportedOperationException(); }
	@Override
	public DhApiResult setDataAtRegionPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	
	
	//===============//
	// debug methods //
	//===============//
	
	/**
	 * Shouldn't be visible to API users, is only valid
	 * for use on singleplayer worlds, and should only be
	 * used for debugging. <br><br>
	 *
	 * Suggested use when testing is to call this during the ClientApi.render() method.
	 *
	 * @param levelWrapper which level the player is in
	 */
	public static void logTopBlockAtBlockPosition(ILevelWrapper levelWrapper, DhBlockPos blockPos)
	{
		IMinecraftClientWrapper mcClientWrapper = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
		
		if (!debugThreadRunning)
		{
			debugThreadRunning = true;
			
			// thread to prevent locking up the render thread
			Thread thread = new Thread(() ->
			{
				try
				{
					IDhLevel level = SharedApi.currentWorld.getLevel(levelWrapper);
					DhClientServerLevel serverLevel = (DhClientServerLevel) level;
					
					DhLodPos inputPos = new DhLodPos(LodUtil.BLOCK_DETAIL_LEVEL, blockPos.x, blockPos.z);
					
					DhSectionPos sectionPos = inputPos.getSectionPosWithSectionDetailLevel(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
					DhLodPos relativePos = inputPos.getDhSectionRelativePositionForDetailLevel(LodUtil.BLOCK_DETAIL_LEVEL);
					
					
					// attempt to get the data source for this section
					ILodDataSource dataSource = serverLevel.dataFileHandler.read(sectionPos).get();
					if (dataSource != null)
					{
						// attempt to get the LOD data from the data source
						FullDataPointIdMap mapping = dataSource.getMapping();
						SingleFullArrayView dataColumn = dataSource.tryGet(relativePos.x, relativePos.z);
						if (dataColumn == null)
						{
							logBlockBiomeDebugInfoIfDifferent("", -1);
						}
						else
						{
							int yIndex = 0;
							int maxYIndex = dataColumn.getSingleLength() - 1;
							long dataPoint = 0;
							IBlockStateWrapper currentBlockState = null;
							
							// search top down for the top-most (non-air) block
							while (yIndex < maxYIndex && (currentBlockState == null || currentBlockState.serialize().equals("AIR")))
							{
								dataPoint = dataColumn.getSingle(yIndex);
								if (dataPoint != 0)
								{
									currentBlockState = mapping.getBlockStateWrapper(FullDataPoint.getId(dataPoint));
								}
								yIndex++;
							}
							
							
							// log the LOD data if present
							if (dataPoint != 0)
							{
								logBlockBiomeDebugInfoIfDifferent(levelWrapper, mcClientWrapper.getPlayerBlockPos(), dataPoint, mapping);
							}
							else
							{
								// no block data was found for this column
								logBlockBiomeDebugInfoIfDifferent("[VOID]", -1);
							}
						}
					}
				}
				catch (InterruptedException | ExecutionException e)
				{
					// shouldn't normally happen, but just in case
					e.printStackTrace();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					debugThreadRunning = false;
				}
			});
			
			thread.start();
		}
	}
	/** only logs the data if it was different vs the currently stored debug data */
	private static void logBlockBiomeDebugInfoIfDifferent(ILevelWrapper levelWrapper, DhBlockPos blockPos, long dataPoint, FullDataPointIdMap mapping)
	{
		int id = FullDataPoint.getId(dataPoint);
		
		IBiomeWrapper biome = mapping.getBiomeWrapper(id);
		IBlockStateWrapper blockState = mapping.getBlockStateWrapper(id);
		
		String newBiomeName = biome.serialize();
		int newBlockColorInt = ((IClientLevelWrapper)levelWrapper).computeBaseColor(blockPos, biome, blockState);
		logBlockBiomeDebugInfoIfDifferent(newBiomeName, newBlockColorInt);
	}
	/** only logs the data if it was different vs the currently stored debug data */
	private static void logBlockBiomeDebugInfoIfDifferent(String newBiomeName, int newBlockColorInt)
	{
		if (!currentDebugBiomeName.equals(newBiomeName) || currentDebugBlockColorInt != newBlockColorInt)
		{
			Color newBlockColor = colorFromBlockInt(newBlockColorInt);
			
			currentDebugBiomeName = newBiomeName;
			currentDebugBlockColorInt = newBlockColorInt;
			LOGGER.info(newBiomeName + " " + newBlockColor);
		}
	}
	private static Color colorFromBlockInt(int colorInt) { return new Color(ColorUtil.getRed(colorInt), ColorUtil.getGreen(colorInt), ColorUtil.getBlue(colorInt), ColorUtil.getAlpha(colorInt)); }
	
	
}
