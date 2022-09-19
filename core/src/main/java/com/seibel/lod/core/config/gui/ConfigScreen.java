package com.seibel.lod.core.config.gui;

import com.seibel.lod.api.enums.config.EGpuUploadMethod;
import com.seibel.lod.core.render.glObject.GLProxy;
import com.seibel.lod.core.render.glObject.GLState;
import com.seibel.lod.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.lod.core.render.glObject.shader.ShaderProgram;
import com.seibel.lod.core.render.glObject.vertexAttribute.VertexAttribute;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author coolGi
 */
public class ConfigScreen extends AbstractScreen {
    ShaderProgram basicShader;
    GLVertexBuffer sameContextBuffer;
    GLVertexBuffer sharedContextBuffer;
    VertexAttribute va;

    @Override
    public void init() {
        System.out.println("init");

        va = VertexAttribute.create();
        va.bind();
        // Pos
        va.setVertexAttribute(0, 0, VertexAttribute.VertexPointer.addVec2Pointer(false));
        // Color
        va.setVertexAttribute(0, 1, VertexAttribute.VertexPointer.addVec4Pointer(false));
        va.completeAndCheck(Float.BYTES * 6);
        basicShader = new ShaderProgram("shaders/test/vert.vert", "shaders/test/frag.frag",
                "fragColor", new String[]{"vPosition", "color"});
        createBuffer();
    }

    // Render a square with uv color
    private static final float[] vertices = {
            // PosX,Y, ColorR,G,B,A
            -0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f,
            0.4f, -0.4f, 1.0f, 0.0f, 0.0f, 1.0f,
            0.3f, 0.3f, 1.0f, 1.0f, 0.0f, 0.0f,
            -0.2f, 0.2f, 0.0f, 1.0f, 1.0f, 1.0f
    };

    private static GLVertexBuffer createTextingBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(vertices.length * Float.BYTES);
        // Fill buffer with the vertices.
        buffer = buffer.order(ByteOrder.nativeOrder());
        buffer.asFloatBuffer().put(vertices);
        buffer.rewind();
        GLVertexBuffer vbo = new GLVertexBuffer(false);
        vbo.bind();
        vbo.uploadBuffer(buffer, 4, EGpuUploadMethod.DATA, vertices.length * Float.BYTES);
        return vbo;
    }

    private void createBuffer() {
        GLProxy.getInstance().recordOpenGlCall(() -> sharedContextBuffer = createTextingBuffer());
        GLProxy.ensureAllGLJobCompleted();
        sameContextBuffer = createTextingBuffer();
    }

    @Override
    public void render(float delta) {
        System.out.println("Updated config screen with the delta of " + delta);

        GLState state = new GLState();
//        GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, MC_RENDER.getTargetFrameBuffer());
        GL32.glViewport(0,0, width, height);
        GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_FILL);
        GL32.glDisable(GL32.GL_CULL_FACE);
        GL32.glDisable(GL32.GL_DEPTH_TEST);
        GL32.glDisable(GL32.GL_STENCIL_TEST);
        GL32.glDisable(GL32.GL_BLEND);
        //GL32.glDisable(GL32.GL_SCISSOR_TEST);

        basicShader.bind();
        va.bind();

        // Switch between the two buffers per second
        if (System.currentTimeMillis() % 2000 < 1000) {
            sameContextBuffer.bind();
            va.bindBufferToAllBindingPoint(sameContextBuffer.getId());
        } else {
            sameContextBuffer.bind();
            va.bindBufferToAllBindingPoint(sharedContextBuffer.getId());
        }
        // Render the square
        GL32.glDrawArrays(GL32.GL_TRIANGLE_FAN, 0, 4);
        GL32.glClear(GL32.GL_DEPTH_BUFFER_BIT);

        state.restore();
    }

    @Override
    public void tick() {
        System.out.println("Ticked");
    }
}
