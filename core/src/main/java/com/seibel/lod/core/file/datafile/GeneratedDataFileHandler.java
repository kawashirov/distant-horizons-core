package com.seibel.lod.core.file.datafile;

import com.seibel.lod.core.datatype.IIncompleteDataSource;
import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.datatype.full.SparseDataSource;
import com.seibel.lod.core.datatype.full.SpottyDataSource;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class GeneratedDataFileHandler extends DataFileHandler
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
    private AtomicReference<WorldGenerationQueue> worldGenQueueRef = new AtomicReference<>(null);
    // TODO: Should I include a lib that impl weak concurrent hash map?
    private final Map<ILodDataSource, GenTask> worldGenTaskQueue = Collections.synchronizedMap(new WeakHashMap<>());
	
	
	
    public GeneratedDataFileHandler(IDhServerLevel level, File saveRootDir) { super(level, saveRootDir); }
	
	
	
	/**
	 * Assumes there isn't a pre-existing queue.
	 */
    public void setGenerationQueue(WorldGenerationQueue newQueue)
	{
		// this is outside the synchronized block to allow for the assertTrue to catch if the method is incorrectly called twice
        boolean oldQueueExists = this.worldGenQueueRef.compareAndSet(null, newQueue);
        LodUtil.assertTrue(oldQueueExists, "previous queue is still here!");
		
		
        synchronized (this.worldGenTaskQueue)
		{
            for (Map.Entry<ILodDataSource, GenTask> genTaskEntry : this.worldGenTaskQueue.entrySet())
			{
                ILodDataSource source = genTaskEntry.getKey();
				
                DhSectionPos taskPos = source.getSectionPos();
                GenTask task = genTaskEntry.getValue();
				
				this.worldGenQueueRef.get().submitGenTask(taskPos.getSectionBBoxPos(), source.getDataDetail(), task)
                        .whenComplete( (genTaskCompleted, ex) -> this.onWorldGenTaskComplete(genTaskCompleted, ex, task, taskPos) );
            }
        }
    }
	
	
    public WorldGenerationQueue popGenerationQueue()
	{
        WorldGenerationQueue cas = this.worldGenQueueRef.getAndSet(null);
        LodUtil.assertTrue(cas != null, "there is no previous live generation queue!");
        return cas;
    }
	
    @Override
    public CompletableFuture<ILodDataSource> onCreateDataFile(DataMetaFile file)
	{
        DhSectionPos pos = file.pos;
        ArrayList<DataMetaFile> existingFiles = new ArrayList<>();
        ArrayList<DhSectionPos> missing = new ArrayList<>();
		this.selfSearch(pos, pos, existingFiles, missing);
        LodUtil.assertTrue(!missing.isEmpty() || !existingFiles.isEmpty());
        if (missing.size() == 1 && existingFiles.isEmpty() && missing.get(0).equals(pos))
		{
            // None exist.
            IIncompleteDataSource dataSource = pos.sectionDetail <= SparseDataSource.MAX_SECTION_DETAIL ?
                    SparseDataSource.createEmpty(pos) : 
					SpottyDataSource.createEmpty(pos);
			
            WorldGenerationQueue queue = this.worldGenQueueRef.get();
            GenTask task = new GenTask(pos, new WeakReference<>(dataSource));
			this.worldGenTaskQueue.put(dataSource, task);
            if (queue != null)
			{
                queue.submitGenTask(dataSource.getSectionPos().getSectionBBoxPos(),
                        dataSource.getDataDetail(), task)
						.whenComplete( (genTaskCompleted, ex) -> this.onWorldGenTaskComplete(genTaskCompleted, ex, task, pos) );
            }
            return CompletableFuture.completedFuture(dataSource);
        }
		else
		{
            for (DhSectionPos missingPos : missing)
			{
                DataMetaFile newFile = this.atomicGetOrMakeFile(missingPos);
                if (newFile != null)
				{
					existingFiles.add(newFile);
				}
            }
			
            final ArrayList<CompletableFuture<Void>> futures = new ArrayList<>(existingFiles.size());
            final IIncompleteDataSource dataSource = pos.sectionDetail <= SparseDataSource.MAX_SECTION_DETAIL ?
                    SparseDataSource.createEmpty(pos) : SpottyDataSource.createEmpty(pos);
            LOGGER.debug("Creating {} from sampling {} files: {}", pos, existingFiles.size(), existingFiles);

            for (DataMetaFile existingFile : existingFiles)
			{
                futures.add(existingFile.loadOrGetCached()
                        .exceptionally((ex) -> null)
                        .thenAccept((data) ->
						{
                            if (data != null)
							{
                                LOGGER.info("Merging data from {} into {}", data.getSectionPos(), pos);
                                dataSource.sampleFrom(data);
                            }
                        })
                );
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .thenApply((v) -> dataSource.trySelfPromote());
        }
    }
	
	
	
	private void onWorldGenTaskComplete(Boolean genTaskCompleted, Throwable exception, GenTask task, DhSectionPos pos)
	{
		if (exception != null)
		{
			LOGGER.error("Uncaught Gen Task Exception at {}:", pos, exception);
		}
		
		ILodDataSource taskSource = task.targetData.get();
		if (exception == null && genTaskCompleted)
		{
			this.files.get(task.pos).metaData.dataVersion.incrementAndGet();
			
			// remove the completed task
			this.worldGenTaskQueue.remove(taskSource, task);
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
		private final WeakReference<ILodDataSource> targetData;
		private ILodDataSource loadedTargetData = null;
		
		
		
		GenTask(DhSectionPos pos, WeakReference<ILodDataSource> targetData)
		{
			this.pos = pos;
			this.targetData = targetData;
		}
		
		
		
		@Override
		public boolean isValid() { return this.targetData.get() != null; }
		
		@Override
		public Consumer<ChunkSizedData> getConsumer()
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
					GeneratedDataFileHandler.this.write(this.loadedTargetData.getSectionPos(), chunk);
				}
			};
		}
		
		void releaseStrongReference() { this.loadedTargetData = null; }
		
	}
	
	
}
