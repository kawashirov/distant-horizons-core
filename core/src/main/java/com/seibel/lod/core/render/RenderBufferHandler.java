package com.seibel.lod.core.render;

import com.seibel.lod.core.datatype.ILodRenderSource;
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

public class RenderBufferHandler {
    public final LodQuadTree target;
    private final MovableGridRingList<RenderBufferNode> renderBufferNodes;

    private static class LoadedRenderBuffer {
        public final AbstractRenderBuffer buffer;
        public final DhSectionPos pos;
        LoadedRenderBuffer(AbstractRenderBuffer buffer, DhSectionPos pos) {
            this.buffer = buffer;
            this.pos = pos;
        }
    }

    // TODO: Make sorting go into the update loop instead of the render loop as it doesn't need to be done every frame
    private SortedArraySet<LoadedRenderBuffer> loadedNearToFarBuffers = null;

    // The followiing buildRenderList sorting method is based on the following reddit post:
    // https://www.reddit.com/r/VoxelGameDev/comments/a0l8zc/correct_depthordering_for_translucent_discrete/
    public void buildRenderList(Vec3f lookForwardVector) {
        ELodDirection[] axisDirections = new ELodDirection[3];
        // Do the axis that are longest first (i.e. the largest absolute value of the lookForwardVector)
        // , with the sign being the opposite of the respective lookForwardVector component's sign
        float absX = Math.abs(lookForwardVector.x);
        float absY = Math.abs(lookForwardVector.y);
        float absZ = Math.abs(lookForwardVector.z);
        ELodDirection xDir = lookForwardVector.x < 0 ? ELodDirection.EAST : ELodDirection.WEST;
        ELodDirection yDir = lookForwardVector.y < 0 ? ELodDirection.UP : ELodDirection.DOWN;
        ELodDirection zDir = lookForwardVector.z < 0 ? ELodDirection.SOUTH : ELodDirection.NORTH;
        if (absX >= absY && absX >= absZ) {
            axisDirections[0] = xDir;
            if (absY >= absZ) {
                axisDirections[1] = yDir;
                axisDirections[2] = zDir;
            } else {
                axisDirections[1] = zDir;
                axisDirections[2] = yDir;
            }
        } else if (absY >= absX && absY >= absZ) {
            axisDirections[0] = yDir;
            if (absX >= absZ) {
                axisDirections[1] = xDir;
                axisDirections[2] = zDir;
            } else {
                axisDirections[1] = zDir;
                axisDirections[2] = xDir;
            }
        } else {
            axisDirections[0] = zDir;
            if (absX >= absY) {
                axisDirections[1] = xDir;
                axisDirections[2] = yDir;
            } else {
                axisDirections[1] = yDir;
                axisDirections[2] = xDir;
            }
        }

        // Now that we have the axis directions, we can sort the render list
        Comparator<LoadedRenderBuffer> sortFarToNear = (a, b) -> {
            Pos2D aPos = a.pos.getCenter().getCenterBlockPos().toPos2D();
            Pos2D bPos = b.pos.getCenter().getCenterBlockPos().toPos2D();
            for (ELodDirection axisDirection : axisDirections) {
                if (axisDirection.getAxis().isVertical()) continue; // We works on the horizontal plane only for section sorting
                int abDiff;
                if (axisDirection.getAxis().equals(ELodDirection.Axis.X)) {
                    abDiff = aPos.x - bPos.x;
                } else {
                    abDiff = aPos.y - bPos.y;
                }
                if (abDiff == 0) continue;
                if (axisDirection.getAxisDirection().equals(ELodDirection.AxisDirection.NEGATIVE)) {
                    abDiff = -abDiff; // Reverse the sign
                }
                return abDiff;
            }
            return a.pos.sectionDetailLevel - b.pos.sectionDetailLevel; // If all else fails, sort by detail
        };
        Comparator<LoadedRenderBuffer> sortNearToFar = (a, b) -> -sortFarToNear.compare(a, b);
        // Build the sorted list
        loadedNearToFarBuffers = new SortedArraySet<>(sortNearToFar);
        // Add all the loaded buffers to the sorted list
        renderBufferNodes.forEach((r) -> {if (r!=null) r.collect(loadedNearToFarBuffers);});
    }


    class RenderBufferNode implements AutoCloseable {
        public final DhSectionPos pos;
        public volatile RenderBufferNode[] children = null;

        //FIXME: The multiple Atomics will cause race conditions between them!
        public final AtomicReference<AbstractRenderBuffer> renderBufferSlot = new AtomicReference<>();

        public RenderBufferNode(DhSectionPos pos) {
            this.pos = pos;
        }

        public void collect(SortedArraySet<LoadedRenderBuffer> sortedSet) {
            AbstractRenderBuffer buff;
            buff = renderBufferSlot.get();
            if (buff != null) {
                sortedSet.add(new LoadedRenderBuffer(buff, pos));
            } else {
                RenderBufferNode[] childs = children;
                if (childs != null) {
                    for (RenderBufferNode child : childs) {
                        child.collect(sortedSet);
                    }
                }
            }
        }

        //TODO: In the future make this logic a bit more complex so that when children are just created,
        //      the buffer is only unloaded if all children's buffers are ready. This will make the
        //      transition between buffers no longer causing any flicker.
        public void update() {
            LodRenderSection section = target.getSection(pos);
            // If this fails, there may be concurrent modification of the quad tree
            //  (as this update() should be called from the same thread that calls update() on the quad tree)
            LodUtil.assertTrue(section != null);
            ILodRenderSource container = section.getRenderSource();

            // Update self's render buffer state
            boolean shouldRender = section.canRender();
            if (!shouldRender) {
                //TODO: Does this really need to force the old buffer to not be rendered?
                AbstractRenderBuffer buff = renderBufferSlot.getAndSet(null);
                if (buff != null) {
                    buff.close();
                }
            } else {
                LodUtil.assertTrue(container != null); // section.isLoaded() should have ensured this
                container.trySwapRenderBuffer(target, renderBufferSlot);
            }

            // Update children's render buffer state
            // TODO: Improve this! (Checking section.isLoaded() as if its not loaded, it can only be because
            //  it has children. (But this logic is... really hard to read!)
            // FIXME: Above comment is COMPLETELY WRONG! I am an idiot!
            boolean shouldHaveChildren = section.FIXME_BYPASS_DONT_USE_getChildCount() > 0;
            if (shouldHaveChildren) {
                if (children == null) {
                    RenderBufferNode[] childs = new RenderBufferNode[4];
                    for (int i = 0; i < 4; i++) {
                        childs[i] = new RenderBufferNode(pos.getChildByIndex(i));
                    }
                    children = childs;
                }
                for (RenderBufferNode child : children) {
                    child.update();
                }
            } else {
                if (children != null) {
                    //FIXME: Concurrency issue here: If render thread is concurrently using the child's buffer,
                    //  and this thread got priority to close the buffer, it causes a bug where the render thread
                    //  will be using a closed buffer!!!!
                    RenderBufferNode[] childs = children;
                    children = null;
                    for (RenderBufferNode child : childs) {
                        child.close();
                    }
                }
            }
        }

        @Override
        public void close() {
            if (children != null) {
                for (RenderBufferNode child : children) {
                    child.close();
                }
            }
            AbstractRenderBuffer buff;
            buff = renderBufferSlot.getAndSet(null);
            if (buff != null) {
                buff.close();
            }
        }
    }

    public RenderBufferHandler(LodQuadTree target) {
        this.target = target;
        MovableGridRingList<LodRenderSection> referenceList = target.getRingList((byte) (target.getNumbersOfSectionLevels() - 1));
        Pos2D center = referenceList.getCenter();
        renderBufferNodes = new MovableGridRingList<>(referenceList.getHalfSize(), center);
    }

    //TODO: This might get locked by update() causing move() call. Is there a way to avoid this?
    // Maybe dupe the base list and use atomic swap on render? Or is this not worth it?
    public void prepare(LodRenderer renderContext) {
        buildRenderList(renderContext.getLookVector());
    }

    public void renderOpaque(LodRenderer renderContext) {
        //TODO: Directional culling
        loadedNearToFarBuffers.forEach(b -> b.buffer.renderOpaque(renderContext));
    }
    public void renderTransparent(LodRenderer renderContext) {
        if(LodRenderer.transparencyEnabled)
            loadedNearToFarBuffers.forEach(b -> b.buffer.renderTransparent(renderContext));
    }

    public void update() {
        byte topDetail = (byte) (target.getNumbersOfSectionLevels() - 1);
        MovableGridRingList<LodRenderSection> referenceList = target.getRingList(topDetail);
        Pos2D center = referenceList.getCenter();
        //boolean moved = renderBufferNodes.getCenterBlockPos().x != center.x || renderBufferNodes.getCenterBlockPos().y != center.y;
        renderBufferNodes.moveTo(center.x, center.y, RenderBufferNode::close); // Note: may lock the list



        renderBufferNodes.forEachPosOrdered((node, pos) -> {
            DhSectionPos sectPos = new DhSectionPos(topDetail, pos.x, pos.y);
            LodRenderSection section = target.getSection(sectPos);

            if (section == null) {
                // If section is null, but node exists, remove node
                if (node != null) {
                    renderBufferNodes.remove(pos).close();
                }
                // If section is null, continue
                return;
            }

            // If section is not null, but node does not exist, create node
            if (node == null) {
                node = renderBufferNodes.setChained(pos, new RenderBufferNode(sectPos));
            }
            // Node should be not null here
            // Update node
            node.update();
        });
    }

    public void close() {
        renderBufferNodes.clear(RenderBufferNode::close);
    }

}
