package com.seibel.lod.core.render.glObject.buffer;

import com.seibel.lod.api.items.enums.config.EGpuUploadMethod;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.render.glObject.GLProxy;
import com.seibel.lod.core.render.glObject.GLEnums;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL32;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class QuadElementBuffer extends GLElementBuffer
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
    
    
    public QuadElementBuffer() {
        super(GLProxy.getInstance().bufferStorageSupported);
    }

    public int getCapacity() {
        return super.getSize() / GLEnums.getTypeSize(getType());
    }

    private static void buildBufferByte(int quadCount, ByteBuffer buffer) {
        for (int i = 0; i < quadCount; i++) {
            int vIndex = i * 4;
            // First triangle
            buffer.put((byte) (vIndex));
            buffer.put((byte) (vIndex + 1));
            buffer.put((byte) (vIndex + 2));
            // Second triangle
            buffer.put((byte) (vIndex + 2));
            buffer.put((byte) (vIndex + 3));
            buffer.put((byte) (vIndex));
        }
        if (buffer.hasRemaining()) {
            throw new IllegalStateException("QuadElementBuffer is not full somehow after building");
        }
        buffer.rewind();
    }
    private static void buildBufferShort(int quadCount, ByteBuffer buffer) {
        for (int i = 0; i < quadCount; i++) {
            int vIndex = i * 4;
            // First triangle
            buffer.putShort((short) (vIndex));
            buffer.putShort((short) (vIndex + 1));
            buffer.putShort((short) (vIndex + 2));
            // Second triangle
            buffer.putShort((short) (vIndex + 2));
            buffer.putShort((short) (vIndex + 3));
            buffer.putShort((short) (vIndex));
        }
        if (buffer.hasRemaining()) {
            throw new IllegalStateException("QuadElementBuffer is not full somehow after building");
        }
        buffer.rewind();
    }
    private static void buildBufferInt(int quadCount, ByteBuffer buffer) {
        for (int i = 0; i < quadCount; i++) {
            int vIndex = i * 4;
            // First triangle
            buffer.putInt(vIndex);
            buffer.putInt(vIndex + 1);
            buffer.putInt(vIndex + 2);
            // Second triangle
            buffer.putInt(vIndex + 2);
            buffer.putInt(vIndex + 3);
            buffer.putInt(vIndex);
        }
        if (buffer.hasRemaining()) {
            throw new IllegalStateException("QuadElementBuffer is not full somehow after building");
        }
        buffer.rewind();
    }

    private static void buildBuffer(int quadCount, ByteBuffer buffer, int type) {
        switch (type) {
            case GL32.GL_UNSIGNED_BYTE:
                buildBufferByte(quadCount, buffer);
                break;
            case GL32.GL_UNSIGNED_SHORT:
                buildBufferShort(quadCount, buffer);
                break;
            case GL32.GL_UNSIGNED_INT:
                buildBufferInt(quadCount, buffer);
                break;
            default:
                throw new IllegalStateException("Unknown type: " + type);
        }
    }

    public void reserve(int quadCount) {
        if (quadCount < 0) {
            throw new IllegalArgumentException("quadCount must be greater than 0");
        }
        if (quadCount == 0) return; // FIXME: This doesn't happens yet, but just add this since everything will break if it does
        indicesCount = quadCount * 6; // 2 triangles per quad
        if (indicesCount >= getCapacity() && indicesCount < getCapacity() * BUFFER_SHRINK_TRIGGER) {
            return;
        }
        int vertexCount = quadCount * 4; // 4 vertices per quad
        GLProxy gl = GLProxy.getInstance();

        if (vertexCount < 255) { // Reserve 1 for the reset index
            type = GL32.GL_UNSIGNED_BYTE;
        } else if (vertexCount < 65535) {  // Reserve 1 for the reset index
            type = GL32.GL_UNSIGNED_SHORT;
        } else {
            type = GL32.GL_UNSIGNED_INT;
        }
        LOGGER.info("Quad IBO Resizing from [" + getCapacity() + "] to [" + quadCount + "]" + " with type: " +
                GLEnums.getString(type));

        ByteBuffer buffer = ByteBuffer.allocateDirect(indicesCount * GLEnums.getTypeSize(type)).order(ByteOrder.nativeOrder());
        buildBuffer(quadCount, buffer, type);
        if (!gl.bufferStorageSupported) {

            bind();
            super.uploadBuffer(buffer, EGpuUploadMethod.DATA,
                    indicesCount * GLEnums.getTypeSize(type), GL32.GL_STATIC_DRAW);
        } else {
            bind();
            super.uploadBuffer(buffer, EGpuUploadMethod.BUFFER_STORAGE,
                    indicesCount * GLEnums.getTypeSize(type), 0);
        }
    }
}
