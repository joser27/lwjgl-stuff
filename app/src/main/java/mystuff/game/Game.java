package mystuff.game;

import mystuff.engine.Window;
import mystuff.engine.Camera;
import mystuff.engine.IGameLogic;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import mystuff.utils.Debug;
import mystuff.utils.KeyboardManager;

public class Game implements IGameLogic {
    private Camera camera;
    private Player player;
    private World world;
    private PlayerRenderer playerRenderer;
    private Skybox skybox;
    private int fps;
    private int fpsCount;
    private long lastFpsTime;
    private float[] frameTimeHistory;
    private int frameTimeIndex;
    private static final int FRAME_TIME_SAMPLES = 120; // 2 seconds of history at 60fps
    private float averageFrameTime;
    private float maxFrameTime;

    @Override
    public void init(Window window) {
        // Initialize OpenGL state
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Initialize game objects
        camera = new Camera(0, 0, 0);
        world = new World(camera);
        player = new Player(100*World.BLOCK_SIZE, 50*World.BLOCK_SIZE, 100*World.BLOCK_SIZE, camera, world);
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
        
        lastFpsTime = System.nanoTime();
        frameTimeHistory = new float[FRAME_TIME_SAMPLES];
        frameTimeIndex = 0;
        averageFrameTime = 0;
        maxFrameTime = 0;
    }

    @Override
    public void input(Window window) {
        KeyboardManager.update(window.getWindowHandle());
        
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
            GLFW.glfwSetWindowShouldClose(window.getWindowHandle(), true);
        }
    }

    @Override
    public void update(float interval) {
        player.update(null, interval);
        world.update(null, interval);
        updateFPS();
    }

    @Override
    public void render(Window window) {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
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
        
        // Handle camera position based on mode
        if (player.isNoClipMode()) {
            GL11.glTranslatef(-player.getX(), -player.getY(), -player.getZ());
            camera.update();
            GL11.glLoadIdentity();
            GL11.glRotatef(camera.getPitch(), 1.0f, 0.0f, 0.0f);
            GL11.glRotatef(camera.getYaw(), 0.0f, 1.0f, 0.0f);
            GL11.glTranslatef(-camera.getX(), -camera.getY(), -camera.getZ());
        } else {
            GL11.glTranslatef(-camera.getX(), -camera.getY(), -camera.getZ());
            camera.update();
        }
        
        // Save initial state
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        
        // Render game objects
        skybox.render();
        world.render(camera);
        playerRenderer.render(player, camera.getYaw(), camera.getPitch());
        
        // Render UI
        renderUI(window);
        
        // Restore state
        GL11.glPopAttrib();
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
        
        // Render FPS and frame timing info if enabled
        if (Debug.showFPS()) {
            renderText(String.format("FPS: %d", fps), window.getWidth() - 150, 30);
            renderText(String.format("Frame Time: %.1fms (Max: %.1fms)", averageFrameTime, maxFrameTime), 
                      window.getWidth() - 250, 50);
            
            // Visual warning if we detect significant stutters
            if (maxFrameTime > 32.0f) { // More than 2 frames at 60fps
                GL11.glColor3f(1.0f, 0.0f, 0.0f); // Red warning
                renderText("WARNING: Frame time spikes detected!", window.getWidth() - 300, 70);
                GL11.glColor3f(1.0f, 1.0f, 1.0f); // Reset color
            }
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
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private void renderText(String text, int x, int y) {
        mystuff.utils.FontLoader.renderText(text, x, y);
    }

    private void updateFPS() {
        long currentTime = System.nanoTime();
        float frameTime = (currentTime - lastFpsTime) / 1_000_000.0f; // Convert to milliseconds
        lastFpsTime = currentTime;
        
        // Update frame time history
        frameTimeHistory[frameTimeIndex] = frameTime;
        frameTimeIndex = (frameTimeIndex + 1) % FRAME_TIME_SAMPLES;
        
        // Calculate average and max frame time
        float sum = 0;
        maxFrameTime = 0;
        for (float time : frameTimeHistory) {
            sum += time;
            if (time > maxFrameTime) maxFrameTime = time;
        }
        averageFrameTime = sum / FRAME_TIME_SAMPLES;
        
        // Calculate FPS from average frame time
        fps = (int)(1000.0f / averageFrameTime);
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
        mystuff.engine.GameEngine engine = new mystuff.engine.GameEngine("3D Game", 1920, 1080, game, 60);
        engine.run();
    }
} 