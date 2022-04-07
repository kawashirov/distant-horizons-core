/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
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
 
package com.seibel.lod.core.render.objects;

import org.lwjgl.opengl.GL32;

public class GLState {

    public int prog;
    public int vao;
    public int vbo;
    public int fbo;
    public int text;
    public boolean blend;
    public int blendSrc;
    public int blendDst;
    public boolean depth;
    public int depthFunc;
    public boolean stencil;
    public int stencilFunc;
    public int stencilRef;
    public int stencilMask;
    public int[] view;
    public boolean cull;
    public int cullMode;
    public int polyMode;

    public void saveState() {
        prog = GL32.glGetInteger(GL32.GL_CURRENT_PROGRAM);
        vao = GL32.glGetInteger(GL32.GL_VERTEX_ARRAY_BINDING);
        vbo = GL32.glGetInteger(GL32.GL_ARRAY_BUFFER_BINDING);
        fbo = GL32.glGetInteger(GL32.GL_FRAMEBUFFER_BINDING);
        text = GL32.glGetInteger(GL32.GL_TEXTURE_BINDING_2D);
        blend = GL32.glIsEnabled(GL32.GL_BLEND);
        blendSrc = GL32.glGetInteger(GL32.GL_BLEND_SRC);
        blendDst = GL32.glGetInteger(GL32.GL_BLEND_DST);
        depth = GL32.glIsEnabled(GL32.GL_DEPTH_TEST);
        depthFunc = GL32.glGetInteger(GL32.GL_DEPTH_FUNC);
        stencil = GL32.glIsEnabled(GL32.GL_STENCIL_TEST);
        stencilFunc = GL32.glGetInteger(GL32.GL_STENCIL_FUNC);
        stencilRef = GL32.glGetInteger(GL32.GL_STENCIL_REF);
        stencilMask = GL32.glGetInteger(GL32.GL_STENCIL_VALUE_MASK);
        view = new int[4];
        GL32.glGetIntegerv(GL32.GL_VIEWPORT, view);
        cull = GL32.glIsEnabled(GL32.GL_CULL_FACE);
        cullMode = GL32.glGetInteger(GL32.GL_CULL_FACE_MODE);
        polyMode = GL32.glGetInteger(GL32.GL_POLYGON_MODE);
    }

    @Override
    public String toString() {
        return "GLState{" + "prog=" + prog + ", vao=" + vao + ", vbo=" + vbo + ", fbo=" + fbo + ", text=" + GLEnums.getString(text) +
                ", blend=" + blend + ", blendMode=" + GLEnums.getString(blendSrc) + "," + GLEnums.getString(blendDst) +
                ", depth=" + depth +
                ", depthFunc=" + GLEnums.getString(depthFunc) + ", stencil=" + stencil + ", stencilFunc=" +
                GLEnums.getString(stencilFunc) + ", stencilRef=" + stencilRef + ", stencilMask=" + stencilMask +
                ", view={x:" + view[0] + ", y:" + view[1] +
                ", w:" + view[2] + ", h:" + view[3] + "}" + ", cull=" + cull + ", cullMode="
                + GLEnums.getString(cullMode) + ", polyMode=" + GLEnums.getString(polyMode) + '}';
    }

    public void restore() {
        GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, fbo);
        if (blend) {
            GL32.glEnable(GL32.GL_BLEND);
        } else {
            GL32.glDisable(GL32.GL_BLEND);
        }
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, text);
        GL32.glBindVertexArray(vao);
        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo);

        GL32.glBlendFunc(blendSrc, blendDst);
        if (depth) {
            GL32.glEnable(GL32.GL_DEPTH_TEST);
        } else {
            GL32.glDisable(GL32.GL_DEPTH_TEST);
        }
        GL32.glDepthFunc(depthFunc);
        if (stencil) {
            GL32.glEnable(GL32.GL_STENCIL_TEST);
        } else {
            GL32.glDisable(GL32.GL_STENCIL_TEST);
        }
        GL32.glStencilFunc(stencilFunc, stencilRef, stencilMask);
        GL32.glViewport(view[0], view[1], view[2], view[3]);
        GL32.glUseProgram(prog);
        if (cull) {
            GL32.glEnable(GL32.GL_CULL_FACE);
        } else {
            GL32.glDisable(GL32.GL_CULL_FACE);
        }
        GL32.glCullFace(cullMode);
        GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, polyMode);
    }

    public GLState() {
        saveState();
    }
}
