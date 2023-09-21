/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.core.dataObjects.fullData.loader;

import com.google.common.collect.HashMultimap;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.distanthorizons.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataInputStream;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractFullDataSourceLoader
{
	public static final HashMultimap<Class<? extends IFullDataSource>, AbstractFullDataSourceLoader> LOADER_REGISTRY = HashMultimap.create();
	public static final HashMap<Long, Class<? extends IFullDataSource>> DATATYPE_ID_REGISTRY = new HashMap<>();
	
	private static final int AVAILABLE_PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();
	
	
	public final Class<? extends IFullDataSource> fullDataSourceClass;
	
	public final long datatypeId;
	public final byte[] loaderSupportedVersions;
	
	/** used when pooling data sources */
	private final ArrayList<IFullDataSource> cachedSources = new ArrayList<>();
	private final ReadWriteLock cacheReadWriteLock = new ReentrantReadWriteLock();
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public AbstractFullDataSourceLoader(Class<? extends IFullDataSource> fullDataSourceClass, long datatypeId, byte[] loaderSupportedVersions)
	{
		this.datatypeId = datatypeId;
		this.loaderSupportedVersions = loaderSupportedVersions;
		Arrays.sort(loaderSupportedVersions); // sort to allow fast access
		this.fullDataSourceClass = fullDataSourceClass;
		if (DATATYPE_ID_REGISTRY.containsKey(datatypeId) && DATATYPE_ID_REGISTRY.get(datatypeId) != fullDataSourceClass)
		{
			throw new IllegalArgumentException("Loader for datatypeId " + datatypeId + " already registered with different class: "
					+ DATATYPE_ID_REGISTRY.get(datatypeId) + " != " + fullDataSourceClass);
		}
		Set<AbstractFullDataSourceLoader> loaders = LOADER_REGISTRY.get(fullDataSourceClass);
		if (loaders.stream().anyMatch(other ->
		{
			// see if any loaderSupportsVersion conflicts with this one
			for (byte otherVer : other.loaderSupportedVersions)
			{
				if (Arrays.binarySearch(loaderSupportedVersions, otherVer) >= 0)
				{
					return true;
				}
			}
			return false;
		}))
		{
			throw new IllegalArgumentException("Loader for class " + fullDataSourceClass + " that supports one of the version in "
					+ Arrays.toString(loaderSupportedVersions) + " already registered!");
		}
		DATATYPE_ID_REGISTRY.put(datatypeId, fullDataSourceClass);
		LOADER_REGISTRY.put(fullDataSourceClass, this);
	}
	
	
	
	//================//
	// loader getters // 
	//================//
	
	public static AbstractFullDataSourceLoader getLoader(long dataTypeId, byte dataVersion)
	{
		return LOADER_REGISTRY.get(DATATYPE_ID_REGISTRY.get(dataTypeId)).stream()
				.filter(loader -> Arrays.binarySearch(loader.loaderSupportedVersions, dataVersion) >= 0)
				.findFirst().orElse(null);
	}
	
	public static AbstractFullDataSourceLoader getLoader(Class<? extends IFullDataSource> clazz, byte dataVersion)
	{
		return LOADER_REGISTRY.get(clazz).stream()
				.filter(loader -> Arrays.binarySearch(loader.loaderSupportedVersions, dataVersion) >= 0)
				.findFirst().orElse(null);
	}
	
	
	
	//==================//
	// abstract methods //
	//==================//
	
	protected abstract IFullDataSource createEmptyDataSource(DhSectionPos pos);
	
	
	
	//==============//
	// data loading //
	//==============//
	
	/**
	 * Can return null if any of the requirements aren't met.
	 *
	 * @throws InterruptedException if the loader thread is interrupted, generally happens when the level is shutting down
	 */
	public IFullDataSource loadDataSource(FullDataMetaFile dataFile, DhDataInputStream inputStream, IDhLevel level) throws IOException, InterruptedException
	{
		IFullDataSource dataSource = this.createEmptyDataSource(dataFile.pos);
		dataSource.populateFromStream(dataFile, inputStream, level);
		return dataSource;
	}
	
	/** Should be used in conjunction with {@link AbstractFullDataSourceLoader#returnPooledDataSource} to return the pooled sources. */
	public IFullDataSource loadTemporaryDataSource(FullDataMetaFile dataFile, DhDataInputStream inputStream, IDhLevel level) throws IOException, InterruptedException
	{
		IFullDataSource dataSource = this.tryGetPooledSource();
		if (dataSource != null)
		{
			dataSource.repopulateFromStream(dataFile, inputStream, level);
		}
		else
		{
			dataSource = this.loadDataSource(dataFile, inputStream, level);
		}
		
		return dataSource;
	}
	
	
	
	//=====================//
	// data source pooling //
	//=====================//

	/** @return null if no pooled source exists */ 
	public IFullDataSource tryGetPooledSource()
	{
		try
		{
			this.cacheReadWriteLock.readLock().lock();

			int index = this.cachedSources.size() - 1;
			if (index == -1)
			{
				return null;
			}
			else
			{
				return this.cachedSources.remove(index);
			}
		}
		finally
		{
			this.cacheReadWriteLock.readLock().unlock();
		}
	}
	
	/** 
	 * Doesn't have to be called, if a data source isn't returned, nothing will be leaked. 
	 * It just means a new source must be constructed next time {@link AbstractFullDataSourceLoader#tryGetPooledSource} is called.
	 */
	public void returnPooledDataSource(IFullDataSource dataSource)
	{
		if (dataSource == null)
		{
			return;
		}
		else if (dataSource.getClass() != this.fullDataSourceClass)
		{
			return;
		}
		else if (this.cachedSources.size() > 25)
		{
			return;
		}
		
		try
		{
			this.cacheReadWriteLock.writeLock().lock();
			this.cachedSources.add(dataSource);
		}
		finally
		{
			this.cacheReadWriteLock.writeLock().unlock();
		}
	}
	
}
