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

import com.seibel.distanthorizons.core.render.glObject.shader.ShaderProgram;
import com.seibel.distanthorizons.core.render.renderer.LodRenderer;
import com.seibel.distanthorizons.core.render.renderer.ScreenQuad;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;

/**
 * Copies {@link LodRenderer}'s currently active color and depth texture to Minecraft's framebuffer. 
 */
public class DhApplyShader extends AbstractShaderRenderer
{
	public static DhApplyShader INSTANCE = new DhApplyShader();
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	// uniforms
	public int gDhColorTextureUniform;
	public int gDepthMapUniform;
	
	public int tempFramebufferId;
	public int tempColorTextureId;
	public int tempDepthTextureId;
	
	
	@Override
	public void onInit()
	{
		this.shader = new ShaderProgram(
				"shaders/normal.vert",
				"shaders/apply.frag",
				"fragColor",
				new String[]{"vPosition"});
		
		// uniform setup
		this.gDhColorTextureUniform = this.shader.getUniformLocation("gDhColorTexture");
		this.gDepthMapUniform = this.shader.getUniformLocation("gDhDepthTexture");
		
		this.tempFramebufferId = GL32.glGenFramebuffers();
		this.tempColorTextureId = GL32.glGenTextures();
		this.tempDepthTextureId = GL32.glGenTextures();
		
	}
	
	@Override
	protected void onApplyUniforms(float partialTicks)
	{
		
	}
	
	
	//========//
	// render //
	//========//
	
	private boolean texturesCreated = false;
	
	@Override
	protected void onRender()
	{
		GL32.glDisable(GL32.GL_DEPTH_TEST);
		
		GL32.glEnable(GL32.GL_BLEND);
		GL32.glBlendEquation(GL32.GL_FUNC_ADD);
		GL32.glBlendFunc(GL32.GL_ONE, GL32.GL_ONE_MINUS_SRC_ALPHA);
		
		GL32.glActiveTexture(GL32.GL_TEXTURE0);
		GL32.glBindTexture(GL32.GL_TEXTURE_2D, LodRenderer.getActiveColorTextureId());
		GL32.glUniform1i(this.gDhColorTextureUniform, 0);
		
		GL32.glActiveTexture(GL32.GL_TEXTURE1);
		GL32.glBindTexture(GL32.GL_TEXTURE_2D, LodRenderer.getActiveDepthTextureId());
		GL32.glUniform1i(this.gDepthMapUniform, 1);
		
		// Copy to MC's framebuffer
		GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, MC_RENDER.getTargetFrameBuffer());
		
		ScreenQuad.INSTANCE.render();
		
		//LOGGER.info("apply start");
		//
		//GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, 0);
		//
		//// create the temp textures
		//GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.tempFramebufferId);
		//GL32.glReadBuffer(GL32.GL_COLOR_ATTACHMENT0);
		//
		//LOGGER.info("bind color");
		//
		//GL32.glBindTexture(GL32.GL_TEXTURE_2D, this.tempColorTextureId);
		//if (!texturesCreated)
		//{
		//	GL32.glTexImage2D(GL32.GL_TEXTURE_2D,
		//			0,
		//			GL32.GL_RGB, //RGBA
		//			MC_RENDER.getTargetFrameBufferViewportWidth(), MC_RENDER.getTargetFrameBufferViewportHeight(),
		//			0,
		//			GL32.GL_RGB,
		//			GL32.GL_UNSIGNED_BYTE,
		//			(ByteBuffer) null);
		//	GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_LINEAR);
		//	GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_LINEAR);
		//}
		//GL32.glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D, this.tempColorTextureId, 0);
		//
		//
		//LOGGER.info("bind depth");
		//
		//GL32.glBindTexture(GL32.GL_TEXTURE_2D, this.tempDepthTextureId);
		//if (!texturesCreated)
		//{
		//	GL32.glTexImage2D(GL32.GL_TEXTURE_2D,
		//			0,
		//			GL32.GL_DEPTH_COMPONENT32,
		//			MC_RENDER.getTargetFrameBufferViewportWidth(), MC_RENDER.getTargetFrameBufferViewportHeight(),
		//			0,
		//			GL32.GL_DEPTH_COMPONENT,
		//			GL32.GL_UNSIGNED_BYTE,
		//			(ByteBuffer) null);
		//	GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_LINEAR);
		//	GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_LINEAR);
		//}
		//GL32.glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_DEPTH_ATTACHMENT, GL32.GL_TEXTURE_2D, this.tempDepthTextureId, 0);
		//
		//texturesCreated = true;
		//GL32.glClear(GL32.GL_COLOR_BUFFER_BIT | GL32.GL_DEPTH_BUFFER_BIT);
		//
		//
		//int tempStatus = GL32.glCheckFramebufferStatus(GL32.GL_FRAMEBUFFER);
		//boolean tempComplete = tempStatus == GL32.GL_FRAMEBUFFER_COMPLETE;
		//LOGGER.info("copy mc to temp. " + MC_RENDER.getTargetFrameBuffer() + " -> " + this.tempFramebufferId + " (" + tempStatus + " - " + tempComplete + ")");
		//
		//GL32.glDisable(GL32.GL_MULTISAMPLE);
		//
		//// copy MC to temp
		//GL32.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, MC_RENDER.getTargetFrameBuffer());
		//GL32.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, this.tempFramebufferId);
		//GL32.glBlitFramebuffer(0, 0, MC_RENDER.getTargetFrameBufferViewportWidth(), MC_RENDER.getTargetFrameBufferViewportHeight(),
		//		0, 0, MC_RENDER.getTargetFrameBufferViewportWidth(), MC_RENDER.getTargetFrameBufferViewportHeight(),
		//		GL32.GL_COLOR_BUFFER_BIT | GL32.GL_DEPTH_BUFFER_BIT,
		//		GL32.GL_NEAREST);
		//
		//LOGGER.info("post copy unbind");
		//
		//GL32.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, 0);
		//GL32.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, 0);
		//
		//
		//
		//
		//// write DH to temp
		//LOGGER.info("write dh to temp");
		//
		//GL32.glActiveTexture(GL32.GL_TEXTURE0);
		//GL32.glBindTexture(GL32.GL_TEXTURE_2D, LodRenderer.getActiveColorTextureId());
		//GL32.glUniform1i(this.gDhColorTextureUniform, 0);
		//
		//GL32.glActiveTexture(GL32.GL_TEXTURE1);
		//GL32.glBindTexture(GL32.GL_TEXTURE_2D, LodRenderer.getActiveDepthTextureId());
		//GL32.glUniform1i(this.gDepthMapUniform, 1);
		//
		//GL32.glDisable(GL32.GL_DEPTH_TEST);
		//
		//GL32.glEnable(GL32.GL_BLEND);
		//GL32.glBlendEquation(GL32.GL_FUNC_ADD);
		//GL32.glBlendFunc(GL32.GL_ONE, GL32.GL_ONE_MINUS_SRC_ALPHA);
		//
		//LOGGER.info("bind temp");
		//GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.tempFramebufferId);
		//
		//ScreenQuad.INSTANCE.render();
		//
		//LOGGER.info("unbind temp");
		//GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, 0);
		//
		//
		//
		//// copy temp to MC
		//LOGGER.info("copy temp to MC - bind.");
		//
		//GL32.glFlush();
		//
		//GL32.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, this.tempFramebufferId);
		//GL32.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, MC_RENDER.getTargetFrameBuffer());
		//
		//int mcStatus = GL32.glCheckFramebufferStatus(GL32.GL_DRAW_FRAMEBUFFER);
		//boolean mcComplete = mcStatus == GL32.GL_FRAMEBUFFER_COMPLETE;
		//LOGGER.info("copy temp to MC - blit " + this.tempFramebufferId + " -> " + MC_RENDER.getTargetFrameBuffer() + " (" + mcStatus + " - " + mcComplete + ")");
		//
		//GL32.glBlitFramebuffer(0, 0, MC_RENDER.getTargetFrameBufferViewportWidth(), MC_RENDER.getTargetFrameBufferViewportHeight(),
		//		0, 0, MC_RENDER.getTargetFrameBufferViewportWidth(), MC_RENDER.getTargetFrameBufferViewportHeight(),
		//		GL32.GL_COLOR_BUFFER_BIT,
		//		GL32.GL_NEAREST);
		//
		//GL32.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, 0);
		//GL32.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, 0);
		//
		//GL32.glFlush();
		//GL32.glFinish();
		//
		//GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, LodRenderer.getActiveFramebufferId());
		//
		//LOGGER.info("copy temp to MC - done.");
	}
}
