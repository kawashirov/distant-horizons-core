package com.seibel.lod.core.config.gui;

import org.lwjgl.system.*;
import org.lwjgl.system.jawt.JAWT;
import org.lwjgl.system.macosx.*;

import java.awt.*;
import java.lang.reflect.*;
import java.util.regex.*;

import static org.lwjgl.glfw.GLFWNativeCocoa.*;
import static org.lwjgl.glfw.GLFWNativeWin32.*;
import static org.lwjgl.glfw.GLFWNativeX11.*;
import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.jawt.JAWTFunctions.*;
import static org.lwjgl.system.macosx.ObjCRuntime.*;

// Some of the code is from https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/system/jawt/EmbeddedFrameUtil.java
// which is licensed under https://www.lwjgl.org/license

public final class EmbeddedFrameUtil {

    private static final int JAVA_VERSION;

    private static final JAWT awt;

    static {
        Pattern p = Pattern.compile("^(?:1[.])?([1-9][0-9]*)[.-]");
        Matcher m = p.matcher(System.getProperty("java.version"));

        if (!m.find()) {
            throw new IllegalStateException("Failed to parse java.version");
        }

        JAVA_VERSION = Integer.parseInt(m.group(1));

        awt = JAWT.calloc();
        awt.version(JAVA_VERSION < 9 ? JAWT_VERSION_1_4 : JAWT_VERSION_9);
        if (!JAWT_GetAWT(awt)) {
            throw new RuntimeException("GetAWT failed");
        }
    }

    private static String getEmbeddedFrameImpl() {
        switch (Platform.get()) {
            case LINUX:
                return "sun.awt.X11.XEmbeddedFrame";
            case MACOSX:
                return "sun.lwawt.macosx.CViewEmbeddedFrame";
            case WINDOWS:
                return "sun.awt.windows.WEmbeddedFrame";
            default:
                throw new IllegalStateException();
        }
    }

    private static long getEmbeddedFrameHandle(long window) {
        switch (Platform.get()) {
            case LINUX:
                return glfwGetX11Window(window);
            case MACOSX:
                long objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
                return invokePPP(glfwGetCocoaWindow(window), sel_getUid("contentView"), objc_msgSend);
            case WINDOWS:
                return glfwGetWin32Window(window);
            default:
                throw new IllegalStateException();
        }
    }

    public static Frame embeddedFrameCreate(long window) {
        if (JAVA_VERSION < 9) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Frame> EmdeddedFrame = (Class<? extends Frame>)Class.forName(getEmbeddedFrameImpl());
                Constructor<? extends Frame> c = EmdeddedFrame.getConstructor(long.class);

                return c.newInstance(getEmbeddedFrameHandle(window));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return nJAWT_CreateEmbeddedFrame(getEmbeddedFrameHandle(window), awt.CreateEmbeddedFrame());
        }
    }

    static void embeddedFrameSynthesizeWindowActivation(Frame embeddedFrame, boolean doActivate) {
        if (JAVA_VERSION < 9) {
            try {
                embeddedFrame
                        .getClass()
                        .getMethod("synthesizeWindowActivation", boolean.class)
                        .invoke(embeddedFrame, doActivate);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            JAWT_SynthesizeWindowActivation(embeddedFrame, doActivate, awt.SynthesizeWindowActivation());
        }
    }

    public static void embeddedFrameSetBounds(Frame embeddedFrame, int x, int y, int width, int height) {
        if (JAVA_VERSION < 9) {
            try {
                Method setLocationPrivate = embeddedFrame
                        .getClass()
                        .getSuperclass()
                        .getDeclaredMethod("setBoundsPrivate", int.class, int.class, int.class, int.class);
                setLocationPrivate.setAccessible(true);
                setLocationPrivate.invoke(embeddedFrame, x, y, width, height);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            JAWT_SetBounds(embeddedFrame, x, y, width, height, awt.SetBounds());
        }
    }

    // The code below is by Ran

    public static void hideFrame(Frame embeddedFrame) {
        if (embeddedFrame != null) {
            embeddedFrame.setVisible(false);
            embeddedFrameSynthesizeWindowActivation(embeddedFrame, false);
        }
    }

    public static void showFrame(Frame embeddedFrame) {
        if (embeddedFrame != null) {
            embeddedFrameSynthesizeWindowActivation(embeddedFrame, true);
            embeddedFrame.setVisible(true);
        }
    }

    public static void placeAtCenter(Frame embeddedFrame, int windowWidth, int windowHeight, int frameWidth, int frameHeight, float scale) {
        float scaleFactor = (100.0F - scale) / 100.0F;
        float newWidth = frameWidth * scaleFactor;
        float newHeight = frameHeight * scaleFactor;
        float newX = (windowWidth - newWidth) / 2F;
        float newY = (windowHeight - newHeight) / 2F;
        embeddedFrameSetBounds(embeddedFrame, Math.round(newX), Math.round(newY), Math.round(newWidth), Math.round(newHeight));
    }
}