package mystuff.game;

import mystuff.engine.Window;
import mystuff.engine.Camera;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import mystuff.game.BlockType;
import mystuff.utils.Debug;
import mystuff.utils.KeyboardManager;

public class Game {
    private Window window;
    private Camera camera;
    private Player player;
    private World world;
    private long lastFpsTime;
    private int fps;
    private int fpsCount;
    private long lastFrameTime;
    private float deltaTime;
    private int testTextureID = -1;
    private PlayerRenderer playerRenderer;
    private Skybox skybox;

    public Game() {
        window = new Window("3D Game", 1920, 1080); 
        camera = new Camera(0, 0, 0);  // Camera starts at origin
        lastFpsTime = System.currentTimeMillis();
        lastFrameTime = System.nanoTime();
        fps = 0;
        fpsCount = 0;
        deltaTime = 0;
        skybox = new Skybox();
    }

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    private void init() {
        window.init();
        
        // Initialize world and player after OpenGL context is created
        world = new World(camera);
        player = new Player(3, 20 * World.BLOCK_SIZE, 3, camera, world);
        playerRenderer = new PlayerRenderer();
        playerRenderer.init();
        skybox.init();
        
        // Try to load the dirt texture
        System.out.println("Loading dirt texture...");
        testTextureID = mystuff.utils.TextureLoader.loadTexture("resources/textures/dirt.png");
        if (testTextureID == -1) {
            System.err.println("Failed to load dirt texture!");
        } else {
            System.out.println("Successfully loaded dirt texture with ID: " + testTextureID);
            // Set texture parameters
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, testTextureID);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }

        // Initialize font
        mystuff.utils.FontLoader.init("resources/fonts/reflow-sans-demo/Reflow Sans DEMO.ttf");
        
        // Set up mouse cursor
        GLFW.glfwSetInputMode(window.getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        
        // Set up mouse callback
        GLFW.glfwSetCursorPosCallback(window.getWindowHandle(), (window, xpos, ypos) -> {
            player.handleMouseInput((float)xpos, (float)ypos);
        });
    }

    private void loop() {
        while (!window.shouldClose()) {
            // Update keyboard state
            KeyboardManager.update(window.getWindowHandle());

            // Update delta time
            updateDeltaTime();

            // Update game state
            player.update(window, deltaTime);
            world.update(window, deltaTime);

            // Clear buffers
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            
            // Set up camera view
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            
            // Apply camera rotation
            GL11.glRotatef(camera.getPitch(), 1.0f, 0.0f, 0.0f);
            GL11.glRotatef(camera.getYaw(), 0.0f, 1.0f, 0.0f);
            
            // Apply camera translation
            GL11.glTranslatef(-camera.getX(), -camera.getY(), -camera.getZ());

            // Enable depth testing and setup alpha
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            
            // Render skybox first
            skybox.render();
            
            // Render opaque objects first
            GL11.glDepthMask(true);
            world.render();  // Render blocks and tree trunks
            playerRenderer.render(player, camera.getYaw(), camera.getPitch());

            // Setup for transparent objects
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);  // Don't write to depth buffer for transparent objects
            
            // Now render transparent objects (like leaves)
            // The world's render method will handle this
            
            // Restore depth mask
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);

            // Update FPS counter
            updateFPS();

            // Check for escape key
            if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
                GLFW.glfwSetWindowShouldClose(window.getWindowHandle(), true);
            }

            // Render FPS counter and debug info (in 2D)
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            
            // Render FPS counter if enabled
            if (Debug.showFPS()) {
                renderText(String.format("FPS: %d", fps), window.getWidth() - 150, 30);
            }
            
            // Render position info if debug mode is enabled
            if (Debug.showPlayerInfo()) {
                renderText(String.format("Position: %.2f, %.2f, %.2f", 
                    camera.getX(), camera.getY(), camera.getZ()), 10, 30);
            }
            
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            
            // Re-enable depth testing
            GL11.glEnable(GL11.GL_DEPTH_TEST);

            window.update();
        }
    }

    public void cleanup() {
        if (player != null) {
            player.cleanup();
        }
        if (world != null) {
            world.cleanup();
        }
        if (skybox != null) {
            skybox.cleanup();
        }
        window.cleanup();
        mystuff.utils.TextureLoader.cleanup();
        mystuff.utils.FontLoader.cleanup();
        playerRenderer.cleanup();
    }

    private void renderText(String text, int x, int y) {
        // Use our new FontLoader to render text
        mystuff.utils.FontLoader.renderText(text, x, y);
    }

    private void updateFPS() {
        fpsCount++;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFpsTime > 1000) {  // Update every second
            fps = fpsCount;
            fpsCount = 0;
            lastFpsTime = currentTime;
        }
    }

    private void updateDeltaTime() {
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f; // Convert nanoseconds to seconds
        lastFrameTime = currentTime;
    }

    private void renderTestTexture() {
        if (testTextureID == -1) return;
        
        // Save current matrices and set up orthographic projection for 2D rendering
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(-1, 1, -1, 1, -1, 1);
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        // Enable 2D texturing
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, testTextureID);
        
        // Draw a quad in the center of the screen
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);  // White color to show texture clearly
        
        // Bottom-left
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(-0.5f, -0.5f);
        
        // Bottom-right
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(0.5f, -0.5f);
        
        // Top-right
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(0.5f, 0.5f);
        
        // Top-left
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(-0.5f, 0.5f);
        
        GL11.glEnd();
        
        // Restore state
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        
        // Restore matrices
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    private void render() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        // Just render our test texture for now
        renderTestTexture();
        
        window.update();
    }

    public static void main(String[] args) {
        new Game().run();
    }
} 