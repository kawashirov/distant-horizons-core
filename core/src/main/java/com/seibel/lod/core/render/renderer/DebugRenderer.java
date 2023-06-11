package com.seibel.lod.core.render.renderer;

import com.seibel.lod.api.enums.config.EGpuUploadMethod;
import com.seibel.lod.api.enums.config.ELoggerMode;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.logging.ConfigBasedSpamLogger;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.render.glObject.GLState;
import com.seibel.lod.core.render.glObject.buffer.GLElementBuffer;
import com.seibel.lod.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.lod.core.render.glObject.shader.ShaderProgram;
import com.seibel.lod.core.render.glObject.vertexAttribute.VertexAttribute;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.coreapi.util.math.Mat4f;
import com.seibel.lod.coreapi.util.math.Vec3d;
import com.seibel.lod.coreapi.util.math.Vec3f;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL32;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class DebugRenderer {
    public static DebugRenderer INSTANCE = new DebugRenderer();
    public DebugRenderer() {}

    public static final ConfigBasedLogger logger = new ConfigBasedLogger(
            LogManager.getLogger(TestRenderer.class), () -> ELoggerMode.LOG_ALL_TO_CHAT);
    public static final ConfigBasedSpamLogger spamLogger = new ConfigBasedSpamLogger(
            LogManager.getLogger(TestRenderer.class), () -> ELoggerMode.LOG_ALL_TO_CHAT, 1);
    private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);

    // A box from 0,0,0 to 1,1,1
    private static final float[] box_vertices = {
            // Pos x y z
            0, 0, 0,
            1, 0, 0,
            1, 1, 0,
            0, 1, 0,
            0, 0, 1,
            1, 0, 1,
            1, 1, 1,
            0, 1, 1,
    };

    private static final int[] box_outline_indices = {
            0, 1,
            1, 2,
            2, 3,
            3, 0,

            4, 5,
            5, 6,
            6, 7,
            7, 4,

            0, 4,
            1, 5,
            2, 6,
            3, 7,
    };

    ShaderProgram basicShader;
    GLVertexBuffer boxBuffer;
    GLElementBuffer boxOutlineBuffer;
    VertexAttribute va;
    boolean init = false;

    public static void unregister(IDebugRenderable r) {
        if (INSTANCE == null) return;
        INSTANCE.removeRenderer(r);
    }

    private void removeRenderer(IDebugRenderable r) {
        synchronized (renderers) {
            Iterator<WeakReference<IDebugRenderable>> it = renderers.iterator();
            while (it.hasNext()) {
                WeakReference<IDebugRenderable> ref = it.next();
                if (ref.get() == null) {
                    it.remove();
                    continue;
                }
                if (ref.get() == r) {
                    it.remove();
                    return;
                }
            }
        }
    }

    public void init() {
        if (init) return;
        init = true;
        va = VertexAttribute.create();
        va.bind();
        // Pos\
        va.setVertexAttribute(0, 0, VertexAttribute.VertexPointer.addVec3Pointer(false));
        va.completeAndCheck(Float.BYTES * 3);
        basicShader = new ShaderProgram("shaders/debug/vert.vert", "shaders/debug/frag.frag",
                "fragColor", new String[]{"vPosition"});
        createBuffer();
    }

    private void createBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(box_vertices.length * Float.BYTES);
        buffer.order(ByteOrder.nativeOrder());
        buffer.asFloatBuffer().put(box_vertices);
        buffer.rewind();
        boxBuffer = new GLVertexBuffer(false);
        boxBuffer.bind();
        boxBuffer.uploadBuffer(buffer, 8, EGpuUploadMethod.DATA, box_vertices.length * Float.BYTES);

        buffer = ByteBuffer.allocateDirect(box_outline_indices.length * Integer.BYTES);
        buffer.order(ByteOrder.nativeOrder());
        buffer.asIntBuffer().put(box_outline_indices);
        buffer.rewind();
        boxOutlineBuffer = new GLElementBuffer(false);
        boxOutlineBuffer.bind();
        boxOutlineBuffer.uploadBuffer(buffer, EGpuUploadMethod.DATA, box_outline_indices.length * Integer.BYTES, GL32.GL_STATIC_DRAW);
    }

    private final LinkedList<WeakReference<IDebugRenderable>> renderers = new LinkedList<>();

    public void addRenderer(IDebugRenderable r) {
        if (!Config.Client.Advanced.Debugging.debugWireframeRendering.get()) return;
        synchronized (renderers) {
            renderers.add(new WeakReference<>(r));
        }
    }

    public static void register(IDebugRenderable r) {
        if (INSTANCE == null) return;
        INSTANCE.addRenderer(r);
    }

    private Mat4f transform_this_frame;
    private Vec3f camf;

    public void renderBox(DhLodPos pos, float minY, float maxY, float marginPercent, Color color) {
        DhBlockPos2D blockMin = pos.getCornerBlockPos();
        DhBlockPos2D blockMax = blockMin.add(pos.getBlockWidth(), pos.getBlockWidth());
        float edge = pos.getBlockWidth() * marginPercent;
        renderBox(blockMin.x + edge, minY, blockMin.z + edge, blockMax.x - edge, maxY, blockMax.z - edge, color);
    }

    public void renderBox(DhLodPos pos, float minY, float maxY, Color color) {
        renderBox(pos, minY, maxY, 0, color);
    }

    public void renderBox(DhSectionPos sectPos, float minY, float maxY, Color color) {
        renderBox(sectPos.getSectionBBoxPos(), minY, maxY, 0, color);
    }

    public void renderBox(DhSectionPos sectPos, float minY, float maxY, float marginPercent, Color color) {
        renderBox(sectPos.getSectionBBoxPos(), minY, maxY, marginPercent, color);
    }

    public void renderBox(float x, float y, float z, float x2, float y2, float z2, Color color) {
        Vec3f boxPos = new Vec3f(x - camf.x, y - camf.y, z - camf.z);
        Mat4f boxTransform = Mat4f.createTranslateMatrix(boxPos.x, boxPos.y, boxPos.z);
        boxTransform.multiply(Mat4f.createScaleMatrix(x2-x, y2-y, z2-z));
        Mat4f t = transform_this_frame.copy();
        t.multiply(boxTransform);

        basicShader.setUniform(basicShader.getUniformLocation("transform"), t);
        basicShader.setUniform(basicShader.getUniformLocation("uColor"), color);
        GL32.glDrawElements(GL32.GL_LINES, box_outline_indices.length, GL32.GL_UNSIGNED_INT, 0);
    }

    public void render(Mat4f transform) {
        if (!Config.Client.Advanced.Debugging.debugWireframeRendering.get()) return;

        transform_this_frame = transform;
        Vec3d cam = MC_RENDER.getCameraExactPosition();
        camf = new Vec3f((float)cam.x, (float)cam.y, (float)cam.z);

        GLState state = new GLState();
        init();

        GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, MC_RENDER.getTargetFrameBuffer());
        GL32.glViewport(0,0, MC_RENDER.getTargetFrameBufferViewportWidth(), MC_RENDER.getTargetFrameBufferViewportHeight());
        GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_LINE);
        //GL32.glLineWidth(2);
        GL32.glDisable(GL32.GL_DEPTH_TEST);
        GL32.glDisable(GL32.GL_STENCIL_TEST);
        GL32.glDisable(GL32.GL_BLEND);
        GL32.glDisable(GL32.GL_SCISSOR_TEST);

        basicShader.bind();
        va.bind();
        va.bindBufferToAllBindingPoint(boxBuffer.getId());

        boxOutlineBuffer.bind();

        synchronized (renderers)
        {
            Iterator<WeakReference<IDebugRenderable>> it = renderers.iterator();
            while (it.hasNext()) {
                WeakReference<IDebugRenderable> ref = it.next();
                IDebugRenderable r = ref.get();
                if (r == null) {
                    it.remove();
                    continue;
                }
                r.debugRender(this);
            }
        }

        state.restore();
    }


}
