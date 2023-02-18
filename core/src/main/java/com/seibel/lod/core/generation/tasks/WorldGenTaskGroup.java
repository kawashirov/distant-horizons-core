package com.seibel.lod.core.generation.tasks;

import com.seibel.lod.core.datatype.full.ChunkSizedFullDataSource;
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
	public final LinkedList<WorldGenTask> generatorTasks = new LinkedList<>();
	
	
	
	public WorldGenTaskGroup(DhLodPos pos, byte dataDetail)
	{
		this.pos = pos;
		this.dataDetail = dataDetail;
	}
	
	
	
	public void accept(ChunkSizedFullDataSource data)
	{
		Iterator<WorldGenTask> tasks = this.generatorTasks.iterator();
		while (tasks.hasNext())
		{
			WorldGenTask task = tasks.next();
			Consumer<ChunkSizedFullDataSource> consumer = task.taskTracker.getConsumer();
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
