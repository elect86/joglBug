/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bug1167;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_BUFFER_SIZE;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_READ_ONLY;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.NVShaderBufferLoad.GL_BUFFER_GPU_ADDRESS_NV;
import static org.lwjgl.system.MemoryUtil.*;

public class lwjgl {

    // We need to strongly reference callback instances.
    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback keyCallback;

    // The window handle
    private long window;

    /**
     * time at last frame
     */
    long lastFrame;

    /**
     * frames per second
     */
    int fps;
    /**
     * last fps time
     */
    long lastFPS;

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        try {
            init();
            loop();

            // Release window and window callbacks
            glfwDestroyWindow(window);
            keyCallback.release();
        } finally {
            // Terminate GLFW and release the GLFWErrorCallback
            glfwTerminate();
            errorCallback.release();
        }
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (glfwInit() != GLFW_TRUE) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure our window
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        int WIDTH = 300;
        int HEIGHT = 300;

        // Create the window
        window = glfwCreateWindow(WIDTH, HEIGHT, "Hello World!", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                    glfwSetWindowShouldClose(window, GLFW_TRUE); // We will detect this in our rendering loop
                }
            }
        });

        // Get the resolution of the primary monitor
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        // Center our window
        glfwSetWindowPos(
                window,
                (vidmode.width() - WIDTH) / 2,
                (vidmode.height() - HEIGHT) / 2
        );

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
//        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);

        getDelta(); // call once before loop to initialise lastFrame
        lastFPS = getTime(); // call before loop to initialise fps timer
    }

    private int SQRT_BUILDING_COUNT = 100;
    private ByteBuffer vertexBufferGPUPtr = BufferUtils.createByteBuffer(Long.BYTES),
            indexBufferGPUPtr = BufferUtils.createByteBuffer(Long.BYTES),
            vertexBufferSize = BufferUtils.createByteBuffer(Integer.BYTES),
            indexBufferSize = BufferUtils.createByteBuffer(Integer.BYTES),
            vertexBuffer = BufferUtils.createByteBuffer(SQRT_BUILDING_COUNT * SQRT_BUILDING_COUNT * Integer.BYTES),
            indexBuffer = BufferUtils.createByteBuffer(SQRT_BUILDING_COUNT * SQRT_BUILDING_COUNT * Integer.BYTES);

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        for (int i = 0; i < SQRT_BUILDING_COUNT; i++) {

            for (int k = 0; k < SQRT_BUILDING_COUNT; k++) {

                int index = (i * SQRT_BUILDING_COUNT + k) * Integer.BYTES;
                
                vertexBuffer.position(index);
                GL45.glCreateBuffers(1, vertexBuffer);

                indexBuffer.position(index);
                GL45.glCreateBuffers(1, indexBuffer);

                // Stick the data for the vertices and indices in their respective buffers
                ByteBuffer verticesBuffer = BufferUtils.createByteBuffer(512 * 6);

                GL45.glNamedBufferData(vertexBuffer.getInt(index), verticesBuffer.capacity() * Byte.BYTES,
                        verticesBuffer, GL_STATIC_DRAW);
//                gl4.glBufferData(GL_ARRAY_BUFFER, verticesBuffer.capacity(), verticesBuffer, GL_STATIC_DRAW);

                ByteBuffer indicesBuffer = BufferUtils.createByteBuffer(6 * 6 * Short.BYTES);
                GL45.glNamedBufferData(indexBuffer.getInt(index), indicesBuffer.capacity() * Byte.BYTES,
                        indicesBuffer, GL_STATIC_DRAW);

                // *** INTERESTING ***
                // get the GPU pointer for the vertex buffer and make the vertex buffer resident on the GPU
                GL15.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer.getInt(index));
                NVShaderBufferLoad.glGetBufferParameterui64vNV(GL_ARRAY_BUFFER, GL_BUFFER_GPU_ADDRESS_NV,
                        vertexBufferGPUPtr);
                GL15.glGetBufferParameteriv(GL_ARRAY_BUFFER, GL_BUFFER_SIZE, vertexBufferSize);
                NVShaderBufferLoad.glMakeBufferResidentNV(GL_ARRAY_BUFFER, GL_READ_ONLY);
                GL15.glBindBuffer(GL_ARRAY_BUFFER, 0);
//                // *** INTERESTING ***
                // get the GPU pointer for the index buffer and make the index buffer resident on the GPU
                GL15.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.getInt(index));
                NVShaderBufferLoad.glGetBufferParameterui64vNV(GL_ELEMENT_ARRAY_BUFFER, GL_BUFFER_GPU_ADDRESS_NV,
                        indexBufferGPUPtr);
                GL15.glGetBufferParameteriv(GL_ELEMENT_ARRAY_BUFFER, GL_BUFFER_SIZE, indexBufferSize);
                NVShaderBufferLoad.glMakeBufferResidentNV(GL_ELEMENT_ARRAY_BUFFER, GL_READ_ONLY);
                GL15.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            }
        }
        
//        for (int i = 0; i < SQRT_BUILDING_COUNT*SQRT_BUILDING_COUNT; i++) {
//            System.out.println(""+vertexBuffer.getInt(i*Integer.BYTES));
//        }

        clearColor.rewind();

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (glfwWindowShouldClose(window) == GLFW_FALSE) {

            glClearColor(1.0f, 0.5f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
            
            GL30.glClearBufferfv(GL_COLOR, 0, clearColor);

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();

            updateFPS();
        }
    }
    
    FloatBuffer clearColor = BufferUtils.createFloatBuffer(4).put(new float[]{1.0f, 0.5f, 0.0f, 1.0f});

    public static void main(String[] args) {
        new lwjgl().run();
    }

    /**
     * Get the time in milliseconds
     *
     * @return The system time in milliseconds
     */
    public long getTime() {
        return System.nanoTime() / 1000000;
    }

    public int getDelta() {
        long time = getTime();
        int delta = (int) (time - lastFrame);
        lastFrame = time;

        return delta;
    }

    public void start() {
        //some startup code
        lastFPS = getTime(); //set lastFPS to current Time
    }

    /**
     * Calculate the FPS and set it in the title bar
     */
    public void updateFPS() {
        if (getTime() - lastFPS > 1000) {
            System.out.println("FPS: " + fps);
            fps = 0; //reset the FPS counter
            lastFPS += 1000; //add one second
        }
        fps++;
    }
}
