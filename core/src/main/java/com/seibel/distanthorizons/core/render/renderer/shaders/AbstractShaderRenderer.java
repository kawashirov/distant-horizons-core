package com.seibel.distanthorizons.core.render.renderer.shaders;

import com.seibel.distanthorizons.api.enums.config.EGpuUploadMethod;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.render.glObject.GLState;
import com.seibel.distanthorizons.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.distanthorizons.core.render.glObject.shader.ShaderProgram;
import com.seibel.distanthorizons.core.render.glObject.vertexAttribute.VertexAttribute;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class AbstractShaderRenderer {
    protected static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
    protected static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);

    private static final float[] box_vertices = {
            -1, -1,
            1, -1,
            1, 1,
            -1, -1,
            1, 1,
            -1, 1,
    };

    protected final ShaderProgram shader;
    protected final ShaderProgram applyShader;
    protected GLVertexBuffer boxBuffer;
    protected VertexAttribute va;
	boolean init = false;

    private int width = -1;
    private int height = -1;
    private int framebuffer = -1;
    private int shaderTexture = -1;


    protected AbstractShaderRenderer(ShaderProgram shader) {
        this(shader, null);
    }

    protected AbstractShaderRenderer(ShaderProgram shader, ShaderProgram applyShader) {
        this.shader = shader;
        this.applyShader = applyShader;


    }
    private void init() {
		if (init) return;
		init = true;

        va = VertexAttribute.create();
        va.bind();
        // Pos
        va.setVertexAttribute(0, 0, VertexAttribute.VertexPointer.addVec2Pointer(false));
        va.completeAndCheck(Float.BYTES * 2);

        // Some shader stuff needs to be set a bit later than
        this.postInit();
        // Framebuffer
        createBuffer();
    }

    /** Overwrite this to apply uniforms to the shader */
    void setShaderUniforms(float partialTicks) {};
    /** Overwrite this to apply uniforms to the apply shader */
    void setApplyShaderUniforms(float partialTicks) {};
    /** Overwrite if you need to run something on runtime */
    void postInit() {};

    public void render(float partialTicks) {
        GLState state = new GLState();
		init();
        int width = MC_RENDER.getTargetFrameBufferViewportWidth();
        int height = MC_RENDER.getTargetFrameBufferViewportHeight();

        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            createFramebuffer(width, height);
        }



        GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, framebuffer);
        GL32.glViewport(0, 0, width, height);
        GL32.glDisable(GL32.GL_DEPTH_TEST);
        GL32.glDisable(GL32.GL_BLEND);
        GL32.glDisable(GL32.GL_SCISSOR_TEST);


        shader.bind();
        this.setShaderUniforms(partialTicks);
        va.bind();
        va.bindBufferToAllBindingPoint(boxBuffer.getId());

        GL32.glActiveTexture(GL32.GL_TEXTURE0);
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, MC_RENDER.getDepthTextureId());

        GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, 6);

        if (applyShader != null) {
            applyShader.bind();
            this.setApplyShaderUniforms(partialTicks);
        }
        GL32.glEnable(GL11.GL_BLEND);
        GL32.glBlendFunc(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA);
        GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, MC_RENDER.getTargetFrameBuffer());
        GL32.glActiveTexture(GL32.GL_TEXTURE0);
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, shaderTexture);
        GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, 6);



        state.restore();
    }


    private void createFramebuffer(int width, int height) {
        if (framebuffer != -1) {
            GL32.glDeleteFramebuffers(framebuffer);
            framebuffer = -1;
        }

        if (shaderTexture != -1) {
            GL32.glDeleteTextures(shaderTexture);
            shaderTexture = -1;
        }

        framebuffer = GL32.glGenFramebuffers();
        GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, framebuffer);

        shaderTexture = GL32.glGenTextures();
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, shaderTexture);
        GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, GL32.GL_RED, width, height, 0, GL32.GL_RED, GL32.GL_FLOAT, (ByteBuffer) null);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
        GL32.glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D, shaderTexture, 0);
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

    public void free() {
        this.shader.free();
        if (this.applyShader != null)
            this.applyShader.free();
    }
}