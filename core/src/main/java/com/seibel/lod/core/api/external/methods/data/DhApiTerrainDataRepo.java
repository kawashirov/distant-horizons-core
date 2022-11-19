package com.seibel.lod.core.api.external.methods.data;

import com.seibel.lod.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.objects.DhApiResult;
import com.seibel.lod.api.objects.data.DhApiRaycastResult;
import com.seibel.lod.api.objects.data.DhApiTerrainDataPoint;
import com.seibel.lod.api.interfaces.data.IDhApiTerrainDataRepo;
import com.seibel.lod.api.objects.math.DhApiVec3i;
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
import com.seibel.lod.core.util.*;
import com.seibel.lod.core.util.math.Vec3d;
import com.seibel.lod.core.util.math.Vec3f;
import com.seibel.lod.core.util.math.Vec3i;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;


/**
 * Allows interfacing with the terrain data Distant Horizons has stored.
 *
 * @author James Seibel
 * @version 2022-11-19
 */
public class DhApiTerrainDataRepo implements IDhApiTerrainDataRepo
{
	public static DhApiTerrainDataRepo INSTANCE = new DhApiTerrainDataRepo();
	
	private static final Logger LOGGER = LogManager.getLogger(DhApiTerrainDataRepo.class.getSimpleName());
	
	// debugging values
	private static volatile boolean debugThreadRunning = false;
	private static String currentDebugBiomeName = "";
	private static int currentDebugBlockColorInt = -1;
	private static DhApiVec3i currentDebugVec3i = new Vec3i();
	
	
	
	private DhApiTerrainDataRepo() 
	{
		
	}
	
	
	
	public DhApiResult<DhApiRaycastResult> raycastLodData(IDhApiLevelWrapper levelWrapper, 
			double rayOriginX, double rayOriginY, double rayOriginZ, 
			float rayDirectionX, float rayDirectionY, float rayDirectionZ,
			int maxRayBlockLength)
	{
		// TODO add an overloaded method with a default max length
		return this.raycastLodData(levelWrapper, new Vec3d(rayOriginX, rayOriginY, rayOriginZ), new Vec3f(rayDirectionX, rayDirectionY, rayDirectionZ), maxRayBlockLength);
	}
	
	/** 
	 * private since it uses non-API objects <br><br>
	 * 
	 * Works by walking through the world and attempting to get the LOD <br>
	 * data present at each step.
	 */
	private DhApiResult<DhApiRaycastResult> raycastLodData(IDhApiLevelWrapper levelWrapper, Vec3d rayOrigin, Vec3f rayDirection, int maxRayBlockLength)
	{
		rayDirection.normalize();
		
		int minBlockHeight = levelWrapper.getMinHeight();
		int maxBlockHeight = levelWrapper.getHeight();
		
		
		
		// walk through the grid //
		
		int currentLength = 0;
		
		// the exact position of this step
		Vec3d exactPos = new Vec3d(rayOrigin.x, rayOrigin.y, rayOrigin.z);
		// the block position for this step
		Vec3i blockPos = new Vec3i((int) Math.round(rayOrigin.x), (int) Math.round(rayOrigin.y), (int) Math.round(rayOrigin.z));
		
		DhApiRaycastResult closetFoundDataPoint = null;
		
		
		while (blockPos.y >= minBlockHeight && blockPos.y < maxBlockHeight
				&& currentLength <= maxRayBlockLength)
		{
			// get the LOD columns around this position
			ArrayList<Vec3i> columnPositions = getIntersectingColumnsAtPosition(blockPos, rayDirection);
			for (Vec3i columnPos : columnPositions)
			{
				// check each column
				DhApiResult<DhApiTerrainDataPoint[]> result = this.getColumnDataAtBlockPos(levelWrapper, columnPos.x, columnPos.z);
				if (!result.success)
				{
					// if there was an error, stop and return it
					return DhApiResult.createFail(result.errorMessage);
				}
				
				// is there a LOD at this position?
				for (DhApiTerrainDataPoint dataPoint : result.payload)
				{
					// is this LOD air?
					if (dataPoint.blockStateWrapper != null && !dataPoint.blockStateWrapper.isAir())
					{
						// does this LOD contain the given Y position?
						Vec3i dataPointPos = new Vec3i(columnPos.x, dataPoint.bottomYBlockPos, columnPos.z);
						if (exactPos.y >= dataPoint.bottomYBlockPos &&  exactPos.y <= dataPoint.topYBlockPos)
						{
							if (closetFoundDataPoint == null)
							{
								closetFoundDataPoint = new DhApiRaycastResult(dataPoint, dataPointPos);
							}
							else
							{
								// use the LOD closest to the ray's origin
								double previousDistanceSquared = Math.pow(rayOrigin.x - closetFoundDataPoint.pos.x, 2) + Math.pow(rayOrigin.y - closetFoundDataPoint.pos.y, 2) + Math.pow(rayOrigin.z - closetFoundDataPoint.pos.z, 2);
								double newDistanceSquared = Math.pow(rayOrigin.x - dataPointPos.x, 2) + Math.pow(rayOrigin.y - dataPointPos.y, 2) + Math.pow(rayOrigin.z - dataPointPos.z, 2);
								
								if (previousDistanceSquared > newDistanceSquared)
								{
									closetFoundDataPoint = new DhApiRaycastResult(dataPoint, dataPointPos);
								}
							}
						}
					}
				}
			}
			
			if (closetFoundDataPoint != null)
			{
				return DhApiResult.createSuccess(closetFoundDataPoint);
			}
			
			
			
			// take the next step in the ray //
			
			exactPos.x += rayDirection.x;
			exactPos.y += rayDirection.y;
			exactPos.z += rayDirection.z;
			
			blockPos.x = (int) Math.round(exactPos.x);
			blockPos.y = (int) Math.round(exactPos.y);
			blockPos.z = (int) Math.round(exactPos.z);
			
			// calculate the taxiCab Distance
			currentLength = (int) (Math.abs(rayOrigin.x - exactPos.x) + Math.abs(rayOrigin.y - exactPos.y) + Math.abs(rayOrigin.z - exactPos.z));
		}
		
		return DhApiResult.createSuccess(null);
	}
	
	/** 
	 * checks the surrounding 3x3 block columns and returns those that intersect with the ray. <br><br>
	 * 
	 * Used to make sure the raycast step doesn't accidentally walk over any adjacent data.
	 */
	private static ArrayList<Vec3i> getIntersectingColumnsAtPosition(Vec3i rayEndingPos, Vec3f rayDirection)
	{
		ArrayList<Vec3i> returnList = new ArrayList<Vec3i>(9);
		
		for (int x = -1; x <= 1; x++)
		{
			for (int z = -1; z <= 1; z++)
			{
				Vec3i pos = new Vec3i(rayEndingPos.x + x, rayEndingPos.y, rayEndingPos.z + z);
				
				// check if this column is intersected by the ray
				if (RayCastUtil.rayIntersectsSquare(rayEndingPos.x, rayEndingPos.z, rayDirection.x, rayDirection.z, pos.x, pos.z, 1))
				{
					returnList.add(pos);
				}
			}
		}
		
		return returnList;
	}	
	
	
	
	
	@Override
	public DhApiResult<DhApiTerrainDataPoint> getSingleDataPointAtBlockPos(IDhApiLevelWrapper levelWrapper, int blockPosX, int blockPosY, int blockPosZ) 
	{
		return getTerrainDataAtBlockYPos(levelWrapper, new DhLodPos(LodUtil.BLOCK_DETAIL_LEVEL, blockPosX, blockPosZ), blockPosY);
	}
	@Override
	public DhApiResult<DhApiTerrainDataPoint[]> getColumnDataAtBlockPos(IDhApiLevelWrapper levelWrapper, int blockPosX, int blockPosZ) 
	{
		return getTerrainDataColumnArray(levelWrapper, new DhLodPos(LodUtil.BLOCK_DETAIL_LEVEL, blockPosX, blockPosZ), null);
	}
//	@Override
//	public DhApiResult setDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	@Override
	public DhApiResult<DhApiTerrainDataPoint[][][]> getAllTerrainDataAtChunkPos(IDhApiLevelWrapper levelWrapper, int chunkPosX, int chunkPosZ)
	{
		return getTerrainDataOverAreaForPositionDetailLevel(levelWrapper, new DhLodPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkPosX, chunkPosZ));
	}
//	@Override
//	public DhApiResult setDataAtChunkPos(int chunkPosX, int chunkPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	@Override
	public DhApiResult<DhApiTerrainDataPoint[][][]> getAllTerrainDataAtRegionPos(IDhApiLevelWrapper levelWrapper, int regionPosX, int regionPosZ)
	{
		return getTerrainDataOverAreaForPositionDetailLevel(levelWrapper, new DhLodPos(LodUtil.REGION_DETAIL_LEVEL, regionPosX, regionPosZ));
	}
//	@Override
//	public DhApiResult setDataAtRegionPos(int regionPosX, int regionPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	
	@Override
	public DhApiResult<DhApiTerrainDataPoint[][][]> getAllTerrainDataAtDetailLevelAndPos(IDhApiLevelWrapper levelWrapper, byte detailLevel, int posX, int posZ)
	{
		return getTerrainDataOverAreaForPositionDetailLevel(levelWrapper, new DhLodPos(detailLevel, posX, posZ));
	}
//	@Override
//	public DhApiResult setDataAtRegionPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	
	
	//================//
	// Getter Methods //
	//================//
	
	/** Returns a single API terrain datapoint that contains the given Y block position */
	private static DhApiResult<DhApiTerrainDataPoint> getTerrainDataAtBlockYPos(IDhApiLevelWrapper levelWrapper, DhLodPos requestedColumnPos, Integer blockYPos)
	{
		DhApiResult<DhApiTerrainDataPoint[]> result = getTerrainDataColumnArray(levelWrapper, requestedColumnPos, blockYPos);
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
	 * Returns all the block columns represented by the given {@link DhLodPos}. <br>
	 * IE, A position with the detail level: <br>
	 * 0 (block): will return a 1x1 matrix of data. (don't do this, we have a specific method for that.) <br>
	 * 1 (2 blocks): will return a 2x2 matrix of data. <br>
	 * 4 (chunk): will return a 16x16 matrix of data. <br> <br>
	 * 
	 * will stop and return the in progress data if any errors are encountered. 
	 */
	private static DhApiResult<DhApiTerrainDataPoint[][][]> getTerrainDataOverAreaForPositionDetailLevel(IDhApiLevelWrapper levelWrapper, DhLodPos requestedAreaPos)
	{
		DhLodPos startingBlockPos = requestedAreaPos.getCorner(LodUtil.BLOCK_DETAIL_LEVEL);
		int widthOfAreaInBlocks = BitShiftUtil.powerOfTwo(requestedAreaPos.detailLevel);
		
		DhApiTerrainDataPoint[][][] returnArray = new DhApiTerrainDataPoint[widthOfAreaInBlocks][widthOfAreaInBlocks][];
		int dataColumnsReturned = 0;
		
		// get each column over the area
		for (var x = 0; x < widthOfAreaInBlocks; x++)
		{
			for (var z = 0; z < widthOfAreaInBlocks; z++)
			{
				DhLodPos blockColumnPos = new DhLodPos(LodUtil.BLOCK_DETAIL_LEVEL, startingBlockPos.x + x, startingBlockPos.z + z);
				DhApiResult<DhApiTerrainDataPoint[]> result = getTerrainDataColumnArray(levelWrapper, blockColumnPos, null);
				if (result.success)
				{
					returnArray[x][z] = result.payload;
					dataColumnsReturned++;
				}
				else
				{
					return DhApiResult.createFail(result.errorMessage, returnArray);
				}
			}
		}
		
		return dataColumnsReturned != 0 ? DhApiResult.createSuccess("[" + dataColumnsReturned + "] columns returned.", returnArray) : DhApiResult.createSuccess("No data found.", returnArray);
	}
	
	/** 
	 * If nullableBlockYPos is null: returns every datapoint in the column defined by the DhLodPos. <br>
	 * If nullableBlockYPos is NOT null: returns a single datapoint in the column defined by the DhLodPos which contains the block Y position. <br><br>
	 * 
	 * If the ApiResult is successful there will be an array of data. <br>
	 * The returned array will be empty if no data could be retrieved.
	 */
	private static DhApiResult<DhApiTerrainDataPoint[]> getTerrainDataColumnArray(IDhApiLevelWrapper levelWrapper, DhLodPos requestedColumnPos, Integer nullableBlockYPos)
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
							returnArray[i] = generateApiDatapoint(levelWrapper, mapping, requestedDetailLevel, dataPoint);
						}
						else
						{
							// we are looking for a specific datapoint,
							// don't look at null ones
							if (dataPoint != 0)
							{
								int requestedY = nullableBlockYPos;
								int bottomY = FullDataPoint.getBottomY(dataPoint) + levelMinimumHeight;
								int height = FullDataPoint.getHeight(dataPoint);
								int topY = bottomY + height;
								
								// does this datapoint contain the requested Y position? 
								if (bottomY <= requestedY && requestedY < topY) // blockPositions start from the bottom of the block, thus "<=" for bottomY, just "<" for topY
								{
									// this datapoint contains the requested block position, return it
									DhApiTerrainDataPoint apiTerrainData = generateApiDatapoint(levelWrapper, mapping, requestedDetailLevel, dataPoint);
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
	
	private static DhApiTerrainDataPoint generateApiDatapoint(IDhApiLevelWrapper levelWrapper, FullDataPointIdMap mapping, byte detailLevel, long dataPoint)
	{
		IBlockStateWrapper blockState = mapping.getBlockStateWrapper(FullDataPoint.getId(dataPoint));
		IBiomeWrapper biomeWrapper = mapping.getBiomeWrapper(FullDataPoint.getId(dataPoint));
		
		int bottomY = FullDataPoint.getBottomY(dataPoint) + levelWrapper.getMinHeight();
		int height = FullDataPoint.getHeight(dataPoint);
		int topY = bottomY + height;
		
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
//					DhApiResult<DhApiTerrainDataPoint> single = getTerrainDataAtBlockYPos(levelWrapper, new DhLodPos(LodUtil.BLOCK_DETAIL_LEVEL, blockPosX, blockPosZ), blockPosY);
//					DhApiResult<DhApiTerrainDataPoint[]> column = getTerrainDataColumnArray(levelWrapper, new DhLodPos(LodUtil.BLOCK_DETAIL_LEVEL, blockPosX, blockPosZ), null);
					
//					DhLodPos chunkPos = new DhLodPos(LodUtil.BLOCK_DETAIL_LEVEL, blockPosX, blockPosZ).convertUpwardsTo(LodUtil.CHUNK_DETAIL_LEVEL);
//					DhApiResult<DhApiTerrainDataPoint[][][]> area = getTerrainDataOverAreaForPositionDetailLevel(levelWrapper, chunkPos);
					
					
					IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
					DhApiResult<DhApiRaycastResult> rayCast = INSTANCE.raycastLodData(levelWrapper, MC_RENDER.getCameraExactPosition(), MC_RENDER.getLookAtVector(), 50);
					if (rayCast.payload != null && !rayCast.payload.pos.equals(currentDebugVec3i))
					{
						currentDebugVec3i = rayCast.payload.pos;
						
						// get a string for the block
						String blockString = "[NULL BLOCK]"; // shouldn't normally happen unless there is an issue with getting the terrain at the given position
						if (rayCast.payload.dataPoint.blockStateWrapper != null)
						{
							if (!rayCast.payload.dataPoint.blockStateWrapper.isAir() && rayCast.payload.dataPoint.blockStateWrapper.getWrappedMcObject_UNSAFE() != null)
							{
								blockString = rayCast.payload.dataPoint.blockStateWrapper.getWrappedMcObject_UNSAFE().toString();
							}
							else
							{
								blockString = "[AIR]";	
							}
						}
						
						LOGGER.info("raycast: " + currentDebugVec3i + "\t block: " + blockString);
					}
					else if (rayCast.payload == null && currentDebugVec3i != null)
					{
						currentDebugVec3i = null;
						LOGGER.info("raycast: [INFINITY]");
					}
					
					
					int debugPoint = 0; // a place to put a debugger break point
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
