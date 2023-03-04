package com.seibel.lod.core.render;

import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.file.renderfile.ILodRenderSourceProvider;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.Pos2D;
import com.seibel.lod.core.util.BitShiftUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.MathUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;
import org.apache.logging.log4j.Logger;

/**
 * This quadTree structure is our core data structure and holds
 * all rendering data. <br><br>
 * 
 * This class represent a circular quadTree of lodSections. <br>
 * Each section at level n is populated in one or more ways: <br> 
 *      -by constructing it from the data of all the children sections (lower levels) <br> 
 *      -by loading from file <br> 
 *      -by adding data with the lodBuilder <br> 
 * <br><br> 
 * The QuadTree is built from several layers of 2d ring buffers.
 */
public class LodQuadTree implements AutoCloseable
{
    /**
     * Note: all config values should be via the class that extends this class, and
     *          by implementing different abstract methods
     */
    public static final byte TREE_LOWEST_DETAIL_LEVEL = ColumnRenderSource.SECTION_SIZE_OFFSET;
	
    private static final boolean SUPER_VERBOSE_LOGGING = false;
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	public final byte getLayerDataDetailOffset() { return ColumnRenderSource.SECTION_SIZE_OFFSET; }
	public final byte getLayerDataDetail(byte sectionDetailLevel) { return (byte) (sectionDetailLevel - this.getLayerDataDetailOffset()); }
	
    public final byte getLayerSectionDetailOffset() { return ColumnRenderSource.SECTION_SIZE_OFFSET; }
    public final byte getLayerSectionDetail(byte dataDetail) { return (byte) (dataDetail + this.getLayerSectionDetailOffset()); }
	
	
	/** AKA how many detail levels are in this quad tree, adding this to the {@link LodQuadTree#TREE_LOWEST_DETAIL_LEVEL} is equivalent to the maximum detail level in this tree */
    public final byte numbersOfSectionDetailLevels;
	
    private final MovableGridRingList<LodRenderSection>[] renderSectionRingLists;
	
    public final int blockRenderDistance;
    private final ILodRenderSourceProvider renderSourceProvider;
	
    private final IDhClientLevel level; //FIXME: Proper hierarchy to remove this reference!
	
	
	
	
	/**
     * Constructor of the quadTree
     * @param viewDistance View distance in blocks
     * @param initialPlayerX player x block coordinate
     * @param initialPlayerZ player z block coordinate
     */
    public LodQuadTree(
			IDhClientLevel level, int viewDistance, 
			int initialPlayerX, int initialPlayerZ, 
			ILodRenderSourceProvider provider)
	{
        DetailDistanceUtil.updateSettings(); //TODO: Move this to somewhere else
        this.level = level;
		this.renderSourceProvider = provider;
        this.blockRenderDistance = viewDistance;
		
		
		
        // Calculate the max section detail level //
		
		byte maxDetailLevel = this.getMaxDetailInRange(viewDistance * Math.sqrt(2));
		byte topSectionDetailLevel = this.getLayerSectionDetail(maxDetailLevel);
		this.numbersOfSectionDetailLevels = (byte) (topSectionDetailLevel + 1);
		this.renderSectionRingLists = new MovableGridRingList[this.numbersOfSectionDetailLevels - TREE_LOWEST_DETAIL_LEVEL];
        
		
		
		// Construct the ringLists //
		
		LOGGER.info("Creating "+MovableGridRingList.class.getSimpleName()+" with player center at {}", new Pos2D(initialPlayerX, initialPlayerZ));
		for (byte sectionDetailLevel = TREE_LOWEST_DETAIL_LEVEL; sectionDetailLevel < this.numbersOfSectionDetailLevels; sectionDetailLevel++)
		{
			byte targetDetailLevel = this.getLayerDataDetail(sectionDetailLevel);
			int maxDist = this.getFurthestDistance(targetDetailLevel);
			int halfSize = MathUtil.ceilDiv(maxDist, BitShiftUtil.powerOfTwo(sectionDetailLevel)) + 8; // +8 to make sure the section is fully contained in the ringList //TODO what does the "8" represent?
			
			
			// check that the detail level and position are valid
			DhSectionPos checkedPos = new DhSectionPos(sectionDetailLevel, halfSize, halfSize);
			byte checkedDetailLevel = this.calculateExpectedDetailLevel(new DhBlockPos2D(initialPlayerX, initialPlayerZ), checkedPos);
			// validate the detail level
			LodUtil.assertTrue(checkedDetailLevel > targetDetailLevel,
					"in "+sectionDetailLevel+", getFurthestDistance would return "+maxDist+" which would be contained in range "+(halfSize-2)+", but calculateExpectedDetailLevel at "+checkedPos+" is "+checkedDetailLevel+" <= "+targetDetailLevel);
			
			
			// create the new ring list
			Pos2D ringListCenterPos = new Pos2D(BitShiftUtil.divideByPowerOfTwo(initialPlayerX, sectionDetailLevel), BitShiftUtil.divideByPowerOfTwo(initialPlayerZ, sectionDetailLevel));
			
			LOGGER.info("Creating "+MovableGridRingList.class.getSimpleName()+" centered on "+ringListCenterPos+" with halfSize ["+halfSize+"] (maxDist ["+maxDist+"], dataDetail ["+targetDetailLevel+"])");
			this.renderSectionRingLists[sectionDetailLevel - TREE_LOWEST_DETAIL_LEVEL] = new MovableGridRingList<>(halfSize, ringListCenterPos.x, ringListCenterPos.y);
			
		}
		
    }
	
	
	
    /**
     * This method return the LodSection given the Section Pos
     * @param pos the section position.
     * @return the LodSection
     */
    public LodRenderSection getSection(DhSectionPos pos) { return this.getSection(pos.sectionDetailLevel, pos.sectionX, pos.sectionZ); }
	
    /**
     * This method returns the RingList of a given detail level
     * @apiNote The returned ringList should not be modified!
     * @param detailLevel the detail level
     * @return the RingList
     */
    public MovableGridRingList<LodRenderSection> getRingList(byte detailLevel) { return this.renderSectionRingLists[detailLevel - TREE_LOWEST_DETAIL_LEVEL]; }
	
    /**
     * This method returns the number of detail levels in the quadTree
     * @return the number of detail levels
     */
    public byte getNumbersOfSectionDetailLevels() { return this.numbersOfSectionDetailLevels; }

    public byte getStartingSectionLevel() { return TREE_LOWEST_DETAIL_LEVEL; }

    /**
     * This method return the LodSection at the given detail level and level coordinate x and z
     * @param detailLevel detail level of the section
     * @param x x coordinate of the section
     * @param z z coordinate of the section
     * @return the LodSection
     */
    public LodRenderSection getSection(byte detailLevel, int x, int z) { return this.renderSectionRingLists[detailLevel - TREE_LOWEST_DETAIL_LEVEL].get(x, z); }

	
	
	
    
    /**
     * This method will compute the detail level based on player position and section pos
     * Override this method if you want to use a different algorithm
     * @param playerPos player position as a reference for calculating the detail level
     * @param sectionPos section position
     * @return detail level of this section pos
     */
    public byte calculateExpectedDetailLevel(DhBlockPos2D playerPos, DhSectionPos sectionPos)
	{
        return DetailDistanceUtil.getDetailLevelFromDistance(
                playerPos.dist(sectionPos.getCenter().getCenterBlockPos()));
    }

    /**
     * The method will return the highest detail level in a circle around the center
     * Override this method if you want to use a different algorithm
     * Note: the returned distance should always be the ceiling estimation of the distance
     * //TODO: Make this input a bbox or a circle or something....
     * @param distance the circle radius
     * @return the highest detail level in the circle
     */
    public byte getMaxDetailInRange(double distance) { return DetailDistanceUtil.getDetailLevelFromDistance(distance); }

    /**
     * The method will return the furthest distance to the center for the given detail level
     * Override this method if you want to use a different algorithm
     * Note: the returned distance should always be the ceiling estimation of the distance
     * //TODO: Make this return a bbox instead of a distance in circle
     * @param detailLevel detail level
     * @return the furthest distance to the center, in blocks
     */
    public int getFurthestDistance(byte detailLevel)
	{
        return (int)Math.ceil(DetailDistanceUtil.getDrawDistanceFromDetail(detailLevel + 1));
        // +1 because that's the border to the next detail level, and we want to include up to it.
    }
    
    /**
     * Given a section pos at level n this method returns the parent section at level n+1
     * @param pos the section position
     * @return the parent LodSection
     */
    public LodRenderSection getParentSection(DhSectionPos pos) { return this.getSection(pos.getParentPos()); }
    
    /**
     * Given a section pos at level n and a child index this method return the
     * child section at level n-1
     * @param child0to3 since there are 4 possible children this index identify which one we are getting
     * @return one of the child LodSection
     */
    public LodRenderSection getChildSection(DhSectionPos pos, int child0to3) { return this.getSection(pos.getChildByIndex(child0to3)); }
	
	
	
	
	// tick //
	
    /**
     * This function updates the quadTree based on the playerPos and the current game configs (static and global)
     * @param playerPos the reference position for the player
     */
    public void tick(DhBlockPos2D playerPos)
	{
		// recenter the grid lists if necessary
        for (int sectionDetailLevel = TREE_LOWEST_DETAIL_LEVEL; sectionDetailLevel < this.numbersOfSectionDetailLevels; sectionDetailLevel++)
		{
			Pos2D expectedCenterPos = new Pos2D(BitShiftUtil.divideByPowerOfTwo(playerPos.x, sectionDetailLevel), BitShiftUtil.divideByPowerOfTwo(playerPos.z, sectionDetailLevel));
			MovableGridRingList<LodRenderSection> gridList = this.renderSectionRingLists[sectionDetailLevel-TREE_LOWEST_DETAIL_LEVEL];
			
			if (!gridList.getCenter().equals(expectedCenterPos))
			{
//				LOGGER.info("TreeTick: Moving ring list "+sectionDetailLevel+" from "+gridList.getCenter()+" to "+expectedCenterPos);
				gridList.moveTo(expectedCenterPos.x, expectedCenterPos.y, LodRenderSection::disposeRenderData);
			}
		}
		
		
		updateAllRenderSectionChildCounts(playerPos);
		
		updateAllRenderSections();
	}
	
	private void updateAllRenderSectionChildCounts(DhBlockPos2D playerPos)
	{
		
		// TODO: inline comments should be added everywhere for this tick pass, so this comment block should be removed (having duplicate comments in two places is a bad idea) 
		// First tick pass: update all sections' childCount from bottom level to top level. Step:
		//   If sectLevel is bottom && section != null:
		//     - set childCount to 0
		//   If section != null && child != 0: //TODO: Should I move this createChild steps to Second tick pass?
		//     - // Section will be in the unloaded state.
		//     - create parent if not at final level and if it doesn't exist, with childCount = 1
		//     - for each child:
		//       - if null, create new with childCount = 0 (force load due to neighboring issues)
		//       - else if childCount == -1, set childCount = 0 (rescue it)
		//     - set childCount to 4
		//   Else:
		//     - Calculate targetLevel at that section
		//     - If sectLevel == numberOfSectionLevels - 1:
		//       - // Section is the top level.
		//       - If targetLevel > dataLevel@sectLevel && section != null:
		//         - set childCount to -1 (Signal that section is to be freed) (this prob not be rescued as it is the top level)
		//       - If targetLevel <= dataLevel@sectLevel && section == null: (direct use the current sectLevel's dataLevel)
		//         - create new section with childCount = 0
		//     - Else:
		//       - // Section is not the top level. So we also need to consider the parent.
		//       - If targetLevel >= dataLevel@(sectLevel+1) && section != null: (use the next level's dataLevel)
		//         - Parent's childCount-- (Assert parent != null && childCount > 0 before decrementing)
		//         - // Note that this doesn't necessarily mean this section will be freed as it may be rescued later
		//              due to neighboring quadrants not able to be freed (they pass targetLevel checks or has children)
		//              or due to parent's layer is in the Always Cascade mode. (containerType == null)
		//         - set childCount to -1 (Signal that this section will be freed if not rescued)
		//       - If targetLevel < dataLevel@(sectLevel+1) && section == null: (use the next level's dataLevel)
		//         - create new section with childCount = 0
		//         - Parent's childCount++ (Create parent if needed)
		for (byte sectionDetailLevelIteration = TREE_LOWEST_DETAIL_LEVEL; sectionDetailLevelIteration < this.numbersOfSectionDetailLevels; sectionDetailLevelIteration++)
		{
			final byte sectionDetailLevel = sectionDetailLevelIteration; // final to prevent accidentally setting (and because intellij highlights final values different so it is easier to identify)
			
			final MovableGridRingList<LodRenderSection> ringList = this.renderSectionRingLists[sectionDetailLevel- TREE_LOWEST_DETAIL_LEVEL];
			
			// child and parent are relative to the detail level
			final MovableGridRingList<LodRenderSection> childRingList = (sectionDetailLevel == TREE_LOWEST_DETAIL_LEVEL) ? null : this.renderSectionRingLists[sectionDetailLevel- TREE_LOWEST_DETAIL_LEVEL -1];
			final MovableGridRingList<LodRenderSection> parentRingList = (sectionDetailLevel == this.numbersOfSectionDetailLevels-1) ? null : this.renderSectionRingLists[sectionDetailLevel- TREE_LOWEST_DETAIL_LEVEL +1];
			
			
			ringList.forEachPosOrdered((section, pos) ->
			{
				// TODO why do we need to use the halfPos to get sections?
				final Pos2D halfPos = new Pos2D(BitShiftUtil.half(pos.x), BitShiftUtil.half(pos.y));
				
				
				if (section != null && sectionDetailLevel == TREE_LOWEST_DETAIL_LEVEL)
				{
					// this section is a leaf node, set its children to 0
					section.childCount = 0;
					//LOGGER.info("sect {} in first layer with non-null. Reset childCount", section.pos);
				}
				
				if (section != null && section.childCount != 0)
				{
					// Section will be in the unloaded state, with 1-3 children
					// load its parent and children
					//TODO: Should I move this createChild steps to the Second tick pass?
					
					if (SUPER_VERBOSE_LOGGING)
					{
						LOGGER.info("sect "+section.pos+" has child");
					}
					
					
					// if this isn't the top detail level, make sure it has a valid parent
					if (parentRingList != null)
					{
						LodRenderSection parentSection = this._getRenderSectionFromGridList(parentRingList, halfPos.x, halfPos.y);
						if (parentSection == null)
						{
							// the parent render section is missing, create it
							if (SUPER_VERBOSE_LOGGING)
							{
								LOGGER.info("sect "+section.pos+" missing parent. Creating at "+section.pos.getParentPos());
							}
							
							parentSection = this._setRenderSectionInGridList(parentRingList, halfPos.x, halfPos.y, new LodRenderSection(section.pos.getParentPos()));
							parentSection.childCount++;
							
							if (SUPER_VERBOSE_LOGGING)
							{
								LOGGER.info("parent sect "+section.pos.getParentPos()+" now has "+parentSection.childCount+" children.");
							}
						}
						
						LodUtil.assertTrue(parentSection.childCount > 0 && parentSection.childCount <= 4);
					}
					
					
					// load this section's children
					LodUtil.assertTrue(childRingList != null); // this shouldn't be a leaf node
					for (int childIndex = 0; childIndex < 4; childIndex++)
					{
						DhSectionPos childPos = section.pos.getChildByIndex(childIndex);
						
						LodRenderSection child = this._getRenderSectionFromGridList(childRingList, childPos.sectionX, childPos.sectionZ);
						if (child == null)
						{
							// no child exists, create one
							
							if (SUPER_VERBOSE_LOGGING)
							{
								LOGGER.info("sect "+section.pos+" missing child at "+childPos+". Creating.");
							}
							
							child = this._setRenderSectionInGridList(childRingList, childPos.sectionX, childPos.sectionZ, new LodRenderSection(childPos));
							child.childCount = 0;
						}
						else if (child.childCount == -1)
						{
							// a child existed but was marked for deletion,
							// rescue (reuse) it
							
							if (SUPER_VERBOSE_LOGGING)
							{
								LOGGER.info("sect "+section.pos+" rescued child at "+childPos+".");
							}
							
							child.childCount = 0;
						}
					}
					
					// the section is now fully loaded
					section.childCount = 4;
				}
				else
				{
					// this render section is either a leaf node, or null
					
					final DhSectionPos sectPos = (section != null) ? section.pos : new DhSectionPos(sectionDetailLevel, pos.x, pos.y);
					
					// confirm the sectPos is correct
					LodUtil.assertTrue(sectPos.sectionDetailLevel == sectionDetailLevel 
									&& sectPos.sectionX == pos.x 
									&& sectPos.sectionZ == pos.y,
							"sectPos "+sectPos+" != "+pos+" @ "+sectionDetailLevel);
					
					
					byte targetDetailLevel = this.calculateExpectedDetailLevel(playerPos, sectPos);
					if (SUPER_VERBOSE_LOGGING)
					{
						String layerDetailLevel = (sectionDetailLevel == this.numbersOfSectionDetailLevels-1) ? "N/A" : this.getLayerDataDetail((byte) (sectionDetailLevel+1))+"";
						LOGGER.info("0 child sect "+sectPos+"(null?"+(section==null)+") - target:"+targetDetailLevel+"/"+this.getLayerDataDetail(sectionDetailLevel)+" (parent:"+layerDetailLevel+")");
					}
					
					
					if (sectionDetailLevel == this.numbersOfSectionDetailLevels-1) // TODO equivalent to ... == treeMaxDetailLevel
					{
						// Render Section is at the top detail level.
						
						// TODO what does any of this mean? shouldn't both values in this if statement be independent of the section?
						if (section != null && targetDetailLevel > this.getLayerDataDetail(sectionDetailLevel))
						{
							// this section is a higher detail level than we want, mark it for deletion
							if (SUPER_VERBOSE_LOGGING)
							{
								LOGGER.info("sect "+sectPos+" in top detail level & target>current. Mark as free.");
							}
							section.childCount = -1;
						}
						
						if (section == null && targetDetailLevel <= this.getLayerDataDetail(sectionDetailLevel))
						{
							// the render section for this detail level is missing, create it
							
							if (SUPER_VERBOSE_LOGGING)
							{
								LOGGER.info("null sect "+sectPos+" in top & target<=current. Creating.");
							}
							section = this._setRenderSectionInGridList(ringList, pos.x, pos.y, new LodRenderSection(sectPos));
						}
					}
					else
					{
						// Section is not in the top detail level,
						// so we also need to consider its parent.
						
						if (section != null && targetDetailLevel >= this.getLayerDataDetail((byte) (sectionDetailLevel + 1)))
						{
							// this section is a higher detail level than what we want, mark it for deletion
							if (SUPER_VERBOSE_LOGGING)
							{
								LOGGER.info("sect "+sectPos+" target>=nextLevel. Mark as free.");
							}
							LodUtil.assertTrue(parentRingList != null);
							
							LodRenderSection parent = this._getNotNull(parentRingList, halfPos.x, halfPos.y);
							LodUtil.assertTrue(parent.childCount > 0 && parent.childCount <= 4);
							
							parent.childCount--;
							if (SUPER_VERBOSE_LOGGING)
							{
								LOGGER.info("parent sect "+sectPos+" now has "+parent.childCount+" child.");
							}
							
							// mark this section for deletion
							section.childCount = -1;
							
							// Note that this doesn't necessarily mean this section will be freed as it may be rescued later
							//	  due to neighboring quadrants not able to be freed (they pass targetLevel checks or has children)
							//	  or due to parent's layer is in the Always Cascade mode. (containerType == null)
						}
						
						
						if (section == null && targetDetailLevel < this.getLayerDataDetail((byte) (sectionDetailLevel + 1)))
						{
							// the render section for this detail level is missing, create it
							
							if (SUPER_VERBOSE_LOGGING)
							{
								LOGGER.info("null sect "+sectPos+" target<nextLevel. Creating.");
							}
							
							section = this._setRenderSectionInGridList(ringList, pos.x, pos.y, new LodRenderSection(sectPos));
							LodUtil.assertTrue(parentRingList != null);
							
							
							LodRenderSection parent = this._getRenderSectionFromGridList(parentRingList, halfPos.x, halfPos.y);
							if (parent == null)
							{
								// this render section's parent is missing, create it
								
								if (SUPER_VERBOSE_LOGGING)
								{
									LOGGER.info("sect "+sectPos+" missing parent. Creating at "+sectPos.getParentPos());
								}
								
								parent = this._setRenderSectionInGridList(parentRingList, halfPos.x, halfPos.y, new LodRenderSection(sectPos.getParentPos()));
							}
							
							
							parent.childCount++;
							if (SUPER_VERBOSE_LOGGING)
							{
								LOGGER.info("parent render section "+sectPos.getParentPos()+" now has "+parent.childCount+" children.");
							}
						}
					}
				}
				
				
				// Final quick assert to insure section pos is correct.
				if (section != null)
				{
					LodUtil.assertTrue(section.pos.sectionDetailLevel == sectionDetailLevel, "section.pos: " + section.pos + " vs level: " + sectionDetailLevel);
					LodUtil.assertTrue(section.pos.sectionX == pos.x, "section.pos: " + section.pos + " vs pos: " + pos);
					LodUtil.assertTrue(section.pos.sectionZ == pos.y, "section.pos: " + section.pos + " vs pos: " + pos);
				}
			});
		}
	}
	
	private void updateAllREnderSections()
	private void updateAllRenderSections()
	{
		// TODO: inline comments should be added everywhere for this tick pass, so this comment block should be removed (having duplicate comments in two places is a bad idea)
		// Second tick pass:
		// Cascade the layers that is in Always Cascade Mode from top to bottom. (Not yet exposed or used)
		// At the same time, load and unload sections (and can also be used to assert everything is working).
		// 
		//   // ===Assertion steps===
		//   assert childCount == 4 || childCount == 0 || childCount == -1
		//   if childCount == 4 assert all children exist
		//   if childCount == 0 assert all children are null
		//   if childCount == -1 assert parent childCount is 0
		//   // ======================
		// 
		//   if childCount == 4 && section is loaded:
		//     - unload section
		//   if childCount == 0 && section is unloaded:
		//     - load section
		//   if childCount == -1: // (section could be loaded or unloaded if the player is moving fast)
		//     - set this section to null (TODO: Is this needed to be first or last or don't matter for concurrency?)
		//     - If loaded unload section
		// start with close sections and move outward
		for (byte sectLevel = TREE_LOWEST_DETAIL_LEVEL; sectLevel < (byte) (this.numbersOfSectionDetailLevels - 1); sectLevel++)
		{
			final MovableGridRingList<LodRenderSection> ringList = this.renderSectionRingLists[sectLevel - TREE_LOWEST_DETAIL_LEVEL];
			//final MovableGridRingList<LodRenderSection> childRingList = sectLevel == TREE_LOWEST_DETAIL_LEVEL ? null : this.renderSectionRingLists[sectLevel - TREE_LOWEST_DETAIL_LEVEL - 1];
			//final boolean doCascade = false; // TODO: Utilize this cascade mode or at least expose this option
			
			ringList.forEachPosOrdered((section, pos) ->
			{
				if (section == null)
				{
					return;
				}
				
				// Cascade layers
//                if (doCascade && section.childCount == 0) {
//                    LodUtil.assertTrue(childRingList != null);
//                    // Create children to cascade the layer.
//                    for (byte i = 0; i < 4; i++) {
//                        DhSectionPos childPos = section.pos.getChild(i);
//                        LodRenderSection child = childRingList.get(childPos.sectionX, childPos.sectionZ);
//                        if (child == null) {
//                            child = childRingList.setChained(childPos.sectionX, childPos.sectionZ,
//                                    new LodRenderSection(childPos));
//                            child.childCount = 0;
//                        } else {
//                            LodUtil.assertTrue(child.childCount == -1,
//                                    "Self has child count 0 but an existing child's child count != -1!");
//                            child.childCount = 0;
//                        }
//                    }
//                    section.childCount = 4;
//                }
				
				
				//======================//
				// load new sections,   //
				// tick existing ones,  //
				// dispose old sections //
				//======================//
				
				if (section.childCount == -1)
				{
					// dispose the old section
					
					if (section.pos.sectionDetailLevel < this.numbersOfSectionDetailLevels-1)
					{
						LodUtil.assertTrue(this.getParentSection(section.pos).childCount == 0);
					}
					
					ringList.remove(pos.x, pos.y);
					section.disposeRenderData();
					
					return;
				}
				else
				{
					if (!section.isLoaded() && !section.isLoading())
					{
						// load in the new section
						section.load(this.renderSourceProvider);
					}
					else if (section.isOutdated())
					{
						// replace the out of date data
						section.reload(this.renderSourceProvider);
					}
					
					
					// enable rendering if this section is a leaf node in the tree, otherwise disable rendering 
					if (section.childCount == 4)
					{
						// only disable rendering if the next section is ready to render, 
						// isRenderingEnabled check to prevent calling the recursive method more than necessary
						if (section.isRenderingEnabled() && areChildRenderSectionsLoaded(section)) // FIXME: this is an imperfect solution, some sections will still appear/disappear incorrectly
						{
							section.disableRender();
						}
					}
					else if (section.childCount == 0)
					{
						section.enableRender();
					}
					
					
					// update the section
					section.tick(this, this.level);
				}
				
				
				// should be called after the section has been updated
				assertRenderSectionIsValid(section);
			});
		}
	
	/** @throws LodUtil.AssertFailureException if the section isn't valid */
	private void assertRenderSectionIsValid(LodRenderSection section) throws LodUtil.AssertFailureException
	{
		// section validation
		LodUtil.assertTrue(section.childCount == 4|| section.childCount == 0,"Expected render section to have a child count of 0, or 4. Found value: "+ section.childCount);
		
		if (section.pos.sectionDetailLevel == TREE_LOWEST_DETAIL_LEVEL)
		{
			// sections at the bottom of the tree (leaves) should have no additional children
			LodUtil.assertTrue(section.childCount == 0);
		}
		else
		{
			LodRenderSection child0 = this.getChildSection(section.pos, 0);
			LodRenderSection child1 = this.getChildSection(section.pos, 1);
			LodRenderSection child2 = this.getChildSection(section.pos, 2);
			LodRenderSection child3 = this.getChildSection(section.pos, 3);
			
			if (section.childCount == 4)
			{
				LodUtil.assertTrue(
						child0 != null && child0.childCount != -1 &&
								child1 != null && child1.childCount != -1 &&
								child2 != null && child2.childCount != -1 &&
								child3 != null && child3.childCount != -1,
						"Sect "+ section.pos+" child count 4 but child has null or is being disposed: {} {} {} {}", child0, child1, child2, child3);
			}
			else if (section.childCount == 0)
			{
				LodUtil.assertTrue(
						(child0 == null || child0.childCount == -1) &&
								(child1 == null || child1.childCount == -1) &&
								(child2 == null || child2.childCount == -1) &&
								(child3 == null || child3.childCount == -1),
						"Sect "+ section.pos+" child count 0 but child is neither null or being disposed: {} {} {} {}",
						child0, child1, child2, child3);
			}
		}
	}
	
	
	private boolean areChildRenderSectionsLoaded(LodRenderSection renderSection)
	{
		if (renderSection == null)
		{
			// this section isn't loaded
			return false;
		}
		if (renderSection.pos.sectionDetailLevel == TREE_LOWEST_DETAIL_LEVEL)
		{
			// this section is at the bottom detail level and has no children
			return isSectionLoaded(renderSection);
		}
		else
		{
			// recursively look for a loaded child
			LodRenderSection child0 = this.getChildSection(renderSection.pos, 0);
			LodRenderSection child1 = this.getChildSection(renderSection.pos, 1);
			LodRenderSection child2 = this.getChildSection(renderSection.pos, 2);
			LodRenderSection child3 = this.getChildSection(renderSection.pos, 3);
			
			// either the child section is loaded, or check the next section down
			return (isSectionLoaded(child0) || areChildRenderSectionsLoaded(child0))
					&& (isSectionLoaded(child1) || areChildRenderSectionsLoaded(child1))
					&& (isSectionLoaded(child2) || areChildRenderSectionsLoaded(child2))
					&& (isSectionLoaded(child3) || areChildRenderSectionsLoaded(child3));
		}
	}
	private static boolean isSectionLoaded(LodRenderSection renderSection)
	{
		return renderSection != null && renderSection.isLoaded() && !renderSection.getRenderSource().isEmpty();
	}
	
	
	
	/** 
	 * Re-creates the color, render data. 
	 * This method should be called after resource packs are changed or LOD settings are modified.
	 */
	public void clearRenderDataCache()
	{
		LOGGER.info("Clearing render cache...");
		
		// clear each ring list
		for (byte sectionDetailLevel = TREE_LOWEST_DETAIL_LEVEL; sectionDetailLevel < this.numbersOfSectionDetailLevels; sectionDetailLevel++)
		{
			MovableGridRingList<LodRenderSection> ringList = this.renderSectionRingLists[sectionDetailLevel-TREE_LOWEST_DETAIL_LEVEL];
			if (ringList != null)
			{
				ringList.clear((section) -> section.disposeRenderData());
				
				LOGGER.info("Finished deleting render files for detail level ["+sectionDetailLevel+"]...");
			}
		}
		
		// delete the cache files
		this.renderSourceProvider.deleteRenderCache();
		
		LOGGER.info("Render cache invalidated");
	}
	
	
	
	
	
	//=========================//
	// internal helper methods //
	//=========================//
	
	private LodRenderSection _setRenderSectionInGridList(MovableGridRingList<LodRenderSection> list, int x, int z, LodRenderSection renderSection)
	{
		LodUtil.assertTrue(renderSection != null, "setting null at [{},{}] in {}", x, z, list.toString());
		LodUtil.assertTrue(renderSection.pos.sectionX == x && renderSection.pos.sectionZ == z, "pos {} != [{},{}] in {}", renderSection.pos, x, z, list.toString());
		
		LodRenderSection section = list.setChained(x,z,renderSection);
		LodUtil.assertTrue(section != null, "returned null at [{},{}]: {}", x, z, list.toString());
		LodUtil.assertTrue(section == renderSection,"{} != {} in {}",section,renderSection, list.toString());
		return section;
	}
	private LodRenderSection _getNotNull(MovableGridRingList<LodRenderSection> list, int x, int z)
	{
		LodUtil.assertTrue(list.inRange(x,z), "[{},{}] not in range of {}", x, z, list.toString());
		
		LodRenderSection section = list.get(x,z);
		LodUtil.assertTrue(section != null, "getting null at [{},{}] in {}", x, z, list.toString());
		LodUtil.assertTrue(section.pos.sectionX == x && section.pos.sectionZ == z, "obj {} != [{},{}] in {}", section, x, z, list.toString());
		return section;
	}
	private LodRenderSection _getRenderSectionFromGridList(MovableGridRingList<LodRenderSection> list, int x, int z)
	{
		LodRenderSection section = list.get(x,z);
		LodUtil.assertTrue(section == null || (section.pos.sectionX == x && section.pos.sectionZ == z), "obj {} != [{},{}] in {}", section, x, z, list.toString());
		return section;
	}
	
	
	
	//==============//
	// base methods //
	//==============//
	
	public String getDebugString()
	{
		StringBuilder sb = new StringBuilder();
		for (byte i = 0; i < this.renderSectionRingLists.length; i++)
		{
			sb.append("Layer ").append(i + TREE_LOWEST_DETAIL_LEVEL).append(":\n");
			sb.append(this.renderSectionRingLists[i].toDetailString());
			sb.append("\n");
			sb.append("\n");
		}
		return sb.toString();
	}

    @Override
	public void close()
	{
		LOGGER.info("Shutting down "+ LodQuadTree.class.getSimpleName()+"...");
		
		for (MovableGridRingList<LodRenderSection> ringList : this.renderSectionRingLists)
		{
			ringList.forEach((section) ->
			{
				if (section != null)
				{
					section.disposeRenderData();
				}
			});
		}
		
		LOGGER.info("Finished shutting down "+ LodQuadTree.class.getSimpleName());
	}
	
}
