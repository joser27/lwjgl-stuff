package mystuff.engine;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Window {
    private long windowHandle;
    private int width, height;
    private String title;
    private boolean isFullscreen;
    private boolean vSync;
    private String baseTitle; // Store the original title
    private GameEngine parentEngine; // Reference to the parent engine

    public Window(String title, int width, int height) {
        this(title, width, height, false);
    }

    public Window(String title, int width, int height, boolean fullscreen) {
        this.title = title;
        this.baseTitle = title; // Store original title
        this.width = width;
        this.height = height;
        this.isFullscreen = fullscreen;
        this.vSync = false;
    }

    public void init() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure window hints
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        
        // Use OpenGL 3.3 with compatibility profile to support legacy functions
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_COMPAT_PROFILE);
        
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
        
        // Disable v-sync by default
        GLFW.glfwSwapInterval(0);
        this.vSync = false;
        
        // Make the window visible
        GLFW.glfwShowWindow(windowHandle);

        // Initialize OpenGL capabilities
        GL.createCapabilities();

        // Print basic information (detailed info will be printed in Game.init)
        System.out.println("Window initialized with OpenGL context");

        // Set up initial OpenGL state
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Clear color and depth buffer
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClearDepth(1.0);
    }

    /**
     * Sets the window icon from an image file
     * @param iconPath Path to the icon image file (PNG/JPG/BMP/TGA supported)
     */
    public void setIcon(String iconPath) {
        if (windowHandle == 0) {
            System.err.println("Cannot set icon: Window not initialized");
            return;
        }

        // Check if icon file exists
        if (!Files.exists(Paths.get(iconPath))) {
            System.err.println("Icon file does not exist: " + iconPath);
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load the icon image
            STBImage.stbi_set_flip_vertically_on_load(false);
            ByteBuffer iconData = STBImage.stbi_load(iconPath, width, height, channels, 4);
            
            if (iconData == null) {
                System.err.println("Failed to load icon: " + STBImage.stbi_failure_reason());
                return;
            }

            try {
                // Create GLFWImage buffer for the icon
                GLFWImage.Buffer iconBuffer = GLFWImage.malloc(1);
                GLFWImage icon = iconBuffer.get(0);
                
                // Set icon properties
                icon.set(width.get(0), height.get(0), iconData);
                
                // Set the window icon
                GLFW.glfwSetWindowIcon(windowHandle, iconBuffer);
                
                System.out.println("Successfully set window icon: " + iconPath);
                
                // Clean up the buffer
                iconBuffer.free();
                
            } finally {
                // Free the image memory
                STBImage.stbi_image_free(iconData);
            }
            
        } catch (Exception e) {
            System.err.println("Error setting window icon: " + iconPath);
            e.printStackTrace();
        }
    }

    /**
     * Sets multiple window icons for different sizes (recommended for best results)
     * @param iconPaths Array of icon paths, typically in different sizes (16x16, 32x32, 48x48, etc.)
     */
    public void setIcons(String[] iconPaths) {
        if (windowHandle == 0) {
            System.err.println("Cannot set icons: Window not initialized");
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer iconBuffer = GLFWImage.malloc(iconPaths.length);
            
            int validIcons = 0;
            for (int i = 0; i < iconPaths.length; i++) {
                String iconPath = iconPaths[i];
                
                if (!Files.exists(Paths.get(iconPath))) {
                    System.err.println("Icon file does not exist: " + iconPath);
                    continue;
                }

                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                STBImage.stbi_set_flip_vertically_on_load(false);
                ByteBuffer iconData = STBImage.stbi_load(iconPath, width, height, channels, 4);
                
                if (iconData == null) {
                    System.err.println("Failed to load icon: " + iconPath + " - " + STBImage.stbi_failure_reason());
                    continue;
                }

                GLFWImage icon = iconBuffer.get(validIcons);
                icon.set(width.get(0), height.get(0), iconData);
                validIcons++;
                
                System.out.println("Loaded icon: " + iconPath + " (" + width.get(0) + "x" + height.get(0) + ")");
            }
            
            if (validIcons > 0) {
                // Resize buffer to only include valid icons
                iconBuffer.limit(validIcons);
                GLFW.glfwSetWindowIcon(windowHandle, iconBuffer);
                System.out.println("Successfully set " + validIcons + " window icons");
            }
            
            iconBuffer.free();
            
        } catch (Exception e) {
            System.err.println("Error setting window icons");
            e.printStackTrace();
        }
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

    public boolean isVSync() {
        return vSync;
    }

    public void setVSync(boolean vSync) {
        this.vSync = vSync;
        if (vSync) {
            GLFW.glfwSwapInterval(1);
        } else {
            GLFW.glfwSwapInterval(0);
        }
    }

    public void setTitle(String title) {
        this.title = title;
        if (windowHandle != 0) {
            GLFW.glfwSetWindowTitle(windowHandle, title);
        }
    }

    public String getTitle() {
        return title;
    }

    public String getBaseTitle() {
        return baseTitle;
    }

    public void resetTitle() {
        setTitle(baseTitle);
    }

    /**
     * Sets the parent game engine for this window
     */
    public void setParentEngine(GameEngine engine) {
        this.parentEngine = engine;
    }
    
    /**
     * Gets the parent game engine
     */
    public GameEngine getParentEngine() {
        return parentEngine;
    }
} 