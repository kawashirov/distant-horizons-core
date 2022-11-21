package com.seibel.lod.core.world;

import com.seibel.lod.api.interfaces.world.IDhApiDimensionTypeWrapper;
import com.seibel.lod.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.interfaces.world.IDhApiWorldProxy;
import com.seibel.lod.core.api.internal.SharedApi;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Used to interact with the currently loaded world.
 * This is separate from the world itself to prevent issues
 * with API implementors referencing said world when it needs
 * to be loaded/unloaded.
 * 
 * @author James Seibel
 * @version 2022-11-20
 */
public class DhApiWorldProxy implements IDhApiWorldProxy
{
	public static DhApiWorldProxy INSTANCE = new DhApiWorldProxy();
	
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final String NO_WORLD_EXCEPTION_STRING = "No world loaded";
	
	
	
	private DhApiWorldProxy() { }
	
	
	
	@Override 
	public boolean worldLoaded() { return SharedApi.currentWorld != null; }
	
	@Override
	public IDhApiLevelWrapper getSinglePlayerLevel() 
	{
		if (SharedApi.currentWorld == null)
		{
			throw new IllegalStateException(NO_WORLD_EXCEPTION_STRING);
		}
		
		
		if (!SharedApi.MC.isDedicatedServer())
		{
			return MC.getWrappedClientWorld();
		}
		else
		{
			return null;
		}
	}
	
	
	@Override
	public Iterable<IDhApiLevelWrapper> getAllLoadedLevelWrappers()
	{
		if (SharedApi.currentWorld == null)
		{
			throw new IllegalStateException(NO_WORLD_EXCEPTION_STRING);
		}
		
		
		ArrayList<IDhApiLevelWrapper> returnList = new ArrayList<>();
		for (IDhLevel dhLevel : SharedApi.currentWorld.getAllLoadedLevels())
		{
			returnList.add(dhLevel.getLevelWrapper());
		}
		return returnList;
	}
	
	@Override
	public Iterable<IDhApiLevelWrapper> getAllLoadedLevelsForDimensionType(IDhApiDimensionTypeWrapper dimensionTypeWrapper)
	{
		if (SharedApi.currentWorld == null)
		{
			throw new IllegalStateException(NO_WORLD_EXCEPTION_STRING);
		}
		
		
		ArrayList<IDhApiLevelWrapper> returnList = new ArrayList<>();
		for (IDhLevel dhLevel : SharedApi.currentWorld.getAllLoadedLevels())
		{
			ILevelWrapper levelWrapper = dhLevel.getLevelWrapper();
			if (levelWrapper.getDimensionType().equals(dimensionTypeWrapper))
			{
				returnList.add(levelWrapper);
			}
		}
		return returnList;
	}
	
	@Override
	public Iterable<IDhApiLevelWrapper> getAllLoadedLevelsWithDimensionNameLike(String dimensionName)
	{
		if (SharedApi.currentWorld == null)
		{
			throw new IllegalStateException(NO_WORLD_EXCEPTION_STRING);
		}
		
		
		String soughtDimName = dimensionName.toLowerCase();
		
		ArrayList<IDhApiLevelWrapper> returnList = new ArrayList<>();
		for (IDhLevel dhLevel : SharedApi.currentWorld.getAllLoadedLevels())
		{
			ILevelWrapper levelWrapper = dhLevel.getLevelWrapper();
			String levelDimName = levelWrapper.getDimensionType().getDimensionName().toLowerCase();
			if (levelDimName.contains(soughtDimName))
			{
				returnList.add(levelWrapper);
			}
		}
		
		return returnList;
	}
	
}
