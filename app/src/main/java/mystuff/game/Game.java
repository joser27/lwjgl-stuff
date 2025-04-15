package mystuff.game;

import mystuff.engine.Window;
import mystuff.engine.Camera;
import mystuff.engine.IGameLogic;
import mystuff.engine.Timer;
import mystuff.engine.GameEngine;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import mystuff.utils.Debug;
import mystuff.utils.KeyboardManager;

/**
 * Main game class that implements the game logic interface
 */
public class Game implements IGameLogic {
    // Core components
    private Camera camera;
    private Player player;
    private World world;
    private PlayerRenderer playerRenderer;
    private Skybox skybox;
    private Timer timer;
    
    // Game state
    private boolean wireframeMode = false;
    private boolean paused = false;
    private float gameTime = 0;
    
    // Performance metrics
    private float[] cpuUtilizationHistory = new float[60]; // 1 second at 60fps
    private int utilizationIndex = 0;

    @Override
    public void init(Window window) {
        try {
            // Don't create capabilities again - they were created in Window.init
            
            System.out.println("Initializing OpenGL for Minecraft-like rendering...");
            
            // Initialize OpenGL state
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glClearColor(0.1f, 0.1f, 0.1f, 1.0f); // Dark background color
            
            // Print more detailed OpenGL information
            System.out.println("Using OpenGL version: " + GL11.glGetString(GL11.GL_VERSION));
            System.out.println("OpenGL vendor: " + GL11.glGetString(GL11.GL_VENDOR));
            System.out.println("OpenGL renderer: " + GL11.glGetString(GL11.GL_RENDERER));
            
            // Initialize game objects
            camera = new Camera(0, 0, 0);
            world = new World(camera);
            player = new Player(50*World.BLOCK_SIZE, 50*World.BLOCK_SIZE, 50*World.BLOCK_SIZE, camera, world);
            world.setPlayer(player);
            
            playerRenderer = new PlayerRenderer();
            playerRenderer.init();
            
            skybox = new Skybox();
            skybox.init();
            
            // Initialize font
            mystuff.utils.FontLoader.init("resources/fonts/reflow-sans-demo/Reflow Sans DEMO.ttf");
            
            // Set up mouse cursor
            GLFW.glfwSetInputMode(window.getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            
            // Set up mouse callback
            GLFW.glfwSetCursorPosCallback(window.getWindowHandle(), (windowHandle, xpos, ypos) -> {
                player.handleMouseInput((float)xpos, (float)ypos);
            });
            
            // Initialize performance metrics
            for (int i = 0; i < cpuUtilizationHistory.length; i++) {
                cpuUtilizationHistory[i] = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void input(Window window) {
        KeyboardManager.update(window.getWindowHandle());
        
        // Game exit
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
            GLFW.glfwSetWindowShouldClose(window.getWindowHandle(), true);
        }
        
        // Toggle wireframe mode
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_F)) {
            wireframeMode = !wireframeMode;
        }
        
        // Toggle pause with P key
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_P) && timer != null) {
            paused = !paused;
            if (window.getParentEngine() != null) {
                window.getParentEngine().setPaused(paused);
            }
        }
        
        // Time scaling with [ and ] keys
        if (timer != null) {
            if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_LEFT_BRACKET)) {
                timer.setTimeScale(Math.max(0.1, timer.getTimeScale() - 0.01));
            }
            if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_RIGHT_BRACKET)) {
                timer.setTimeScale(Math.min(2.0, timer.getTimeScale() + 0.01));
            }
        }
    }

    @Override
    public void update(float interval) {
        // Update game time
        gameTime += interval;
        
        // Get player position for dynamic chunk loading optimization
        float playerX = camera.getX();
        float playerY = camera.getY();
        float playerZ = camera.getZ();
        
        // Update player first (for responsive controls)
        player.update(null, interval);
        
        // Update world with player position for chunk loading optimization
        // This allows the world to prioritize loading chunks near the player
        world.update(null, interval);
        
        // Store performance metrics if timer available
        if (timer != null) {
            cpuUtilizationHistory[utilizationIndex] = timer.getFrameUtilization();
            utilizationIndex = (utilizationIndex + 1) % cpuUtilizationHistory.length;
        }
    }

    @Override
    public void render(Window window) {
        try {
            // Ensure we have a valid OpenGL context
            if (!org.lwjgl.opengl.GL.getCapabilities().OpenGL11) {
                System.err.println("OpenGL 1.1 capabilities are not available. Skipping render cycle.");
                return;
            }
            
            // Clear buffers
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            
            // Set wireframe mode if enabled
            if (wireframeMode) {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            } else {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            }
            
            // Set up projection matrix
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            float fov = 60.0f;
            float aspectRatio = (float) window.getWidth() / window.getHeight();
            float zNear = 0.1f;
            float zFar = 10000.0f;
            float yScale = (float) (1.0f / Math.tan(Math.toRadians(fov / 2.0f)));
            float xScale = yScale / aspectRatio;
            float frustumLength = zFar - zNear;
            float[] matrix = new float[16];
            matrix[0] = xScale;
            matrix[5] = yScale;
            matrix[10] = -((zFar + zNear) / frustumLength);
            matrix[11] = -1;
            matrix[14] = -((2 * zNear * zFar) / frustumLength);
            matrix[15] = 0;
            GL11.glLoadMatrixf(matrix);
            
            // Set up modelview matrix
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            
            // Apply camera rotation
            GL11.glRotatef(camera.getPitch(), 1.0f, 0.0f, 0.0f);
            GL11.glRotatef(camera.getYaw(), 0.0f, 1.0f, 0.0f);
            
            // Set up the camera transform based on current view/mode
            if (player.isNoClipMode()) {
                // In no-clip mode, the camera moves freely (spectator view)
                // while player body stays at its original position
                GL11.glTranslatef(-camera.getX(), -camera.getY(), -camera.getZ());
            } else {
                // In normal mode, camera is attached to player
                GL11.glTranslatef(-camera.getX(), -camera.getY(), -camera.getZ());
            }
            
            // Update frustum for culling
            camera.update();
            
            // Save initial state
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            
            // Render game objects
            skybox.render();
            world.render(camera);
            
            // When in no-clip mode, the player body should remain stationary
            // while the camera can move around freely
            playerRenderer.render(player, camera.getYaw(), camera.getPitch());
            
            // Render UI
            renderUI(window);
            
            // Restore state
            GL11.glPopAttrib();
            
            // Always reset polygon mode after rendering
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderUI(Window window) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        // Reset color for UI elements
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Enhanced performance metrics
        if (Debug.showFPS() && timer != null) {
            int startY = 30;
            int lineHeight = 20;
            int line = 0;
            
            // Show game state (paused, time scale)
            if (paused) {
                GL11.glColor3f(1.0f, 0.3f, 0.3f);
                renderText("PAUSED", window.getWidth() / 2 - 50, 30);
                GL11.glColor3f(1.0f, 1.0f, 1.0f);
            }
            
            if (timer.getTimeScale() != 1.0) {
                GL11.glColor3f(1.0f, 1.0f, 0.0f);
                renderText(String.format("Time Scale: %.2fx", timer.getTimeScale()), 
                          window.getWidth() - 200, startY + lineHeight * line++);
                GL11.glColor3f(1.0f, 1.0f, 1.0f);
            }
            
            // Show instantaneous frame time and current FPS from Timer
            String fpsText = String.format("FPS: %d", timer.getFPS());
            renderText(fpsText, window.getWidth() - 150, startY + lineHeight * line++);
            
            // Show frame timing statistics from Timer
            String frameTimeText = String.format("Frame Time: %.1fms (Avg: %.1fms, Max: %.1fms, Min: %.1fms)", 
                timer.getFrameTimeHistory()[timer.getCurrentFrameTimeIndex()],
                timer.getAverageFrameTime(), 
                timer.getMaxFrameTime(),
                timer.getMinFrameTime());
            renderText(frameTimeText, window.getWidth() - 400, startY + lineHeight * line++);
            
            // Show CPU utilization
            renderText(String.format("CPU: %.1f%%", timer.getFrameUtilization()), 
                     window.getWidth() - 150, startY + lineHeight * line++);
            
            // Draw CPU utilization graph
            drawUtilizationGraph(window.getWidth() - 300, startY + lineHeight * line, 280, 40);
            line += 3; // Graph takes 3 lines worth of space
            
            // Memory usage
            long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            long usedMemory = totalMemory - freeMemory;
            String memoryText = String.format("Memory: %dMB / %dMB", usedMemory, totalMemory);
            renderText(memoryText, window.getWidth() - 200, startY + lineHeight * line++);
            
            // Frame timing breakdown
            String timingText = String.format("Update: %.2fms, Render: %.2fms, Sleep: %.2fms", 
                                           timer.getUpdateTimeMs(), timer.getRenderTimeMs(), timer.getSleepTimeMs());
            renderText(timingText, window.getWidth() - 350, startY + lineHeight * line++);
            
            // Visual warnings for performance issues
            if (timer.getMaxFrameTime() > 32.0f) {
                GL11.glColor3f(1.0f, 0.0f, 0.0f);
                renderText("WARNING: Frame time spikes detected!", window.getWidth() - 300, startY + lineHeight * line++);
                GL11.glColor3f(1.0f, 1.0f, 1.0f);
            }
            
            if (usedMemory > totalMemory * 0.9) {
                GL11.glColor3f(1.0f, 0.5f, 0.0f);
                renderText("WARNING: High memory usage!", window.getWidth() - 250, startY + lineHeight * line++);
                GL11.glColor3f(1.0f, 1.0f, 1.0f);
            }
        }
        
        // Enhanced player info
        if (Debug.showPlayerInfo()) {
            String posText = String.format("Position: %.2f, %.2f, %.2f", 
                camera.getX(), camera.getY(), camera.getZ());
            renderText(posText, 10, 30);
            
            String rotText = String.format("Rotation: Pitch %.1f°, Yaw %.1f°", 
                camera.getPitch(), camera.getYaw());
            renderText(rotText, 10, 50);
            
            if (player != null) {
                String modeText = "Mode: " + (player.isNoClipMode() ? "NoClip" : "Normal");
                renderText(modeText, 10, 70);
                
                // Display sprint status
                if (player.isSprinting()) {
                    GL11.glColor3f(0.0f, 1.0f, 0.0f); // Green for sprint
                    renderText("SPRINTING", 10, 90);
                    GL11.glColor3f(1.0f, 1.0f, 1.0f); // Reset color
                }
                
                // Display wireframe mode
                if (wireframeMode) {
                    renderText("Wireframe: ON", 10, 110);
                }
            }
            
            // Game time
            renderText(String.format("Game Time: %.1fs", gameTime), 10, 130);
        }
        
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }
    
    /**
     * Draws CPU utilization graph
     */
    private void drawUtilizationGraph(int x, int y, int width, int height) {
        // Draw background
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
        
        // Draw grid lines
        GL11.glColor4f(0.3f, 0.3f, 0.3f, 0.5f);
        GL11.glBegin(GL11.GL_LINES);
        // Horizontal lines at 25%, 50%, 75%
        for (int i = 1; i < 4; i++) {
            float lineY = y + height * (1.0f - i/4.0f);
            GL11.glVertex2f(x, lineY);
            GL11.glVertex2f(x + width, lineY);
        }
        GL11.glEnd();
        
        // Draw graph
        GL11.glColor4f(0.0f, 1.0f, 0.0f, 1.0f);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < cpuUtilizationHistory.length; i++) {
            int idx = (utilizationIndex + i) % cpuUtilizationHistory.length;
            float value = cpuUtilizationHistory[idx] / 100.0f; // Convert to 0.0-1.0 range
            value = Math.min(1.0f, Math.max(0.0f, value)); // Clamp to valid range
            float pointX = x + (width * i / (float)cpuUtilizationHistory.length);
            float pointY = y + height - (value * height);
            GL11.glVertex2f(pointX, pointY);
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void renderText(String text, int x, int y) {
        mystuff.utils.FontLoader.renderText(text, x, y);
    }

    @Override
    public void cleanup() {
        if (player != null) player.cleanup();
        if (world != null) world.cleanup();
        if (skybox != null) skybox.cleanup();
        if (playerRenderer != null) playerRenderer.cleanup();
        mystuff.utils.TextureLoader.cleanup();
        mystuff.utils.FontLoader.cleanup();
    }

    public static void main(String[] args) {
        Game game = new Game();
        GameEngine engine = new GameEngine("Minecraft Clone", 1920, 1080, game, 144); // Higher target FPS
        
        // Enable high-performance options
        engine.setHighPrecisionThread(true);
        engine.setSleepMode(GameEngine.SleepMode.BUSY_WAIT); // Better timing accuracy
        
        // Start the game
        engine.start();
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }
} 