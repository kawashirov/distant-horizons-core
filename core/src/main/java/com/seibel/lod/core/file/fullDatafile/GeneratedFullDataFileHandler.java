package com.seibel.lod.core.file.fullDatafile;

import com.seibel.lod.core.dataObjects.fullData.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.IIncompleteFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.SparseFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.SingleChunkFullDataSource;
import com.seibel.lod.core.generation.tasks.AbstractWorldGenTaskTracker;
import com.seibel.lod.core.generation.WorldGenerationQueue;
import com.seibel.lod.core.level.IDhServerLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class GeneratedFullDataFileHandler extends FullDataFileHandler
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
    private final AtomicReference<WorldGenerationQueue> worldGenQueueRef = new AtomicReference<>(null);
	
	
	
    public GeneratedFullDataFileHandler(IDhServerLevel level, File saveRootDir) { super(level, saveRootDir); }
	
	
	
	/** Assumes there isn't a pre-existing queue. */
    public void setGenerationQueue(WorldGenerationQueue newWorldGenQueue)
	{
        boolean oldQueueExists = this.worldGenQueueRef.compareAndSet(null, newWorldGenQueue);
        LodUtil.assertTrue(oldQueueExists, "previous world gen queue is still here!");
    }
	
	public void clearGenerationQueue() { this.worldGenQueueRef.set(null); }
	
	
	
    @Override
    public CompletableFuture<IFullDataSource> onCreateDataFile(FullDataMetaFile file)
	{
        DhSectionPos pos = file.pos;
        
		ArrayList<FullDataMetaFile> existingFiles = new ArrayList<>();
        ArrayList<DhSectionPos> missingPositions = new ArrayList<>();
		this.getDataFilesForPosition(pos, pos, existingFiles, missingPositions);
		
		// confirm the quad tree has at least one node in it
        LodUtil.assertTrue(!missingPositions.isEmpty() || !existingFiles.isEmpty());
		
		// determine the type of dataSource that should be used for this position
		IIncompleteFullDataSource dataSource = pos.sectionDetailLevel <= SparseFullDataSource.MAX_SECTION_DETAIL ?
				SparseFullDataSource.createEmpty(pos) :
				SingleChunkFullDataSource.createEmpty(pos);
		
		
        if (missingPositions.size() == 1 && existingFiles.isEmpty() && missingPositions.get(0).equals(pos))
		{
            // No LOD data exists for this position yet
			
            WorldGenerationQueue worldGenQueue = this.worldGenQueueRef.get();
            if (worldGenQueue != null)
			{
				// queue this section to be generated
				GenTask task = new GenTask(pos, new WeakReference<>(dataSource));
				worldGenQueue.submitGenTask(dataSource.getSectionPos().getSectionBBoxPos(), dataSource.getDataDetail(), task)
							 .whenComplete((genTaskCompleted, ex) -> this.onWorldGenTaskComplete(genTaskCompleted, ex, task, pos));
            }
			
			// return the empty dataSource (it will be populated later)
            return CompletableFuture.completedFuture(dataSource);
        }
		else
		{
			// LOD data exists for this position
			
			// create the missing metaData files
            for (DhSectionPos missingPos : missingPositions)
			{
                FullDataMetaFile newFile = this.getOrMakeFile(missingPos);
                if (newFile != null)
				{
					existingFiles.add(newFile);
				}
            }
			
            LOGGER.debug("Creating {} from sampling {} files: {}", pos, existingFiles.size(), existingFiles);
			
			// read in the existing data
			final ArrayList<CompletableFuture<Void>> futures = new ArrayList<>(existingFiles.size());
            for (FullDataMetaFile existingFile : existingFiles)
			{
                futures.add(existingFile.loadOrGetCachedAsync()
                        .exceptionally((ex) -> /*Ignore file read errors*/null)
                        .thenAccept((data) ->
						{
                            if (data != null)
							{
                                //LOGGER.info("Merging data from {} into {}", data.getSectionPos(), pos);
                                dataSource.sampleFrom(data);
                            }
                        })
                );
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply((voidValue) -> dataSource.trySelfPromote());
        }
    }
	
	
	
	private void onWorldGenTaskComplete(Boolean genTaskCompleted, Throwable exception, GenTask task, DhSectionPos pos)
	{
		if (exception != null)
		{
			// don't log the shutdown exceptions
			if (!(exception instanceof CancellationException || exception.getCause() instanceof CancellationException))
			{
				LOGGER.error("Uncaught Gen Task Exception at " + pos + ":", exception);
			}
		}
		
		if (exception == null && genTaskCompleted)
		{
//			this.files.get(task.pos).metaData.dataVersion.incrementAndGet();
			return;
		}
		task.releaseStrongReference();
	}
	
	
	
	//==============//
	// helper class //
	//==============//
	
	class GenTask extends AbstractWorldGenTaskTracker
	{
		private final DhSectionPos pos;
		private final WeakReference<IFullDataSource> targetData;
		private IFullDataSource loadedTargetData = null;
		
		
		
		GenTask(DhSectionPos pos, WeakReference<IFullDataSource> targetData)
		{
			this.pos = pos;
			this.targetData = targetData;
		}
		
		
		
		@Override
		public boolean isMemoryAddressValid() { return this.targetData.get() != null; }
		
		@Override
		public Consumer<ChunkSizedFullDataSource> getConsumer()
		{
			if (this.loadedTargetData == null)
			{
				this.loadedTargetData = this.targetData.get();
				if (this.loadedTargetData == null)
				{
					return null;
				}
			}
			
			return (chunk) ->
			{
				if (chunk.getBBoxLodPos().overlaps(this.loadedTargetData.getSectionPos().getSectionBBoxPos()))
				{
					GeneratedFullDataFileHandler.this.write(this.loadedTargetData.getSectionPos(), chunk);
				}
			};
		}
		
		void releaseStrongReference() { this.loadedTargetData = null; }
		
	}
	
	
}
