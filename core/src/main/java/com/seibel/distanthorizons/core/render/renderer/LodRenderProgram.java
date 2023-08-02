/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

package com.seibel.distanthorizons.core.render.renderer;

import java.awt.Color;

import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.render.glObject.GLProxy;
import com.seibel.distanthorizons.core.render.glObject.shader.Shader;
import com.seibel.distanthorizons.core.render.glObject.shader.ShaderProgram;
import com.seibel.distanthorizons.core.render.glObject.vertexAttribute.VertexAttribute;
import com.seibel.distanthorizons.core.render.glObject.vertexAttribute.VertexAttributePostGL43;
import com.seibel.distanthorizons.core.render.glObject.vertexAttribute.VertexAttributePreGL43;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.render.fog.LodFogConfig;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import com.seibel.distanthorizons.coreapi.util.math.Vec3f;
import com.seibel.distanthorizons.core.wrapperInterfaces.IVersionConstants;

public class LodRenderProgram extends ShaderProgram
{
	public static final String VERTEX_SHADER_PATH = "shaders/standard.vert";
	public static final String VERTEX_CURVE_SHADER_PATH = "shaders/curve.vert";
	public static final String FRAGMENT_SHADER_PATH = "shaders/flat_shaded.frag";
	private static final IVersionConstants VERSION_CONSTANTS = SingletonInjector.INSTANCE.get(IVersionConstants.class);
	
	public final VertexAttribute vao;

	// Uniforms
	public final int combinedMatUniform;
	public final int modelOffsetUniform;
	public final int worldYOffsetUniform;

	public final int mircoOffsetUniform;

	public final int earthRadiusUniform;

	public final int lightMapUniform;

	// Noise Uniforms
	public final int noiseEnabledUniform;
	public final int noiseStepsUniform;
	public final int noiseIntensityUniform;
	public final int noiseDropoffUniform;

	public final LodFogConfig fogConfig;

	// This will bind  VertexAttribute
	public LodRenderProgram(LodFogConfig fogConfig) {
		super(
				fogConfig.earthCurveRatio!=0 ? VERTEX_CURVE_SHADER_PATH : VERTEX_SHADER_PATH,
				FRAGMENT_SHADER_PATH,
				"fragColor", new String[] { "vPosition", "color" }
		);
		this.fogConfig = fogConfig;

		combinedMatUniform = getUniformLocation("combinedMatrix");
		modelOffsetUniform = getUniformLocation("modelOffset");
		worldYOffsetUniform = tryGetUniformLocation("worldYOffset");
		mircoOffsetUniform = getUniformLocation("mircoOffset");
		earthRadiusUniform = tryGetUniformLocation("earthRadius");

		lightMapUniform = getUniformLocation("lightMap");

		// Noise Uniforms
		noiseEnabledUniform = getUniformLocation("noiseEnabled");
		noiseStepsUniform = getUniformLocation("noiseSteps");
		noiseIntensityUniform = getUniformLocation("noiseIntensity");
		noiseDropoffUniform = getUniformLocation("noiseDropoff");


		// TODO: Add better use of the LODFormat thing
		int vertexByteCount = LodUtil.LOD_VERTEX_FORMAT.getByteSize();
		if (GLProxy.getInstance().VertexAttributeBufferBindingSupported)
			vao = new VertexAttributePostGL43(); // also binds VertexAttribute
		else
			vao = new VertexAttributePreGL43(); // also binds VertexAttribute
		vao.bind();
		// Now a pos+light.
		vao.setVertexAttribute(0, 0, VertexAttribute.VertexPointer.addUnsignedShortsPointer(4, false, true)); // 2+2+2+2
		//vao.setVertexAttribute(0, posAttrib, VertexAttribute.VertexPointer.addVec3Pointer(false)); // 4+4+4
		vao.setVertexAttribute(0, 1, VertexAttribute.VertexPointer.addUnsignedBytesPointer(4, true, false)); // +4
		//vao.setVertexAttribute(0, lightAttrib, VertexAttribute.VertexPointer.addUnsignedBytesPointer(2, false)); // +4 due to how it aligns
		try {
			vao.completeAndCheck(vertexByteCount);
		} catch (RuntimeException e) {
			System.out.println(LodUtil.LOD_VERTEX_FORMAT);
			throw e;
		}

		if (earthRadiusUniform != -1) setUniform(earthRadiusUniform,
				/*6371KM*/ 6371000.0f / fogConfig.earthCurveRatio);

		// Noise Uniforms
		setUniform(noiseEnabledUniform, fogConfig.noiseEnable);
		setUniform(noiseStepsUniform, fogConfig.noiseSteps);
		setUniform(noiseIntensityUniform, fogConfig.noiseIntensity);
		setUniform(noiseDropoffUniform, fogConfig.noiseDropoff);
	}

	// If not usable, return a new LodFogConfig to be constructed
	public LodFogConfig isShaderUsable() {
		LodFogConfig newConfig = LodFogConfig.generateFogConfig();
		if (fogConfig.equals(newConfig)) return null;
		return newConfig;
	}
	
	// Override ShaderProgram.bind()
	public void bind() {
		super.bind();
		vao.bind();
	}
	// Override ShaderProgram.unbind()
	public void unbind() {
		super.unbind();
		vao.unbind();
	}
	
	// Override ShaderProgram.free()
	public void free() {
		vao.free();
		super.free();
	}
	
	public void bindVertexBuffer(int vbo) {
		vao.bindBufferToAllBindingPoint(vbo);
	}
	
	public void unbindVertexBuffer() {
		vao.unbindBuffersFromAllBindingPoint();
	}
	
	public void fillUniformData(Mat4f combinedMatrix, Color fogColor,
								int lightmapBindPoint, int worldHeight, int worldYOffset, int lodDrawDistance,
								int vanillaDrawDistance, boolean fullFogMode) {
        super.bind();
		vanillaDrawDistance += 32; // Give it a 2 chunk boundary for near fog.
		// uniforms
		setUniform(combinedMatUniform, combinedMatrix);
		setUniform(mircoOffsetUniform, 0.01f); // 0.01 block offset

		// setUniform(skyLightUniform, skyLight);
		setUniform(lightMapUniform, lightmapBindPoint);

		if (worldYOffsetUniform != -1) setUniform(worldYOffsetUniform, (float)worldYOffset);
	}

	public void setModelPos(Vec3f modelPos) {
		setUniform(modelOffsetUniform, modelPos);
	}

}
