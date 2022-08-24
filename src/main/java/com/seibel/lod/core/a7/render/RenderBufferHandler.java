package com.seibel.lod.core.a7.render;

import com.seibel.lod.core.a7.datatype.LodRenderSource;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.objects.Pos2D;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;

import java.util.concurrent.atomic.AtomicReference;

public class RenderBufferHandler {
    public final LodQuadTree target;
    private final MovableGridRingList<RenderBufferNode> renderBufferNodes;

    class RenderBufferNode implements AutoCloseable {
        public final DhSectionPos pos;
        public volatile RenderBufferNode[] children = null;
        public final AtomicReference<RenderBuffer> renderBufferSlotOpaque = new AtomicReference<>();
        public final AtomicReference<RenderBuffer> renderBufferSlotTransparent = new AtomicReference<>();

        public RenderBufferNode(DhSectionPos pos) {
            this.pos = pos;
        }

        public void render(a7LodRenderer renderContext) {
            RenderBuffer buff;

            buff = renderBufferSlotOpaque.get();
            if (buff != null) {
                buff.render(renderContext);
            } else {
                RenderBufferNode[] childs = children;
                if (childs != null) {
                    for (RenderBufferNode child : childs) {
                        child.render(renderContext);
                    }
                }
            }

            buff = renderBufferSlotTransparent.get();
            if (buff != null) {
                buff.render(renderContext);
            } else {
                RenderBufferNode[] childs = children;
                if (childs != null) {
                    for (RenderBufferNode child : childs) {
                        child.render(renderContext);
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
            LodRenderSource container = section.getRenderContainer();

            // Update self's render buffer state
            boolean shouldRender = section.canRender();
            if (!shouldRender) {
                //TODO: Does this really need to force the old buffer to not be rendered?
//                RenderBuffer buff = renderBufferSlot.getAndSet(null);
//                if (buff != null) {
//                    buff.close();
//                }
            } else {
                LodUtil.assertTrue(container != null); // section.isLoaded() should have ensured this
                container.trySwapRenderBuffer(target, renderBufferSlotOpaque, renderBufferSlotTransparent);
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
                        childs[i] = new RenderBufferNode(pos.getChild(i));
                    }
                    children = childs;
                }
                for (RenderBufferNode child : children) {
                    child.update();
                }
            } else {
                if (children != null) {
                    //FIXME: Concurrency issue here: If render thread is concurrently using the child's buffer,
                    //  and this thread got priority to close the buffer, it causes a bug wher the render thread
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
            RenderBuffer buff;
            buff = renderBufferSlotOpaque.getAndSet(null);
            if (buff != null) {
                buff.close();
            }
            buff = renderBufferSlotTransparent.getAndSet(null);
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

    public void render(a7LodRenderer renderContext) {
        //TODO: This might get locked by update() causing move() call. Is there a way to avoid this?
        // Maybe dupe the base list and use atomic swap on render? Or is this not worth it?
        //TODO: Directional culling
        //TODO: Ordered by distance
        renderBufferNodes.forEachOrdered(n -> n.render(renderContext));
    }

    public void update() {
        byte topDetail = (byte) (target.getNumbersOfSectionLevels() - 1);
        MovableGridRingList<LodRenderSection> referenceList = target.getRingList(topDetail);
        Pos2D center = referenceList.getCenter();
        renderBufferNodes.move(center.x, center.y, RenderBufferNode::close); // Note: may lock the list
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
