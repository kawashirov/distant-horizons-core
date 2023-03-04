package com.seibel.lod.core.util;

import com.seibel.lod.core.util.objects.LodThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadUtil
{
	public static int MINIMUM_RELATIVE_PRIORITY = -5;
	public static int DEFAULT_RELATIVE_PRIORITY = 0;
	
	
	
	
	
	// create thread pool // 
	
	public static ExecutorService makeThreadPool(int poolSize, String name, int relativePriority)
	{
		return Executors.newFixedThreadPool(poolSize, new LodThreadFactory(name, Thread.NORM_PRIORITY+relativePriority));
	}
	
	public static ExecutorService makeThreadPool(int poolSize, Class<?> clazz, int relativePriority)
	{
		return makeThreadPool(poolSize, clazz.getSimpleName(), relativePriority);
	}
	public static ExecutorService makeThreadPool(int poolSize, String name)
	{
		return makeThreadPool(poolSize, name, 0);
	}
	public static ExecutorService makeThreadPool(int poolSize, Class<?> clazz)
	{
		return makeThreadPool(poolSize, clazz.getSimpleName(), 0);
	}
	
	
	// create single thread pool //
	
	public static ExecutorService makeSingleThreadPool(String name, int relativePriority) 
	{ 
		return makeThreadPool(1, name, Thread.NORM_PRIORITY+relativePriority); 
	}
	public static ExecutorService makeSingleThreadPool(Class<?> clazz, int relativePriority) 
	{ 
		return makeThreadPool(1, clazz.getSimpleName(), relativePriority); 
	}
	public static ExecutorService makeSingleThreadPool(String name) 
	{ 
		return makeThreadPool(1, name, 0); 
	}
	public static ExecutorService makeSingleThreadPool(Class<?> clazz) 
	{ 
		return makeThreadPool(1, clazz.getSimpleName(), 0); 
	}
	
	
}
