package com.seibel.distanthorizons.core.dataObjects.fullData;

import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataInputStream;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataOutputStream;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.util.objects.dataStreams.*;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** 
 * WARNING: This is not THREAD-SAFE! 
 * <p>
 * Used to map a numerical IDs to a Biome/BlockState pair.
 * 
 * @author Leetom
 * @version 2022-10-2
 */
public class FullDataPointIdMap
{
	final ArrayList<Entry> entries = new ArrayList<>();
	final ConcurrentHashMap<Entry, Integer> idMap = new ConcurrentHashMap<>(); // FIXME: Improve performance
	
	
	
	public IBiomeWrapper getBiomeWrapper(int id) { return this.entries.get(id).biome; }
	public IBlockStateWrapper getBlockStateWrapper(int id) { return this.entries.get(id).blockState; }
	
	/** 
	 * If an entry with the given values already exists nothing will 
	 * be added but the existing item's ID will still be returned.
	 */
	public int addIfNotPresentAndGetId(IBiomeWrapper biome, IBlockStateWrapper blockState) { return this.addIfNotPresentAndGetId(new Entry(biome, blockState)); }
	private int addIfNotPresentAndGetId(Entry biomeBlockStateEntry)
	{
		return this.idMap.computeIfAbsent(biomeBlockStateEntry, (entry) -> {
			int id = this.entries.size();
			this.entries.add(entry);
			return id;
		});
	}
	
	
	/** 
	 * Adds each entry from the given map to this map. 
	 * @return an array of each added entry's ID in this map in order
	 */
	public int[] mergeAndReturnRemappedEntityIds(FullDataPointIdMap target)
	{
		ArrayList<Entry> entriesToMerge = target.entries;
		
		int[] remappedEntryIds = new int[entriesToMerge.size()];
		for (int i = 0; i < entriesToMerge.size(); i++)
		{
			remappedEntryIds[i] = this.addIfNotPresentAndGetId(entriesToMerge.get(i));
		}
		return remappedEntryIds;
	}
	
	/** Serializes all contained entries into the given stream, formatted in UTF */
	public void serialize(DhDataOutputStream outputStream) throws IOException
	{
		outputStream.writeInt(this.entries.size());
		for (Entry entry : this.entries)
		{
			outputStream.writeUTF(entry.serialize());
		}
	}
	
	/** Creates a new IdBiomeBlockStateMap from the given UTF formatted stream */
	public static FullDataPointIdMap deserialize(DhDataInputStream inputStream) throws IOException, InterruptedException
	{
		int entityCount = inputStream.readInt();
		FullDataPointIdMap newMap = new FullDataPointIdMap();
		for (int i = 0; i < entityCount; i++)
		{
			newMap.entries.add(Entry.deserialize(inputStream.readUTF()));
		}
		return newMap;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == this)
			return true;
//        if (!(other instanceof IdBiomeBlockStateMap)) return false;
//        IdBiomeBlockStateMap otherMap = (IdBiomeBlockStateMap) other;
//        if (entries.size() != otherMap.entries.size()) return false;
//        for (int i=0; i<entries.size(); i++) {
//            if (!entries.get(i).equals(otherMap.entries.get(i))) return false;
//        }
		return false;
	}
	
	
	
	//==============//
	// helper class //
	//==============//
	
	private static final class Entry
	{
		public static final IWrapperFactory WRAPPER_FACTORY = SingletonInjector.INSTANCE.get(IWrapperFactory.class);
		
		public final IBiomeWrapper biome;
		public final IBlockStateWrapper blockState;
		
		
		public Entry(IBiomeWrapper biome, IBlockStateWrapper blockState)
		{
			this.biome = biome;
			this.blockState = blockState;
		}
		
		
		@Override
		public int hashCode() { return Objects.hash(this.biome, this.blockState); }
		
		@Override
		public boolean equals(Object other)
		{
			if (other == this)
				return true;
			
			if (!(other instanceof Entry))
				return false;
			
			return ((Entry) other).biome.equals(this.biome) && ((Entry) other).blockState.equals(this.blockState);
		}
		
		
		public String serialize() { return this.biome.serialize() + " " + this.blockState.serialize(); }
		
		public static Entry deserialize(String str) throws IOException, InterruptedException
		{
			String[] stringArray = str.split(" ");
			if (stringArray.length != 2)
			{
				throw new IOException("Failed to deserialize BiomeBlockStateEntry");
			}
			
			// necessary to prevent issues with deserializing objects after the level has been closed
			if (Thread.interrupted())
			{
				throw new InterruptedException(FullDataPointIdMap.class.getSimpleName()+" task interrupted.");
			}
			
			IBiomeWrapper biome = WRAPPER_FACTORY.deserializeBiomeWrapper(stringArray[0]);
			IBlockStateWrapper blockState = WRAPPER_FACTORY.deserializeBlockStateWrapper(stringArray[1]);
			return new Entry(biome, blockState);
		}
		
	}
	
	
}
