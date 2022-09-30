package com.seibel.lod.core.datatype;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.file.renderfile.RenderMetaFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public abstract class RenderSourceLoader {
    public static final HashMultimap<Class<? extends LodRenderSource>, RenderSourceLoader> loaderRegistry = HashMultimap.create();
    public static final HashMap<Long, Class<? extends LodRenderSource>> renderTypeIdRegistry = new HashMap<>();
    public static RenderSourceLoader getLoader(long renderTypeId, byte loaderVersion) {
        return loaderRegistry.get(renderTypeIdRegistry.get(renderTypeId)).stream()
                .filter(l -> Arrays.binarySearch(l.loaderSupportedVersions, loaderVersion) >= 0)
                .findFirst().orElse(null);
    }
    public static RenderSourceLoader getLoader(Class<? extends LodRenderSource> clazz, byte loaderVersion) {
        return loaderRegistry.get(clazz).stream()
                .filter(l -> Arrays.binarySearch(l.loaderSupportedVersions, loaderVersion) >= 0)
                .findFirst().orElse(null);
    }

    public final Class<? extends LodRenderSource> clazz;
    public final long renderTypeId;
    public final byte[] loaderSupportedVersions;
    public final byte detailOffset;

    public RenderSourceLoader(Class<? extends LodRenderSource> clazz, long renderTypeId, byte[] loaderSupportedVersions, byte detailOffset) {
        this.renderTypeId = renderTypeId;
        this.loaderSupportedVersions = loaderSupportedVersions;
        Arrays.sort(loaderSupportedVersions); // sort to allow fast access
        this.clazz = clazz;
        if (renderTypeIdRegistry.containsKey(renderTypeId) && renderTypeIdRegistry.get(renderTypeId) != clazz) {
            throw new IllegalArgumentException("Loader for renderTypeId " + renderTypeId + " already registered with different class: "
                    + renderTypeIdRegistry.get(renderTypeId) + " != " + clazz);
        }
        Set<RenderSourceLoader> loaders = loaderRegistry.get(clazz);
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
        renderTypeIdRegistry.put(renderTypeId, clazz);
        loaderRegistry.put(clazz, this);
        this.detailOffset = detailOffset;
    }

    // Can return null as meaning the file is out of date or something
    public abstract LodRenderSource loadRender(RenderMetaFile renderFile, InputStream data, IDhLevel level) throws IOException;
    public abstract LodRenderSource createRender(LodDataSource dataSource, IDhClientLevel level);


}
