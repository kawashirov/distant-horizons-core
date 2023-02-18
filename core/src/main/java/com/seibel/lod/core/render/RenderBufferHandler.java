package com.seibel.lod.core.render;

import com.seibel.lod.core.datatype.render.ColumnRenderSource;
import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.pos.Pos2D;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.render.renderer.LodRenderer;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;
import com.seibel.lod.core.util.math.Vec3f;
import com.seibel.lod.core.util.objects.SortedArraySet;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This object tells the {@link LodRenderer} what buffers to render
 */
public class RenderBufferHandler
{
    public final LodQuadTree quadTree;
    private final MovableGridRingList<RenderBufferNode> renderBufferNodesGridList;
	
	// TODO: Make sorting go into the update loop instead of the render loop as it doesn't need to be done every frame
	private SortedArraySet<LoadedRenderBuffer> loadedNearToFarBuffers = null;
	
	
	
	public RenderBufferHandler(LodQuadTree quadTree)
	{
		this.quadTree = quadTree;
		
		MovableGridRingList<LodRenderSection> referenceList = quadTree.getRingList((byte) (quadTree.getNumbersOfSectionDetailLevels() - 1));
		Pos2D center = referenceList.getCenter();
		this.renderBufferNodesGridList = new MovableGridRingList<>(referenceList.getHalfSize(), center);
	}
	
	
	
	/**
     * The following buildRenderList sorting method is based on the following reddit post:
     * https://www.reddit.com/r/VoxelGameDev/comments/a0l8zc/correct_depthordering_for_translucent_discrete/
	 * 
	 * TODO: This might get locked by update() causing move() call. Is there a way to avoid this?
	 *       Maybe dupe the base list and use atomic swap on render? Or is this not worth it?
	 */
	public void buildRenderList(Vec3f lookForwardVector)
	{
		ELodDirection[] axisDirections = new ELodDirection[3];
		
		// Do the axis that are longest first (i.e. the largest absolute value of the lookForwardVector),
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
		Comparator<LoadedRenderBuffer> sortFarToNear = (loadedBufferA, loadedBufferB) ->
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
		this.loadedNearToFarBuffers = new SortedArraySet<>((a, b) -> -sortFarToNear.compare(a, b));
		
		// Add all the loaded buffers to the sorted list
		this.renderBufferNodesGridList.forEach((renderBufferNode) ->
		{
			if (renderBufferNode != null)
			{
				renderBufferNode.collect(this.loadedNearToFarBuffers);
			}
		});
	}
	
    public void renderOpaque(LodRenderer renderContext)
	{
		//TODO: Directional culling
		this.loadedNearToFarBuffers.forEach(loadedBuffer -> loadedBuffer.buffer.renderOpaque(renderContext));
	}
    public void renderTransparent(LodRenderer renderContext)
	{
		if (LodRenderer.transparencyEnabled)
		{
			this.loadedNearToFarBuffers.forEach(loadedBuffer -> loadedBuffer.buffer.renderTransparent(renderContext));
		}
	}
	
	public void update()
	{
		byte topDetailLevel = (byte) (this.quadTree.getNumbersOfSectionDetailLevels() - 1);
		MovableGridRingList<LodRenderSection> renderSectionGridList = this.quadTree.getRingList(topDetailLevel);
		
		Pos2D newCenterPos = renderSectionGridList.getCenter();
		this.renderBufferNodesGridList.moveTo(newCenterPos.x, newCenterPos.y, RenderBufferNode::close); // Note: may lock the list
		
		
		
		this.renderBufferNodesGridList.forEachPosOrdered((renderBufferNode, pos) ->
		{
			DhSectionPos sectPos = new DhSectionPos(topDetailLevel, pos.x, pos.y);
			LodRenderSection renderSection = this.quadTree.getSection(sectPos);
			
			if (renderSection == null && renderBufferNode != null)
			{
				// section is null, but a node exists, remove the node
				this.renderBufferNodesGridList.remove(pos).close();
			}
			else if (renderSection != null)
			{
				
				if (renderBufferNode == null)
				{
					// renderSection exists, but node does not
					renderBufferNode = this.renderBufferNodesGridList.setChained(pos, new RenderBufferNode(sectPos));
				}
				
				// Update the render node
				renderBufferNode.update();	
			}
		});
	}
	
    public void close() { this.renderBufferNodesGridList.clear(RenderBufferNode::close); }
	
	
	
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
	
	
	private class RenderBufferNode implements AutoCloseable
	{
		private final DhSectionPos pos;
		private volatile RenderBufferNode[] children = null;
		
		//FIXME: The multiple Atomics will cause race conditions between them!
		private final AtomicReference<AbstractRenderBuffer> renderBufferRef = new AtomicReference<>();
		
		
		
		public RenderBufferNode(DhSectionPos pos) { this.pos = pos; }
		
		
		
		public void collect(SortedArraySet<LoadedRenderBuffer> sortedSet)
		{
			AbstractRenderBuffer renderBuffer = this.renderBufferRef.get();
			if (renderBuffer != null)
			{
				sortedSet.add(new LoadedRenderBuffer(renderBuffer, this.pos));
			}
			else
			{
				RenderBufferNode[] children = this.children;
				if (children != null)
				{
					for (RenderBufferNode child : children)
					{
						child.collect(sortedSet);
					}
				}
			}
		}
		
		
		//TODO: In the future make this logic a bit more complete so that when children are just created,
		//      the buffer is only unloaded if all children's buffers are ready. This will make the
		//      transition between buffers no longer causing any flicker.
		public void update()
		{
			LodRenderSection section = quadTree.getSection(this.pos);
			
			// If this fails, there may be concurrent modification of the quad tree
			//  (as this update() should be called from the same thread that calls update() on the quad tree)
			LodUtil.assertTrue(section != null);
			
			ColumnRenderSource currentRenderSource = section.getRenderSource();
			
			// Update self's render buffer state
			if (!section.shouldRender())
			{
				//TODO: Does this really need to force the old buffer to not be rendered?
				AbstractRenderBuffer renderBuffer = this.renderBufferRef.getAndSet(null);
				if (renderBuffer != null)
				{
					renderBuffer.close();
				}
			}
			else
			{
				LodUtil.assertTrue(currentRenderSource != null); // section.isLoaded() should have ensured this
				currentRenderSource.trySwapRenderBufferAsync(quadTree, this.renderBufferRef);
			}
			
			
			// Update children's render buffer state
			// TODO: Improve this! (Checking section.isLoaded() as if its not loaded, it can only be because
			//  it has children. (But this logic is... really hard to read!)
			// FIXME: Above comment is COMPLETELY WRONG! I am an idiot!
			boolean sectionHasChildren = section.FIXME_BYPASS_DONT_USE_getChildCount() > 0;
			if (sectionHasChildren)
			{
				if (this.children == null)
				{
					this.children = new RenderBufferNode[4];
					for (int i = 0; i < 4; i++)
					{
						this.children[i] = new RenderBufferNode(this.pos.getChildByIndex(i));
					}
				}
				
				for (RenderBufferNode child : this.children)
				{
					child.update();
				}
			}
			else
			{
//				if (this.children != null)
//				{
//					//FIXME: Concurrency issue here: If render thread is concurrently using the child's buffer,
//					//  and this thread got priority to close the buffer, it causes a bug where the render thread
//					//  will be using a closed buffer!!!!
//					RenderBufferNode[] children = this.children;
//					this.children = null;
//					for (RenderBufferNode child : children)
//					{
//						child.close();
//					}
//				}
			}
		}
		
		@Override
		public void close()
		{
			if (this.children != null)
			{
				for (RenderBufferNode child : this.children)
				{
					child.close();
				}
			}
			
			AbstractRenderBuffer renderBuffer = this.renderBufferRef.getAndSet(null);
			if (renderBuffer != null)
			{
				renderBuffer.close();
			}
		}
	}
	
	
}
