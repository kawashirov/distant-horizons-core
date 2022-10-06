package com.seibel.lod.core.datatype;

import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.render.RenderBuffer;
import com.seibel.lod.core.file.renderfile.RenderMetaFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

public interface ILodRenderSource
{
	DhSectionPos getSectionPos();
	
	byte getDataDetail();
	
	void enableRender(IDhClientLevel level, LodQuadTree quadTree);
	
	void disableRender();
	
	void dispose(); // notify the container that the parent lodSection is now disposed (can be in loaded or unloaded state)
	
	
	/**
	 * Try and swap in new render buffer for this section. Note that before this call, there should be no other
	 * places storing or referencing the render buffer.
	 * @param referenceSlot The slot for swapping in the new buffer.
	 * @return True if the swap was successful. False if swap is not needed or if it is in progress.
	 */
	boolean trySwapRenderBuffer(LodQuadTree quadTree, AtomicReference<RenderBuffer> referenceSlot);
	
	void saveRender(IDhClientLevel level, RenderMetaFile file, OutputStream dataStream) throws IOException;
	
	byte getRenderVersion();
	
	/** Whether this object is still valid. If not, a new one should be created. */
	boolean isValid();
	
	boolean isEmpty();
	
	void fastWrite(ChunkSizedData chunkData, IDhClientLevel level);
	
	/** Overrides any data that has not been written directly using write(). Skips empty source dataPoints. */ 
	void updateFromRenderSource(ILodRenderSource source);
}
