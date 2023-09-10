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
import org.lwjgl.opengl.GL32;

public class SSAOApplyShader extends AbstractShaderRenderer
{
	public static SSAOApplyShader INSTANCE = new SSAOApplyShader();
	
	
	// apply uniforms
	private final ApplyShaderUniforms applyShaderUniforms = new ApplyShaderUniforms();
	
	private static class ApplyShaderUniforms
	{
		public int gSSAOMapUniform;
		public int gDepthMapUniform;
		public int gViewSizeUniform;
		public int gBlurRadiusUniform;
		public int gNearUniform;
		public int gFarUniform;
	}
	
	
	@Override
	public void init()
	{
		super.init();
		
		this.shader = new ShaderProgram(
				"shaders/normal.vert",
				"shaders/ssao/apply.frag",
				"fragColor",
				new String[]{"vPosition"});
		
		// uniform setup
		this.applyShaderUniforms.gSSAOMapUniform = this.shader.getUniformLocation("gSSAOMap");
		this.applyShaderUniforms.gDepthMapUniform = this.shader.getUniformLocation("gDepthMap");
		this.applyShaderUniforms.gViewSizeUniform = this.shader.tryGetUniformLocation("gViewSize");
		this.applyShaderUniforms.gBlurRadiusUniform = this.shader.tryGetUniformLocation("gBlurRadius");
		this.applyShaderUniforms.gNearUniform = this.shader.tryGetUniformLocation("gNear");
		this.applyShaderUniforms.gFarUniform = this.shader.tryGetUniformLocation("gFar");
	}
	
	private void setShaderUniforms(float partialTicks, int ssaoTexture)
	{
		GL32.glActiveTexture(GL32.GL_TEXTURE0);
		GL32.glBindTexture(GL32.GL_TEXTURE_2D, MC_RENDER.getDepthTextureId());
		GL32.glUniform1i(this.applyShaderUniforms.gDepthMapUniform, 0);
		
		GL32.glActiveTexture(GL32.GL_TEXTURE1);
		GL32.glBindTexture(GL32.GL_TEXTURE_2D, ssaoTexture);
		GL32.glUniform1i(this.applyShaderUniforms.gSSAOMapUniform, 1);
		
		int blurRadius = Config.Client.Advanced.Graphics.Ssao.blurRadius.get();
		GL32.glUniform1i(this.applyShaderUniforms.gBlurRadiusUniform, blurRadius);
		
		if (this.applyShaderUniforms.gViewSizeUniform >= 0)
		{
			int width = MC_RENDER.getTargetFrameBufferViewportWidth();
			int height = MC_RENDER.getTargetFrameBufferViewportHeight();
			GL32.glUniform2f(this.applyShaderUniforms.gViewSizeUniform, width, height);
		}
		
		if (this.applyShaderUniforms.gNearUniform >= 0)
		{
			float near = RenderUtil.getNearClipPlaneDistanceInBlocks(partialTicks);
			GL32.glUniform1f(this.applyShaderUniforms.gNearUniform, near);
		}
		
		if (this.applyShaderUniforms.gFarUniform >= 0)
		{
			float far = (float) ((RenderUtil.getFarClipPlaneDistanceInBlocks() + LodUtil.REGION_WIDTH) * Math.sqrt(2));
			GL32.glUniform1f(this.applyShaderUniforms.gFarUniform, far);
		}
	}
	
	//========//
	// render //
	//========//
	
	public void render(float partialTicks, int ssaoTexture)
	{
		this.init();
		
		this.shader.bind();
		
		setShaderUniforms(partialTicks, ssaoTexture);
		
		GL32.glEnable(GL32.GL_BLEND);
		GL32.glBlendEquation(GL32.GL_FUNC_ADD);
		GL32.glBlendFuncSeparate(GL32.GL_ZERO, GL32.GL_SRC_ALPHA, GL32.GL_ZERO, GL32.GL_ONE);
		
		ScreenQuad.INSTANCE.render();
	}
}
