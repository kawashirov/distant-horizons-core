package com.seibel.lod.core.a7.datatype;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.render.LodQuadTree;
import com.seibel.lod.core.a7.render.RenderBuffer;
import com.seibel.lod.core.a7.save.io.file.DataMetaFile;
import com.seibel.lod.core.a7.save.io.render.RenderMetaFile;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.objects.DHRegionPos;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

public interface LodRenderSource {
    DhSectionPos getSectionPos();

    void enableRender(IClientLevel level, LodQuadTree quadTree);
    void disableRender();
    boolean isRenderReady();
    void dispose(); // notify the container that the parent lodSection is now disposed (can be in loaded or unloaded state)
    byte getDetailOffset();

    /**
     * Try and swap in new render buffer for this section. Note that before this call, there should be no other
     *  places storing or referencing the render buffer.
     * @param referenceSlot The slot for swapping in the new buffer.
     * @return True if the swap was successful. False if swap is not needed or if it is in progress.
     */
    boolean trySwapRenderBuffer(LodQuadTree quadTree, AtomicReference<RenderBuffer> referenceSlot);

    void saveRender(IClientLevel level, RenderMetaFile file, OutputStream dataStream) throws IOException;

    void update(ChunkSizedData chunkData);

    byte getRenderVersion();

    void markInvalid();

    /**
     * Whether this object is still valid. If not, a new one should be created.
     */
    boolean isValid();
}
