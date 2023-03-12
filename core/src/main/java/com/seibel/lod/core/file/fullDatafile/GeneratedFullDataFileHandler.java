package com.seibel.lod.core.file.fullDatafile;

import com.seibel.lod.core.dataObjects.fullData.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.IIncompleteFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.SparseFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.SingleChunkFullDataSource;
import com.seibel.lod.core.generation.tasks.IWorldGenTaskTracker;
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
	
	private final ArrayList<IOnWorldGenCompleteListener> onWorldGenTaskCompleteListeners = new ArrayList<>();
	
	
	
    public GeneratedFullDataFileHandler(IDhServerLevel level, File saveRootDir) { super(level, saveRootDir); }
	
	
	
	//==================//
	// generation queue //
	//==================//
	
	/** Assumes there isn't a pre-existing queue. */
    public void setGenerationQueue(WorldGenerationQueue newWorldGenQueue)
	{
        boolean oldQueueExists = this.worldGenQueueRef.compareAndSet(null, newWorldGenQueue);
        LodUtil.assertTrue(oldQueueExists, "previous world gen queue is still here!");
    }
	
	public void clearGenerationQueue() { this.worldGenQueueRef.set(null); }
	
	
	
	//=================//
	// event listeners //
	//=================//
	
	public void addWorldGenCompleteListener(IOnWorldGenCompleteListener listener)
	{
		this.onWorldGenTaskCompleteListeners.add(listener);
	}
	
	public void removeWorldGenCompleteListener(IOnWorldGenCompleteListener listener)
	{
		this.onWorldGenTaskCompleteListeners.remove(listener);
	}
	
	
	
	//========//
	// events //
	//========//
	
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
		IIncompleteFullDataSource incompleteFullDataSource;
		if (pos.sectionDetailLevel <= SparseFullDataSource.MAX_SECTION_DETAIL)
		{
			incompleteFullDataSource = SparseFullDataSource.createEmpty(pos);
		}
		else
		{
			incompleteFullDataSource = SingleChunkFullDataSource.createEmpty(pos);
		}
		
		
        if (missingPositions.size() == 1 && existingFiles.isEmpty() && missingPositions.get(0).equals(pos))
		{
            // No LOD data exists for this position yet
			
            WorldGenerationQueue worldGenQueue = this.worldGenQueueRef.get();
            if (worldGenQueue != null)
			{
				// queue this section to be generated
				GenTask genTask = new GenTask(pos, new WeakReference<>(incompleteFullDataSource));
				worldGenQueue.submitGenTask(incompleteFullDataSource.getSectionPos().getSectionBBoxPos(), incompleteFullDataSource.getDataDetail(), genTask)
							 .whenComplete((genTaskCompleted, ex) -> this.onWorldGenTaskComplete(genTaskCompleted, ex, genTask, pos));
            }
			
			// return the empty dataSource (it will be populated later)
            return CompletableFuture.completedFuture(incompleteFullDataSource);
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
			final ArrayList<CompletableFuture<Void>> loadDataFutures = new ArrayList<>(existingFiles.size());
            for (FullDataMetaFile existingFile : existingFiles)
			{
                loadDataFutures.add(existingFile.loadOrGetCachedAsync()
                        .exceptionally((ex) -> /*Ignore file read errors*/null)
                        .thenAccept((data) ->
						{
                            if (data != null)
							{
                                //LOGGER.info("Merging data from {} into {}", data.getSectionPos(), pos);
                                incompleteFullDataSource.sampleFrom(data);
                            }
                        })
                );
            }
			
            return CompletableFuture.allOf(loadDataFutures.toArray(new CompletableFuture[0]))
                    .thenApply((voidValue) -> incompleteFullDataSource.trySelfPromote());
        }
    }
	
	private void onWorldGenTaskComplete(Boolean genTaskCompleted, Throwable exception, GenTask genTask, DhSectionPos pos)
	{
		if (exception != null)
		{
			// don't log the shutdown exceptions
			if (!(exception instanceof CancellationException || exception.getCause() instanceof CancellationException))
			{
				LOGGER.error("Uncaught Gen Task Exception at " + pos + ":", exception);
			}
		}
		else if (genTaskCompleted)
		{
			this.files.get(genTask.pos).flushAndSave();
			
			// fire the event listeners 
			for (IOnWorldGenCompleteListener listener : this.onWorldGenTaskCompleteListeners)
			{
				listener.onWorldGenTaskComplete(genTask.pos);
			}
			
//			this.files.get(genTask.pos).metaData.dataVersion.incrementAndGet();
			return;
		}
		
		genTask.releaseStrongReference();
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	private class GenTask implements IWorldGenTaskTracker
	{
		private final DhSectionPos pos;
		
		// weak reference (probably) used to prevent overloading the GC when lots of gen tasks are created?
		private final WeakReference<IFullDataSource> targetFullDataSourceRef;
		// the target data source is where the generated chunk data will be put when completed
		private IFullDataSource loadedTargetFullDataSource = null;
		
		
		
		public GenTask(DhSectionPos pos, WeakReference<IFullDataSource> targetFullDataSourceRef)
		{
			this.pos = pos;
			this.targetFullDataSourceRef = targetFullDataSourceRef;
		}
		
		
		
		@Override
		public boolean isMemoryAddressValid() { return this.targetFullDataSourceRef.get() != null; }
		
		@Override
		public Consumer<ChunkSizedFullDataSource> getOnGenTaskCompleteConsumer()
		{
			if (this.loadedTargetFullDataSource == null)
			{
				this.loadedTargetFullDataSource = this.targetFullDataSourceRef.get();
				if (this.loadedTargetFullDataSource == null)
				{
					return null;
				}
			}
			
			return (chunkSizedFullDataSource) ->
			{
				if (chunkSizedFullDataSource.getBBoxLodPos().overlaps(this.loadedTargetFullDataSource.getSectionPos().getSectionBBoxPos()))
				{
					GeneratedFullDataFileHandler.this.write(this.loadedTargetFullDataSource.getSectionPos(), chunkSizedFullDataSource);
				}
			};
		}
		
		public void releaseStrongReference() { this.loadedTargetFullDataSource = null; }
		
	}
	
	/** 
	 * used by external event listeners <br> 
	 * TODO may or may not be best to have this in a separate file
	 */
	@FunctionalInterface
	public interface IOnWorldGenCompleteListener
	{
		/** Fired whenever a section has completed generating */
		void onWorldGenTaskComplete(DhSectionPos pos);
	}
	
}
