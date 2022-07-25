package com.seibel.lod.core.a7.render;

import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.datatype.LodRenderSource;
import com.seibel.lod.core.a7.save.io.render.IRenderSourceProvider;

import java.util.concurrent.CompletableFuture;

public class LodRenderSection {
    public final DhSectionPos pos;

    /* Following used for LodQuadTree tick() method, and ONLY for that method! */
    // the number of children of this section
    // (Should always be 4 after tick() is done, or 0 only if this is an unloaded node)
    public byte childCount = 0;

    // TODO: Should I provide a way to change the render source?
    private LodRenderSource lodRenderSource;
    private CompletableFuture<LodRenderSource> loadFuture;
    private boolean isRenderEnabled = false;
    private IClientLevel level; //FIXME: Hack to pass level into enableRender() for renderSource

    // Create sub region
    public LodRenderSection(DhSectionPos pos) {
        this.pos = pos;
    }

    public void enableRender(IClientLevel level, LodQuadTree quadTree) {
        if (isRenderEnabled) return;
        if (lodRenderSource != null) {
            lodRenderSource.enableRender(level, quadTree);
        }
        this.level = level;
        isRenderEnabled = true;
    }
    public void disableRender() {
        if (!isRenderEnabled) return;
        level = null;
        if (lodRenderSource != null) {
            lodRenderSource.disableRender();
        }
        isRenderEnabled = false;
    }

    public void load(IRenderSourceProvider renderDataProvider) {
        if (loadFuture != null || lodRenderSource != null) throw new IllegalStateException("Reloading is not supported!");
        loadFuture = renderDataProvider.read(pos);
    }
    public void reload(IRenderSourceProvider renderDataProvider) {
        if (loadFuture != null) throw new IllegalStateException("This section is already loading!");
        if (lodRenderSource == null) throw new IllegalStateException("This section is not loaded!");
        lodRenderSource.dispose();
        lodRenderSource = null;
        loadFuture = renderDataProvider.read(pos);
    }

    public void tick(LodQuadTree quadTree) {
        if (loadFuture != null && loadFuture.isDone()) {
            lodRenderSource = loadFuture.join();
            loadFuture = null;
            if (isRenderEnabled) {
                lodRenderSource.enableRender(level, quadTree);
            }
        }
    }

    public void dispose() {
        if (lodRenderSource != null) {
            lodRenderSource.dispose();
        } else if (loadFuture != null) {
            loadFuture.cancel(true);
        }
    }

    public boolean canRender() {
        return isLoaded() && lodRenderSource.isRenderReady();
    }

    public boolean isLoaded() {
        return lodRenderSource != null;
    }

    public boolean isLoading() {
        return loadFuture != null;
    }

    public boolean isOutdated() {
        return lodRenderSource != null && !lodRenderSource.isValid();
    }

    public LodRenderSource getRenderContainer() {
        return lodRenderSource;
    }


    public String toString() {
        return "LodRenderSection{" +
                "pos=" + pos +
                ", childCount=" + childCount +
                ", lodRenderSource=" + lodRenderSource +
                ", loadFuture=" + loadFuture +
                ", isRenderEnabled=" + isRenderEnabled +
                ", level=" + level +
                '}';
    }
}
