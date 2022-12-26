package com.seibel.lod.core.datatype;

import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.render.RenderBuffer;
import com.seibel.lod.core.file.renderfile.RenderMetaDataFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

public class PlaceHolderRenderSource implements ILodRenderSource
{
	final DhSectionPos pos;
	boolean isValid = true;
	
	public PlaceHolderRenderSource(DhSectionPos pos) { this.pos = pos; }
	
	@Override
	public DhSectionPos getSectionPos() { return pos; }
	
	@Override
	public byte getDataDetail() { return 0; }
	
	@Override
	public void enableRender(IDhClientLevel level, LodQuadTree quadTree) { /* TODO */ }
	
	@Override
	public void disableRender() { /* TODO */ }
	
	@Override
	public void dispose() { /* TODO */ }
	
	@Override
	public boolean trySwapRenderBuffer(LodQuadTree quadTree, AtomicReference<RenderBuffer> referenceSlots) { return false; }
	
	@Override
	public void saveRender(IDhClientLevel level, RenderMetaDataFile file, OutputStream dataStream) throws IOException
	{
		throw new UnsupportedOperationException("EmptyRenderSource should NEVER be saved!");
	}
	
	@Override
	public byte getRenderVersion() { return 0; }
	
	public void markInvalid() { isValid = false; }
	
	@Override
	public boolean isValid() { return isValid; }
	
	@Override
	public boolean isEmpty() { return true; }
	
	@Override
	public void fastWrite(ChunkSizedData chunkData, IDhClientLevel level) { /* TODO */ }
	
	@Override
	public void updateFromRenderSource(ILodRenderSource source) { /* TODO */ }
	
}
