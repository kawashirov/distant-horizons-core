package com.seibel.lod.core.generation.tasks;

import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.lod.core.pos.DhLodPos;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * @author Leetom
 * @version 2022-11-25
 */
public final class WorldGenTaskGroup
{
	public final DhLodPos pos;
	public byte dataDetail;
	/** Only accessed by the generator polling thread */
	public final LinkedList<WorldGenTask> worldGenTasks = new LinkedList<>();
	
	
	
	public WorldGenTaskGroup(DhLodPos pos, byte dataDetail)
	{
		this.pos = pos;
		this.dataDetail = dataDetail;
	}
	
	
	
	public void onGenerationComplete(ChunkSizedFullDataAccessor chunkSizedFullDataView)
	{
		Iterator<WorldGenTask> tasks = this.worldGenTasks.iterator();
		while (tasks.hasNext())
		{
			WorldGenTask task = tasks.next();
			Consumer<ChunkSizedFullDataAccessor> onGenTaskCompleteConsumer = task.taskTracker.getOnGenTaskCompleteConsumer();
			if (onGenTaskCompleteConsumer == null)
			{
				tasks.remove();
				task.future.complete(WorldGenResult.CreateFail());
			}
			else
			{
				// TODO why aren't we removing the task if it has a consumer?
				onGenTaskCompleteConsumer.accept(chunkSizedFullDataView);
			}
		}
	}
	
}
