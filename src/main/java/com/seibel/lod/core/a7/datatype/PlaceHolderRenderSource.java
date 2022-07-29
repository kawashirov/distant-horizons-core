package com.seibel.lod.core.a7.datatype;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.render.LodQuadTree;
import com.seibel.lod.core.a7.render.RenderBuffer;
import com.seibel.lod.core.a7.save.io.render.RenderMetaFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

public class PlaceHolderRenderSource implements LodRenderSource {
    final DhSectionPos pos;
    boolean isValid = true;
    public PlaceHolderRenderSource(DhSectionPos pos) {
        this.pos = pos;
    }

    @Override
    public DhSectionPos getSectionPos() {
        return pos;
    }

    @Override
    public void enableRender(IClientLevel level, LodQuadTree quadTree) {
    }

    @Override
    public void disableRender() {}

    @Override
    public boolean isRenderReady() {
        return false;
    }
    @Override
    public void dispose() {}
    @Override
    public byte getDetailOffset() {
        return 0;
    }
    @Override
    public boolean trySwapRenderBuffer(LodQuadTree quadTree, AtomicReference<RenderBuffer> referenceSlot) {
        return false;
    }
    @Override
    public void saveRender(IClientLevel level, RenderMetaFile file, OutputStream dataStream) throws IOException {
        throw new UnsupportedOperationException("EmptyRenderSource should NEVER be saved!");
    }
    @Override
    public void update(ChunkSizedData chunkData) {}

    @Override
    public byte getRenderVersion() {
        return 0;
    }

    @Override
    public void markInvalid() {
        isValid = false;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }


}
