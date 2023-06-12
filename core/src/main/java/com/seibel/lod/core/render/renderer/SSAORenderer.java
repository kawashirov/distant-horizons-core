package com.seibel.lod.core.render.renderer;

import com.seibel.lod.api.enums.config.EGpuUploadMethod;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.render.glObject.GLState;
import com.seibel.lod.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.lod.core.render.glObject.shader.ShaderProgram;
import com.seibel.lod.core.render.glObject.vertexAttribute.VertexAttribute;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.RenderUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.coreapi.util.math.Mat4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

import javax.swing.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

public class SSAORenderer {
	public static SSAORenderer INSTANCE = new SSAORenderer();

	public SSAORenderer() {
	}

	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);

	private static final float[] box_vertices = {
			-1, -1,
			1, -1,
			1, 1,
			-1, -1,
			1, 1,
			-1, 1,
	};

	ShaderProgram ssaoShader;
	ShaderProgram applyShader;
	GLVertexBuffer boxBuffer;
	VertexAttribute va;
	boolean init = false;

	private static final int MAX_KERNEL_SIZE = 32;
	private float[] kernel = new float[MAX_KERNEL_SIZE * 3];

	public void init() {
		if (init) return;

		init = true;
		va = VertexAttribute.create();
		va.bind();
		// Pos
		va.setVertexAttribute(0, 0, VertexAttribute.VertexPointer.addVec2Pointer(false));
		va.completeAndCheck(Float.BYTES * 2);
		ssaoShader = new ShaderProgram("shaders/ssao/ao-vert.vert", "shaders/ssao/ao-frag.frag",
				"fragColor", new String[]{"vPosition"});

		applyShader = new ShaderProgram("shaders/ssao/ao-vert.vert", "shaders/ssao/apply-frag.frag",
				"fragColor", new String[]{"vPosition"});


		// Generate kernel
		kernel = genKernel();
		// Framebuffer
		createBuffer();
	}

	private int width = -1;
	private int height = -1;
	private int ssaoFramebuffer = -1;

	private int ssaoTexture = -1;

	private void createFramebuffer(int width, int height) {
		if (ssaoFramebuffer != -1) {
			GL32.glDeleteFramebuffers(ssaoFramebuffer);
			ssaoFramebuffer = -1;
		}

		if (ssaoTexture != -1) {
			GL32.glDeleteTextures(ssaoTexture);
			ssaoTexture = -1;
		}

		ssaoFramebuffer = GL32.glGenFramebuffers();
		GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, ssaoFramebuffer);

		ssaoTexture = GL32.glGenTextures();
		GL32.glBindTexture(GL32.GL_TEXTURE_2D, ssaoTexture);
		GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, GL32.GL_RED, width, height, 0, GL32.GL_RED, GL32.GL_FLOAT, (ByteBuffer) null);
		GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
		GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
		GL32.glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D, ssaoTexture, 0);
	}

	private void createBuffer() {
		ByteBuffer buffer = ByteBuffer.allocateDirect(box_vertices.length * Float.BYTES);
		buffer.order(ByteOrder.nativeOrder());
		buffer.asFloatBuffer().put(box_vertices);
		buffer.rewind();
		boxBuffer = new GLVertexBuffer(false);
		boxBuffer.bind();
		boxBuffer.uploadBuffer(buffer, box_vertices.length, EGpuUploadMethod.DATA, box_vertices.length * Float.BYTES);
	}

	private static float[] genKernel() {
		float[] kernel = new float[MAX_KERNEL_SIZE * 3];
		for (int i = 0; i < MAX_KERNEL_SIZE; i++) {
			float sampleX = (float) (Math.random() * 2.0 - 1.0);
			float sampleY = (float) (Math.random() * 2.0 - 1.0);
			float sampleZ = (float) Math.random();


			// Normalize
			float magnitude = (float) Math.sqrt(Math.pow(sampleX, 2) + Math.pow(sampleY, 2) + Math.pow(sampleZ, 2));
			sampleX /= magnitude;
			sampleY /= magnitude;
			sampleZ /= magnitude;

			float scale = i / (float) MAX_KERNEL_SIZE;
			float interpolatedScale = (float) (0.1 + (scale * scale) * (0.9));

			sampleX *= interpolatedScale;
			sampleY *= interpolatedScale;
			sampleZ *= interpolatedScale;
			kernel[i * 3] = sampleX;
			kernel[i * 3 + 1] = sampleY;
			kernel[i * 3 + 2] = sampleZ;
		}
		return kernel;
	}

	public void render(float partialTicks) {
		GLState state = new GLState();
		init();
		//GL32.glDepthMask(false);
		int width = MC_RENDER.getTargetFrameBufferViewportWidth();
		int height = MC_RENDER.getTargetFrameBufferViewportHeight();

		if (this.width != width || this.height != height) {
			this.width = width;
			this.height = height;
			createFramebuffer(width, height);
		}

		GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, ssaoFramebuffer);
		GL32.glViewport(0, 0, width, height);
		GL32.glDisable(GL32.GL_DEPTH_TEST);
		GL32.glDisable(GL32.GL_BLEND);
		GL32.glDisable(GL32.GL_SCISSOR_TEST);


		Mat4f perspective = Mat4f.perspective(
				(float) MC_RENDER.getFov(partialTicks),
				MC_RENDER.getTargetFrameBufferViewportWidth() / (float) MC_RENDER.getTargetFrameBufferViewportHeight(),
				RenderUtil.getNearClipPlaneDistanceInBlocks(partialTicks),
				(float) ((RenderUtil.getFarClipPlaneDistanceInBlocks() + LodUtil.REGION_WIDTH) * Math.sqrt(2)));

		ssaoShader.bind();
		ssaoShader.setUniform(ssaoShader.getUniformLocation("gProj"), perspective);
		ssaoShader.setUniform(ssaoShader.getUniformLocation("gSampleRad"), 3.0f);
		ssaoShader.setUniform(ssaoShader.getUniformLocation("gFactor"), 0.8f);
		ssaoShader.setUniform(ssaoShader.getUniformLocation("gPower"), 1.0f);
		va.bind();
		va.bindBufferToAllBindingPoint(boxBuffer.getId());

		GL32.glActiveTexture(GL32.GL_TEXTURE0);
		GL32.glBindTexture(GL32.GL_TEXTURE_2D, MC_RENDER.getDepthTextureId());

		GL32.glUniform3fv(ssaoShader.getUniformLocation("gKernel"), kernel);
		GL32.glUniform1i(ssaoShader.getUniformLocation("gDepthMap"), 0);
		GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, 6);

		applyShader.bind();
		GL32.glEnable(GL11.GL_BLEND);
		GL32.glBlendFunc(GL32.GL_ZERO, GL32.GL_SRC_ALPHA);
		GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, MC_RENDER.getTargetFrameBuffer());
		GL32.glActiveTexture(GL32.GL_TEXTURE0);
		GL32.glBindTexture(GL32.GL_TEXTURE_2D, ssaoTexture);
		GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, 6);

		//GL32.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, ssaoFramebuffer);
		//GL32.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, MC_RENDER.getTargetFrameBuffer());
		//GL32.glBlitFramebuffer(
		//		0, 0, width, height,
		//		0, 0, width, height,
		//		GL11.GL_COLOR_BUFFER_BIT,
		//		GL11.GL_NEAREST
		//);

		state.restore();
	}


}
