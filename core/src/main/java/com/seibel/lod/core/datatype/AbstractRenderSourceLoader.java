package com.seibel.lod.core.datatype;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.file.renderfile.RenderMetaDataFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Abstract for loading and creating {@link ILodRenderSource} objects 
 * from {@link RenderMetaDataFile}'s and {@link ILodDataSource}'s. <br><Br>
 * 
 * Also holds all {@link AbstractRenderSourceLoader}'s
 * that have been created to allow for migrating old render data formats.
 */
public abstract class AbstractRenderSourceLoader
{
	public static final HashMultimap<Class<? extends ILodRenderSource>, AbstractRenderSourceLoader> LOADER_BY_SOURCE_TYPE = HashMultimap.create();
	public static final HashMap<Long, Class<? extends ILodRenderSource>> SOURCE_TYPE_BY_METADATA_VERSION = new HashMap<>();
	
	public static AbstractRenderSourceLoader getLoader(long renderTypeId, byte loaderVersion)
	{
		return LOADER_BY_SOURCE_TYPE.get(SOURCE_TYPE_BY_METADATA_VERSION.get(renderTypeId)).stream()
				.filter(loader -> Arrays.binarySearch(loader.loaderSupportedRenderDataVersions, loaderVersion) >= 0)
				.findFirst().orElse(null);
	}
	
	public static AbstractRenderSourceLoader getLoader(Class<? extends ILodRenderSource> clazz, byte loaderVersion)
	{
		return LOADER_BY_SOURCE_TYPE.get(clazz).stream()
				.filter(l -> Arrays.binarySearch(l.loaderSupportedRenderDataVersions, loaderVersion) >= 0)
				.findFirst().orElse(null);
	}
	
	
	
	public final Class<? extends ILodRenderSource> renderSourceClass;
	public final long renderTypeId;
	public final byte[] loaderSupportedRenderDataVersions;
	public final byte detailOffset;
	
	
	
	/**
	 * Will automatically add the new render source to the
	 * {@link AbstractRenderSourceLoader#LOADER_BY_SOURCE_TYPE}
	 * 
	 * @throws IllegalArgumentException if another render source already exists for the given renderTypeId or supported render data versions
	 */
	public AbstractRenderSourceLoader(Class<? extends ILodRenderSource> renderSourceClass, long renderTypeId, byte[] loaderSupportedRenderDataVersions, byte detailOffset) throws IllegalArgumentException
	{
		this.renderTypeId = renderTypeId;
		this.loaderSupportedRenderDataVersions = loaderSupportedRenderDataVersions;
		Arrays.sort(loaderSupportedRenderDataVersions); // sort to allow fast access
		this.renderSourceClass = renderSourceClass;
		this.detailOffset = detailOffset;
		
		
		// register the loader //
		
		// validate there isn't another loader for the given renderTypeId
		if (SOURCE_TYPE_BY_METADATA_VERSION.containsKey(renderTypeId)
			&& SOURCE_TYPE_BY_METADATA_VERSION.get(renderTypeId) != renderSourceClass)
		{
			throw new IllegalArgumentException("Loader for renderTypeId " + renderTypeId + " already registered with different class: "
					+ SOURCE_TYPE_BY_METADATA_VERSION.get(renderTypeId) + " != " + renderSourceClass);
		}
		Set<AbstractRenderSourceLoader> loaders = LOADER_BY_SOURCE_TYPE.get(renderSourceClass);
		
		// validate there isn't another loader that supports the same render data version(s)
		boolean loaderAlreadyExistsForDataVersion = loaders.stream().anyMatch(other ->
		{
			for (byte otherVer : other.loaderSupportedRenderDataVersions)
			{
				if (Arrays.binarySearch(loaderSupportedRenderDataVersions, otherVer) >= 0)
				{
					return true;
				}
			}
			return false;
		});
		if (loaderAlreadyExistsForDataVersion)
		{
			throw new IllegalArgumentException("Loader for class " + renderSourceClass + " that supports one of the render data versions in "
					+ Arrays.toString(loaderSupportedRenderDataVersions) + " already registered!");
		}
		
		// register the loader
		SOURCE_TYPE_BY_METADATA_VERSION.put(renderTypeId, renderSourceClass);
		LOADER_BY_SOURCE_TYPE.put(renderSourceClass, this);
	}
	
	/** 
	 * Can return null if the file is out of date. 
	 *
	 * @throws IOException if the file uses a unsupported data version 
	 * 					   or there was an issue reading the file
	 */
	public abstract ILodRenderSource loadRenderSource(RenderMetaDataFile renderFile, InputStream data, IDhLevel level) throws IOException;
	/** Should not return null */
	public abstract ILodRenderSource createRenderSource(ILodDataSource dataSource, IDhClientLevel level);
	
}
