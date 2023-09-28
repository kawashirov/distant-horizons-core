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

package com.seibel.distanthorizons.core.render.glObject;

import org.lwjgl.opengl.GL32;

public class GLState
{
	private static final int FBO_MAX = 4;
	
	public int program;
	public int vao;
	public int vbo;
	public int ebo;
	public int[] fbo;
	public int depthBuffer;
	public int texture2D;
	/** IE: GL_TEXTURE0, GL_TEXTURE1, etc. */
	public int activeTextureNumber;
	public int texture0;
	public int texture1;
	public boolean blend;
	public int blendEqRGB;
	public int blendEqAlpha;
	public int blendSrcColor;
	public int blendSrcAlpha;
	public int blendDstColor;
	public int blendDstAlpha;
	public boolean depth;
	public boolean writeToDepthBuffer;
	public int depthFunc;
	public boolean stencil;
	public int stencilFunc;
	public int stencilRef;
	public int stencilMask;
	public int[] view;
	public boolean cull;
	public int cullMode;
	public int polyMode;
	
	
	public GLState()
	{
		this.fbo = new int[FBO_MAX];
		
		for (int i = 0; i < FBO_MAX; i++)
		{
			this.fbo[i] = GL32.glGenFramebuffers();
		}
		
		GL32.glEnable(GL32.GL_DEPTH_TEST);
		GL32.glDepthFunc(GL32.GL_LESS);
		GL32.glDepthMask(true);
		
		this.saveState();
	}
	
	public void saveState()
	{
		this.program = GL32.glGetInteger(GL32.GL_CURRENT_PROGRAM);
		this.vao = GL32.glGetInteger(GL32.GL_VERTEX_ARRAY_BINDING);
		this.vbo = GL32.glGetInteger(GL32.GL_ARRAY_BUFFER_BINDING);
		this.ebo = GL32.glGetInteger(GL32.GL_ELEMENT_ARRAY_BUFFER_BINDING);
		
		GL32.glGetIntegerv(GL32.GL_FRAMEBUFFER_BINDING, this.fbo);

		this.texture2D = GL32.glGetInteger(GL32.GL_TEXTURE_BINDING_2D);
		this.texture0 = GL32.glGenTextures();
		this.texture1 = GL32.glGenTextures();

		this.blend = GL32.glIsEnabled(GL32.GL_BLEND);
		this.blendEqRGB = GL32.glGetInteger(GL32.GL_BLEND_EQUATION_RGB);
		this.blendEqAlpha = GL32.glGetInteger(GL32.GL_BLEND_EQUATION_ALPHA);
		this.blendSrcColor = GL32.glGetInteger(GL32.GL_BLEND_SRC_RGB);
		this.blendSrcAlpha = GL32.glGetInteger(GL32.GL_BLEND_SRC_ALPHA);
		this.blendDstColor = GL32.glGetInteger(GL32.GL_BLEND_DST_RGB);
		this.blendDstAlpha = GL32.glGetInteger(GL32.GL_BLEND_DST_ALPHA);
		this.depth = GL32.glIsEnabled(GL32.GL_DEPTH_TEST);
		this.writeToDepthBuffer = GL32.glGetInteger(GL32.GL_DEPTH_WRITEMASK) == GL32.GL_TRUE;
		this.depthFunc = GL32.glGetInteger(GL32.GL_DEPTH_FUNC);
		this.stencil = GL32.glIsEnabled(GL32.GL_STENCIL_TEST);
		this.stencilFunc = GL32.glGetInteger(GL32.GL_STENCIL_FUNC);
		this.stencilRef = GL32.glGetInteger(GL32.GL_STENCIL_REF);
		this.stencilMask = GL32.glGetInteger(GL32.GL_STENCIL_VALUE_MASK);
		this.view = new int[4];
		GL32.glGetIntegerv(GL32.GL_VIEWPORT, this.view);
		this.cull = GL32.glIsEnabled(GL32.GL_CULL_FACE);
		this.cullMode = GL32.glGetInteger(GL32.GL_CULL_FACE_MODE);
		this.polyMode = GL32.glGetInteger(GL32.GL_POLYGON_MODE);
	}
	
	@Override
	public String toString()
	{
		return "GLState{" +
				"program=" + this.program + ", vao=" + this.vao + ", vbo=" + this.vbo + ", ebo=" + this.ebo + ", fbo=" + this.fbo +
				", text=" + GLEnums.getString(this.texture2D) + "@" + this.activeTextureNumber + ", text0=" + GLEnums.getString(this.texture0) +
				", blend=" + this.blend + ", blendMode=" + GLEnums.getString(this.blendSrcColor) + "," + GLEnums.getString(this.blendDstColor) +
				", depth=" + this.depth +
				", depthFunc=" + GLEnums.getString(this.depthFunc) + ", stencil=" + this.stencil + ", stencilFunc=" +
				GLEnums.getString(this.stencilFunc) + ", stencilRef=" + this.stencilRef + ", stencilMask=" + this.stencilMask +
				", view={x:" + this.view[0] + ", y:" + this.view[1] +
				", w:" + this.view[2] + ", h:" + this.view[3] + "}" + ", cull=" + this.cull + ", cullMode="
				+ GLEnums.getString(this.cullMode) + ", polyMode=" + GLEnums.getString(this.polyMode) +
				'}';
	}
	
	public void restoreFrameBuffer()
	{
		GL32.glBindTexture(GL32.GL_TEXTURE_2D, 0);
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, 0);
	}
	
	public void restore(int minecraftFramebufferId)
	{
		this.restoreFrameBuffer();
		
		if (this.blend)
		{
			GL32.glEnable(GL32.GL_BLEND);
		}
		else
		{
			GL32.glDisable(GL32.GL_BLEND);
		}
		
		//GL32.glActiveTexture(GL32.GL_TEXTURE0);
		//GL32.glBindTexture(GL32.GL_TEXTURE_2D, GL32.glIsTexture(this.texture0) ? this.texture0 : 0);
		//
		//GL32.glActiveTexture(GL32.GL_TEXTURE1);
		//GL32.glBindTexture(GL32.GL_TEXTURE_2D, GL32.glIsTexture(this.texture1) ? this.texture1 : 0);

		//GL32.glActiveTexture(this.activeTextureNumber);
		GL32.glBindTexture(GL32.GL_TEXTURE_2D, GL32.glIsTexture(this.texture2D) ? this.texture2D : 0);

		GL32.glBindVertexArray(0);
		GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, minecraftFramebufferId);
		//GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, GL32.glIsBuffer(this.vbo) ? this.vbo : 0);
		//GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, GL32.glIsBuffer(this.ebo) ? this.ebo : 0);
		//GL32.glUseProgram(GL32.glIsProgram(this.program) ? this.program : 0);
		
		//GL32.glDepthMask(this.writeToDepthBuffer);
		//GL32.glBlendFunc(this.blendSrcColor, this.blendDstColor);
		GL32.glBlendEquationSeparate(this.blendEqRGB, this.blendEqAlpha);
		GL32.glBlendFuncSeparate(this.blendSrcColor, this.blendDstColor, this.blendSrcAlpha, this.blendDstAlpha);
		
		if (this.stencil)
		{
			GL32.glEnable(GL32.GL_STENCIL_TEST);
		}
		else
		{
			GL32.glDisable(GL32.GL_STENCIL_TEST);
		}
		GL32.glStencilFunc(this.stencilFunc, this.stencilRef, this.stencilMask);
		
		GL32.glViewport(this.view[0], this.view[1], this.view[2], this.view[3]);
		if (this.cull)
		{
			GL32.glEnable(GL32.GL_CULL_FACE);
		}
		else
		{
			GL32.glDisable(GL32.GL_CULL_FACE);
		}
		GL32.glCullFace(this.cullMode);
		GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, this.polyMode);
	}
	
}
