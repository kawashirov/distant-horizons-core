package com.seibel.lod.core.api.external.methods.data;

import com.seibel.lod.api.interfaces.world.IDhApiLevelWrapper;
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


/**
 * Allows getting and setting any terrain data Distant Horizons has stored.
 *
 * @author James Seibel
 * @version 2022-11-13
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
	public DhApiResult<DhApiTerrainDataPoint> getSingleDataPointAtBlockPos(IDhApiLevelWrapper levelWrapper, int blockPosX, int blockPosY, int blockPosZ) 
	{
		return getTerrainDataAtBlockYPos(levelWrapper, new DhLodPos(LodUtil.BLOCK_DETAIL_LEVEL, blockPosX, blockPosZ), blockPosY);
	}
	@Override
	public DhApiResult<DhApiTerrainDataPoint[]> getColumnDataAtBlockPos(IDhApiLevelWrapper levelWrapper, int blockPosX, int blockPosZ) 
	{
		return getTerrainDataArray(levelWrapper, new DhLodPos(LodUtil.BLOCK_DETAIL_LEVEL, blockPosX, blockPosZ), null);
	}
//	@Override
//	public DhApiResult setDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	@Override
	public DhApiResult<DhApiTerrainDataPoint[]> getColumnDataAtChunkPos(IDhApiLevelWrapper levelWrapper, int chunkPosX, int chunkPosZ)
	{
		return getTerrainDataArray(levelWrapper, new DhLodPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkPosX, chunkPosZ), null);
	}
//	@Override
//	public DhApiResult setDataAtChunkPos(int chunkPosX, int chunkPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	@Override
	public DhApiResult<DhApiTerrainDataPoint[]> getColumnDataAtRegionPos(IDhApiLevelWrapper levelWrapper, int regionPosX, int regionPosZ)
	{
		return getTerrainDataArray(levelWrapper, new DhLodPos(LodUtil.REGION_DETAIL_LEVEL, regionPosX, regionPosZ), null);
	}
//	@Override
//	public DhApiResult setDataAtRegionPos(int regionPosX, int regionPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	
	@Override
	public DhApiResult<DhApiTerrainDataPoint[]> getColumnDataAtDetailLevelAndPos(IDhApiLevelWrapper levelWrapper, byte detailLevel, int posX, int posZ)
	{
		return getTerrainDataArray(levelWrapper, new DhLodPos(detailLevel, posX, posZ), null);
	}
//	@Override
//	public DhApiResult setDataAtRegionPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	
	
	//================//
	// Getter Methods //
	//================//
	
	/** Returns a single API terrain datapoint that contains the given Y block position */
	private static DhApiResult<DhApiTerrainDataPoint> getTerrainDataAtBlockYPos(IDhApiLevelWrapper levelWrapper, DhLodPos requestedColumnPos, Integer blockYPos)
	{
		DhApiResult<DhApiTerrainDataPoint[]> result = getTerrainDataArray(levelWrapper, requestedColumnPos, blockYPos);
		if (result.success && result.payload.length > 0)
		{
			return DhApiResult.createSuccess(result.errorMessage, result.payload[0]);
		}
		else
		{
			return DhApiResult.createFail(result.errorMessage);
		}
	}
	
	/** 
	 * If nullableBlockYPos is null: returns every datapoint in the column defined by the DhLodPos. <br>
	 * If nullableBlockYPos is NOT null: returns a single datapoint in the column defined by the DhLodPos which contains the block Y position. <br><br>
	 * 
	 * Returns an empty array if no data could be returned.
	 */
	private static DhApiResult<DhApiTerrainDataPoint[]> getTerrainDataArray(IDhApiLevelWrapper levelWrapper, DhLodPos requestedColumnPos, Integer nullableBlockYPos)
	{
		if (SharedApi.currentWorld == null)
		{
			return DhApiResult.createFail("Unable to get terrain data before the world has loaded.");
		}
		if (!(levelWrapper instanceof ILevelWrapper coreLevelWrapper))
		{
			// custom level wrappers aren't supported,
			// the API user must get a level wrapper from our code somewhere
			return DhApiResult.createFail("Unsupported [" + IDhApiLevelWrapper.class.getSimpleName() + "] implementation, only the core class [" + IDhLevel.class.getSimpleName() + "] is a valid parameter.");
		}
		
		IDhLevel level = SharedApi.currentWorld.getLevel(coreLevelWrapper);
		if (level == null)
		{
			return DhApiResult.createFail("Unable to get terrain data before the world has loaded.");
		}
//		DhClientServerLevel serverLevel = (DhClientServerLevel) level;
//		IDhLevel serverLevel = (IDhLevel) level;
		
		// get the detail levels for this request
		byte requestedDetailLevel = requestedColumnPos.detailLevel;
		byte sectionDetailLevel = (byte) (requestedDetailLevel + DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL);
		
		// get the positions for this request
		DhSectionPos sectionPos = requestedColumnPos.getSectionPosWithSectionDetailLevel(sectionDetailLevel);
		DhLodPos relativePos = requestedColumnPos.getDhSectionRelativePositionForDetailLevel();
		
		
		try
		{
			// attempt to get/generate the data source for this section
			ILodDataSource dataSource = level.getFileHandler().read(sectionPos).get();
			if (dataSource == null)
			{
				return DhApiResult.createFail("Unable to find/generate any data at the " + DhSectionPos.class.getSimpleName() + " [" + sectionPos + "].");
			}
			else
			{
				// attempt to get the LOD data from the data source
				FullDataPointIdMap mapping = dataSource.getMapping();
				SingleFullArrayView dataColumn = dataSource.tryGet(relativePos.x, relativePos.z);
				if (dataColumn != null)
				{
					int dataColumnIndexCount = dataColumn.getSingleLength();
					DhApiTerrainDataPoint[] returnArray = new DhApiTerrainDataPoint[dataColumnIndexCount];
					long dataPoint;
					
					boolean getSpecificYCoordinate = nullableBlockYPos != null;
					int levelMinimumHeight = levelWrapper.getMinHeight();
					
					
					// search for a datapoint that contains the block y position
					for (int i = 0; i < dataColumnIndexCount; i++)
					{
						dataPoint = dataColumn.getSingle(i);
						
						if (!getSpecificYCoordinate)
						{
							// if we aren't look for a specific datapoint, add each datapoint to the return array
							returnArray[i] = generateApiDatapoint(mapping, requestedDetailLevel, dataPoint);
						}
						else
						{
							// we are looking for a specific datapoint,
							// don't look at null ones
							if (dataPoint != 0)
							{
								int requestedY = nullableBlockYPos;
								int bottomY = FullDataPoint.getY(dataPoint) + levelMinimumHeight; // TODO rename getY to getBottomY
								int height = FullDataPoint.getDepth(dataPoint); // TODO rename to getHeight
								int topY = bottomY + height;
								
								// does this datapoint contain the requested Y position? 
								if (bottomY < requestedY && requestedY <= topY)
								{
									// this datapoint contains the requested block position, return it
									DhApiTerrainDataPoint apiTerrainData = generateApiDatapoint(mapping, requestedDetailLevel, dataPoint);
									return DhApiResult.createSuccess(new DhApiTerrainDataPoint[]{apiTerrainData});
								}
							}
						}
					}
					
					// return all collected data
					return DhApiResult.createSuccess(returnArray);
				}
				
				// the requested data wasn't present in this column (and/or the column wasn't able to be accessed/generated)
				return DhApiResult.createSuccess(new DhApiTerrainDataPoint[0]);
			}
		}
		catch (InterruptedException | ExecutionException e)
		{
			// shouldn't normally happen, but just in case
			e.printStackTrace();
			return DhApiResult.createFail("Unexpected exception: [" + e.getMessage() + "].");
		}
	}
	
	private static DhApiTerrainDataPoint generateApiDatapoint(FullDataPointIdMap mapping, byte detailLevel, long dataPoint)
	{
		IBlockStateWrapper blockState = mapping.getBlockStateWrapper(FullDataPoint.getId(dataPoint));
		IBiomeWrapper biomeWrapper = mapping.getBiomeWrapper(FullDataPoint.getId(dataPoint));
		
		int topY = FullDataPoint.getY(dataPoint);
		int depth = FullDataPoint.getDepth(dataPoint);
		int bottomY = topY - depth;
		
		return new DhApiTerrainDataPoint(detailLevel, 
				FullDataPoint.getLight(dataPoint), topY, bottomY,
				blockState, biomeWrapper);
	}	
	
	
	
	//===============//
	// debug methods //
	//===============//
	
	/** debug methods need to be async because pausing the main thread to debug and hot swapping will crash the program */
	public static void asyncDebugMethod(IDhApiLevelWrapper levelWrapper, int blockPosX, int blockPosY, int blockPosZ)
	{
		if (!debugThreadRunning)
		{
			debugThreadRunning = true;
			Thread thread = new Thread(() -> {
				try
				{
					DhApiResult<DhApiTerrainDataPoint> x = getTerrainDataAtBlockYPos(levelWrapper, new DhLodPos(LodUtil.BLOCK_DETAIL_LEVEL, blockPosX, blockPosZ), blockPosY);
					DhApiResult<DhApiTerrainDataPoint[]> y = getTerrainDataArray(levelWrapper, new DhLodPos(LodUtil.BLOCK_DETAIL_LEVEL, blockPosX, blockPosZ), null);
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
