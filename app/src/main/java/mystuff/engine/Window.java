package mystuff.engine;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

public class Window {
    private long windowHandle;
    private int width, height;
    private String title;
    private boolean isFullscreen;

    public Window(String title, int width, int height) {
        this(title, width, height, false);
    }

    public Window(String title, int width, int height, boolean fullscreen) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.isFullscreen = fullscreen;
    }

    public void init() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure window hints
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // Hide window until we position it
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        // Create the window
        windowHandle = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        
        if (windowHandle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Get the resolution of the primary monitor
        long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
        org.lwjgl.glfw.GLFWVidMode vidmode = GLFW.glfwGetVideoMode(primaryMonitor);
        if (vidmode != null) {
            // Center horizontally, position 50 pixels from top
            GLFW.glfwSetWindowPos(
                windowHandle,
                (vidmode.width() - width) / 2,
                50  // Fixed distance from top of screen
            );
        }

        // Make the window visible
        GLFW.glfwShowWindow(windowHandle);

        GLFW.glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();

        // Enable depth testing
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        
        setupProjection();
    }

    private void setupProjection() {
        // Reduce FOV to 45 degrees
        float fov = 45.0f;
        float aspectRatio = (float) width / height;
        // Adjust near and far planes
        float zNear = 0.1f;
        float zFar = 1000.0f;
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        
        // Calculate perspective projection values
        float yScale = (float) (1.0f / Math.tan(Math.toRadians(fov / 2.0f)));
        float xScale = yScale / aspectRatio;
        float frustumLength = zFar - zNear;
        
        // Create perspective projection matrix
        float[] matrix = new float[16];
        matrix[0] = xScale;
        matrix[5] = yScale;
        matrix[10] = -((zFar + zNear) / frustumLength);
        matrix[11] = -1;
        matrix[14] = -((2 * zNear * zFar) / frustumLength);
        matrix[15] = 0;
        
        GL11.glLoadMatrixf(matrix);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    public void update() {
        GLFW.glfwSwapBuffers(windowHandle);
        GLFW.glfwPollEvents();
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(windowHandle);
    }

    public void cleanup() {
        GLFW.glfwDestroyWindow(windowHandle);
        GLFW.glfwTerminate();
    }

    public long getWindowHandle() {
        return windowHandle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
} 