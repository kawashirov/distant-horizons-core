package com.seibel.lod.core.dataObjects.render.bufferBuilding;

import com.seibel.lod.api.enums.config.EGpuUploadMethod;
import com.seibel.lod.api.enums.rendering.EDebugMode;
import com.seibel.lod.api.enums.rendering.EGLProxyContext;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.dataObjects.render.columnViews.ColumnArrayView;
import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.render.glObject.GLProxy;
import com.seibel.lod.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.lod.core.util.RenderDataPointUtil;
import com.seibel.lod.core.util.ThreadUtil;
import com.seibel.lod.core.util.objects.Reference;
import com.seibel.lod.core.util.objects.UncheckedInterruptedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Used to populate the buffers in a {@link ColumnRenderSource} object.
 * 
 * @see ColumnRenderSource
 */
public class ColumnRenderBufferBuilder
{
	public static final ConfigBasedLogger EVENT_LOGGER = new ConfigBasedLogger(LogManager.getLogger(),
			() -> Config.Client.Advanced.Debugging.DebugSwitch.logRendererBufferEvent.get());
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	//TODO: Make the pool change thread count after the config value is changed
	public static final ExecutorService BUFFER_BUILDER_THREADS = ThreadUtil.makeThreadPool(Config.Client.Advanced.Threading.numberOfBufferBuilderThreads.get(), "BufferBuilder");
	public static final ExecutorService BUFFER_UPLOADER = ThreadUtil.makeSingleThreadPool("ColumnBufferUploader");
	// TODO this should probably be based on the number of builder threads
	public static final int MAX_CONCURRENT_CALL = 8;
	
	
	
	
	//==============//
	// vbo building //
	//==============//
	
	/// TODO this is static, it should be moved to its own class to prevent confusion
	/** @return null if busy */
	public static CompletableFuture<ColumnRenderBuffer> buildBuffers(IDhClientLevel clientLevel, Reference<ColumnRenderBuffer> renderBufferRef, ColumnRenderSource renderSource, ColumnRenderSource[] adjData)
	{
		if (isBusy())
		{
			return null;
		}
		
		//LOGGER.info("RenderRegion startBuild @ {}", renderSource.sectionPos);
		return CompletableFuture.supplyAsync(() ->
				{
					try
					{
						boolean enableTransparency = Config.Client.Graphics.Quality.transparency.get().tranparencyEnabled;
						
						EVENT_LOGGER.trace("RenderRegion start QuadBuild @ "+renderSource.sectionPos);
						boolean enableSkyLightCulling = Config.Client.Graphics.AdvancedGraphics.enableCaveCulling.get();
						
						int skyLightCullingBelow = Config.Client.Graphics.AdvancedGraphics.caveCullingHeight.get();
						// FIXME: Clamp also to the max world height.
						skyLightCullingBelow = Math.max(skyLightCullingBelow, clientLevel.getMinY());
						
						LodQuadBuilder builder = new LodQuadBuilder(enableSkyLightCulling,
								(short) (skyLightCullingBelow - clientLevel.getMinY()), enableTransparency);
						
						makeLodRenderData(builder, renderSource, adjData);
						EVENT_LOGGER.trace("RenderRegion end QuadBuild @ "+renderSource.sectionPos);
						return builder;
					}
					catch (UncheckedInterruptedException e)
					{
						throw e;
					}
					catch (Throwable e3)
					{
						LOGGER.error("\"LodNodeBufferBuilder\" was unable to build quads: ", e3);
						throw e3;
					}
					
				}, BUFFER_BUILDER_THREADS)
				.thenApplyAsync((quadBuilder) ->
						{
							try
							{
								EVENT_LOGGER.trace("RenderRegion start Upload @ "+renderSource.sectionPos);
								GLProxy glProxy = GLProxy.getInstance();
								EGpuUploadMethod method = GLProxy.getInstance().getGpuUploadMethod();
								EGLProxyContext oldContext = glProxy.getGlContext();
								glProxy.setGlContext(EGLProxyContext.LOD_BUILDER);
								ColumnRenderBuffer buffer = renderBufferRef.swap(null);
								
								if (buffer == null)
								{
									buffer = new ColumnRenderBuffer(new DhBlockPos(renderSource.sectionPos.getCorner().getCornerBlockPos(), clientLevel.getMinY()));
								}
								buffer.buffersUploaded = false;
								
								try
								{
									buffer.uploadBuffer(quadBuilder, method);
									EVENT_LOGGER.trace("RenderRegion end Upload @ "+renderSource.sectionPos);
									return buffer;
								}
								catch (Exception e)
								{
									buffer.close();
									throw e;
								}
								finally
								{
									glProxy.setGlContext(oldContext);
								}
							}
							catch (InterruptedException e)
							{
								throw UncheckedInterruptedException.convert(e);
							}
							catch (Throwable e3)
							{
								LOGGER.error("\"LodNodeBufferBuilder\" was unable to upload buffer: ", e3);
								throw e3;
							}
						},
						BUFFER_UPLOADER).handle((columnRenderBuffer, ex) ->
				{
					//LOGGER.info("RenderRegion endBuild @ {}", renderSource.sectionPos);
					if (ex != null)
					{
						LOGGER.warn("Buffer building failed: "+ex.getMessage(), ex);
						
						ColumnRenderBuffer buffer;
						if (!renderBufferRef.isEmpty())
						{
							buffer = renderBufferRef.swap(null);
							buffer.close();
						}
						
						return null;
					}
					else
					{
						return columnRenderBuffer;
					}
				});
	}
	private static void makeLodRenderData(LodQuadBuilder quadBuilder, ColumnRenderSource region, ColumnRenderSource[] adjRegions)
	{
		// Variable initialization
		EDebugMode debugMode = Config.Client.Advanced.Debugging.debugMode.get();
		
		// TODO pass in which child DhSectionPos should be built to allow generating partial sections so non-full quadTree RenderSections will render
		byte detailLevel = region.getDataDetail();
		for (int x = 0; x < ColumnRenderSource.SECTION_SIZE; x++)
		{
			for (int z = 0; z < ColumnRenderSource.SECTION_SIZE; z++)
			{
				UncheckedInterruptedException.throwIfInterrupted();
				
				ColumnArrayView posData = region.getVerticalDataPointView(x, z);
				if (posData.size() == 0
						|| !RenderDataPointUtil.doesDataPointExist(posData.get(0))
						|| RenderDataPointUtil.isVoid(posData.get(0)))
				{
					continue;
				}
				
				ColumnRenderSource.DebugSourceFlag debugSourceFlag = region.debugGetFlag(x, z);
				
				ColumnArrayView[][] adjData = new ColumnArrayView[4][];
				// We extract the adj data in the four cardinal direction
				
				// we first reset the adjShadeDisabled. This is used to disable the shade on the
				// border when we have transparent block like water or glass
				// to avoid having a "darker border" underground
				// Arrays.fill(adjShadeDisabled, false);
				
				// We check every adj block in each direction
				
				// If the adj block is rendered in the same region and with same detail
				// and is positioned in a place that is not going to be rendered by vanilla game
				// then we can set this position as adj
				// We avoid cases where the adjPosition is in player chunk while the position is
				// not
				// to always have a wall underwater
				for (ELodDirection lodDirection : ELodDirection.ADJ_DIRECTIONS)
				{
					try
					{
						int xAdj = x + lodDirection.getNormal().x;
						int zAdj = z + lodDirection.getNormal().z;
						boolean isCrossRegionBoundary =
								(xAdj < 0 || xAdj >= ColumnRenderSource.SECTION_SIZE) ||
										(zAdj < 0 || zAdj >= ColumnRenderSource.SECTION_SIZE);
						
						ColumnRenderSource adjRegion;
						byte adjDetail;
						
						//we check if the detail of the adjPos is equal to the correct one (region border fix)
						//or if the detail is wrong by 1 value (region+circle border fix)
						if (isCrossRegionBoundary)
						{
							//we compute at which detail that position should be rendered
							adjRegion = adjRegions[lodDirection.ordinal() - 2];
							if (adjRegion == null)
							{
								continue;
							}
							
							adjDetail = adjRegion.getDataDetail();
							if (adjDetail != detailLevel)
							{
								//TODO: Implement this
							}
							else
							{
								if (xAdj < 0)
									xAdj += ColumnRenderSource.SECTION_SIZE;
								
								if (zAdj < 0)
									zAdj += ColumnRenderSource.SECTION_SIZE;
								
								if (xAdj >= ColumnRenderSource.SECTION_SIZE)
									xAdj -= ColumnRenderSource.SECTION_SIZE;
								
								if (zAdj >= ColumnRenderSource.SECTION_SIZE)
									zAdj -= ColumnRenderSource.SECTION_SIZE;
							}
						}
						else
						{
							adjRegion = region;
							adjDetail = detailLevel;
						}
						
						if (adjDetail < detailLevel - 1 || adjDetail > detailLevel + 1)
						{
							continue;
						}
						
						if (adjDetail == detailLevel || adjDetail > detailLevel)
						{
							adjData[lodDirection.ordinal() - 2] = new ColumnArrayView[1];
							adjData[lodDirection.ordinal() - 2][0] = adjRegion.getVerticalDataPointView(xAdj, zAdj);
						}
						else
						{
							adjData[lodDirection.ordinal() - 2] = new ColumnArrayView[2];
							adjData[lodDirection.ordinal() - 2][0] = adjRegion.getVerticalDataPointView(xAdj, zAdj);
							adjData[lodDirection.ordinal() - 2][1] = adjRegion.getVerticalDataPointView(
									xAdj + (lodDirection.getAxis() == ELodDirection.Axis.X ? 0 : 1),
									zAdj + (lodDirection.getAxis() == ELodDirection.Axis.Z ? 0 : 1));
						}
					}
					catch (RuntimeException e)
					{
						EVENT_LOGGER.warn("Failed to get adj data for [{}:{},{}] at [{}]", detailLevel, x, z, lodDirection);
						EVENT_LOGGER.warn("Detail exception: ", e);
					}
				} // for adjacent directions
				
				// We render every vertical lod present in this position
				// We only stop when we find a block that is void or non-existing block
				for (int i = 0; i < posData.size(); i++)
				{
					long data = posData.get(i);
					// If the data is not renderable (Void or non-existing) we stop since there is
					// no data left in this position
					if (RenderDataPointUtil.isVoid(data) || !RenderDataPointUtil.doesDataPointExist(data))
					{
						break;
					}
					
					long adjDataTop = i - 1 >= 0 ? posData.get(i - 1) : RenderDataPointUtil.EMPTY_DATA;
					long adjDataBot = i + 1 < posData.size() ? posData.get(i + 1) : RenderDataPointUtil.EMPTY_DATA;
					
					CubicLodTemplate.addLodToBuffer(data, adjDataTop, adjDataBot, adjData, detailLevel,
							x, z, quadBuilder, debugMode, debugSourceFlag);
				}
				
			}// for z
		}// for x
		
		quadBuilder.mergeQuads();
	}
	
	
	
	//=================//
	// vbo interaction //
	//=================//
	
	public static GLVertexBuffer[] resizeBuffer(GLVertexBuffer[] vbos, int newSize)
	{
		if (vbos.length == newSize)
		{
			return vbos;
		}
		
		GLVertexBuffer[] newVbos = new GLVertexBuffer[newSize];
		System.arraycopy(vbos, 0, newVbos, 0, Math.min(vbos.length, newSize));
		if (newSize < vbos.length)
		{
			for (int i = newSize; i < vbos.length; i++)
			{
				if (vbos[i] != null)
				{
					vbos[i].close();
				}
			}
		}
		return newVbos;
	}
	
	public static GLVertexBuffer getOrMakeBuffer(GLVertexBuffer[] vbos, int iIndex, boolean useBuffStorage)
	{
		if (vbos[iIndex] == null)
		{
			vbos[iIndex] = new GLVertexBuffer(useBuffStorage);
		}
		return vbos[iIndex];
	}
	
	
	
	//=========//
	// getters //
	//=========//
	
	// TODO move static methods to their own class to avoid confusion
	private static long getCurrentJobsCount()
	{
		long jobs = ((ThreadPoolExecutor) BUFFER_BUILDER_THREADS).getQueue().stream().filter(runnable -> !((Future<?>) runnable).isDone()).count();
		jobs += ((ThreadPoolExecutor) BUFFER_UPLOADER).getQueue().stream().filter(runnable -> !((Future<?>) runnable).isDone()).count();
		return jobs;
	}
	public static boolean isBusy() { return getCurrentJobsCount() > MAX_CONCURRENT_CALL; }
	
}