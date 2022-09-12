package com.seibel.lod.core.datatype;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.level.ILevel;
import com.seibel.lod.core.io.datafile.DataMetaFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public abstract class DataSourceLoader {

    public static final HashMultimap<Class<? extends LodDataSource>, DataSourceLoader> loaderRegistry = HashMultimap.create();
    public final Class<? extends LodDataSource> clazz;
    public static final HashMap<Long, Class<? extends LodDataSource>> datatypeIdRegistry = new HashMap<>();
    public static DataSourceLoader getLoader(long dataTypeId, byte dataVersion) {
        return loaderRegistry.get(datatypeIdRegistry.get(dataTypeId)).stream()
                .filter(l -> Arrays.binarySearch(l.loaderSupportedVersions, dataVersion) >= 0)
                .findFirst().orElse(null);
    }
    public static DataSourceLoader getLoader(Class<? extends LodDataSource> clazz, byte dataVersion) {
        return loaderRegistry.get(clazz).stream()
                .filter(l -> Arrays.binarySearch(l.loaderSupportedVersions, dataVersion) >= 0)
                .findFirst().orElse(null);
    }

    public final long datatypeId;
    public final byte[] loaderSupportedVersions;

    public DataSourceLoader(Class<? extends LodDataSource> clazz, long datatypeId, byte[] loaderSupportedVersions) {
        this.datatypeId = datatypeId;
        this.loaderSupportedVersions = loaderSupportedVersions;
        Arrays.sort(loaderSupportedVersions); // sort to allow fast access
        this.clazz = clazz;
        if (datatypeIdRegistry.containsKey(datatypeId) && datatypeIdRegistry.get(datatypeId) != clazz) {
            throw new IllegalArgumentException("Loader for datatypeId " + datatypeId + " already registered with different class: "
                    + datatypeIdRegistry.get(datatypeId) + " != " + clazz);
        }
        Set<DataSourceLoader> loaders = loaderRegistry.get(clazz);
        if (loaders.stream().anyMatch(other -> {
            // see if any loaderSupportsVersion conflicts with this one
            for (byte otherVer : other.loaderSupportedVersions) {
                if (Arrays.binarySearch(loaderSupportedVersions, otherVer) >= 0) return true;
            }
            return false;
        })) {
            throw new IllegalArgumentException("Loader for class " + clazz + " that supports one of the version in "
                    + Arrays.toString(loaderSupportedVersions) + " already registered!");
        }
        datatypeIdRegistry.put(datatypeId, clazz);
        loaderRegistry.put(clazz, this);
    }

    // Can return null as meaning the requirement is not met
    public abstract LodDataSource loadData(DataMetaFile dataFile, InputStream data, ILevel level) throws IOException;


}
