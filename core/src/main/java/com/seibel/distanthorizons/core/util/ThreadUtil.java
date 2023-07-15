package com.seibel.distanthorizons.core.util;

import com.seibel.distanthorizons.core.util.objects.DhThreadFactory;

import java.util.concurrent.*;

public class ThreadUtil
{
	public static int MINIMUM_RELATIVE_PRIORITY = -5;
	public static int DEFAULT_RELATIVE_PRIORITY = 0;
	
	
	
	
	
	// create thread pool // 
	
	public static ThreadPoolExecutor makeThreadPool(int poolSize, String name, int relativePriority)
	{
		// this is what was being internally used by Executors.newFixedThreadPool
		// I'm just calling it explicitly here so we can reference the more feature-rich
		// ThreadPoolExecutor vs the more generic ExecutorService
		return new ThreadPoolExecutor(/*corePoolSize*/poolSize, /*maxPoolSize*/poolSize,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(),
				new DhThreadFactory("DH-" + name, Thread.NORM_PRIORITY+relativePriority));
	}
	
	public static ThreadPoolExecutor makeThreadPool(int poolSize, Class<?> clazz, int relativePriority)
	{
		return makeThreadPool(poolSize, clazz.getSimpleName(), relativePriority);
	}
	public static ThreadPoolExecutor makeThreadPool(int poolSize, String name)
	{
		return makeThreadPool(poolSize, name, 0);
	}
	public static ThreadPoolExecutor makeThreadPool(int poolSize, Class<?> clazz)
	{
		return makeThreadPool(poolSize, clazz.getSimpleName(), 0);
	}
	
	
	// create single thread pool //
	
	public static ThreadPoolExecutor makeSingleThreadPool(String name, int relativePriority) 
	{ 
		return makeThreadPool(1, name, relativePriority); 
	}
	public static ThreadPoolExecutor makeSingleThreadPool(Class<?> clazz, int relativePriority) 
	{ 
		return makeThreadPool(1, clazz.getSimpleName(), relativePriority); 
	}
	public static ThreadPoolExecutor makeSingleThreadPool(String name) 
	{ 
		return makeThreadPool(1, name, 0); 
	}
	public static ThreadPoolExecutor makeSingleThreadPool(Class<?> clazz) 
	{ 
		return makeThreadPool(1, clazz.getSimpleName(), 0); 
	}
	
	
}
