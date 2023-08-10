package com.seibel.distanthorizons.core.render.renderer.shaders;

import com.seibel.distanthorizons.api.enums.rendering.EFogColorMode;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.render.fog.LodFogConfig;
import com.seibel.distanthorizons.core.render.glObject.shader.Shader;
import com.seibel.distanthorizons.core.render.glObject.shader.ShaderProgram;
import com.seibel.distanthorizons.core.render.glObject.vertexAttribute.VertexAttribute;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.IVersionConstants;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import org.lwjgl.opengl.GL32;

import java.awt.*;

public class FogShader extends AbstractShaderRenderer
{
    public static FogShader INSTANCE = new FogShader(LodFogConfig.generateFogConfig());
    private static final IVersionConstants VERSION_CONSTANTS = SingletonInjector.INSTANCE.get(IVersionConstants.class);


//    public final int modelOffsetUniform;
//    public final int worldYOffsetUniform;

    public final int gModelViewProjectionUniform;
    public final int gDepthMapUniform;

    // Fog Uniforms
    public final int fogColorUniform;
    public final int fogScaleUniform;
    public final int fogVerticalScaleUniform;
    public final int nearFogStartUniform;
    public final int nearFogLengthUniform;;
    public final int fullFogModeUniform;
	
	
	
    public FogShader(LodFogConfig fogConfig) 
    {
        // TODO & Note: This code is a bit jank, so try to make it better later (preferably not using something to process the shader)
        // This code is just a temp fix so that it looks fine for the time being
        // and even with the jank soloution, i cannot get it to work
        super(new ShaderProgram(
                () -> Shader.loadFile("shaders/normal.vert", false, new StringBuilder()).toString(),
                () -> fogConfig.loadAndProcessFragShader("shaders/fog/fog.frag", false).toString(),
                "fragColor", new String[] { "vPosition" }
        ));
		
	    this.gModelViewProjectionUniform = this.shader.getUniformLocation("gMvmProj");
	    this.gDepthMapUniform = this.shader.getUniformLocation("gDepthMap");
        // Fog uniforms
	    this.fogColorUniform = this.shader.getUniformLocation("fogColor");
	    this.fullFogModeUniform = this.shader.getUniformLocation("fullFogMode");
	    this.fogScaleUniform = this.shader.tryGetUniformLocation("fogScale");
	    this.fogVerticalScaleUniform = this.shader.tryGetUniformLocation("fogVerticalScale");
        // near
	    this.nearFogStartUniform = this.shader.tryGetUniformLocation("nearFogStart");
	    this.nearFogLengthUniform = this.shader.tryGetUniformLocation("nearFogLength");
    }

    @Override
    void setVertexAttributes() 
    {
	    this.va.setVertexAttribute(0, 0, VertexAttribute.VertexPointer.addVec2Pointer(false));
    }

    @Override
    void setShaderUniforms(float partialTicks) 
    {
	    this.shader.bind();
		
        int lodDrawDistance = RenderUtil.getFarClipPlaneDistanceInBlocks();
        int vanillaDrawDistance = MC_RENDER.getRenderDistance() * LodUtil.CHUNK_WIDTH;
	    vanillaDrawDistance += 32; // Give it a 2 chunk boundary for near fog.
		

	    // bind the depth buffer
	    // FIXME having this texture bound causes rendering issues
	    GL32.glActiveTexture(GL32.GL_TEXTURE3);
	    GL32.glBindTexture(GL32.GL_TEXTURE_2D, MC_RENDER.getDepthTextureId());
	    GL32.glUniform1i(this.gDepthMapUniform, 3);
	    
		
        // Fog
        this.shader.setUniform(this.fullFogModeUniform, MC_RENDER.isFogStateSpecial() ? 1 : 0);
        this.shader.setUniform(this.fogColorUniform, MC_RENDER.isFogStateSpecial() ? this.getSpecialFogColor(partialTicks) : this.getFogColor(partialTicks));

        float nearFogLen = vanillaDrawDistance * 0.2f / lodDrawDistance;
        float nearFogStart = vanillaDrawDistance * (VERSION_CONSTANTS.isVanillaRenderedChunkSquare() ? (float)Math.sqrt(2.) : 1.f) / lodDrawDistance;
        if (this.nearFogStartUniform != -1) this.shader.setUniform(this.nearFogStartUniform, nearFogStart);
        if (this.nearFogLengthUniform != -1) this.shader.setUniform(this.nearFogLengthUniform, nearFogLen);
        if (this.fogScaleUniform != -1) this.shader.setUniform(this.fogScaleUniform, 1.f/lodDrawDistance);
        if (this.fogVerticalScaleUniform != -1) this.shader.setUniform(this.fogVerticalScaleUniform, 1.f/MC.getWrappedClientWorld().getHeight());
    }



    private Color getFogColor(float partialTicks)
    {
        Color fogColor;

        if (Config.Client.Advanced.Graphics.Fog.colorMode.get() == EFogColorMode.USE_SKY_COLOR)
        {
	        fogColor = MC_RENDER.getSkyColor();
        }
        else
        {
	        fogColor = MC_RENDER.getFogColor(partialTicks);
        }

        return fogColor;
    }
    private Color getSpecialFogColor(float partialTicks) { return MC_RENDER.getSpecialFogColor(partialTicks); }

	public void setModelViewProjectionMatrix(Mat4f combinedModelViewProjectionMatrix)
	{
	    this.shader.bind();
		this.shader.setUniform(this.gModelViewProjectionUniform, combinedModelViewProjectionMatrix);
	    this.shader.unbind();
	}
	
	
}
