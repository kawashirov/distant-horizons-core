package com.seibel.lod.core.file.fullDatafile;

import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.lod.core.dataObjects.fullData.sources.HighDetailIncompleteFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.interfaces.IIncompleteFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.LowDetailIncompleteFullDataSource;
import com.seibel.lod.core.generation.tasks.IWorldGenTaskTracker;
import com.seibel.lod.core.generation.WorldGenerationQueue;
import com.seibel.lod.core.generation.tasks.WorldGenResult;
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
	
	/** 
	 * Keeps track of which partially generated {@link IFullDataSource} {@link DhSectionPos}' are waiting to be generated.
	 * This is done to prevent sending duplicate generation requests for the same position.
	 */
	private final HashSet<DhSectionPos> incompleteSourceGenRequests = new HashSet<>();
	
	
	
    public GeneratedFullDataFileHandler(IDhServerLevel level, File saveRootDir) { super(level, saveRootDir); }
	
	
	
	//======//
	// data //
	//======//
	
	public CompletableFuture<IFullDataSource> read(DhSectionPos pos)
	{
		return super.read(pos).whenComplete((fullDataSource, ex) -> 
		{
			this.checkIfSectionNeedsAdditionalGeneration(pos, fullDataSource);
		});
	}
	
	
	
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
	
	public void addWorldGenCompleteListener(IOnWorldGenCompleteListener listener) { this.onWorldGenTaskCompleteListeners.add(listener); }
	public void removeWorldGenCompleteListener(IOnWorldGenCompleteListener listener) { this.onWorldGenTaskCompleteListeners.remove(listener); }
	
	
	
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
		if (pos.sectionDetailLevel <= HighDetailIncompleteFullDataSource.MAX_SECTION_DETAIL)
		{
			incompleteFullDataSource = HighDetailIncompleteFullDataSource.createEmpty(pos);
		}
		else
		{
			incompleteFullDataSource = LowDetailIncompleteFullDataSource.createEmpty(pos);
		}
		
		
        if (missingPositions.size() == 1 && existingFiles.isEmpty() && missingPositions.get(0).equals(pos))
		{
            // No LOD data exists for this position yet
			
            WorldGenerationQueue worldGenQueue = this.worldGenQueueRef.get();
            if (worldGenQueue != null)
			{
				// queue this section to be generated
				GenTask genTask = new GenTask(pos, new WeakReference<>(incompleteFullDataSource));
				worldGenQueue.submitGenTask(incompleteFullDataSource.getSectionPos().getSectionBBoxPos(), incompleteFullDataSource.getDataDetailLevel(), genTask)
							 .whenComplete((genTaskResult, ex) -> this.onWorldGenTaskComplete(genTaskResult, ex, genTask, pos));
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
			
//            LOGGER.debug("Creating "+pos+" from sampling "+existingFiles.size()+" files: "+existingFiles);
			
			// read in the existing data
			final ArrayList<CompletableFuture<Void>> loadDataFutures = new ArrayList<>(existingFiles.size());
            for (FullDataMetaFile existingFile : existingFiles)
			{
                loadDataFutures.add(existingFile.loadOrGetCachedAsync()
					.exceptionally((ex) -> /*Ignore file read errors*/null)
					.thenAccept((fullDataSource) ->
					{
						if (fullDataSource == null)
						{
							return;
						}
						
						this.checkIfSectionNeedsAdditionalGeneration(pos, fullDataSource);
						
						//LOGGER.info("Merging data from {} into {}", data.getSectionPos(), pos);
						incompleteFullDataSource.sampleFrom(fullDataSource);
					})
                );
            }
			
            return CompletableFuture.allOf(loadDataFutures.toArray(new CompletableFuture[0]))
                    .thenApply((voidValue) -> incompleteFullDataSource.tryPromotingToCompleteDataSource());
        }
    }
	
	/** 
	 * Checks if the given {@link IFullDataSource} is fully generated and
	 * if it isn't, new world gen request(s) will be created.
	 */
	private void checkIfSectionNeedsAdditionalGeneration(DhSectionPos pos, IFullDataSource fullDataSource)
	{
		boolean generateSection = fullDataSource == null || (!fullDataSource.isCompletelyGenerated() && !incompleteSourceGenRequests.contains(pos));
		if (!generateSection)
		{
			// this section doesn't need to be generated
			return;
		}
		
		WorldGenerationQueue worldGenQueue = this.worldGenQueueRef.get();
		if (worldGenQueue == null)
		{
			// world generation is disabled
			return;
		}
		
		
		// the data source could be null if no file exists for this position
		if (fullDataSource == null)
		{
			if (pos.sectionDetailLevel <= HighDetailIncompleteFullDataSource.MAX_SECTION_DETAIL)
			{
				fullDataSource = HighDetailIncompleteFullDataSource.createEmpty(pos);
			}
			else
			{
				fullDataSource = LowDetailIncompleteFullDataSource.createEmpty(pos);
			}
		}
		
		
		
		incompleteSourceGenRequests.add(pos);
		//LOGGER.info("["+ungeneratedPosList.size()+"] missing sub positions for pos: ["+pos+"]. Number of gen requests queued: ["+queuedGenRequests.size()+"].");

		// note: this will potentially re-generate terrain, however due to the generator setup this is currently unavoidable and probably not worth worrying about
		GenTask genTask = new GenTask(pos, new WeakReference<>(fullDataSource));
		worldGenQueue.submitGenTask(fullDataSource.getSectionPos().getSectionBBoxPos(), fullDataSource.getDataDetailLevel(), genTask)
				.whenComplete((genTaskResult, ex) ->
				{
					incompleteSourceGenRequests.remove(pos);
					//LOGGER.info("Partial generation completed for pos: ["+pos+"]. Remaining gen requests queued: ["+queuedGenRequests.size()+"].");

					this.onWorldGenTaskComplete(genTaskResult, ex, genTask, pos);
				});
	}
	
	private void onWorldGenTaskComplete(WorldGenResult genTaskResult, Throwable exception, GenTask genTask, DhSectionPos pos)
	{
		if (exception != null)
		{
			// don't log shutdown exceptions
			if (!(exception instanceof CancellationException || exception.getCause() instanceof CancellationException))
			{
				LOGGER.error("Uncaught Gen Task Exception at " + pos + ":", exception);
			}
		}
		else if (genTaskResult.success)
		{
			// generation completed, update the files and listener(s)
			
//			LOGGER.info("gen task completed for pos: ["+pos+"].");
			this.files.get(genTask.pos).flushAndSaveAsync();
			
			// fire the event listeners 
			for (IOnWorldGenCompleteListener listener : this.onWorldGenTaskCompleteListeners)
			{
				listener.onWorldGenTaskComplete(genTask.pos);
			}
			
//			this.files.get(genTask.pos).metaData.dataVersion.incrementAndGet();
			return;
		}
		else
		{
			// generation didn't complete
			
			// if the generation task was split up into smaller positions, wait for them to complete
			for (CompletableFuture<WorldGenResult> siblingFuture : genTaskResult.childFutures)
			{
				siblingFuture.whenComplete((siblingGenTaskResult, siblingEx) -> this.onWorldGenTaskComplete(siblingGenTaskResult, siblingEx, genTask, pos));
			}
		}
		
		genTask.releaseStrongReference();
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	private class GenTask implements IWorldGenTaskTracker
	{
		private final DhSectionPos pos;
		
		// weak reference (probably) used to prevent overloading the GC when lots of gen tasks are created? // TODO do we still need a weak reference here?
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
		public Consumer<ChunkSizedFullDataAccessor> getOnGenTaskCompleteConsumer()
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
				if (chunkSizedFullDataSource.getLodPos().overlapsExactly(this.loadedTargetFullDataSource.getSectionPos().getSectionBBoxPos()))
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
