package com.seibel.lod.core.datatype;

import com.seibel.lod.core.datatype.full.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.render.AbstractRenderBuffer;
import com.seibel.lod.core.file.renderfile.RenderMetaDataFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This represents LOD data that is stored in system memory <br>
 * Example: {@link com.seibel.lod.core.datatype.column.ColumnRenderSource ColumnRenderSource} <br><br>
 * 
 * These are created via {@link com.seibel.lod.core.file.renderfile.ILodRenderSourceProvider ILodRenderSourceProvider}'s
 */
public interface IRenderSource
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
	boolean trySwapRenderBufferAsync(LodQuadTree quadTree, AtomicReference<AbstractRenderBuffer> referenceSlot);
	
	void saveRender(IDhClientLevel level, RenderMetaDataFile file, OutputStream dataStream) throws IOException;
	
	byte getRenderVersion();
	
	/** Whether this object is still valid. If not, a new one should be created. */
	boolean isValid();
	
	boolean isEmpty();
	
	void fastWrite(ChunkSizedFullDataSource chunkData, IDhClientLevel level);
	
	/** Overrides any data that has not been written directly using write(). Skips empty source dataPoints. */ 
	void updateFromRenderSource(IRenderSource source);
	
}
