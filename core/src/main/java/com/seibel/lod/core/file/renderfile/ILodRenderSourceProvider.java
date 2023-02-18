package com.seibel.lod.core.file.renderfile;

import com.seibel.lod.core.datatype.render.IRenderSource;
import com.seibel.lod.core.datatype.full.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.pos.DhSectionPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * This represents LOD data that is stored in long term storage (IE LOD files stored on the hard drive) <br>
 * Example: {@link RenderFileHandler RenderFileHandler} <br><br>
 * 
 * This is used to create {@link IRenderSource}'s 
 */
public interface ILodRenderSourceProvider extends AutoCloseable
{
    CompletableFuture<IRenderSource> read(DhSectionPos pos);
    void addScannedFile(Collection<File> detectedFiles);
    void write(DhSectionPos sectionPos, ChunkSizedFullDataSource chunkData);
    CompletableFuture<Void> flushAndSave();
	
	/** Returns true if the data was refreshed, false otherwise */
    boolean refreshRenderSource(IRenderSource source);
	
	/** Deletes any data stored in the render cache so it can be re-created */
	void deleteRenderCache();
	
}
