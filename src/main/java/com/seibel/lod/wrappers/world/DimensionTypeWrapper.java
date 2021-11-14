package com.seibel.lod.wrappers.world;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.world.DimensionType;

public class DimensionTypeWrapper
{
	private static final ConcurrentMap<DimensionType, DimensionTypeWrapper> dimensionTypeWrapperMap = new ConcurrentHashMap<>();
	private final DimensionType dimensionType;
	
	public DimensionTypeWrapper(DimensionType dimensionType)
	{
		this.dimensionType = dimensionType;
	}
	
	public static DimensionTypeWrapper getDimensionTypeWrapper(DimensionType dimensionType)
	{
		//first we check if the biome has already been wrapped
		if(dimensionTypeWrapperMap.containsKey(dimensionType) && dimensionTypeWrapperMap.get(dimensionType) != null)
			return dimensionTypeWrapperMap.get(dimensionType);
		
		
		//if it hasn't been created yet, we create it and save it in the map
		DimensionTypeWrapper dimensionTypeWrapper = new DimensionTypeWrapper(dimensionType);
		dimensionTypeWrapperMap.put(dimensionType, dimensionTypeWrapper);
		
		//we return the newly created wrapper
		return dimensionTypeWrapper;
	}
	
	public static void clearMap()
	{
		dimensionTypeWrapperMap.clear();
	}
	
	public String getDimensionName()
	{
		return dimensionType.effectsLocation().getPath();
	}
	
	public boolean hasCeiling()
	{
		return dimensionType.hasCeiling();
	}

	public boolean hasSkyLight()
	{
		return dimensionType.hasSkyLight();
	}
}
