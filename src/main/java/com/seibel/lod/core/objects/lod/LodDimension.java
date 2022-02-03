/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.objects.lod;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.crypto.spec.GCMParameterSpec;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.DropoffQuality;
import com.seibel.lod.core.enums.config.GenerationPriority;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.handlers.LodDimensionFileHandler;
import com.seibel.lod.core.objects.PosToGenerateContainer;
import com.seibel.lod.core.objects.PosToRenderContainer;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.MovabeGridRingList;
import com.seibel.lod.core.util.MovabeGridRingList.Pos;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;


//FIXME: Race condition on lodDim move/resize!

/**
 * This object holds all loaded LOD regions
 * for a given dimension. <Br><Br>
 *
 * <strong>Coordinate Standard: </strong><br>
 * Coordinate called posX or posZ are relative LevelPos coordinates <br>
 * unless stated otherwise. <br>
 * 
 * @author Leonardo Amato
 * @author James Seibel
 * @version 11-12-2021
 */
public class LodDimension
{
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	
	public final IDimensionTypeWrapper dimension;
	
	/** measured in regions */
	private volatile int width;
	/** measured in regions */
	private volatile int halfWidth;
	
	// these three variables are private to force use of the getWidth() method
	// which is a safer way to get the width then directly asking the arrays
	/** stores all the regions in this dimension */
	public MovabeGridRingList<LodRegion> regions;
	//NOTE: This list pos is relative to center
	private volatile RegionPos[] iteratorList = null;
	
	/** stores if the region at the given x and z index needs to be saved to disk */
	/** stores if the region at the given x and z index needs to be regenerated */
	// Use int because I need Tri state:
	
	/**
	 * if true that means there are regions in this dimension
	 * that need to have their buffers rebuilt.
	 */
	public volatile boolean regenDimensionBuffers = false;
	
	private LodDimensionFileHandler fileHandler;
	
	public volatile int dirtiedRegionsRoughCount = 0;
	
	private boolean isCutting = false;
	private boolean isExpanding = false;
	
	private final ExecutorService cutAndExpandThread = Executors.newSingleThreadExecutor(
			new LodThreadFactory(this.getClass().getSimpleName() + " - Cut and Expand", Thread.NORM_PRIORITY-1));
	
	
	/**
	 * Creates the dimension centered at (0,0)
	 * @param newWidth in regions
	 */
	public LodDimension(IDimensionTypeWrapper newDimension, LodWorld lodWorld, int newWidth)
	{
		dimension = newDimension;
		width = newWidth;
		halfWidth = width / 2;
		
		if (newDimension != null && lodWorld != null)
		{
			try
			{
				// determine the save folder
				File saveDir;
				if (MC.hasSinglePlayerServer())
				{
					// local world
					
					IWorldWrapper serverWorld = LodUtil.getServerWorldFromDimension(newDimension);
					saveDir = new File(serverWorld.getSaveFolder().getCanonicalFile().getPath() + File.separatorChar + "lod");
				}
				else
				{
					// connected to server
					
					saveDir = new File(MC.getGameDirectory().getCanonicalFile().getPath() +
											   File.separatorChar + "Distant_Horizons_server_data" + File.separatorChar + MC.getCurrentDimensionId());
				}
				
				fileHandler = new LodDimensionFileHandler(saveDir, this);
			}
			catch (IOException e)
			{
				// the file handler wasn't able to be created
				// we won't be able to read or write any files
			}
		}
		
		
		regions = new MovabeGridRingList<LodRegion>(halfWidth, 0, 0);
		generateIteratorList();
	}
	
	private void generateIteratorList() {
		iteratorList = null;
		RegionPos[] list = new RegionPos[width*width];
		
		int i = 0;
		for (int ix=-halfWidth; ix<=halfWidth; ix++) {
			for (int iz=-halfWidth; iz<=halfWidth; iz++) {
				list[i] = new RegionPos(ix, iz);
				i++;
			}
		}
		Arrays.sort(list, (a, b) -> {
			double disSqrA = a.x* a.x+ a.z* a.z;
			double disSqrB = b.x* b.x+ b.z* b.z;
			return Double.compare(disSqrA, disSqrB);
		});
		iteratorList = list;
	}
	
	
	
	//FIXME: Race condition on this move and other reading regions!
	/**
	 * Move the center of this LodDimension and move all owned
	 * regions over by the given x and z offset. <br><br>
	 * <p>
	 * Synchronized to prevent multiple moves happening on top of each other.
	 */
	public synchronized void move(RegionPos regionOffset)
	{
		ClientApi.LOGGER.info("LodDim MOVE. Offset: "+regionOffset);
		saveDirtyRegionsToFile(false); //async add dirty regions to be saved.
		Pos p = regions.getCenter();
		regions.move(p.x+regionOffset.x, p.y+regionOffset.z);
		ClientApi.LOGGER.info("LodDim MOVE complete. Offset: "+regionOffset);
	}
	
	
	/**
	 * Gets the region at the given LevelPos
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodRegion getRegion(byte detailLevel, int levelPosX, int levelPosZ)
	{
		int xRegion = LevelPosUtil.getRegion(detailLevel, levelPosX);
		int zRegion = LevelPosUtil.getRegion(detailLevel, levelPosZ);
		LodRegion region = regions.get(xRegion, zRegion);
		
		if (region != null && region.getMinDetailLevel() > detailLevel)
			return null;
		//throw new InvalidParameterException("Region for level pos " + LevelPosUtil.toString(detailLevel, posX, posZ) + " currently only reach level " + regions[xIndex][zIndex].getMinDetailLevel());
		
		return region;
	}
	
	/**
	 * Gets the region at the given X and Z
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodRegion getRegion(int regionPosX, int regionPosZ)
	{
		return regions.get(regionPosX, regionPosZ);
	}
	
	/** Useful when iterating over every region. */
	@Deprecated
	public LodRegion getRegionByArrayIndex(int xIndex, int zIndex)
	{
		Pos p = regions.getMinInRange();
		return regions.get(p.x+xIndex, p.y+zIndex);
	}
	
	/**
	 * Overwrite the LodRegion at the location of newRegion with newRegion.
	 * @throws ArrayIndexOutOfBoundsException if newRegion is outside what can be stored in this LodDimension.
	 */
	/*public synchronized void addOrOverwriteRegion(LodRegion newRegion) throws ArrayIndexOutOfBoundsException
	{
		if (!regionIsInRange(newRegion.regionPosX, newRegion.regionPosZ))
			// out of range
			throw new ArrayIndexOutOfBoundsException("Region " + newRegion.regionPosX + ", " + newRegion.regionPosZ + " out of range");
		regions[xIndex][zIndex] = newRegion;
	}*/
	
	public interface PosComsumer {
		void run(int x, int z);
	}
	
	public void iterateWithSpiral(PosComsumer r) {
		int ox,oy,dx,dy;
	    ox = oy = dx = 0;
	    dy = -1;
	    int len = regions.getSize();
	    int maxI = len*len;
	    int halfLen = len/2;
	    for(int i =0; i < maxI; i++){
	        if ((-halfLen <= ox) && (ox <= halfLen) && (-halfLen <= oy) && (oy <= halfLen)){
	        	int x = ox+halfLen;
	        	int z = oy+halfLen;
	        	r.run(x, z);
	        }
	        if( (ox == oy) || ((ox < 0) && (ox == -oy)) || ((ox > 0) && (ox == 1-oy))){
	            int temp = dx;
	            dx = -dy;
	            dy = temp;
	        }
	        ox += dx;
	        oy += dy;
	    }
	}
	public void iterateByDistance(PosComsumer r) {
		if (iteratorList==null) return;
		for (RegionPos relativePos : iteratorList) {
			r.run(relativePos.x+halfWidth, relativePos.z+halfWidth);
		}
		
	}
	
	
	/**
	 * Deletes nodes that are a higher detail then necessary, freeing
	 * up memory.
	 */
	private int totalDirtiedRegions = 0;
	
	public void cutRegionNodesAsync(int playerPosX, int playerPosZ)
	{
		if (isCutting) return;
		isCutting = true;
		// don't run the tree cutter multiple times
		// for the same location
		Runnable thread = () -> {
			//ClientApi.LOGGER.info("LodDim cut Region: " + playerPosX + "," + playerPosZ);
			totalDirtiedRegions = 0;
			Pos minPos = regions.getMinInRange();
			// go over every region in the dimension
			iterateWithSpiral((int x, int z) -> {
				int minDistance;
				byte detail;
				
				LodRegion region = regions.get(x+minPos.x, z+minPos.y);
				if (region != null && region.needSaving) totalDirtiedRegions++;
				if (region != null && !region.needSaving && region.isWriting==0) {
					// check what detail level this region should be
					// and cut it if it is higher then that
					minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, x+minPos.x, z+minPos.y,
							playerPosX, playerPosZ);
					detail = DetailDistanceUtil.getDetailLevelFromDistance(minDistance);
					if (region.getMinDetailLevel() < detail) {
						if (region.needSaving) return; // FIXME: A crude attempt at lowering chance of race condition!
						region.cutTree(detail);
						region.needRegenBuffer = 2;
						regenDimensionBuffers = true;
					}
				}
			});
			if (totalDirtiedRegions > 8) this.saveDirtyRegionsToFile(false);
			dirtiedRegionsRoughCount = totalDirtiedRegions;
			//ClientApi.LOGGER.info("LodDim cut Region complete: " + playerPosX + "," + playerPosZ);
			isCutting = false;
			
			// See if we need to save and flush some data out.
		};
		cutAndExpandThread.execute(thread);
	}

	private boolean expandOrLoadPaused = false;
	/** Either expands or loads all regions in the rendered LOD area */
	public void expandOrLoadRegionsAsync(int playerPosX, int playerPosZ) {

		if (isExpanding) return;
		// We have less than 10% or 1MB ram left. Don't expend.
		if (expandOrLoadPaused && !LodUtil.checkRamUsage(0.4, 512)) {
			//ClientApi.LOGGER.info("Not enough ram for expandOrLoadThread. Skipping...");
			return;
		} else if (expandOrLoadPaused) {
			ClientApi.LOGGER.info("Enough ram for expandOrLoadThread. Restarting...");
		}
		isExpanding = true;
		expandOrLoadPaused = false;
		
		VerticalQuality verticalQuality = CONFIG.client().graphics().quality().getVerticalQuality();
		DropoffQuality dropoffQuality = CONFIG.client().graphics().quality().getDropoffQuality();
		if (dropoffQuality == DropoffQuality.AUTO)
			dropoffQuality = CONFIG.client().graphics().quality().getLodChunkRenderDistance() < 128 ?
					DropoffQuality.SMOOTH_DROPOFF : DropoffQuality.PERFORMANCE_FOCUSED;
		int dropoffSwitch = dropoffQuality.fastModeSwitch;
		// don't run the expander multiple times
		// for the same location
		Runnable thread = () -> {
			//ClientApi.LOGGER.info("LodDim expend Region: " + playerPosX + "," + playerPosZ);
			Pos minPos = regions.getMinInRange();
			iterateWithSpiral((int x, int z) -> {
				if (expandOrLoadPaused) return;
				if (!LodUtil.checkRamUsage(0.1, 32)) {
					Runtime.getRuntime().gc();
					if (!LodUtil.checkRamUsage(0.2, 64)) {
						ClientApi.LOGGER.warn("Not enough ram for expandOrLoadThread. Pausing until Ram is freed...");
						// We have less than 10% or 1MB ram left. Don't expend.
						expandOrLoadPaused = true;
						saveDirtyRegionsToFile(false);
						return;
					}
				}
				int regionX;
				int regionZ;
				LodRegion region;
				int minDistance;
				int maxDistance;
				byte minDetail;
				byte maxDetail;
				regionX = x + minPos.x;
				regionZ = z + minPos.y;
				final RegionPos regionPos = new RegionPos(regionX, regionZ);
				region = regions.get(regionX, regionZ);
				if (region != null && region.isWriting!=0) return; // FIXME: A crude attempt at lowering chance of race condition!

				minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ, playerPosX,
						playerPosZ);
				maxDistance = LevelPosUtil.maxDistance(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ, playerPosX,
						playerPosZ);
				minDetail = DetailDistanceUtil.getDetailLevelFromDistance(minDistance);
				maxDetail = DetailDistanceUtil.getDetailLevelFromDistance(maxDistance);
				boolean updated = false;
				boolean expended = false;
				if (region == null) {
					region = getRegionFromFile(regionPos, minDetail, verticalQuality);
					regions.set(regionX, regionZ, region);
					updated = true;
				} else if (region.getVerticalQuality() != verticalQuality ||
						region.getMinDetailLevel() > minDetail) {
					// The 'getRegionFromFile' will flush and save the region if it returns a new one
					region = getRegionFromFile(region, minDetail, verticalQuality);
					regions.set(regionX, regionZ, region);
					updated = true;
				} else if (minDetail <= dropoffSwitch && region.lastMaxDetailLevel != maxDetail) {
					region.lastMaxDetailLevel = maxDetail;
					updated = true;
				} else if (minDetail <= dropoffSwitch && region.lastMaxDetailLevel != region.getMinDetailLevel()) {
					updated = true;
				}
				if (updated) {
					region.needRegenBuffer = 2;
					region.needRecheckGenPoint = true;
					regenDimensionBuffers = true;
				}
			});
			//ClientApi.LOGGER.info("LodDim expend Region complete: " + playerPosX + "," + playerPosZ);
			isExpanding = false;
		};

		cutAndExpandThread.execute(thread);
	}
	
	/**
	 * Add whole column of LODs to this dimension at the coordinate
	 * stored in the LOD. If an LOD already exists at the given
	 * coordinate it will be overwritten.
	 */
	public Boolean addVerticalData(byte detailLevel, int posX, int posZ, long[] data, boolean override)
	{
		int regionPosX = LevelPosUtil.getRegion(detailLevel, posX);
		int regionPosZ = LevelPosUtil.getRegion(detailLevel, posZ);
		
		// don't continue if the region can't be saved
		LodRegion region = getRegion(regionPosX, regionPosZ);
		if (region == null)
			return false;
		
		boolean nodeAdded = region.addVerticalData(detailLevel, posX, posZ, data, override);
		if (nodeAdded) {
			regenDimensionBuffers = true;
		}
		return nodeAdded;
	}
	
	/** marks the region at the given region position to have its buffer rebuilt */
	public void markRegionBufferToRegen(int xRegion, int zRegion)
	{
		LodRegion r = getRegion(xRegion,zRegion);
		if (r!=null) {
			r.needRegenBuffer = 2;
			regenDimensionBuffers = true;
		}
	}
	
	/**
	 * Returns every position that need to be generated based on the position of the player
	 */
	public PosToGenerateContainer getPosToGenerate(int maxDataToGenerate, int playerBlockPosX, int playerBlockPosZ,
			GenerationPriority priority, DistanceGenerationMode genMode)
	{
		PosToGenerateContainer posToGenerate;
		posToGenerate = new PosToGenerateContainer(maxDataToGenerate, playerBlockPosX, playerBlockPosZ);
		
		
		// This ensures that we don't spawn way too much regions without finish flushing them first.
		if (dirtiedRegionsRoughCount > 16) return posToGenerate;
		GenerationPriority allowedPriority = dirtiedRegionsRoughCount>12 ? GenerationPriority.NEAR_FIRST : priority;
		Pos minPos = regions.getMinInRange();
		iterateByDistance((int x, int z) -> {
			boolean isCloseRange = (Math.abs(x-halfWidth)+Math.abs(z-halfWidth)<=2);
			//boolean isCloseRange = true;
			//All of this is handled directly by the region, which scan every pos from top to bottom of the quad tree
			LodRegion lodRegion = regions.get(minPos.x+x, minPos.y+z);
			
			
			if (lodRegion != null && lodRegion.needRecheckGenPoint) {
				int nearCount = posToGenerate.getNumberOfNearPos();
				int farCount = posToGenerate.getNumberOfFarPos();
				boolean checkForFlag = (nearCount < posToGenerate.getMaxNumberOfNearPos() && farCount < posToGenerate.getMaxNumberOfFarPos());
				if (checkForFlag) {
					lodRegion.needRecheckGenPoint = false;
				}
				lodRegion.getPosToGenerate(posToGenerate, playerBlockPosX, playerBlockPosZ, allowedPriority, genMode,
						isCloseRange);
				if (checkForFlag) {
					if (nearCount != posToGenerate.getNumberOfNearPos() || farCount != posToGenerate.getNumberOfFarPos()) {
						lodRegion.needRecheckGenPoint = true;
					}
				}
			}
		});
	return posToGenerate;
	}
	
	/**
	 * Fills the posToRender with the position to render for the regionPos given in input
	 */
	public void getPosToRender(PosToRenderContainer posToRender, RegionPos regionPos, int playerPosX,
			int playerPosZ)
	{
		LodRegion region = getRegion(regionPos.x, regionPos.z);
		
		// use FAR_FIRST on local worlds and NEAR_FIRST on servers
		GenerationPriority generationPriority = CONFIG.client().worldGenerator().getGenerationPriority();
		if (generationPriority == GenerationPriority.AUTO)
			generationPriority = MC.hasSinglePlayerServer() ? GenerationPriority.FAR_FIRST : GenerationPriority.BALANCED;

		DropoffQuality dropoffQuality = CONFIG.client().graphics().quality().getDropoffQuality();
		if (dropoffQuality == DropoffQuality.AUTO)
			dropoffQuality = CONFIG.client().graphics().quality().getLodChunkRenderDistance() < 128 ?
					DropoffQuality.SMOOTH_DROPOFF : DropoffQuality.PERFORMANCE_FOCUSED;
		
		if (region != null)
			region.getPosToRender(posToRender, playerPosX, playerPosZ, generationPriority, dropoffQuality);
	}
	
	/**
	 * Determines how many vertical LODs could be used
	 * for the given region at the given detail level
	 */
	public int getMaxVerticalData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getMaxVerticalData given a level of [" + detailLevel + "] when [" + LodUtil.REGION_DETAIL_LEVEL + "] is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return 0;
		
		return region.getMaxVerticalData(detailLevel);
	}
	
	/**
	 * Get the data point at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public long getData(byte detailLevel, int posX, int posZ, int verticalIndex)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return DataPointUtil.EMPTY_DATA;
		
		return region.getData(detailLevel, posX, posZ, verticalIndex);
	}
	
	
	/**
	 * Get the data point at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public long getSingleData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return DataPointUtil.EMPTY_DATA;
		
		return region.getSingleData(detailLevel, posX, posZ);
	}
	
	/**
	 * Returns if the buffer at the given array index needs
	 * to have its buffer regenerated. Also decrease the state by 1
	 */
	public boolean getAndClearRegionNeedBufferRegen(int regionX, int regionZ)
	{
		//FIXME: Use actual atomics on needRegenBuffer
		LodRegion region = getRegion(regionX, regionZ);
		if (region == null) return false;
		int i = region.needRegenBuffer;
		if (i > 0) {
			region.needRegenBuffer--;
			return true;
		}
		return false;
	}
	
	/**
	 * Get the data point at the given LevelPos
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public void updateData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");

		int xRegion = LevelPosUtil.getRegion(detailLevel, posX);
		int zRegion = LevelPosUtil.getRegion(detailLevel, posZ);
		LodRegion region = getRegion(xRegion, zRegion);
		if (region == null) return;
		region.updateArea(detailLevel, posX, posZ);
		region.needRegenBuffer = 2;
		regenDimensionBuffers = true;
	}
	
	/** Returns true if a region exists at the given LevelPos */
	public boolean doesDataExist(byte detailLevel, int posX, int posZ, DistanceGenerationMode requiredMode)
	{
		LodRegion region = getRegion(detailLevel, posX, posZ);
		return region != null && region.doesDataExist(detailLevel, posX, posZ, requiredMode);
	}
	
	/**
	 * Loads the region at the given RegionPos from file,
	 * if a file exists for that region.
	 */
	public LodRegion getRegionFromFile(RegionPos regionPos, byte detailLevel, VerticalQuality verticalQuality)
	{
		return fileHandler != null ? fileHandler.loadRegionFromFile(detailLevel, regionPos, verticalQuality) : 
			new LodRegion(detailLevel, regionPos, verticalQuality);
	}
	/**
	 * Loads the region at the given region from file,
	 * if a file exists for that region.
	 */
	public LodRegion getRegionFromFile(LodRegion existingRegion, byte detailLevel, VerticalQuality verticalQuality)
	{
		return fileHandler != null ? fileHandler.loadRegionFromFile(detailLevel, existingRegion, verticalQuality) : 
			new LodRegion(detailLevel, existingRegion.getRegionPos(), verticalQuality);
	}
	
	/** Save all dirty regions in this LodDimension to file. */
	public void saveDirtyRegionsToFile(boolean blockUntilFinished)
	{
		if (fileHandler == null) return;
		fileHandler.saveDirtyRegionsToFile(blockUntilFinished);
	}
	
	
	/** Return true if the chunk has been pregenerated in game */
	//public boolean isChunkPreGenerated(int xChunkPosWrapper, int zChunkPosWrapper)
	//{
	//
	//	LodRegion region = getRegion(LodUtil.CHUNK_DETAIL_LEVEL, xChunkPosWrapper, zChunkPosWrapper);
	//	if (region == null)
	//		return false;
	//
	//	return region.isChunkPreGenerated(xChunkPosWrapper, zChunkPosWrapper);
	//}
	
	/**
	 * Returns whether the region at the given RegionPos
	 * is within the loaded range.
	 */
	public boolean regionIsInRange(int regionX, int regionZ)
	{
		return regions.inRange(regionX, regionZ);
	}
	
	/** Returns the dimension's center region position X value */
	@Deprecated // Use getCenterRegionPos() instead
	public int getCenterRegionPosX()
	{
		return regions.getCenter().x;
	}
	
	/** Returns the dimension's center region position Z value */
	@Deprecated // Use getCenterRegionPos() instead
	public int getCenterRegionPosZ()
	{
		return regions.getCenter().y;
	}
	
	public RegionPos getCenterRegionPos() {
		Pos p = regions.getCenter();
		return new RegionPos(p.x, p.y);
	}
	
	/** returns the width of the dimension in regions */
	public int getWidth()
	{
		// we want to get the length directly from the
		// source to make sure it is in sync with region
		// and isRegionDirty
		return regions != null ? regions.getSize() : width;
	}
	
	/** Update the width of this dimension, in regions */
	public void setRegionWidth(int newWidth)
	{
		width = newWidth;
		halfWidth = width/ 2;
		Pos p = regions.getCenter();
		regions = new MovabeGridRingList<LodRegion>(halfWidth, p.x, p.y);
		generateIteratorList();
	}
	
	
	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Dimension : \n");
		stringBuilder.append(regions.toDetailString());
		return stringBuilder.toString();
	}

	public void shutdown() {
		cutAndExpandThread.shutdown();
		try {
			boolean worked = cutAndExpandThread.awaitTermination(5, TimeUnit.SECONDS);
			if (!worked)
				ClientApi.LOGGER.error("Cut And Expend threads timed out! May cause crash on game exit due to cleanup failure.");
		} catch (InterruptedException e) {
			ClientApi.LOGGER.error("Cut And Expend threads shutdown is interrupted! May cause crash on game exit due to cleanup failure: ", e);
		}
		
	}
}
