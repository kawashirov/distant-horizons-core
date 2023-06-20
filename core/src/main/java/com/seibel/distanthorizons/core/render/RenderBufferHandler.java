package com.seibel.distanthorizons.core.render;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.Pos2D;
import com.seibel.distanthorizons.core.util.objects.SortedArraySet;
import com.seibel.distanthorizons.core.util.objects.quadTree.QuadNode;
import com.seibel.distanthorizons.core.enums.ELodDirection;
import com.seibel.distanthorizons.core.render.renderer.LodRenderer;
import com.seibel.distanthorizons.coreapi.util.math.Vec3f;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.Iterator;

/** 
 * This object tells the {@link LodRenderer} what buffers to render 
 * TODO rename this class, maybe RenderBufferOrganizer or something more specific?
 */
public class RenderBufferHandler
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	/** contains all relevant data */
	public final LodQuadTree lodQuadTree;
	
	// TODO: Make sorting go into the update loop instead of the render loop as it doesn't need to be done every frame
	private SortedArraySet<LoadedRenderBuffer> loadedNearToFarBuffers = null;
	
	
	
	public RenderBufferHandler(LodQuadTree lodQuadTree) { this.lodQuadTree = lodQuadTree; }
	
	
	
	/**
	 * The following buildRenderList sorting method is based on the following reddit post: <br>
	 * <a href="https://www.reddit.com/r/VoxelGameDev/comments/a0l8zc/correct_depthordering_for_translucent_discrete/">correct_depth_ordering_for_translucent_discrete</a> <br><br>
	 *
	 * TODO: This might get locked by update() causing move() call. Is there a way to avoid this?
	 *       Maybe dupe the base list and use atomic swap on render? Or is this not worth it?
	 */
	public void buildRenderList(Vec3f lookForwardVector)
	{
		ELodDirection[] axisDirections = new ELodDirection[3];
		
		// Do the axis that are the longest first (i.e. the largest absolute value of the lookForwardVector),
		// with the sign being the opposite of the respective lookForwardVector component's sign
		float absX = Math.abs(lookForwardVector.x);
		float absY = Math.abs(lookForwardVector.y);
		float absZ = Math.abs(lookForwardVector.z);
		ELodDirection xDir = lookForwardVector.x < 0 ? ELodDirection.EAST : ELodDirection.WEST;
		ELodDirection yDir = lookForwardVector.y < 0 ? ELodDirection.UP : ELodDirection.DOWN;
		ELodDirection zDir = lookForwardVector.z < 0 ? ELodDirection.SOUTH : ELodDirection.NORTH;
		
		if (absX >= absY && absX >= absZ)
		{
			axisDirections[0] = xDir;
			if (absY >= absZ)
			{
				axisDirections[1] = yDir;
				axisDirections[2] = zDir;
			}
			else
			{
				axisDirections[1] = zDir;
				axisDirections[2] = yDir;
			}
		}
		else if (absY >= absX && absY >= absZ)
		{
			axisDirections[0] = yDir;
			if (absX >= absZ)
			{
				axisDirections[1] = xDir;
				axisDirections[2] = zDir;
			}
			else
			{
				axisDirections[1] = zDir;
				axisDirections[2] = xDir;
			}
		}
		else
		{
			axisDirections[0] = zDir;
			if (absX >= absY)
			{
				axisDirections[1] = xDir;
				axisDirections[2] = yDir;
			}
			else
			{
				axisDirections[1] = yDir;
				axisDirections[2] = xDir;
			}
		}
	
		// Now that we have the axis directions, we can sort the render list
		Comparator<LoadedRenderBuffer> farToNearComparator = (loadedBufferA, loadedBufferB) ->
		{
			Pos2D aPos = loadedBufferA.pos.getCenter().getCenterBlockPos().toPos2D();
			Pos2D bPos = loadedBufferB.pos.getCenter().getCenterBlockPos().toPos2D();
			for (ELodDirection axisDirection : axisDirections)
			{
				if (axisDirection.getAxis().isVertical())
				{
					continue; // We only sort in the horizontal direction
				}
				
				int abPosDifference;
				if (axisDirection.getAxis().equals(ELodDirection.Axis.X))
				{
					abPosDifference = aPos.x - bPos.x;
				}
				else
				{
					abPosDifference = aPos.y - bPos.y;
				}
				
				if (abPosDifference == 0)
				{
					continue;
				}
				
				if (axisDirection.getAxisDirection().equals(ELodDirection.AxisDirection.NEGATIVE))
				{
					abPosDifference = -abPosDifference; // Reverse the sign
				}
				return abPosDifference;
			}
			
			return loadedBufferA.pos.sectionDetailLevel - loadedBufferB.pos.sectionDetailLevel; // If all else fails, sort by detail
		};
		
		// Build the sorted list
		this.loadedNearToFarBuffers = new SortedArraySet<>((a, b) -> -farToNearComparator.compare(a, b)); // TODO is the comparator named wrong?
		
		Iterator<QuadNode<LodRenderSection>> nodeIterator = this.lodQuadTree.nodeIterator();
		while (nodeIterator.hasNext())
		{
			QuadNode<LodRenderSection> node = nodeIterator.next();
			
			DhSectionPos sectionPos = node.sectionPos;
			LodRenderSection renderSection = node.value;
			
			if (renderSection != null && renderSection.isRenderingEnabled())
			{
				AbstractRenderBuffer buffer = renderSection.activeRenderBufferRef.get();
				if (buffer != null)
				{
					this.loadedNearToFarBuffers.add(new LoadedRenderBuffer(buffer, sectionPos));
				}
			}
		}
	}
	
    public void renderOpaque(LodRenderer renderContext)
	{
		//TODO: Directional culling
		this.loadedNearToFarBuffers.forEach(loadedBuffer -> loadedBuffer.buffer.renderOpaque(renderContext));
	}
    public void renderTransparent(LodRenderer renderContext)
	{
		//TODO: Directional culling
		this.loadedNearToFarBuffers.forEach(loadedBuffer -> loadedBuffer.buffer.renderTransparent(renderContext));
	}

	private boolean rebuildAllBuffers = false;

	public void MarkAllBuffersDirty()
	{
		this.rebuildAllBuffers = true;
	}

	public void updateQuadTreeRenderSources()
	{
		boolean rebuildAllBuffers = this.rebuildAllBuffers;
		this.rebuildAllBuffers = false;

		Iterator<QuadNode<LodRenderSection>> nodeIterator = this.lodQuadTree.nodeIterator();
		while (nodeIterator.hasNext())
		{
			LodRenderSection renderSection = nodeIterator.next().value;
			try {
				if (renderSection != null)
				{
					if (rebuildAllBuffers)
					{
						renderSection.markBufferDirty();
					}
					renderSection.tryBuildAndSwapBuffer();
				}
			}
			catch (Exception e)
			{
				LOGGER.error("Error updating QuadTree render source at "+renderSection.pos+".", e);
			}
		}
	}
	
    public void close() 
	{
		Iterator<QuadNode<LodRenderSection>> nodeIterator = this.lodQuadTree.nodeIterator();
		while (nodeIterator.hasNext())
		{
			LodRenderSection renderSection = nodeIterator.next().value;
			if (renderSection != null)
			{
				renderSection.dispose();
			}
		}
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	private static class LoadedRenderBuffer
	{
		public final AbstractRenderBuffer buffer;
		public final DhSectionPos pos;
		
		LoadedRenderBuffer(AbstractRenderBuffer buffer, DhSectionPos pos)
		{
			this.buffer = buffer;
			this.pos = pos;
		}
	}
	
}
