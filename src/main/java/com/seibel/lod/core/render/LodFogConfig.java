/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.handlers.IReflectionHandler;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.render.objects.Shader;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.seibel.lod.core.render.GLProxy.GL_LOGGER;

/**
 * This holds fog related settings and
 * creates the fog related shader code.
 *
 * @author Leetom
 * @author James Seibel
 * @version 2022-4-14
 */
public class LodFogConfig
{
	private static final IReflectionHandler REFLECTION_HANDLER = SingletonHandler.get(IReflectionHandler.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	public static final boolean DEBUG_DUMP_GENERATED_CODE = false;
	
	public final FogSetting farFogSetting;
	public final FogSetting heightFogSetting;
	public final HeightFogMixMode heightFogMixMode;
	public final HeightFogMode heightFogMode;
	public final float heightFogHeight;
	
	final boolean drawNearFog;

	final int earthCurveRatio; // FIXME: Move this out of here

	
	public static LodFogConfig generateFogConfig()
	{
		FogDrawMode fogMode = CONFIG.client().graphics().fogQuality().getFogDrawMode();
		if (fogMode == FogDrawMode.USE_OPTIFINE_SETTING)
			fogMode = REFLECTION_HANDLER.getFogDrawMode();
		
		return new LodFogConfig(fogMode);
	}
	
	/** sets all fog options from the config */
	private LodFogConfig(FogDrawMode fogDrawMode)
	{
		earthCurveRatio = CONFIG.client().graphics().advancedGraphics().getEarthCurveRatio(); //FIXME: Move this out of here

		if (fogDrawMode != FogDrawMode.FOG_DISABLED)
		{
			ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality fogSettings = CONFIG.client().graphics().fogQuality();
			
			FogDistance fogDistance = fogSettings.getFogDistance();
			drawNearFog = (fogDistance == FogDistance.NEAR || fogDistance == FogDistance.NEAR_AND_FAR);
			
			if (fogDistance == FogDistance.FAR || fogDistance == FogDistance.NEAR_AND_FAR)
			{
				// far fog should be drawn
				
				farFogSetting = fogSettings.advancedFog().computeFarFogSetting();
				
				heightFogMixMode = fogSettings.advancedFog().heightFog().getHeightFogMixMode();
				if (heightFogMixMode == HeightFogMixMode.IGNORE_HEIGHT || heightFogMixMode == HeightFogMixMode.BASIC)
				{
					// basic fog mixing
					
					heightFogSetting = null;
					heightFogMode = null;
					heightFogHeight = 0.f;
				}
				else
				{
					// advanced fog mixing
					
					heightFogSetting = fogSettings.advancedFog().heightFog().computeHeightFogSetting();
					heightFogMode = fogSettings.advancedFog().heightFog().getHeightFogMode();
					
					if (heightFogMode.basedOnCamera)
					{
						heightFogHeight = 0.f;
					}
					else
					{
						heightFogHeight = (float) fogSettings.advancedFog().heightFog().getHeightFogHeight();
					}
				}
			}
			else
			{
				// far fog should not be drawn
				
				farFogSetting = null;
				heightFogSetting = null;
				heightFogMode = null;
				heightFogMixMode = null;
				heightFogHeight = 0.f;
			}
		}
		else
		{
			// fog disabled
			
			drawNearFog = false;
			farFogSetting = null;
			heightFogMixMode = null;
			heightFogMode = null;
			heightFogSetting = null;
			heightFogHeight = 0.f;
		}
	}
	
	public StringBuilder loadAndProcessFragShader(String path, boolean absoluteFilePath)
	{
		StringBuilder str = makeRuntimeDefine();
		generateRuntimeShaderCode(Shader.loadFile(path, absoluteFilePath, str));
		if (DEBUG_DUMP_GENERATED_CODE)
		{
			try (FileOutputStream file = new FileOutputStream("debugGenerated.frag", false))
			{
				file.write(str.toString().getBytes(StandardCharsets.UTF_8));
				GL_LOGGER.info("Debug dumped generated code to debugGenerated.frag for {}", path);
			}
			catch (IOException e)
			{
				GL_LOGGER.warn("Failed to debug dump generated code to file for {}", path);
			}
		}
		return str;
	}
	
	/** Generates the necessary constants for a fragment shader */
	private void generateRuntimeShaderCode(StringBuilder str)
	{
		str.append("// =======RUNTIME GENERATED CODE SECTION======== //\n");
		
		// Generate method: float getNearFogThickness(float dist);
		str.append("" +
			"float getNearFogThickness(float dist) \n" +
			"{ \n" +
			"	return linearFog(dist, nearFogStart, nearFogLength, 0.0, 1.0); \n" +
			"} \n");
		
		
		if (farFogSetting == null)
		{
			str.append("\n" +
				"float getFarFogThickness(float dist) { return 0.0; } \n" +
				"float getHeightFogThickness(float dist) { return 0.0; } \n" +
				"float calculateFarFogDepth(float horizontal, float dist, float nearFogStart) { return 0.0; } \n" +
				"float calculateHeightFogDepth(float vertical, float realY) { return 0.0; } \n" +
				"float mixFogThickness(float near, float far, float height) \n" +
				"{ \n" +
					(drawNearFog ? "return 1.0-near;" : "return 0.0;") +
				"} \n\n");
		}
		else
		{
			// Generate method: float getFarFogThickness(float dist);
			str.append("" +
				"float getFarFogThickness(float dist) \n" +
				"{ \n" +
					getFarFogMethod(farFogSetting.fogType) + "\n" +
				"} \n");
			
			
			// Generate method: float getHeightFogThickness(float dist);
			str.append("" +
				"float getHeightFogThickness(float dist) \n" +
				"{ \n"+
					(heightFogSetting != null ? getHeightFogMethod(heightFogSetting.fogType) : "	return 0.0;") + "\n" +
				"} \n");
			
			
			// Generate method: float calculateHeightFogDepth(float vertical, float realY);
			str.append("" +
				"float calculateHeightFogDepth(float vertical, float realY) \n" +
				"{ \n" +
					(heightFogSetting != null ? getHeightDepthMethod(heightFogMode, heightFogHeight) : "	return 0.0;") + "\n" +
				"} \n");
			
			
			// Generate method: calculateFarFogDepth(float horizontal, float dist, float nearFogStart);
			str.append("" +
				"float calculateFarFogDepth(float horizontal, float dist, float nearFogStart) \n" +
				"{ \n" +
				"	return " + (heightFogMixMode == HeightFogMixMode.BASIC ?
								"(dist - nearFogStart)/(1.0 - nearFogStart);" :
								"(horizontal - nearFogStart)/(1.0 - nearFogStart);") +
				"} \n");
			
			// Generate method: float mixFogThickness(float near, float far, float height);
			str.append("" +
				"float mixFogThickness(float near, float far, float height) \n" +
				"{ \n" +
					getMixFogLine(heightFogMixMode, drawNearFog) + "\n" +
				"} \n");
		}
	}
	
	
	
	//=================//
	// shader creation //
	// helper methods  //
	//=================//
	
	private StringBuilder makeRuntimeDefine()
	{
		StringBuilder str = new StringBuilder();
		str.append("// =======RUNTIME GENERATED DEFINE SECTION======== //\n");
		str.append("#version 150 core\n");
		
		FogSetting activeFarFogSetting = this.farFogSetting != null ? this.farFogSetting : FogSetting.EMPTY;
		FogSetting activeHeightFogSetting = this.heightFogSetting != null ? this.heightFogSetting : FogSetting.EMPTY;
		
		str.append("\n" +
			"#define farFogStart " + activeFarFogSetting.start + "\n" +
			"#define farFogLength " + (activeFarFogSetting.end - activeFarFogSetting.start) + "\n" +
			"#define farFogMin " + activeFarFogSetting.min + "\n" +
			"#define farFogRange " + (activeFarFogSetting.max - activeFarFogSetting.min) + "\n" +
			"#define farFogDensity " + activeFarFogSetting.density + "\n" +
			"\n" +
			"#define heightFogStart " + activeHeightFogSetting.start + "\n" +
			"#define heightFogLength " + (activeHeightFogSetting.end - activeHeightFogSetting.start) + "\n" +
			"#define heightFogMin " + activeHeightFogSetting.min + "\n" +
			"#define heightFogRange " + (activeHeightFogSetting.max - activeHeightFogSetting.min) + "\n" +
			"#define heightFogDensity " + activeHeightFogSetting.density + "\n" +
			"\n");
		
		str.append("// =======RUNTIME END======== //\n");
		return str;
	}
	
	private static String getFarFogMethod(FogSetting.FogType fogType)
	{
		switch (fogType)
		{
		case LINEAR:
			return "return linearFog(dist, farFogStart, farFogLength, farFogMin, farFogRange);\n";
		case EXPONENTIAL:
			return "return exponentialFog(dist, farFogStart, farFogLength, farFogMin, farFogRange, farFogDensity);\n";
		case EXPONENTIAL_SQUARED:
			return "return exponentialSquaredFog(dist, farFogStart, farFogLength, farFogMin, farFogRange, farFogDensity);\n";
		
		default:
			throw new IllegalArgumentException("FogType [" + fogType + "] not implemented for [getFarFogMethod].");
		}
	}
	
	private static String getHeightDepthMethod(HeightFogMode heightMode, float heightFogHeight)
	{
		String str = "";
		if (!heightMode.basedOnCamera)
		{
			str =  "	vertical = realY - (" + heightFogHeight + ");\n";
		}
		
		if (heightMode.below && heightMode.above)
		{
			str += "	return abs(vertical);\n";
		}
		else if (heightMode.below)
		{
			str += "	return -vertical;\n";
		}
		else if (heightMode.above)
		{
			str += "	return vertical;\n";
		}
		else
		{
			str += "	return 0;\n";
		}
		return str;
	}
	
	/**
	 * Returns the method call for the given fog type. <br>
	 * Example: <br>
	 * <code>"	return linearFog(dist, heightFogStart, heightFogLength, heightFogMin, heightFogRange);"</code>
	 */
	private static String getHeightFogMethod(FogSetting.FogType fogType)
	{
		switch (fogType)
		{
		case LINEAR:
			return "	return linearFog(dist, heightFogStart, heightFogLength, heightFogMin, heightFogRange);\n";
		case EXPONENTIAL:
			return "	return exponentialFog(dist, heightFogStart, heightFogLength, heightFogMin, heightFogRange, heightFogDensity);\n";
		case EXPONENTIAL_SQUARED:
			return "	return exponentialSquaredFog(dist, heightFogStart, heightFogLength, heightFogMin, heightFogRange, heightFogDensity);\n";
		
		default:
			throw new IllegalArgumentException("FogType [" + fogType + "] not implemented for [getHeightFogMethod].");
		}
	}
	
	/**
	 * creates a line in the format <br>
	 * <code>"	return max(1.0-near, far);" </code>
	 */
	private static String getMixFogLine(HeightFogMixMode heightFogMode, boolean drawNearFog)
	{
		String str = "	return ";
		
		switch (heightFogMode)
		{
		case BASIC:
		case IGNORE_HEIGHT:
			if (drawNearFog)
				str += "max(1.0-near, far);\n";
			else
				str += "near * far;\n";
			break;
		
		case ADDITION:
			if (drawNearFog)
				str += "max(1.0-near, far + height);\n";
			else
				str += "near * (far + height);\n";
			break;
		
		case MAX:
			if (drawNearFog)
				str += "max(1.0-near, max(far, height));\n";
			else
				str += "near * max(far, height);\n";
			break;
		
		case INVERSE_MULTIPLY:
			if (drawNearFog)
				str += "max(1.0-near, 1.0 - (1.0-far)*(1.0-height));\n";
			else
				str += "near * (1.0 - (1.0-far)*(1.0-height));\n";
			break;
		
		case MULTIPLY:
			if (drawNearFog)
				str += "max(1.0-near, far*height);\n";
			else
				str += "near * far * height;\n";
			break;
		
		case LIMITED_ADDITION:
			if (drawNearFog)
				str += "max(1.0-near, far + max(far, height));\n";
			else
				str += "near * (far + max(far, height));\n";
			break;
		
		case MULTIPLY_ADDITION:
			if (drawNearFog)
				str += "max(1.0-near, far + far*height);\n";
			else
				str += "near * (far + far*height);\n";
			break;
		
		case INVERSE_MULTIPLY_ADDITION:
			if (drawNearFog)
				str += "max(1.0-near, far + 1.0 - (1.0-far)*(1.0-height));\n";
			else
				str += "near * (far + 1.0 - (1.0-far)*(1.0-height));\n";
			break;
		
		case AVERAGE:
			if (drawNearFog)
				str += "max(1.0-near, far*0.5 + height*0.5);\n";
			else
				str += "near * (far*0.5 + height*0.5);\n";
			break;
		
		default:
			throw new IllegalArgumentException("FogType [" + heightFogMode + "] not implemented for [getMixFogMethod].");
		}
		
		return str;
	}
	
	
	
	
	
	
	//========================//
	// default object methods //
	//========================//
	
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		LodFogConfig that = (LodFogConfig) o;
		return Float.compare(that.heightFogHeight, heightFogHeight) == 0 &&
				drawNearFog == that.drawNearFog && Objects.equals(farFogSetting, that.farFogSetting) &&
				Objects.equals(heightFogSetting, that.heightFogSetting) && heightFogMixMode == that.heightFogMixMode &&
				heightFogMode == that.heightFogMode && earthCurveRatio == that.earthCurveRatio;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(farFogSetting, heightFogSetting, heightFogMixMode, heightFogMode, heightFogHeight, drawNearFog, earthCurveRatio);
	}
}
