/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2021  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.render;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.seibel.lod.core.api.ApiShared;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.rendering.GLProxyContext;
import com.seibel.lod.core.util.GLMessage;
import com.seibel.lod.core.util.GLMessageOutputStream;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

/**
 * A singleton that holds references to different openGL contexts
 * and GPU capabilities.
 *
 * <p>
 * Helpful OpenGL resources:
 * <p>
 * https://www.seas.upenn.edu/~pcozzi/OpenGLInsights/OpenGLInsights-AsynchronousBufferTransfers.pdf <br>
 * https://learnopengl.com/Advanced-OpenGL/Advanced-Data <br>
 * https://www.slideshare.net/CassEveritt/approaching-zero-driver-overhead <br><br>
 * 
 * https://gamedev.stackexchange.com/questions/91995/edit-vbo-data-or-create-a-new-one <br>
 * https://stackoverflow.com/questions/63509735/massive-performance-loss-with-glmapbuffer <br><br>
 * 
 * @author James Seibel
 * @version 12-9-2021
 */
public class GLProxy
{
	
	
	
	
	
	
	
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	
	private ExecutorService workerThread = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(GLProxy.class.getSimpleName() + "-Worker-Thread").build());

	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	private static GLProxy instance = null;
	
	/** Minecraft's GLFW window */
	public final long minecraftGlContext;
	/** Minecraft's GL capabilities */
	public final GLCapabilities minecraftGlCapabilities;
	
	/** the LodBuilder's GLFW window */
	public final long lodBuilderGlContext;
	/** the LodBuilder's GL capabilities */
	public final GLCapabilities lodBuilderGlCapabilities;

	/** the proxyWorker's GLFW window */
	public final long proxyWorkerGlContext;
	/** the proxyWorker's GL capabilities */
	public final GLCapabilities proxyWorkerGlCapabilities;
	
	/** Requires OpenGL 4.4, and offers the best buffer uploading */
	public final boolean bufferStorageSupported;
	
	/** Requires OpenGL 4.5 */
	public final boolean namedObjectSupported;

	/** Requires OpenGL 4.3 */
	public final boolean VertexAttributeBufferBindingSupported;
	
	/** Requires OpenGL 3.0, which will current min requirement as 3.3, should always be true */
	@Deprecated
	public final boolean mapBufferRangeSupported = true;
	
	private final GpuUploadMethod preferredUploadMethod;
	
	public final GLMessage.Builder lodBuilderDebugMessageBuilder;
	public final GLMessage.Builder proxyWorkerDebugMessageBuilder;
	
	
	private String getFailedVersionInfo(GLCapabilities c) {
		
		return "Your supported OpenGL version:\n" + "1.1: " + c.OpenGL11 + "\n" +
				"1.2: " + c.OpenGL12 + "\n" +
				"1.3: " + c.OpenGL13 + "\n" +
				"1.4: " + c.OpenGL14 + "\n" +
				"1.5: " + c.OpenGL15 + "\n" +
				"2.0: " + c.OpenGL20 + "\n" +
				"2.1: " + c.OpenGL21 + "\n" +
				"3.0: " + c.OpenGL30 + "\n" +
				"3.1: " + c.OpenGL31 + "\n" +
				"3.2: " + c.OpenGL32 + " <- REQUIRED\n" +
				"3.3: " + c.OpenGL33 + "\n" +
				"4.0: " + c.OpenGL40 + "\n" +
				"4.1: " + c.OpenGL41 + "\n" +
				"4.2: " + c.OpenGL42 + "\n" +
				"4.3: " + c.OpenGL43 + " <- optional improvement\n" +
				"4.4: " + c.OpenGL44 + " <- optional improvement\n" +
				"4.5: " + c.OpenGL45 + "\n" +
				"4.6: " + c.OpenGL46 + "\n" +
				"If you noticed that your computer supports higher OpenGL versions"
				+ " but not the required version, try running the game in compatibility mode."
				+ " (How you turn that on, I have no clue~)";
	}
	private String getVersionInfo(GLCapabilities c) {
		
		return "Your supported OpenGL version:\n" + "1.1: " + c.OpenGL11 + "\n" +
				"1.2: " + c.OpenGL12 + "\n" +
				"1.3: " + c.OpenGL13 + "\n" +
				"1.4: " + c.OpenGL14 + "\n" +
				"1.5: " + c.OpenGL15 + "\n" +
				"2.0: " + c.OpenGL20 + "\n" +
				"2.1: " + c.OpenGL21 + "\n" +
				"3.0: " + c.OpenGL30 + "\n" +
				"3.1: " + c.OpenGL31 + "\n" +
				"3.2: " + c.OpenGL32 + " <- REQUIRED\n" +
				"3.3: " + c.OpenGL33 + "\n" +
				"4.0: " + c.OpenGL40 + "\n" +
				"4.1: " + c.OpenGL41 + "\n" +
				"4.2: " + c.OpenGL42 + "\n" +
				"4.3: " + c.OpenGL43 + " <- optional improvement\n" +
				"4.4: " + c.OpenGL44 + " <- optional improvement\n" +
				"4.5: " + c.OpenGL45 + "\n" +
				"4.6: " + c.OpenGL46 + "\n";
	}
	
	private static void logMessage(GLMessage msg) {
		GLMessage.Severity s = msg.severity;
		if (msg.type == GLMessage.Type.ERROR ||
			msg.type == GLMessage.Type.UNDEFINED_BEHAVIOR) {
			ApiShared.LOGGER.error("GL ERROR {} from {}: {}", msg.id, msg.source, msg.message);
			throw new RuntimeException("GL ERROR: "+msg.toString());
		}
		RuntimeException e = new RuntimeException("GL MESSAGE: "+msg.toString());
		switch (s) {
		case HIGH:
			ApiShared.LOGGER.error(e);
			break;
		case MEDIUM:
			ApiShared.LOGGER.warn(e);
			break;
		case LOW:
			ApiShared.LOGGER.info(e);
			break;
		case NOTIFICATION:
			ApiShared.LOGGER.debug(e);
			break;
		}
		
	}
	
	
	/** 
	 * @throws IllegalStateException 
	 * @throws RuntimeException 
	 * @throws FileNotFoundException 
	 */
	private GLProxy()
	{
		lodBuilderDebugMessageBuilder = new GLMessage.Builder(
				(type) -> {
					if (type == GLMessage.Type.POP_GROUP) return false;
					if (type == GLMessage.Type.PUSH_GROUP) return false;
					if (type == GLMessage.Type.MARKER) return false;
					// if (type == GLMessage.Type.PERFORMANCE) return false;
					return true;
				}
				,(severity) -> {
					if (severity == GLMessage.Severity.NOTIFICATION) return false;
					return true;
				},null
				);
		proxyWorkerDebugMessageBuilder = new GLMessage.Builder(
				(type) -> {
					if (type == GLMessage.Type.POP_GROUP) return false;
					if (type == GLMessage.Type.PUSH_GROUP) return false;
					if (type == GLMessage.Type.MARKER) return false;
					// if (type == GLMessage.Type.PERFORMANCE) return false;
					return true;
				}
				,(severity) -> {
					if (severity == GLMessage.Severity.NOTIFICATION) return false;
					return true;
				},null
				);
		
		
		
        // this must be created on minecraft's render context to work correctly
		
		ApiShared.LOGGER.info("Creating " + GLProxy.class.getSimpleName() + "... If this is the last message you see in the log there must have been a OpenGL error.");

		ApiShared.LOGGER.info("Lod Render OpenGL version [" + GL11.glGetString(GL11.GL_VERSION) + "].");
		
		// getting Minecraft's context has to be done on the render thread,
		// where the GL context is
		if (GLFW.glfwGetCurrentContext() == 0L)
			throw new IllegalStateException(GLProxy.class.getSimpleName() + " was created outside the render thread!");
		
		//============================//
		// create the builder context //
		//============================//
		
		// get Minecraft's context
		minecraftGlContext = GLFW.glfwGetCurrentContext();
		minecraftGlCapabilities = GL.getCapabilities();

		// crash the game if the GPU doesn't support OpenGL 3.2
		if (!minecraftGlCapabilities.OpenGL32)
		{
			String supportedVersionInfo = getFailedVersionInfo(minecraftGlCapabilities);
			
			// Note: as of MC 1.17 this shouldn't happen since MC
			// requires OpenGL 3.2, but for older MC version this will warn the player.
			String errorMessage = ModInfo.READABLE_NAME + " was initializing " + GLProxy.class.getSimpleName()
					+ " and discovered this GPU doesn't support OpenGL 3.2." + " Sorry I couldn't tell you sooner :(\n"+
					"Additional info:\n"+supportedVersionInfo;
			MC.crashMinecraft(errorMessage, new UnsupportedOperationException("This GPU doesn't support OpenGL 3.2."));
		}
		ApiShared.LOGGER.info("minecraftGlCapabilities:\n"+getVersionInfo(minecraftGlCapabilities));
		
		GLFW.glfwMakeContextCurrent(0L);
		
		// context creation setup
		GLFW.glfwDefaultWindowHints();
		// make the context window invisible
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		// by default the context should get the highest available OpenGL version
		// but this can be explicitly set for testing
//		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
//		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);
		
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		
		// create the LodBuilder context
		lodBuilderGlContext = GLFW.glfwCreateWindow(64, 48, "LOD Builder Window", 0L, minecraftGlContext);
		if (lodBuilderGlContext == 0) {
			ApiShared.LOGGER.error("ERROR: Failed to create GLFW context for OpenGL 3.2 with"
					+ " Forward Compat Core Profile! Your OS may have not been able to support it!");
			throw new UnsupportedOperationException("Forward Compat Core Profile 3.2 creation failure");
		}
		GLFW.glfwMakeContextCurrent(lodBuilderGlContext);
		lodBuilderGlCapabilities = GL.createCapabilities();
		ApiShared.LOGGER.info("lodBuilderGlCapabilities:\n"+getVersionInfo(lodBuilderGlCapabilities));
		GLFW.glfwMakeContextCurrent(0L);
		
		// create the proxyWorker's context
		proxyWorkerGlContext = GLFW.glfwCreateWindow(64, 48, "LOD proxy worker Window", 0L, minecraftGlContext);
		if (proxyWorkerGlContext == 0) {
			ApiShared.LOGGER.error("ERROR: Failed to create GLFW context for OpenGL 3.2 with"
					+ " Forward Compat Core Profile! Your OS may have not been able to support it!");
			throw new UnsupportedOperationException("Forward Compat Core Profile 3.2 creation failure");
		}
		GLFW.glfwMakeContextCurrent(proxyWorkerGlContext);
		proxyWorkerGlCapabilities = GL.createCapabilities();
		ApiShared.LOGGER.info("proxyWorkerGlCapabilities:\n"+getVersionInfo(lodBuilderGlCapabilities));
		GLFW.glfwMakeContextCurrent(0L);

		// Check if we can use the make-over version of Vertex Attribute, which is available in GL4.3 or after
		VertexAttributeBufferBindingSupported = minecraftGlCapabilities.glBindVertexBuffer != 0L; // Nullptr

		// UNUSED currently
		// Check if we can use the named version of all calls, which is available in GL4.5 or after
		namedObjectSupported = minecraftGlCapabilities.glNamedBufferData != 0L; //Nullptr
		
		
		//==================================//
		// get any GPU related capabilities //
		//==================================//
		
		setGlContext(GLProxyContext.LOD_BUILDER);

		GLUtil.setupDebugMessageCallback(new PrintStream(new GLMessageOutputStream((msg) -> {
			logMessage(msg);
		}, lodBuilderDebugMessageBuilder), true));
		
		// get specific capabilities
		// Check if we can use the Buffer Storage, which is available in GL4.4 or after
		bufferStorageSupported = minecraftGlCapabilities.glBufferStorage != 0L && lodBuilderGlCapabilities.glBufferStorage != 0L; // Nullptr
		//bufferStorageSupported = true;
		// display the capabilities
		if (!bufferStorageSupported)
		{
			ApiShared.LOGGER.warn("This GPU doesn't support Buffer Storage (OpenGL 4.4), falling back to using other methods.");			
		}
		
		String vendor = GL32.glGetString(GL32.GL_VENDOR).toUpperCase(); // example return: "NVIDIA CORPORATION"
		if (vendor.contains("NVIDIA") || vendor.contains("GEFORCE"))
		{
			// NVIDIA card
			preferredUploadMethod = bufferStorageSupported ? GpuUploadMethod.BUFFER_STORAGE : GpuUploadMethod.SUB_DATA;
		}
		else
		{
			// AMD or Intel card
			preferredUploadMethod = GpuUploadMethod.BUFFER_MAPPING;
		}
		
		ApiShared.LOGGER.info("GPU Vendor [" + vendor + "], Preferred upload method is [" + preferredUploadMethod + "].");

		setGlContext(GLProxyContext.PROXY_WORKER);
		
		GLUtil.setupDebugMessageCallback(new PrintStream(new GLMessageOutputStream((msg) -> {
			logMessage(msg);
		}, proxyWorkerDebugMessageBuilder), true));
		
		//==========//
		// clean up //
		//==========//
		
		// Since this is created on the render thread, make sure the Minecraft context is used in the end
        setGlContext(GLProxyContext.MINECRAFT);
		
		// GLProxy creation success
		ApiShared.LOGGER.info(GLProxy.class.getSimpleName() + " creation successful. OpenGL smiles upon you this day.");
	}
	
	/**
	 * A wrapper function to make switching contexts easier. <br>
	 * Does nothing if the calling thread is already using newContext.
	 */
	public void setGlContext(GLProxyContext newContext)
	{
		GLProxyContext currentContext = getGlContext();
		
		// we don't have to change the context, we are already there.
		if (currentContext == newContext)
			return;
		
		
		long contextPointer;
		GLCapabilities newGlCapabilities = null;
		
		// get the pointer(s) for this context
		switch (newContext)
		{
		case LOD_BUILDER:
			contextPointer = lodBuilderGlContext;
			newGlCapabilities = lodBuilderGlCapabilities;
			break;
		
		case MINECRAFT:
			contextPointer = minecraftGlContext;
			newGlCapabilities = minecraftGlCapabilities;
			break;
		
		case PROXY_WORKER:
			contextPointer = proxyWorkerGlContext;
			newGlCapabilities = proxyWorkerGlCapabilities;
			break;
			
		default: // default should never happen, it is just here to make the compiler happy
		case NONE:
			// 0L is equivalent to null
			contextPointer = 0L;
			break;
		}
		
		GLFW.glfwMakeContextCurrent(contextPointer);
		GL.setCapabilities(newGlCapabilities);
	}
	
	/** Returns this thread's OpenGL context. */
	public GLProxyContext getGlContext()
	{
		long currentContext = GLFW.glfwGetCurrentContext();
		
		
		if (currentContext == lodBuilderGlContext)
			return GLProxyContext.LOD_BUILDER;
		else if (currentContext == minecraftGlContext)
			return GLProxyContext.MINECRAFT;
		else if (currentContext == proxyWorkerGlContext)
			return GLProxyContext.PROXY_WORKER;
		else if (currentContext == 0L)
			return GLProxyContext.NONE;
		else
			// hopefully this shouldn't happen
			throw new IllegalStateException(Thread.currentThread().getName() + 
					" has a unknown OpenGl context: [" + currentContext + "]. "
					+ "Minecraft context [" + minecraftGlContext + "], "
					+ "LodBuilder context [" + lodBuilderGlContext + "], "
					+ "ProxyWorker context [" + proxyWorkerGlContext + "], "
					+ "no context [0].");
	}
	
	public static boolean hasInstance() {
		return instance != null;
	}
	
	public static GLProxy getInstance()
	{
		if (instance == null)
			instance = new GLProxy();
		return instance;
	}
	
	public GpuUploadMethod getGpuUploadMethod() {
		GpuUploadMethod method = CONFIG.client().advanced().buffers().getGpuUploadMethod();

		if (!bufferStorageSupported && method == GpuUploadMethod.BUFFER_STORAGE)
		{
			// if buffer storage isn't supported
			// default to SUB_DATA
			method = GpuUploadMethod.SUB_DATA;
		}
		
		return method == GpuUploadMethod.AUTO ? preferredUploadMethod : method;
	}
	
	/** 
	 * Asynchronously calls the given runnable on proxy's OpenGL context.
	 * Useful for creating/destroying OpenGL objects in a thread
	 * that doesn't normally have access to a OpenGL context. <br>
	 * No rendering can be done through this method.
	 */
	public void recordOpenGlCall(Runnable renderCall)
	{
		workerThread.execute(new Thread(() -> { runnableContainer(renderCall); }));
	}
	private void runnableContainer(Runnable renderCall)
	{
		try
		{
			// set up the context...
			setGlContext(GLProxyContext.PROXY_WORKER);
			// ...run the actual code...
			renderCall.run();
		}
		catch (Exception e)
		{
			ApiShared.LOGGER.error(Thread.currentThread().getName() + " ran into a issue: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			// ...and make sure the context is released when the thread finishes
			setGlContext(GLProxyContext.NONE);	
		}
	}
	
	public static void ensureAllGLJobCompleted() {
		if (!hasInstance()) return;
		ApiShared.LOGGER.info("Blocking until GL jobs finished!");
		try {
			instance.workerThread.shutdown();
			boolean worked = instance.workerThread.awaitTermination(30, TimeUnit.SECONDS);
			if (!worked)
				ApiShared.LOGGER.error("GLWorkerThread shutdown timed out! Game may crash on exit due to cleanup failure!");
		} catch (InterruptedException e) {
			ApiShared.LOGGER.error("GLWorkerThread shutdown is interrupted! Game may crash on exit due to cleanup failure!");
			e.printStackTrace();
		} finally {
			instance.workerThread = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(GLProxy.class.getSimpleName() + "-Worker-Thread").build());
		}
	}
}
