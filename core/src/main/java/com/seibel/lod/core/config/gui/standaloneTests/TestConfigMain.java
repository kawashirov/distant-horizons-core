package com.seibel.lod.core.config.gui.standaloneTests;

import com.seibel.lod.core.api.internal.ClientApi;
import com.seibel.lod.core.config.gui.AbstractScreen;
import com.seibel.lod.core.config.gui.ConfigScreen;
import com.seibel.lod.core.pos.Pos2D;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * This just allows for a quicker testing of the config screen without loading up the whole game
 *
 * @author coolGi
 */
public class TestConfigMain {
    // The window handle
    private long window;
    public AbstractScreen abstractScreen;


    public static void main(String[] args) {
        new TestConfigMain().run();
    }


    public void run() {
        abstractScreen = new ConfigScreen();
        System.out.println("Hello LWJGL version " + Version.getVersion());

//        ClientApi.INSTANCE.rendererStartupEvent();
        init();

        Pos2D windowDim = getWindowDimentions(window);
        abstractScreen.width = windowDim.x;
        abstractScreen.height = windowDim.y;
        abstractScreen.init();
        loop();
        // Code isnt moved until loop is done (and it is only done if window is closed)

//        ClientApi.INSTANCE.rendererShutdownEvent();
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(640, 480, "DH Test config", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        // Is this really nessesery
//        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
//            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
//                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
//        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
        GL.createCapabilities();
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color (if not set it would just be black)
//        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        // Set this so we can use it for the delta time
        lastLoopTime = getTime();

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        // (only works if glfwPollEvents() is called)
        while ( !glfwWindowShouldClose(window) ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer (will cause a ghosting effect if not called)

            // Poll for window events. (this allows the window to be closed)
            glfwPollEvents();

            Pos2D windowDim = getWindowDimentions(window);
            abstractScreen.width = windowDim.x;
            abstractScreen.height = windowDim.y;

            abstractScreen.render(getDelta());


            glfwSwapBuffers(window); // swap the color buffers
        }
        abstractScreen.onClose();
    }



    public Pos2D getWindowDimentions(long window) {
        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        glfwGetWindowSize(window, w, h);
        return new Pos2D(w.get(0), h.get(0));
    }


    private double lastLoopTime;
    private float timeCount;
    public double getTime() {
        return glfwGetTime();
    }
    public float getDelta() {
        double time = getTime();
        float delta = (float) (time - lastLoopTime);
        lastLoopTime = time;
        timeCount += delta;
        return delta;
    }
}
