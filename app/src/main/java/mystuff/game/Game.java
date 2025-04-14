package mystuff.game;

import mystuff.engine.Window;
import mystuff.engine.Camera;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import mystuff.game.BlockType;

public class Game {
    private Window window;
    private Camera camera;
    private Player player;
    private World world;
    private Block stoneBlock;
    private long lastFpsTime;
    private int fps;
    private int fpsCount;
    private long lastFrameTime;
    private float deltaTime;

    public Game() {
        window = new Window("3D Game", 1920, 1080); 
        camera = new Camera(0, 0, 0);  // Camera starts at origin
        world = new World();
        player = new Player(3, 10 * World.BLOCK_SIZE, 3, camera, world); 
        lastFpsTime = System.currentTimeMillis();
        lastFrameTime = System.nanoTime();
        fps = 0;
        fpsCount = 0;
        deltaTime = 0;
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
        
        // Set up mouse cursor
        GLFW.glfwSetInputMode(window.getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        
        // Set up mouse callback
        GLFW.glfwSetCursorPosCallback(window.getWindowHandle(), (window, xpos, ypos) -> {
            player.handleMouseInput((float)xpos, (float)ypos);
        });
    }

    private void loop() {
        while (!window.shouldClose()) {
            // Update delta time
            updateDeltaTime();

            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glLoadIdentity();

            // Update FPS counter
            updateFPS();

            // Check for escape key
            if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
                GLFW.glfwSetWindowShouldClose(window.getWindowHandle(), true);
            }

            // Update and render game objects
            player.update(window, deltaTime);  // Pass deltaTime to player update
            
            // Clear transformation matrix
            GL11.glLoadIdentity();
            
            // Apply camera transformations in correct order:
            // 1. First rotate the view (pitch and yaw)
            GL11.glRotatef(camera.getPitch(), 1.0f, 0.0f, 0.0f);
            GL11.glRotatef(camera.getYaw(), 0.0f, 1.0f, 0.0f);
            // 2. Then translate to camera position (negative because we're moving the world relative to camera)
            GL11.glTranslatef(-camera.getX(), -camera.getY(), -camera.getZ());
            
            // Render world
            world.render();
            
            // Then render the player (currently disabled for first-person)
            player.render();
            //stoneBlock.render();

            // Switch to orthographic projection for 2D rendering
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            // Change projection to have Y going up instead of down
            GL11.glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();

            // Disable depth testing for 2D rendering
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            // Render FPS counter in top right
            GL11.glColor3f(1.0f, 1.0f, 1.0f);  // White text
            GL11.glPushMatrix();
            GL11.glTranslatef(window.getWidth() - 150, 30, 0);  // Position in top right
            GL11.glScalef(1.0f, -1.0f, 1.0f);  // Flip text vertically
            renderText(String.format("FPS: %d", fps), 0, 0);
            GL11.glPopMatrix();

            // Render player position text
            GL11.glColor3f(1.0f, 1.0f, 1.0f);  // White text
            GL11.glPushMatrix();
            GL11.glTranslatef(10, 30, 0);  // Moved down a bit for better visibility
            GL11.glScalef(1.0f, -1.0f, 1.0f);  // Flip text vertically
            int gridX = (int)Math.floor(player.getX() / World.BLOCK_SIZE);
            int gridY = (int)Math.floor(player.getY() / World.BLOCK_SIZE);
            int gridZ = (int)Math.floor(player.getZ() / World.BLOCK_SIZE);
            renderText(String.format("Grid: %d, %d, %d", gridX, gridY, gridZ), 0, 0);
            GL11.glPopMatrix();

            // Restore 3D rendering state
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();

            window.update();
        }
    }

    private void cleanup() {
        window.cleanup();
    }

    private void renderText(String text, int x, int y) {
        // Save current state
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(0.5f, 0.5f, 1.0f);  // Increased scale from 0.15 to 0.5

        // Render each character
        for (char c : text.toCharArray()) {
            GL11.glLineWidth(2.0f);  // Make the lines thicker for better visibility
            switch(c) {
                case '.': renderDot(); break;
                case ',': renderComma(); break;
                case '-': renderMinus(); break;
                case ' ': GL11.glTranslatef(20, 0, 0); break;
                default: 
                    if (Character.isDigit(c)) {
                        renderDigit(c - '0');
                    } else if (c == 'P') {
                        renderP();
                    } else if (c == 'o') {
                        renderO();
                    } else if (c == 's') {
                        renderS();
                    } else if (c == 'i') {
                        renderI();
                    } else if (c == 't') {
                        renderT();
                    } else if (c == 'n') {
                        renderN();
                    }
                    GL11.glTranslatef(40, 0, 0);  // Move to next character position
            }
        }
        
        GL11.glPopMatrix();
    }

    // Simple vector font rendering methods
    private void renderDigit(int digit) {
        GL11.glBegin(GL11.GL_LINES);
        switch(digit) {
            case 0:
                GL11.glVertex2f(0, 0); GL11.glVertex2f(30, 0);
                GL11.glVertex2f(30, 0); GL11.glVertex2f(30, 50);
                GL11.glVertex2f(30, 50); GL11.glVertex2f(0, 50);
                GL11.glVertex2f(0, 50); GL11.glVertex2f(0, 0);
                break;
            case 1:
                GL11.glVertex2f(15, 0); GL11.glVertex2f(15, 50);
                break;
            case 2:
                GL11.glVertex2f(0, 50); GL11.glVertex2f(30, 50);
                GL11.glVertex2f(30, 50); GL11.glVertex2f(30, 25);
                GL11.glVertex2f(30, 25); GL11.glVertex2f(0, 25);
                GL11.glVertex2f(0, 25); GL11.glVertex2f(0, 0);
                GL11.glVertex2f(0, 0); GL11.glVertex2f(30, 0);
                break;
            case 3:
                GL11.glVertex2f(0, 50); GL11.glVertex2f(30, 50);
                GL11.glVertex2f(30, 50); GL11.glVertex2f(30, 0);
                GL11.glVertex2f(30, 0); GL11.glVertex2f(0, 0);
                GL11.glVertex2f(0, 25); GL11.glVertex2f(30, 25);
                break;
            case 4:
                GL11.glVertex2f(0, 50); GL11.glVertex2f(0, 25);
                GL11.glVertex2f(0, 25); GL11.glVertex2f(30, 25);
                GL11.glVertex2f(30, 50); GL11.glVertex2f(30, 0);
                break;
            case 5:
                GL11.glVertex2f(30, 50); GL11.glVertex2f(0, 50);
                GL11.glVertex2f(0, 50); GL11.glVertex2f(0, 25);
                GL11.glVertex2f(0, 25); GL11.glVertex2f(30, 25);
                GL11.glVertex2f(30, 25); GL11.glVertex2f(30, 0);
                GL11.glVertex2f(30, 0); GL11.glVertex2f(0, 0);
                break;
            case 6:
                GL11.glVertex2f(30, 50); GL11.glVertex2f(0, 50);
                GL11.glVertex2f(0, 50); GL11.glVertex2f(0, 0);
                GL11.glVertex2f(0, 0); GL11.glVertex2f(30, 0);
                GL11.glVertex2f(30, 0); GL11.glVertex2f(30, 25);
                GL11.glVertex2f(30, 25); GL11.glVertex2f(0, 25);
                break;
            case 7:
                GL11.glVertex2f(0, 50); GL11.glVertex2f(30, 50);
                GL11.glVertex2f(30, 50); GL11.glVertex2f(30, 0);
                break;
            case 8:
                GL11.glVertex2f(0, 0); GL11.glVertex2f(30, 0);
                GL11.glVertex2f(30, 0); GL11.glVertex2f(30, 50);
                GL11.glVertex2f(30, 50); GL11.glVertex2f(0, 50);
                GL11.glVertex2f(0, 50); GL11.glVertex2f(0, 0);
                GL11.glVertex2f(0, 25); GL11.glVertex2f(30, 25);
                break;
            case 9:
                GL11.glVertex2f(30, 25); GL11.glVertex2f(0, 25);
                GL11.glVertex2f(0, 25); GL11.glVertex2f(0, 50);
                GL11.glVertex2f(0, 50); GL11.glVertex2f(30, 50);
                GL11.glVertex2f(30, 50); GL11.glVertex2f(30, 0);
                break;
        }
        GL11.glEnd();
    }

    private void renderDot() {
        GL11.glPointSize(4.0f);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(5, 0);
        GL11.glEnd();
        GL11.glTranslatef(10, 0, 0);
    }

    private void renderComma() {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(5, 0);
        GL11.glVertex2f(5, -10);
        GL11.glEnd();
        GL11.glTranslatef(10, 0, 0);
    }

    private void renderMinus() {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(0, 25);
        GL11.glVertex2f(30, 25);
        GL11.glEnd();
    }

    private void renderP() {
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(0, 50);
        GL11.glVertex2f(30, 50);
        GL11.glVertex2f(30, 25);
        GL11.glVertex2f(0, 25);
        GL11.glEnd();
    }

    private void renderO() {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(30, 0);
        GL11.glVertex2f(30, 50);
        GL11.glVertex2f(0, 50);
        GL11.glEnd();
    }

    private void renderS() {
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(30, 50);
        GL11.glVertex2f(0, 50);
        GL11.glVertex2f(0, 25);
        GL11.glVertex2f(30, 25);
        GL11.glVertex2f(30, 0);
        GL11.glVertex2f(0, 0);
        GL11.glEnd();
    }

    private void renderI() {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(15, 0);
        GL11.glVertex2f(15, 50);
        GL11.glEnd();
    }

    private void renderT() {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(0, 50);
        GL11.glVertex2f(30, 50);
        GL11.glVertex2f(15, 50);
        GL11.glVertex2f(15, 0);
        GL11.glEnd();
    }

    private void renderN() {
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(0, 50);
        GL11.glVertex2f(30, 0);
        GL11.glVertex2f(30, 50);
        GL11.glEnd();
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

    public static void main(String[] args) {
        new Game().run();
    }
} 