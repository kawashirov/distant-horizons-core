package com.seibel.lod.core.file.renderfile;

import com.seibel.lod.core.datatype.ILodRenderSource;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.pos.DhSectionPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface IRenderSourceProvider extends AutoCloseable
{
    CompletableFuture<ILodRenderSource> read(DhSectionPos pos);
    void addScannedFile(Collection<File> detectedFiles);
    void write(DhSectionPos sectionPos, ChunkSizedData chunkData);
    CompletableFuture<Void> flushAndSave();
	
	/** Returns true if the data was refreshed, false otherwise */
    boolean refreshRenderSource(ILodRenderSource source);
	
}
