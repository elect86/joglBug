/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bug1167;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BUFFER_SIZE;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_READ_ONLY;
import static com.jogamp.opengl.GL2GL3.GL_BUFFER_GPU_ADDRESS_NV;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author GBarbieri
 */
public class Bug1167 implements GLEventListener {

    private static int screenIdx = 0;
    private static Dimension windowSize = new Dimension(1024, 768);
    private static boolean undecorated = false;
    private static boolean alwaysOnTop = false;
    private static boolean fullscreen = false;
    private static boolean mouseVisible = true;
    private static boolean mouseConfined = false;
    public static GLWindow glWindow;
    public static Animator animator;

    public static void main(String[] args) {

        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, screenIdx);
        GLProfile glProfile = GLProfile.get(GLProfile.GL4);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glWindow = GLWindow.create(screen, glCapabilities);

        glWindow.setSize(windowSize.getWidth(), windowSize.getHeight());
        glWindow.setPosition(50, 50);
        glWindow.setUndecorated(undecorated);
        glWindow.setAlwaysOnTop(alwaysOnTop);
        glWindow.setFullscreen(fullscreen);
        glWindow.setPointerVisible(mouseVisible);
        glWindow.confinePointer(mouseConfined);
        glWindow.setVisible(true);

        Bug1167 bug = new Bug1167();
        glWindow.addGLEventListener(bug);

        animator = new Animator(glWindow);
        animator.setRunAsFastAsPossible(true);
        animator.setUpdateFPSFrames(1000, System.out);
        animator.start();
    }

    private int SQRT_BUILDING_COUNT = 100;
    private int[] building = new int[SQRT_BUILDING_COUNT * SQRT_BUILDING_COUNT * 2];
    private int[] vertexBuffer = {0}, indexBuffer = {0}, vertexBufferSize = {0}, indexBufferSize = {0};
    private long[] vertexBufferGPUPtr = {0}, indexBufferGPUPtr = {0};
    

    public Bug1167() {

    }

    @Override
    public void init(GLAutoDrawable drawable) {
        System.out.println("init");

        GL4 gl4 = drawable.getGL().getGL4();

        for (int i = 0; i < SQRT_BUILDING_COUNT; i++) {

            for (int k = 0; k < SQRT_BUILDING_COUNT; k++) {

                gl4.glCreateBuffers(2, vertexBuffer, 0);

                gl4.glCreateBuffers(1, indexBuffer, 0);

                // Stick the data for the vertices and indices in their respective buffers
                ByteBuffer verticesBuffer = GLBuffers.newDirectByteBuffer(512);
                gl4.glNamedBufferData(vertexBuffer[0], verticesBuffer.capacity(), verticesBuffer.rewind(), GL_STATIC_DRAW);

                ShortBuffer indicesBuffer = GLBuffers.newDirectShortBuffer(6);
                gl4.glNamedBufferData(indexBuffer[0], indicesBuffer.capacity(), indicesBuffer, GL_STATIC_DRAW);

                // *** INTERESTING ***
                // get the GPU pointer for the vertex buffer and make the vertex buffer resident on the GPU
                gl4.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer[0]);
                gl4.glGetBufferParameterui64vNV(GL_ARRAY_BUFFER, GL_BUFFER_GPU_ADDRESS_NV, vertexBufferGPUPtr, 0);
                gl4.glGetBufferParameteriv(GL_ARRAY_BUFFER, GL_BUFFER_SIZE, vertexBufferSize, 0);
                gl4.glMakeBufferResidentNV(GL_ARRAY_BUFFER, GL_READ_ONLY);
                gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

                // *** INTERESTING ***
                // get the GPU pointer for the index buffer and make the index buffer resident on the GPU
                gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer[0]);
                gl4.glGetBufferParameterui64vNV(GL_ELEMENT_ARRAY_BUFFER, GL_BUFFER_GPU_ADDRESS_NV, indexBufferGPUPtr, 0);
                gl4.glGetBufferParameteriv(GL_ELEMENT_ARRAY_BUFFER, GL_BUFFER_SIZE, indexBufferSize, 0);
                gl4.glMakeBufferResidentNV(GL_ELEMENT_ARRAY_BUFFER, GL_READ_ONLY);
                gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            }
        }
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.exit(0);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl4 = drawable.getGL().getGL4();
        gl4.glClearBufferfv(GL_COLOR, 0, new float[]{1.0f, 0.5f, 0.0f, 1.0f}, 0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

}
