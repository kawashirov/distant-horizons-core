package com.seibel.lod.core.render;

import com.seibel.lod.api.enums.config.EVerticalQuality;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.datatype.ILodRenderSource;
import com.seibel.lod.core.file.renderfile.ILodRenderSourceProvider;

import java.util.concurrent.CompletableFuture;

public class LodRenderSection
{
    public final DhSectionPos pos;
	
	// TODO create an enum to represent the section's state instead of using magic numbers in the childCount
	//      states (may not be a complete or correct list): loaded (childCount 4), unloaded (childCount 0), markedForDeletion/markedForFreeing (childCount -1)
	
    /* Following used for LodQuadTree tick() method, and ONLY for that method! */
    // the number of children of this section
    // (Should always be 4 after tick() is done, or 0 only if this is an unloaded node)
    public byte childCount = 0;
	
    private CompletableFuture<ILodRenderSource> loadFuture;
    private boolean isRenderEnabled = false;
	
	// TODO: Should I provide a way to change the render source?
	private ILodRenderSource renderSource;
	private ILodRenderSourceProvider renderSourceProvider = null;
	
	private EVerticalQuality previousVerticalQualitySetting = null;
	
	
	
	// Create sub region
    public LodRenderSection(DhSectionPos pos) { this.pos = pos; }
	
	
	
	//===========//
	// rendering //
	//===========//
	
    public void enableRender(IDhClientLevel level, LodQuadTree quadTree)
	{
        if (this.isRenderEnabled)
		{
			return;
		}
		
		
		this.loadFuture = this.renderSourceProvider.read(this.pos);
		this.isRenderEnabled = true;
    }
    public void disableRender()
	{
        if (!this.isRenderEnabled)
		{
			return;
		}
		
		
        if (this.renderSource != null)
		{
			this.renderSource.disableRender();
			this.renderSource.dispose();
			this.renderSource = null;
        }
        if (this.loadFuture != null)
		{
			this.loadFuture.cancel(true);
			this.loadFuture = null;
        }
		
		this.isRenderEnabled = false;
    }
	
	
	
	//==============//
	// LOD provider //
	//==============//
	
    public void load(ILodRenderSourceProvider renderDataProvider)
	{
		this.renderSourceProvider = renderDataProvider;
		this.previousVerticalQualitySetting = Config.Client.Graphics.Quality.verticalQuality.get();
    }
    public void reload(ILodRenderSourceProvider renderDataProvider)
	{
        if (this.loadFuture != null)
		{
			this.loadFuture.cancel(true);
			this.loadFuture = null;
        }
		
        if (this.renderSource != null)
		{
			this.renderSource.dispose();
			this.renderSource = null;
        }
		
		this.loadFuture = renderDataProvider.read(this.pos);
		this.previousVerticalQualitySetting = Config.Client.Graphics.Quality.verticalQuality.get();
    }
	
	
	
	//================//
	// update methods //
	//================//
	
    public void tick(LodQuadTree quadTree, IDhClientLevel level)
	{
        if (this.loadFuture != null && this.loadFuture.isDone())
		{
			this.renderSource = this.loadFuture.join();
			this.loadFuture = null;
			
            if (this.isRenderEnabled)
			{
				this.renderSource.enableRender(level, quadTree);
            }
        }
		
        if (this.renderSource != null)
		{
			this.renderSourceProvider.refreshRenderSource(this.renderSource);
        }
    }
	
    public void dispose()
	{
		if (this.renderSource != null)
		{
			this.renderSource.dispose();
		}
		else if (this.loadFuture != null)
		{
			this.loadFuture.cancel(true);
		}
	}

	
	
	//========================//
	// getters and properties //
	//========================//
	
    public boolean canRender() { return this.isLoaded() && this.isRenderEnabled && this.renderSource != null; }

    public boolean isLoaded() { return this.renderSourceProvider != null; }
	public boolean isLoading() { return false; }
	
    //FIXME: Used by RenderBufferHandler
    public int FIXME_BYPASS_DONT_USE_getChildCount() { return this.childCount; }
	
    public boolean isOutdated() { return this.previousVerticalQualitySetting != Config.Client.Graphics.Quality.verticalQuality.get() || (this.renderSource != null && !this.renderSource.isValid()); }

    public ILodRenderSource getRenderSource() { return this.renderSource; }
	
	
	
	//==============//
	// base methods //
	//==============//
	
    public String toString() {
        return "LodRenderSection{" +
                "pos=" + this.pos +
                ", childCount=" + this.childCount +
                ", lodRenderSource=" + this.renderSource +
                ", loadFuture=" + this.loadFuture +
                ", isRenderEnabled=" + this.isRenderEnabled +
                '}';
    }
	
}
