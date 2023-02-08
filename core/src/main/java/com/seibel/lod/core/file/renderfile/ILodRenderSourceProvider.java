package com.seibel.lod.core.file.renderfile;

import com.seibel.lod.core.datatype.ILodRenderSource;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.pos.DhSectionPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * This represents LOD data that is stored in long term storage (IE LOD files stored on the hard drive) <br>
 * Example: {@link RenderFileHandler RenderFileHandler} <br><br>
 * 
 * This is used to create {@link ILodRenderSource}'s 
 */
public interface ILodRenderSourceProvider extends AutoCloseable
{
    CompletableFuture<ILodRenderSource> read(DhSectionPos pos);
    void addScannedFile(Collection<File> detectedFiles);
    void write(DhSectionPos sectionPos, ChunkSizedData chunkData);
    CompletableFuture<Void> flushAndSave();
	
	/** Returns true if the data was refreshed, false otherwise */
    boolean refreshRenderSource(ILodRenderSource source);
	
	/** Deletes any data stored in the render cache so it can be re-created */
	void deleteRenderCache();
	
}
