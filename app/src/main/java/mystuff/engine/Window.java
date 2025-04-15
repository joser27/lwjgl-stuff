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
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        
        // Use OpenGL 2.1 for maximum compatibility
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_ANY_PROFILE);
        
        // Create the window
        windowHandle = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        
        if (windowHandle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Get the resolution of the primary monitor
        long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
        org.lwjgl.glfw.GLFWVidMode vidmode = GLFW.glfwGetVideoMode(primaryMonitor);
        if (vidmode != null) {
            GLFW.glfwSetWindowPos(
                windowHandle,
                (vidmode.width() - width) / 2,
                50
            );
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(windowHandle);
        
        // Enable v-sync
        GLFW.glfwSwapInterval(1);
        
        // Make the window visible
        GLFW.glfwShowWindow(windowHandle);

        // Initialize OpenGL capabilities
        GL.createCapabilities();

        // Print OpenGL version and capabilities
        System.out.println("OpenGL Version: " + GL11.glGetString(GL11.GL_VERSION));
        System.out.println("OpenGL Vendor: " + GL11.glGetString(GL11.GL_VENDOR));
        System.out.println("OpenGL Renderer: " + GL11.glGetString(GL11.GL_RENDERER));
        System.out.println("OpenGL Extensions: " + GL11.glGetString(GL11.GL_EXTENSIONS));

        // Set up initial OpenGL state
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Clear color and depth buffer
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClearDepth(1.0);
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