package com.seibel.lod.core.render;

import com.seibel.lod.core.level.IClientLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.datatype.LodRenderSource;
import com.seibel.lod.core.io.renderfile.IRenderSourceProvider;

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
    private IRenderSourceProvider provider = null;

    // Create sub region
    public LodRenderSection(DhSectionPos pos) {
        this.pos = pos;
    }

    public void enableRender(IClientLevel level, LodQuadTree quadTree) {
        if (isRenderEnabled) return;
        loadFuture = provider.read(pos);
        isRenderEnabled = true;
    }
    public void disableRender() {
        if (!isRenderEnabled) return;
        if (lodRenderSource != null) {
            lodRenderSource.disableRender();
            lodRenderSource.dispose();
            lodRenderSource = null;
        }
        if (loadFuture != null) {
            loadFuture.cancel(true);
            loadFuture = null;
        }
        isRenderEnabled = false;
    }

    public void load(IRenderSourceProvider renderDataProvider) {
        provider = renderDataProvider;
    }
    public void reload(IRenderSourceProvider renderDataProvider) {
        if (loadFuture != null) {
            loadFuture.cancel(true);
            loadFuture = null;
        }
        if (lodRenderSource != null) {
            lodRenderSource.dispose();
            lodRenderSource = null;
        }
        loadFuture = renderDataProvider.read(pos);
    }

    public void tick(LodQuadTree quadTree, IClientLevel level) {
        if (loadFuture != null && loadFuture.isDone()) {
            lodRenderSource = loadFuture.join();
            loadFuture = null;
            if (isRenderEnabled) {
                lodRenderSource.enableRender(level, quadTree);
            }
        }
        if (lodRenderSource != null) {
            provider.refreshRenderSource(lodRenderSource);
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
        return isLoaded() && isRenderEnabled && lodRenderSource != null && lodRenderSource.isRenderReady();
    }

    public boolean isLoaded() {
        return provider != null;
    }

    //FIXME: Used by RenderBufferHandler
    public int FIXME_BYPASS_DONT_USE_getChildCount() {
        return childCount;
    }

    public boolean isLoading() {
        return false;
    }

    public boolean isOutdated() {
        return lodRenderSource != null && !lodRenderSource.isValid();
    }

    public LodRenderSource getRenderSource() {
        return lodRenderSource;
    }


    public String toString() {
        return "LodRenderSection{" +
                "pos=" + pos +
                ", childCount=" + childCount +
                ", lodRenderSource=" + lodRenderSource +
                ", loadFuture=" + loadFuture +
                ", isRenderEnabled=" + isRenderEnabled +
                '}';
    }
}
