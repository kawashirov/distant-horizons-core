package com.seibel.distanthorizons.core.level;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.distanthorizons.core.file.renderfile.RenderSourceFileHandler;
import com.seibel.distanthorizons.core.file.structure.AbstractSaveStructure;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.logging.f3.F3Screen;
import com.seibel.distanthorizons.core.pos.DhBlockPos2D;
import com.seibel.distanthorizons.core.pos.DhLodPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.CompleteFullDataSource;
import com.seibel.distanthorizons.core.render.LodQuadTree;
import com.seibel.distanthorizons.core.render.RenderBufferHandler;
import com.seibel.distanthorizons.core.render.renderer.LodRenderer;
import com.seibel.distanthorizons.core.util.FileScanUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class ClientLevelModule {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
    private final IDhClientLevel parent;
    public final AtomicReference<ClientRenderState> ClientRenderStateRef = new AtomicReference<>();
    public final F3Screen.NestedMessage f3Message;
    public ClientLevelModule(IDhClientLevel parent)
    {
        this.parent = parent;
        this.f3Message = new F3Screen.NestedMessage(this::f3Log);
    }

    //==============//
    // tick methods //
    //==============//

    public void clientTick()
    {
        ClientRenderState clientRenderState = this.ClientRenderStateRef.get();
        if (clientRenderState == null)
        {
            return;
        }
        // TODO this should probably be handled via a config change listener
        // recreate the RenderState if the render distance changes
        if (clientRenderState.quadtree.blockRenderDistanceRadius != Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH)
        {
            if (!this.ClientRenderStateRef.compareAndSet(clientRenderState, null))
            {
                return;
            }

            clientRenderState.closeAsync().join(); //TODO: Make it async.
            clientRenderState = new ClientRenderState(parent, parent.getFileHandler(), parent.getSaveStructure());
            if (!this.ClientRenderStateRef.compareAndSet(null, clientRenderState))
            {
                //FIXME: How to handle this?
                LOGGER.warn("Failed to set render state due to concurrency after changing view distance");
                clientRenderState.closeAsync();
                return;
            }
        }
        clientRenderState.quadtree.tick(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
        clientRenderState.renderer.bufferHandler.updateQuadTreeRenderSources();
    }


    //========//
    // render //
    //========//

    /** @return if the {@link ClientRenderState} was successfully swapped */
    public boolean startRenderer()
    {
        ClientRenderState ClientRenderState = new ClientRenderState(parent, parent.getFileHandler(), parent.getSaveStructure());
        if (!this.ClientRenderStateRef.compareAndSet(null, ClientRenderState))
        {
            LOGGER.warn("Failed to start renderer due to concurrency");
            ClientRenderState.closeAsync();
            return false;
        }
        else
        {
            return true;
        }
    }

    public boolean isRendering() {
        return this.ClientRenderStateRef.get() != null;
    }

    public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler)
    {
        ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
        if (ClientRenderState == null)
        {
            // either the renderer hasn't been started yet, or is being reloaded
            return;
        }
        ClientRenderState.renderer.drawLODs(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
    }

    public void stopRenderer()
    {
        LOGGER.info("Stopping renderer for "+this);
        ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
        if (ClientRenderState == null)
        {
            LOGGER.warn("Tried to stop renderer for "+this+" when it was not started!");
            return;
        }
        // stop the render state
        while (!this.ClientRenderStateRef.compareAndSet(ClientRenderState, null)) // TODO why is there a while loop here?
        {
            ClientRenderState = this.ClientRenderStateRef.get();
            if (ClientRenderState == null)
            {
                return;
            }
        }
        ClientRenderState.closeAsync().join(); //TODO: Make it async.
    }

    //===============//
    // data handling //
    //===============//
    public void saveWrites(ChunkSizedFullDataAccessor data)
    {
        ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
        DhLodPos pos = data.getLodPos().convertToDetailLevel(CompleteFullDataSource.SECTION_SIZE_OFFSET);
        if (ClientRenderState != null)
        {
            ClientRenderState.renderSourceFileHandler.writeChunkDataToFile(new DhSectionPos(pos.detailLevel, pos.x, pos.z), data);
        }
        else
        {
            parent.getFileHandler().write(new DhSectionPos(pos.detailLevel, pos.x, pos.z), data);
        }
    }

    public CompletableFuture<Void> saveAsync()
    {
        ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
        if (ClientRenderState != null)
        {
            return ClientRenderState.renderSourceFileHandler.flushAndSaveAsync();
        }
        else
        {
            return CompletableFuture.completedFuture(null);
        }
    }

    public void close()
    {
        // shutdown the renderer
        ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
        if (ClientRenderState != null)
        {
            // TODO does this have to be in a while loop, if so why?
            while (!this.ClientRenderStateRef.compareAndSet(ClientRenderState, null))
            {
                ClientRenderState = this.ClientRenderStateRef.get();
                if (ClientRenderState == null)
                {
                    break;
                }
            }

            if (ClientRenderState != null)
            {
                ClientRenderState.closeAsync().join(); //TODO: Make this async.
            }
        }
    }




    //=======================//
    // misc helper functions //
    //=======================//

    public void dumpRamUsage()
    {
        //TODO
    }

    /** Returns what should be displayed in Minecraft's F3 debug menu */
    protected String[] f3Log()
    {
        String dimName = parent.getClientLevelWrapper().getDimensionType().getDimensionName();
        ClientRenderState renderState = this.ClientRenderStateRef.get();
        if (renderState == null)
        {
            return new String[] { "level @ "+dimName+": Inactive" };
        }
        else
        {
            return new String[] { "level @ "+dimName+": Active" };
        }
    }

    public void clearRenderCache()
    {
        ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
        if (ClientRenderState != null && ClientRenderState.quadtree != null)
        {
            ClientRenderState.quadtree.clearRenderDataCache();
        }
    }

    public void reloadPos(DhSectionPos pos) {
        ClientRenderState clientRenderState = this.ClientRenderStateRef.get();
        if (clientRenderState != null && clientRenderState.quadtree != null)
        {
            clientRenderState.quadtree.reloadPos(pos);
        }
    }

    private static class ClientRenderState
    {
        private static final Logger LOGGER = DhLoggerBuilder.getLogger();
        private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);

        public final ILevelWrapper levelWrapper;
        public final LodQuadTree quadtree;
        public final RenderSourceFileHandler renderSourceFileHandler;
        public final LodRenderer renderer;



        public ClientRenderState(IDhClientLevel dhClientLevel, IFullDataSourceProvider fullDataSourceProvider,
                AbstractSaveStructure saveStructure)
        {
            this.levelWrapper = dhClientLevel.getLevelWrapper();
            this.renderSourceFileHandler = new RenderSourceFileHandler(fullDataSourceProvider, dhClientLevel, saveStructure.getRenderCacheFolder(this.levelWrapper));

            this.quadtree = new LodQuadTree(dhClientLevel, Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH,
                    MC_CLIENT.getPlayerBlockPos().x, MC_CLIENT.getPlayerBlockPos().z, this.renderSourceFileHandler);

            RenderBufferHandler renderBufferHandler = new RenderBufferHandler(this.quadtree);
            FileScanUtil.scanFiles(saveStructure, this.levelWrapper, fullDataSourceProvider, this.renderSourceFileHandler);
            this.renderer = new LodRenderer(renderBufferHandler);
        }



        public CompletableFuture<Void> closeAsync()
        {
            LOGGER.info("Shutting down "+ ClientRenderState.class.getSimpleName()+" async...");

            this.renderer.close();
            this.quadtree.close();
            return this.renderSourceFileHandler.flushAndSaveAsync();
        }

    }
}
