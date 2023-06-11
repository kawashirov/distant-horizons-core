package com.seibel.lod.core.render;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.config.listeners.ConfigChangeListener;
import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.file.renderfile.ILodRenderSourceProvider;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.objects.quadTree.QuadNode;
import com.seibel.lod.core.util.objects.quadTree.QuadTree;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This quadTree structure is our core data structure and holds
 * all rendering data.
 */
public class LodQuadTree extends QuadTree<LodRenderSection> implements AutoCloseable
{
    public static final byte TREE_LOWEST_DETAIL_LEVEL = ColumnRenderSource.SECTION_SIZE_OFFSET;
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
    public final int blockRenderDistance;
    private final ILodRenderSourceProvider renderSourceProvider;
	
	/** 
	 * This holds every {@link DhSectionPos} that should be reloaded next tick. <br>
	 * This is a {@link ConcurrentHashMap} because new sections can be added to this list via the world generator threads.
	 */
	private final ConcurrentHashMap<DhSectionPos, Boolean> sectionsToReload = new ConcurrentHashMap<>();
	
	private final IDhClientLevel level; //FIXME: Proper hierarchy to remove this reference!
	
	private final ConfigChangeListener<Integer> horizontalScaleChangeListener;
	
	
	
	
    public LodQuadTree(
			IDhClientLevel level, int viewDistanceInBlocks, 
			int initialPlayerBlockX, int initialPlayerBlockZ, 
			ILodRenderSourceProvider provider)
	{
		super(viewDistanceInBlocks, new DhBlockPos2D(initialPlayerBlockX, initialPlayerBlockZ), TREE_LOWEST_DETAIL_LEVEL);
		
        DetailDistanceUtil.updateSettings(); //TODO: Move this to somewhere else
        this.level = level;
		this.renderSourceProvider = provider;
        this.blockRenderDistance = viewDistanceInBlocks;
		
		this.horizontalScaleChangeListener = new ConfigChangeListener<>(Config.Client.Advanced.Graphics.Quality.horizontalScale, (newHorizontalScale) -> this.onHorizontalScaleChange());
    }
	
	
	//=============//
	// tick update //
	//=============//
	
    /**
     * This function updates the quadTree based on the playerPos and the current game configs (static and global)
     * @param playerPos the reference position for the player
     */
    public void tick(DhBlockPos2D playerPos)
	{
		if (this.level == null)
		{
			// the level hasn't finished loading yet
			// TODO sometimes null pointers still happen, when logging back into a world (maybe the old level isn't null but isn't valid either?)
			return;
		}
		
		
		try
		{
			// recenter if necessary, removing out of bounds sections
			this.setCenterBlockPos(playerPos, LodRenderSection::disposeRenderData);
			
			updateAllRenderSections(playerPos);
		}
		catch (Exception e)
		{
			LOGGER.error("Quad Tree tick exception for dimension: "+this.level.getClientLevelWrapper().getDimensionType().getDimensionName()+", exception: "+e.getMessage(), e);
		}
	}
	private void updateAllRenderSections(DhBlockPos2D playerPos)
	{
		// reload any sections that need it
		DhSectionPos[] reloadSectionArray = this.sectionsToReload.keySet().toArray(new DhSectionPos[0]);
		this.sectionsToReload.clear();
		for (DhSectionPos pos : reloadSectionArray)
		{
			// walk up the tree until we hit the root node
			// this is done so any high detail changes flow up to the lower detail render sections as well 
			while (pos.sectionDetailLevel <= this.treeMaxDetailLevel)
			{
				try
				{
					LodRenderSection renderSection = this.getValue(pos);
					if (renderSection != null)
					{
						renderSection.reload(this.renderSourceProvider);
					}
				}
				catch (IndexOutOfBoundsException e)
				{ /* the section is now out of bounds, it doesn't need to be reloaded */ }
				
				pos = pos.getParentPos();
			}
		}
		
		
		// walk through each root node
		Iterator<DhSectionPos> rootPosIterator = this.rootNodePosIterator();
		while (rootPosIterator.hasNext())
		{
			// make sure all root nodes have been created
			DhSectionPos rootPos = rootPosIterator.next();
			if (this.getNode(rootPos) == null)
			{
				this.setValue(rootPos, new LodRenderSection(rootPos));
			}
			
			QuadNode<LodRenderSection> rootNode = this.getNode(rootPos);
			this.recursivelyUpdateRenderSectionNode(playerPos, rootNode, rootNode, rootNode.sectionPos, false);
		}
	}
	/** @return whether the current position is able to render (note: not if it IS rendering, just if it is ABLE to.) */
	private boolean recursivelyUpdateRenderSectionNode(DhBlockPos2D playerPos, QuadNode<LodRenderSection> rootNode, QuadNode<LodRenderSection> quadNode, DhSectionPos sectionPos, boolean parentRenderSectionIsEnabled)
	{
		//===============================//
		// node and render section setup //
		//===============================//
		
		// make sure the node is created
		if (quadNode == null && this.isSectionPosInBounds(sectionPos)) // the position bounds should only fail when at the edge of the user's render distance
		{
			rootNode.setValue(sectionPos, new LodRenderSection(sectionPos));
			quadNode = rootNode.getNode(sectionPos);
		}
		if (quadNode == null)
		{
			// this node must be out of bounds, or there was an issue adding it to the tree
			return false;
		}
		
		// make sure the render section is created
		LodRenderSection renderSection = quadNode.value;
		// create a new render section if missing
		if (renderSection == null)
		{
			LodRenderSection newRenderSection = new LodRenderSection(sectionPos);
			rootNode.setValue(sectionPos, newRenderSection);
			
			renderSection = newRenderSection;
		}
		
		
		
		
		//===============================//
		// handle enabling, loading,     //
		// and disabling render sections //
		//===============================//
		
//		byte expectedDetailLevel = 6; // can be used instead of the following logic for testing
		byte expectedDetailLevel = this.calculateExpectedDetailLevel(playerPos, sectionPos);
		expectedDetailLevel += DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL;
		expectedDetailLevel = (byte) Math.min(expectedDetailLevel, this.treeMaxDetailLevel);
		
		if (sectionPos.sectionDetailLevel > expectedDetailLevel)
		{
			// section detail level too high //
			
			
			boolean isThisPositionBeingRendered = renderSection.isRenderingEnabled();
			boolean allChildrenSectionsAreLoaded = true;
			
			// recursively update all child render sections
			Iterator<DhSectionPos> childPosIterator = quadNode.getChildPosIterator();
			while (childPosIterator.hasNext())
			{
				DhSectionPos childPos = childPosIterator.next();
				QuadNode<LodRenderSection> childNode = rootNode.getNode(childPos);
				
				boolean childSectionLoaded = this.recursivelyUpdateRenderSectionNode(playerPos, rootNode, childNode, childPos, isThisPositionBeingRendered || parentRenderSectionIsEnabled);
				allChildrenSectionsAreLoaded = childSectionLoaded && allChildrenSectionsAreLoaded;
			}
			
			
			
			if (!allChildrenSectionsAreLoaded)
			{
				// not all child positions are loaded yet, or this section is out of render range
				return isThisPositionBeingRendered;
			}
			else
			{
				// all child positions are loaded, disable this section and enable its children.
				renderSection.disposeRenderData();
				renderSection.disableRendering();
				
				
				
				// walk back down the tree and enable the child sections //TODO there are probably more efficient ways of doing this, but this will work for now
				childPosIterator = quadNode.getChildPosIterator();
				while (childPosIterator.hasNext())
				{
					DhSectionPos childPos = childPosIterator.next();
					QuadNode<LodRenderSection> childNode = rootNode.getNode(childPos);
					
					boolean childSectionLoaded = this.recursivelyUpdateRenderSectionNode(playerPos, rootNode, childNode, childPos, parentRenderSectionIsEnabled);
					allChildrenSectionsAreLoaded = childSectionLoaded && allChildrenSectionsAreLoaded;
				}
				if (!allChildrenSectionsAreLoaded)
				{
					// FIXME having world generation enabled in a pre-generated world that doesn't have any DH data can cause this to happen
					//  surprisingly reloadPos() doesn't appear to be the culprit, maybe there is an issue with reloading/changing the full data source?
					LOGGER.debug("Potential QuadTree concurrency issue. All child sections should be enabled and ready to render for pos: "+sectionPos);
				}
				
				
				// this section is now being rendered via its children
				return true;
			}
		}
		// TODO this should only equal the expected detail level, the (expectedDetailLevel-1) is a temporary fix to prevent corners from being cut out 
		else if (sectionPos.sectionDetailLevel == expectedDetailLevel || sectionPos.sectionDetailLevel == expectedDetailLevel-1)
		{
			// this is the detail level we want to render //
			
			
			// prepare this section for rendering
			renderSection.loadRenderSource(this.renderSourceProvider, this.level);
			
			// wait for the parent to disable before enabling this section, so we don't overdraw/overlap render sections
			if (!parentRenderSectionIsEnabled && renderSection.isRenderDataLoaded())
			{
				renderSection.enableRendering();
				
				
				// delete/disable children, all of them will be a lower detail level than requested
				quadNode.deleteAllChildren((childRenderSection) ->
				{
					if (childRenderSection != null)
					{
						childRenderSection.disposeRenderData();
						childRenderSection.disableRendering();
					}
				});
			}
			
			return renderSection.isRenderDataLoaded();
		}
		else
		{
			throw new IllegalStateException("LodQuadTree shouldn't be updating renderSections below the expected detail level: ["+expectedDetailLevel+"].");
		}
	}
	
	
	
	//====================//
	// detail level logic //
	//====================//
	
	/**
	 * This method will compute the detail level based on player position and section pos
	 * Override this method if you want to use a different algorithm
	 * @param playerPos player position as a reference for calculating the detail level
	 * @param sectionPos section position
	 * @return detail level of this section pos
	 */
	public byte calculateExpectedDetailLevel(DhBlockPos2D playerPos, DhSectionPos sectionPos)
	{
		return DetailDistanceUtil.getDetailLevelFromDistance(playerPos.dist(sectionPos.getCenter().getCenterBlockPos()));
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
	
	
	
	//=============//
	// render data //
	//=============//
	
	/** 
	 * Re-creates the color, render data. 
	 * This method should be called after resource packs are changed or LOD settings are modified.
	 */
	public void clearRenderDataCache()
	{
		// TODO this causes some (harmless) file errors when called
		LOGGER.info("Clearing render cache...");
		
		Iterator<QuadNode<LodRenderSection>> nodeIterator = this.nodeIterator();
		while (nodeIterator.hasNext())
		{
			QuadNode<LodRenderSection> quadNode = nodeIterator.next();
			if (quadNode.value != null)
			{
				quadNode.value.disposeRenderData();
				quadNode.value = null;
			}
		}
		
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
		if (pos == null)
		{
			// shouldn't happen, but James saw it happen once, so this is here just in case
			LOGGER.warn("reloadPos given a null pos.");
			return;
		}
		
		//LOGGER.info("LodQuadTree reloadPos ["+pos+"].");
		this.sectionsToReload.put(pos, true);
	}
	
	
	
	//==================//
	// config listeners //
	//==================//
	
	private void onHorizontalScaleChange()
	{
		// TODO this Util should probably be somewhere else or handled differently, but it works for now
		// Updating the util is necessary whenever the horizontal quality or scale are changed, otherwise they won't be applied
		DetailDistanceUtil.updateSettings();
		
		
		// flush the current render data to make sure the new settings are used
		Iterator<QuadNode<LodRenderSection>> nodeIterator = this.nodeIterator();
		while (nodeIterator.hasNext())
		{
			QuadNode<LodRenderSection> quadNode = nodeIterator.next();
			if (quadNode.value != null)
			{
				quadNode.value.disposeRenderData();
				quadNode.value = null;
			}
		}
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
		
		this.horizontalScaleChangeListener.close();
		
		Iterator<QuadNode<LodRenderSection>> nodeIterator = this.nodeIterator();
		while (nodeIterator.hasNext())
		{
			QuadNode<LodRenderSection> quadNode = nodeIterator.next();
			if (quadNode.value != null)
			{
				quadNode.value.disposeRenderData();
				quadNode.value = null;
			}
		}
		
		LOGGER.info("Finished shutting down "+ LodQuadTree.class.getSimpleName());
	}
	
}
