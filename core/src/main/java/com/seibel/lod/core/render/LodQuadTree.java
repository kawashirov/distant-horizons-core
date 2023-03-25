package com.seibel.lod.core.render;

import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.file.renderfile.ILodRenderSourceProvider;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;
import com.seibel.lod.core.util.objects.quadTree.QuadNode;
import com.seibel.lod.core.util.objects.quadTree.QuadTree;
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
public class LodQuadTree extends QuadTree<LodRenderSection> implements AutoCloseable
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
	
	
    public final int blockRenderDistance;
    private final ILodRenderSourceProvider renderSourceProvider;
	
	/** How many {@link LodRenderSection}'s are currently loading */
	private int numberOfRenderSectionsLoading = 0;
	/** 
	 * Indicates how many {@link LodRenderSection}'s can load concurrently. <br>
	 * Prevents large number of {@link ILodRenderSourceProvider} tasks from building up when initially loading. 
	 */
	private static final int MAX_NUMBER_OF_LOADING_RENDER_SECTIONS = 2;
	
	private final IDhClientLevel level; //FIXME: Proper hierarchy to remove this reference!
	
	
	
	
    public LodQuadTree(
			IDhClientLevel level, int viewDistanceInBlocks, 
			int initialPlayerBlockX, int initialPlayerBlockZ, 
			ILodRenderSourceProvider provider)
	{
		super(viewDistanceInBlocks, new DhBlockPos2D(initialPlayerBlockX, initialPlayerBlockZ), DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
		
        DetailDistanceUtil.updateSettings(); //TODO: Move this to somewhere else
        this.level = level;
		this.renderSourceProvider = provider;
        this.blockRenderDistance = viewDistanceInBlocks;
		
    }
	
	
	
    /**
     * This method return the LodSection at the given detail level and level coordinate x and z
     * @param detailLevel detail level of the section
     * @param x x coordinate of the section
     * @param z z coordinate of the section
     * @return the LodSection
     */
    public LodRenderSection getSection(byte detailLevel, int x, int z) { return this.get(new DhSectionPos(detailLevel, x, z)); }
    public LodRenderSection getSection(DhSectionPos pos) { return this.get(pos); }
	
	
	
	
	
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
		try
		{
			// recenter if necessary
			this.setCenterBlockPos(playerPos, LodRenderSection::disposeRenderData);
			
			updateAllRenderSections(playerPos);
		}
		catch (Exception e)
		{
			// TODO when we are stable this shouldn't be necessary
			LOGGER.error("Quad Tree tick exception for dimension: "+this.level.getClientLevelWrapper().getDimensionType().getDimensionName()+", exception: "+e.getMessage(), e);
		}
	}
	
	
	
	private void updateAllRenderSections(DhBlockPos2D playerPos)
	{
		this.forEachRootNodePos((rootNode, rootSectionPos) ->
		{
			if (rootNode == null)
			{
				LodRenderSection newRenderSection = new LodRenderSection(rootSectionPos);
				this.set(rootSectionPos, newRenderSection);
				return; // update next tick
			}
			
			
			rootNode.forEachDirectChild((quadNode, sectionPos) ->
			{
				recursivelyUpdateRenderSectionNode(playerPos, rootNode, quadNode, sectionPos);
			});
		});
	}
	private void recursivelyUpdateRenderSectionNode(DhBlockPos2D playerPos, QuadNode<LodRenderSection> rootNode, QuadNode<LodRenderSection> nullableQuadNode, DhSectionPos sectionPos)
	{
		LodRenderSection nullableRenderSection = null;
		if (nullableQuadNode != null)
		{
			nullableRenderSection = nullableQuadNode.value;
		}
		
		
		byte expectedDetailLevel = calculateExpectedDetailLevel(playerPos, sectionPos);
		expectedDetailLevel += DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL;
		expectedDetailLevel = (byte) Math.min(expectedDetailLevel, this.treeMaxDetailLevel);
		
		if (sectionPos.sectionDetailLevel > expectedDetailLevel)
		{
			// section detail level too high...
			
			if (nullableRenderSection != null)
			{
				nullableRenderSection.disableAndDisposeRender();
			}
			
			if (nullableQuadNode == null)
			{
				// ...create self
				if (this.isSectionPosInBounds(sectionPos)) // this should only fail when at the edge of the user's render distance
				{
					rootNode.setValue(sectionPos, new LodRenderSection(sectionPos));
				}
			}
			else
			{
				nullableQuadNode.forEachDirectChild((childQuadNode, childSectionPosition) ->
				{
					recursivelyUpdateRenderSectionNode(playerPos, rootNode, childQuadNode, childSectionPosition);
				});
			}
		}
		else if (sectionPos.sectionDetailLevel == expectedDetailLevel)
		{
			// this is the correct detail level and should be rendered
			
			if (nullableQuadNode == null)
			{
				if (this.isSectionPosInBounds(sectionPos))
				{
					// create new value and update next tick
					rootNode.setValue(sectionPos, new LodRenderSection(sectionPos));
				}
			}
			else
			{
				// create a new render section if missing
				if (nullableRenderSection == null)
				{
					LodRenderSection newRenderSection = new LodRenderSection(sectionPos);
					rootNode.setValue(sectionPos, newRenderSection);
					
					nullableRenderSection = newRenderSection;
				}
				
				// enable the render section
				nullableRenderSection.loadRenderSourceAndEnableRendering(this.renderSourceProvider);
				
				nullableRenderSection.tick(this, this.level);
				
				// delete/disable children
				nullableQuadNode.deleteAllChildren((renderSection) ->
				{
					if (renderSection != null)
					{
						renderSection.disableAndDisposeRender();
					}
				});
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
	
	
	
	//=============//
	// render data //
	//=============//
	
	/** 
	 * Re-creates the color, render data. 
	 * This method should be called after resource packs are changed or LOD settings are modified.
	 */
	public void clearRenderDataCache()
	{
		LOGGER.info("Clearing render cache...");
		
		this.forEachRootNode((rootNode) ->
		{
			rootNode.forEachDirectChild((quadNode, sectionPos) ->
			{
				if (quadNode != null && quadNode.value != null)
				{
					quadNode.value.disposeRenderData();
				}
			});
		});
		
		this.forEachRootNode((rootNode) ->
		{
			rootNode.forEachDirectChild((quadNode, sectionPos) ->
			{
				if (quadNode != null && quadNode.value != null)
				{
					quadNode.value.disposeRenderData();
					quadNode.value = null;
				}
			});
		});
		
		// delete the cache files
		this.renderSourceProvider.deleteRenderCache();
		
		LOGGER.info("Render cache invalidated");
	}
	
	/** 
	 * Can be called whenever a render section's data needs to be refreshed. <br>
	 * This should be called whenever a world generation task is completed or if the connected server has new data to show.
	 */
	public void reloadPos(DhSectionPos pos)
	{
		LodRenderSection renderSection = this.getSection(pos);
		if (renderSection != null)
		{
			renderSection.reload(this.renderSourceProvider);
		}
	}
	
	
	
	
	//=========================//
	// internal helper methods //
	//=========================//
	
	/** @return the renderSection set */
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
	
//	public String getDebugString()
//	{
//		StringBuilder sb = new StringBuilder();
//		for (byte i = 0; i < this.renderSectionRingLists.length; i++)
//		{
//			sb.append("Layer ").append(i + TREE_LOWEST_DETAIL_LEVEL).append(":\n");
//			sb.append(this.renderSectionRingLists[i].toDetailString());
//			sb.append("\n");
//			sb.append("\n");
//		}
//		return sb.toString();
//	}

    @Override
	public void close()
	{
		LOGGER.info("Shutting down "+ LodQuadTree.class.getSimpleName()+"...");
		
		this.forEachRootNode((rootNode) -> 
		{
			rootNode.forEachDirectChild((quadNode, sectionPos) -> 
			{
				if (quadNode != null && quadNode.value != null)
				{
					quadNode.value.disposeRenderData();
				}
			});
		});
		
		LOGGER.info("Finished shutting down "+ LodQuadTree.class.getSimpleName());
	}
	
}
