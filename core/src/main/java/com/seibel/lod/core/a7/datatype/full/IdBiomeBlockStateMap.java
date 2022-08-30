package com.seibel.lod.core.a7.datatype.full;

import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

// WARNING: This is not THREAD-SAFE!
public class IdBiomeBlockStateMap {
    public static final IWrapperFactory FACTORY = SingletonInjector.INSTANCE.get(IWrapperFactory.class);

    public static final class Entry {
        public final IBiomeWrapper biome;
        public final IBlockStateWrapper blockState;
        public Entry(IBiomeWrapper biome, IBlockStateWrapper blockState) {
            this.biome = biome;
            this.blockState = blockState;
        }
        @Override
        public int hashCode() {
            return Objects.hash(biome, blockState);
        }
        @Override
        public boolean equals(Object other) {
            if (other == this) return true;
            if (!(other instanceof Entry)) return false;
            return ((Entry) other).biome.equals(biome) && ((Entry) other).blockState.equals(blockState);
        }

        public String serialize() {
            return biome.serialize() + " " + blockState.serialize();
        }

        public static Entry deserialize(String str) throws IOException {
            String[] strs = str.split(" ");
            if (strs.length != 2) throw new IOException("Failed to deserialize BiomeBlockStateEntry");
            IBiomeWrapper biome = FACTORY.deserializeBiomeWrapper(strs[0]);
            IBlockStateWrapper blockState = FACTORY.deserializeBlockStateWrapper(strs[1]);
            return new Entry(biome, blockState);
        }
    }


    final ArrayList<Entry> entries = new ArrayList<>();
    final HashMap<Entry, Integer> idMap = new HashMap<>();

    public Entry get(int id) {
        return entries.get(id);
    }

    public int setAndGetId(IBiomeWrapper biome, IBlockStateWrapper blockState) {
        return idMap.computeIfAbsent(new Entry(biome, blockState), (e) -> {
            int id = entries.size();
            entries.add(e);
            return id;
        });
    }
    public int setAndGetId(Entry biomeBlockStateEntry) {
        return idMap.computeIfAbsent(biomeBlockStateEntry, (e) -> {
            int id = entries.size();
            entries.add(e);
            return id;
        });
    }

    public int[] computeAndMergeMapFrom(IdBiomeBlockStateMap target) {
        ArrayList<Entry> mergeEntry = target.entries;
        int[] mapper = new int[mergeEntry.size()];
        for (int i=0; i<mergeEntry.size(); i++) {
            mapper[i] = setAndGetId(mergeEntry.get(i));
        }
        return mapper;
    }

    void serialize(OutputStream os) {
        try (DataOutputStream dos = new DataOutputStream(os)) {
            dos.writeInt(entries.size());
            for (Entry e : entries) {
                dos.writeUTF(e.serialize());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static IdBiomeBlockStateMap deserialize(InputStream is) {
        try (DataInputStream dis = new DataInputStream(is)) {
            int size = dis.readInt();
            IdBiomeBlockStateMap map = new IdBiomeBlockStateMap();
            for (int i = 0; i < size; i++) {
                map.entries.add(Entry.deserialize(dis.readUTF()));
            }
            return map;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof IdBiomeBlockStateMap)) return false;
        IdBiomeBlockStateMap otherMap = (IdBiomeBlockStateMap) other;
        if (entries.size() != otherMap.entries.size()) return false;
        for (int i=0; i<entries.size(); i++) {
            if (!entries.get(i).equals(otherMap.entries.get(i))) return false;
        }
        return true;
    }
}
