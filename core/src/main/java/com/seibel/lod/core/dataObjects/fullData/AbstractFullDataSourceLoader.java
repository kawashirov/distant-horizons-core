package com.seibel.lod.core.dataObjects.fullData;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public abstract class AbstractFullDataSourceLoader
{
	
	public static final HashMultimap<Class<? extends IFullDataSource>, AbstractFullDataSourceLoader> loaderRegistry = HashMultimap.create();
	public final Class<? extends IFullDataSource> clazz;
	public static final HashMap<Long, Class<? extends IFullDataSource>> datatypeIdRegistry = new HashMap<>();
	
	public final long datatypeId;
	public final byte[] loaderSupportedVersions;
	
	
	
	public AbstractFullDataSourceLoader(Class<? extends IFullDataSource> clazz, long datatypeId, byte[] loaderSupportedVersions)
	{
		this.datatypeId = datatypeId;
		this.loaderSupportedVersions = loaderSupportedVersions;
		Arrays.sort(loaderSupportedVersions); // sort to allow fast access
		this.clazz = clazz;
		if (datatypeIdRegistry.containsKey(datatypeId) && datatypeIdRegistry.get(datatypeId) != clazz)
		{
			throw new IllegalArgumentException("Loader for datatypeId " + datatypeId + " already registered with different class: "
					+ datatypeIdRegistry.get(datatypeId) + " != " + clazz);
		}
		Set<AbstractFullDataSourceLoader> loaders = loaderRegistry.get(clazz);
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
			throw new IllegalArgumentException("Loader for class " + clazz + " that supports one of the version in "
					+ Arrays.toString(loaderSupportedVersions) + " already registered!");
		}
		datatypeIdRegistry.put(datatypeId, clazz);
		loaderRegistry.put(clazz, this);
	}
	
	/** Can return null as meaning the requirement is not met */
	public abstract IFullDataSource loadData(FullDataMetaFile dataFile, InputStream data, IDhLevel level) throws IOException;
	
	
	
	public static AbstractFullDataSourceLoader getLoader(long dataTypeId, byte dataVersion)
	{
		return loaderRegistry.get(datatypeIdRegistry.get(dataTypeId)).stream()
				.filter(l -> Arrays.binarySearch(l.loaderSupportedVersions, dataVersion) >= 0)
				.findFirst().orElse(null);
	}
	
	public static AbstractFullDataSourceLoader getLoader(Class<? extends IFullDataSource> clazz, byte dataVersion)
	{
		return loaderRegistry.get(clazz).stream()
				.filter(loader -> Arrays.binarySearch(loader.loaderSupportedVersions, dataVersion) >= 0)
				.findFirst().orElse(null);
	}
	
}