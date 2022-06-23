package com.seibel.lod.core.a7.level;

import com.seibel.lod.core.a7.render.RenderBufferHandler;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;

public interface IClientLevel extends ILevel {
    void clientTick();

    void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler);

    RenderBufferHandler getRenderBufferHandler();
}
