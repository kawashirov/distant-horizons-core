package com.seibel.distanthorizons.core.render.renderer.shaders;

import com.seibel.distanthorizons.api.enums.config.EGpuUploadMethod;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.render.glObject.GLState;
import com.seibel.distanthorizons.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.distanthorizons.core.render.glObject.shader.ShaderProgram;
import com.seibel.distanthorizons.core.render.glObject.vertexAttribute.VertexAttribute;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SSAORenderer
{
	public static SSAORenderer INSTANCE = new SSAORenderer();
	
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	
	private static final int MAX_KERNEL_SIZE = 32;
	private static final float[] box_vertices = {
			-1, -1,
			1, -1,
			1, 1,
			-1, -1,
			1, 1,
			-1, 1,
	};
	
	
	private ShaderProgram ssaoShader;

	private GLVertexBuffer boxBuffer;
	private VertexAttribute va;
	private boolean init = false;
	
	private float[] kernel = new float[MAX_KERNEL_SIZE * 3];
	
	// ssao uniforms
	private final SsaoShaderUniforms ssaoShaderUniforms = new SsaoShaderUniforms();
	private static class SsaoShaderUniforms
	{
		public int gProjUniform;
		public int gInvProjUniform;
		public int gSampleRadUniform;
		public int gFactorUniform;
		public int gPowerUniform;
		public int gKernelUniform;
		public int gDepthMapUniform;
	}
	
	
	//=============//
	// constructor //
	//=============//
	
	private SSAORenderer() { }
	
	public void init()
	{
		if (this.init) return;
		this.init = true;
		
		
		this.va = VertexAttribute.create();
		this.va.bind();
		
		// Pos
		this.va.setVertexAttribute(0, 0, VertexAttribute.VertexPointer.addVec2Pointer(false));
		this.va.completeAndCheck(Float.BYTES * 2);
		this.ssaoShader = new ShaderProgram("shaders/normal.vert", "shaders/ssao/ao.frag",
				"fragColor", new String[]{"vPosition"});
		
		// SSAO uniform setup
		this.ssaoShaderUniforms.gProjUniform = this.ssaoShader.getUniformLocation("gProj");
		this.ssaoShaderUniforms.gInvProjUniform = this.ssaoShader.getUniformLocation("gInvProj");
		this.ssaoShaderUniforms.gSampleRadUniform = this.ssaoShader.getUniformLocation("gSampleRad");
		this.ssaoShaderUniforms.gFactorUniform = this.ssaoShader.getUniformLocation("gFactor");
		this.ssaoShaderUniforms.gPowerUniform = this.ssaoShader.getUniformLocation("gPower");
		this.ssaoShaderUniforms.gKernelUniform = this.ssaoShader.getUniformLocation("gKernel");
		this.ssaoShaderUniforms.gDepthMapUniform = this.ssaoShader.getUniformLocation("gDepthMap");
		
		// Generate kernel
		this.kernel = genKernel();
		// Framebuffer
		this.createBuffer();
	}
		
	private void createBuffer()
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(box_vertices.length * Float.BYTES);
		buffer.order(ByteOrder.nativeOrder());
		buffer.asFloatBuffer().put(box_vertices);
		buffer.rewind();
		this.boxBuffer = new GLVertexBuffer(false);
		this.boxBuffer.bind();
		this.boxBuffer.uploadBuffer(buffer, box_vertices.length, EGpuUploadMethod.DATA, box_vertices.length * Float.BYTES);
	}
	
	private static float[] genKernel()
	{
		float[] kernel = new float[MAX_KERNEL_SIZE * 3];
		for (int i = 0; i < MAX_KERNEL_SIZE; i++)
		{
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
	
	
	
	//========//
	// render //
	//========//
	
	public void render(float partialTicks)
	{
		GLState state = new GLState();
		
		this.init();

		int width = MC_RENDER.getTargetFrameBufferViewportWidth();
		int height = MC_RENDER.getTargetFrameBufferViewportHeight();
		
		GL32.glViewport(0, 0, width, height);
		GL32.glDisable(GL32.GL_DEPTH_TEST);
		GL32.glEnable(GL11.GL_BLEND);
		GL32.glBlendEquation(GL32.GL_FUNC_ADD);
		GL32.glBlendFuncSeparate(GL32.GL_ZERO, GL32.GL_SRC_ALPHA, GL32.GL_ZERO, GL32.GL_ONE);
		GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, MC_RENDER.getTargetFrameBuffer());
		GL32.glDisable(GL32.GL_SCISSOR_TEST);
		
		Mat4f perspective = Mat4f.perspective(
				(float) MC_RENDER.getFov(partialTicks),
				MC_RENDER.getTargetFrameBufferViewportWidth() / (float) MC_RENDER.getTargetFrameBufferViewportHeight(),
				RenderUtil.getNearClipPlaneDistanceInBlocks(partialTicks),
				(float) ((RenderUtil.getFarClipPlaneDistanceInBlocks() + LodUtil.REGION_WIDTH) * Math.sqrt(2)));
		
		Mat4f invertedPerspective = new Mat4f(perspective);
		invertedPerspective.invert();
		
		this.ssaoShader.bind();
		this.ssaoShader.setUniform(this.ssaoShaderUniforms.gProjUniform, perspective);
		this.ssaoShader.setUniform(this.ssaoShaderUniforms.gInvProjUniform, invertedPerspective);
		this.ssaoShader.setUniform(this.ssaoShaderUniforms.gSampleRadUniform, 3.0f);
		this.ssaoShader.setUniform(this.ssaoShaderUniforms.gFactorUniform, 0.7f);
		this.ssaoShader.setUniform(this.ssaoShaderUniforms.gPowerUniform, 1.5f);
		
		this.va.bind();
		this.va.bindBufferToAllBindingPoint(this.boxBuffer.getId());
		
		GL32.glActiveTexture(GL32.GL_TEXTURE0);
		GL32.glBindTexture(GL32.GL_TEXTURE_2D, MC_RENDER.getDepthTextureId());
		
		GL32.glUniform3fv(this.ssaoShaderUniforms.gKernelUniform, this.kernel);
		GL32.glUniform1i(this.ssaoShaderUniforms.gDepthMapUniform, 0);
		GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, 6);
		
		
		state.restore();
	}
	
	public void free()
	{
		this.ssaoShader.free();
	}
}
