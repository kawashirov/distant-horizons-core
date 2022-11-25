package com.seibel.lod.core.generation.tasks;

import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.pos.DhLodPos;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * @author Leetom
 * @version 2022-11-25
 */
public final class TaskGroup
{
	public final DhLodPos pos;
	public byte dataDetail;
	/** Only accessed by the generator polling thread */
	public final LinkedList<WorldGenTask> generatorTasks = new LinkedList<>();
	
	
	
	public TaskGroup(DhLodPos pos, byte dataDetail)
	{
		this.pos = pos;
		this.dataDetail = dataDetail;
	}
	
	
	
	public void accept(ChunkSizedData data)
	{
		Iterator<WorldGenTask> tasks = this.generatorTasks.iterator();
		while (tasks.hasNext())
		{
			WorldGenTask task = tasks.next();
			Consumer<ChunkSizedData> consumer = task.taskTracker.getConsumer();
			if (consumer == null)
			{
				tasks.remove();
				task.future.complete(false);
			}
			else
			{
				consumer.accept(data);
			}
		}
	}
	
}
