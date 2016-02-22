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
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
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
import java.nio.IntBuffer;
import java.nio.LongBuffer;

/**
 *
 * @author GBarbieri
 */
public class jogl implements GLEventListener, KeyListener {

    private static int screenIdx = 0;
    private static Dimension windowSize = new Dimension(300, 300);
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

        jogl bug = new jogl();
        glWindow.addGLEventListener(bug);
        glWindow.addKeyListener(bug);

        animator = new Animator(glWindow);
        animator.setRunAsFastAsPossible(true);
        animator.setUpdateFPSFrames(100, System.out);
        animator.start();
    }

    private int SQRT_BUILDING_COUNT = 100;
    private IntBuffer vertexBuffer = GLBuffers.newDirectIntBuffer(SQRT_BUILDING_COUNT * SQRT_BUILDING_COUNT),
            indexBuffer = GLBuffers.newDirectIntBuffer(SQRT_BUILDING_COUNT * SQRT_BUILDING_COUNT),
            vertexBufferSize = GLBuffers.newDirectIntBuffer(SQRT_BUILDING_COUNT * SQRT_BUILDING_COUNT),
            indexBufferSize = GLBuffers.newDirectIntBuffer(SQRT_BUILDING_COUNT * SQRT_BUILDING_COUNT);
    private LongBuffer vertexBufferGPUPtr = GLBuffers.newDirectLongBuffer(SQRT_BUILDING_COUNT * SQRT_BUILDING_COUNT),
            indexBufferGPUPtr = GLBuffers.newDirectLongBuffer(SQRT_BUILDING_COUNT * SQRT_BUILDING_COUNT);
    private boolean bug = false;
    private boolean toggle = false;
    private boolean dsa = false;

    public jogl() {

    }

    @Override
    public void init(GLAutoDrawable drawable) {
        System.out.println("init");

        GL4 gl4 = drawable.getGL().getGL4();

        gl4.setSwapInterval(0);

        gl4.glCreateBuffers(SQRT_BUILDING_COUNT * SQRT_BUILDING_COUNT, vertexBuffer);
//        gl4.glCreateBuffers(SQRT_BUILDING_COUNT * SQRT_BUILDING_COUNT, indexBuffer);

        for (int i = 0; i < SQRT_BUILDING_COUNT; i++) {

            for (int k = 0; k < SQRT_BUILDING_COUNT; k++) {

                int index = i * SQRT_BUILDING_COUNT + k;

//                gl4.glCreateBuffers(1, vertexBuffer, index);
//                gl4.glCreateBuffers(1, indexBuffer, index);
                // Stick the data for the vertices and indices in their respective buffers
                ByteBuffer verticesBuffer = GLBuffers.newDirectByteBuffer(512);
                if (dsa) {
                    gl4.glNamedBufferData(vertexBuffer.get(index), verticesBuffer.capacity() * Byte.BYTES, verticesBuffer,
                            GL_STATIC_DRAW);
                } else {
                    gl4.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer.get(index));
                    gl4.glBufferData(GL_ARRAY_BUFFER, verticesBuffer.capacity() * Byte.BYTES, verticesBuffer, 
                            GL_STATIC_DRAW);
                }

//                ShortBuffer indicesBuffer = GLBuffers.newDirectShortBuffer(6);
//                gl4.glNamedBufferData(indexBuffer.get(index), indicesBuffer.capacity() * Short.BYTES, indicesBuffer,
//                        GL_STATIC_DRAW);

                // *** INTERESTING ***
                // get the GPU pointer for the vertex buffer and make the vertex buffer resident on the GPU
                gl4.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer.get(index));
                vertexBufferGPUPtr.position(index);
                gl4.glGetBufferParameterui64vNV(GL_ARRAY_BUFFER, GL_BUFFER_GPU_ADDRESS_NV, vertexBufferGPUPtr);
                vertexBufferSize.position(index);
                gl4.glGetBufferParameteriv(GL_ARRAY_BUFFER, GL_BUFFER_SIZE, vertexBufferSize);
                gl4.glMakeBufferResidentNV(GL_ARRAY_BUFFER, GL_READ_ONLY);
                gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);
////                // *** INTERESTING ***
//                // get the GPU pointer for the index buffer and make the index buffer resident on the GPU
//                gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.get(index));
//                indexBufferGPUPtr.position(index);
//                gl4.glGetBufferParameterui64vNV(GL_ELEMENT_ARRAY_BUFFER, GL_BUFFER_GPU_ADDRESS_NV, indexBufferGPUPtr);
//                indexBufferSize.position(index);
//                gl4.glGetBufferParameteriv(GL_ELEMENT_ARRAY_BUFFER, GL_BUFFER_SIZE, indexBufferSize);
//                gl4.glMakeBufferResidentNV(GL_ELEMENT_ARRAY_BUFFER, GL_READ_ONLY);
//                gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            }
        }
        vertexBufferGPUPtr.rewind();
//        indexBufferGPUPtr.rewind();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.exit(0);
    }

    private float[] clearColor = new float[]{1.0f, 0.5f, 0.0f, 1.0f};

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl4 = drawable.getGL().getGL4();
        gl4.glClearBufferfv(GL_COLOR, 0, getClearColor(), 0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            animator.stop();
            glWindow.destroy();
        }
        if (e.getKeyCode() == KeyEvent.VK_T) {
            toggle = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    /**
     * @return the clearColor
     */
    public float[] getClearColor() {
        return clearColor;
    }

    /**
     * @param clearColor the clearColor to set
     */
    public void setClearColor(float[] clearColor) {
        this.clearColor = clearColor;
    }
}
