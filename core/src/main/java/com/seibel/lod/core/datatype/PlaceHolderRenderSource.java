package com.seibel.lod.core.datatype;

import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.level.IClientLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.render.RenderBuffer;
import com.seibel.lod.core.io.renderfile.RenderMetaFile;

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
    public byte getDataDetail() {
        return 0;
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
    public boolean trySwapRenderBuffer(LodQuadTree quadTree, AtomicReference<RenderBuffer> referenceSlotsOpaque, AtomicReference<RenderBuffer> referenceSlotsTransparent) {
        return false;
    }
    @Override
    public void saveRender(IClientLevel level, RenderMetaFile file, OutputStream dataStream) throws IOException {
        throw new UnsupportedOperationException("EmptyRenderSource should NEVER be saved!");
    }

    @Override
    public byte getRenderVersion() {
        return 0;
    }

    public void markInvalid() {
        isValid = false;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public void fastWrite(ChunkSizedData chunkData, IClientLevel level) {}

    @Override
    public void weakWrite(LodRenderSource source) {

    }


}
