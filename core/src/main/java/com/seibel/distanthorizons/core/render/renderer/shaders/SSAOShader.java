/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.core.render.renderer.shaders;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.render.glObject.shader.ShaderProgram;
import com.seibel.distanthorizons.core.render.renderer.ScreenQuad;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

public class SSAOShader extends AbstractShaderRenderer
{
	public static SSAOShader INSTANCE = new SSAOShader();
	
	
	// uniforms
	private final SsaoShaderUniforms ssaoShaderUniforms = new SsaoShaderUniforms();
	
	private static class SsaoShaderUniforms
	{
		public int gProjUniform;
		public int gInvProjUniform;
		public int gSampleCountUniform;
		public int gRadiusUniform;
		public int gStrengthUniform;
		public int gMinLightUniform;
		public int gBiasUniform;
		public int gDepthMapUniform;
	}

	
	@Override
	public void init()
	{
		super.init();
				
		this.shader = new ShaderProgram("shaders/normal.vert", "shaders/ssao/ao.frag",
				"fragColor", new String[]{"vPosition"});
		
		// uniform setup
		this.ssaoShaderUniforms.gProjUniform = this.shader.getUniformLocation("gProj");
		this.ssaoShaderUniforms.gInvProjUniform = this.shader.getUniformLocation("gInvProj");
		this.ssaoShaderUniforms.gSampleCountUniform = this.shader.getUniformLocation("gSampleCount");
		this.ssaoShaderUniforms.gRadiusUniform = this.shader.getUniformLocation("gRadius");
		this.ssaoShaderUniforms.gStrengthUniform = this.shader.getUniformLocation("gStrength");
		this.ssaoShaderUniforms.gMinLightUniform = this.shader.getUniformLocation("gMinLight");
		this.ssaoShaderUniforms.gBiasUniform = this.shader.getUniformLocation("gBias");
		this.ssaoShaderUniforms.gDepthMapUniform = this.shader.getUniformLocation("gDepthMap");
	}
	
	void setShaderUniforms(float partialTicks)
	{
		int width = MC_RENDER.getTargetFrameBufferViewportWidth();
		int height = MC_RENDER.getTargetFrameBufferViewportHeight();
		float near = RenderUtil.getNearClipPlaneDistanceInBlocks(partialTicks);
		float far = (float) ((RenderUtil.getFarClipPlaneDistanceInBlocks() + LodUtil.REGION_WIDTH) * Math.sqrt(2));
		
		Mat4f perspective = Mat4f.perspective(
				(float) MC_RENDER.getFov(partialTicks),
				width / (float) height,
				near, far);
		
		Mat4f invertedPerspective = new Mat4f(perspective);
		invertedPerspective.invert();
		
		this.shader.setUniform(this.ssaoShaderUniforms.gProjUniform, perspective);
		
		this.shader.setUniform(this.ssaoShaderUniforms.gInvProjUniform, invertedPerspective);
		
		this.shader.setUniform(this.ssaoShaderUniforms.gSampleCountUniform,
				Config.Client.Advanced.Graphics.Ssao.sampleCount.get());
		
		this.shader.setUniform(this.ssaoShaderUniforms.gRadiusUniform,
				Config.Client.Advanced.Graphics.Ssao.radius.get().floatValue());
		
		this.shader.setUniform(this.ssaoShaderUniforms.gStrengthUniform,
				Config.Client.Advanced.Graphics.Ssao.strength.get().floatValue());
		
		this.shader.setUniform(this.ssaoShaderUniforms.gMinLightUniform,
				Config.Client.Advanced.Graphics.Ssao.minLight.get().floatValue());
		
		this.shader.setUniform(this.ssaoShaderUniforms.gBiasUniform,
				Config.Client.Advanced.Graphics.Ssao.bias.get().floatValue());
		
		GL32.glActiveTexture(GL32.GL_TEXTURE0);
		GL32.glBindTexture(GL32.GL_TEXTURE_2D, MC_RENDER.getDepthTextureId());
		
		GL32.glUniform1i(this.ssaoShaderUniforms.gDepthMapUniform, 0);
	}
	
	
	//========//
	// render //
	//========//
	
	public void render(float partialTicks, int ssaoFramebuffer)
	{
		this.init();

		int width = MC_RENDER.getTargetFrameBufferViewportWidth();
		int height = MC_RENDER.getTargetFrameBufferViewportHeight();
		
		this.shader.bind();
		
		setShaderUniforms(partialTicks);
		
		GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, ssaoFramebuffer);
		GL32.glViewport(0, 0, width, height);
		GL32.glDisable(GL32.GL_SCISSOR_TEST);
		GL32.glDisable(GL32.GL_DEPTH_TEST);
		GL32.glDisable(GL11.GL_BLEND);
		
		ScreenQuad.INSTANCE.render();
		
		shader.unbind();
	}
}
