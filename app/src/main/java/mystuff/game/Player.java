package mystuff.game;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import mystuff.engine.GameObject;
import mystuff.engine.Window;
import mystuff.engine.Camera;
import mystuff.utils.Shapes;

public class Player extends GameObject {
    private float speed = 0.1f;
    private float size = 1.0f;
    private Camera camera;  // Reference to the camera
    private float mouseSensitivity = 0.1f;
    private boolean firstMouse = true;
    private float lastX = 400, lastY = 300;
    private float velocity = 0.0f;
    private float gravity = -20.0f;
    private World world;  // Reference to the world
    private boolean isOnGround = false;
    private float jumpForce = 10.0f;  // Increased jump force
    private boolean debugMode = true;  // Add debug mode
    private boolean wasSpacePressed = false;  // Track space key state
    private static final float GROUND_CHECK_DISTANCE = 0.05f;  // How far below to check for ground

    public Player(float x, float y, float z, Camera camera, World world) {
        super(x, y, z);
        this.camera = camera;
        this.world = world;
        // Set initial camera position to player position
        camera.setPosition(x, y + size, z); // Eye level is above player position
    }

    @Override
    public void update(Window window, float deltaTime) {
        // Ground check first - check a small distance below the player
        isOnGround = world.checkBoxCollision(x, y - GROUND_CHECK_DISTANCE, z, size, size*2, size);
        
        // Handle jumping
        boolean isSpacePressed = GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        if (isSpacePressed && !wasSpacePressed && isOnGround) {
            velocity = jumpForce;
            isOnGround = false;
            if (debugMode) System.out.println("Jump initiated! Velocity: " + velocity);
        }
        wasSpacePressed = isSpacePressed;

        // Apply gravity and vertical movement
        if (!isOnGround) {
            velocity += gravity * deltaTime;
        }
        float newY = y + velocity * deltaTime;
        
        // Check if we would collide with ground
        if (!world.checkBoxCollision(x, newY, z, size, size*2, size)) {
            y = newY;
        } else {
            // We hit something
            if (velocity < 0) {
                // Only stop if we're moving downward
                velocity = 0;
                isOnGround = true;
                
                // Find the block we're colliding with
                int[] gridCoords = world.worldToGridCoords(x, newY - size, z);
                Block block = world.getBlockAt(gridCoords[0], gridCoords[1], gridCoords[2]);
                if (block != null) {
                    // Position player exactly at the block's top surface
                    y = block.getY() + World.BLOCK_SIZE + size;
                }
            } else {
                // We hit a ceiling
                velocity = 0;
            }
        }

        // Calculate movement direction based on camera yaw
        float yaw = (float) Math.toRadians(camera.getYaw());
        
        // Forward vector points where the camera is looking
        float forwardX = (float) Math.sin(yaw);
        float forwardZ = (float) Math.cos(yaw);
        
        // Right vector is perpendicular to forward
        float rightX = (float) Math.cos(yaw);
        float rightZ = (float) Math.sin(yaw);

        float dx = 0, dz = 0;

        // Forward/Backward
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            dx += forwardX * speed;
            dz -= forwardZ * speed;
        }
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            dx -= forwardX * speed;
            dz += forwardZ * speed;
        }

        // Strafe Left/Right
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            dx -= rightX * speed;
            dz -= rightZ * speed;
        }
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            dx += rightX * speed;
            dz += rightZ * speed;
        }

        // Test movement in each axis separately to allow sliding along walls
        float newX = x + dx;
        float newZ = z + dz;

        // Check collision for each axis separately
        if (!world.checkBoxCollision(newX, y, z, size, size*2, size)) {
            x = newX;
        }
        if (!world.checkBoxCollision(x, y, newZ, size, size*2, size)) {
            z = newZ;
        }

        if (debugMode) {
            System.out.printf("Position: (%.2f, %.2f, %.2f) Velocity: %.2f OnGround: %b%n", 
                x, y, z, velocity, isOnGround);
        }

        // Update camera position to follow player
        camera.setPosition(x, y + size, z);
    }

    @Override
    public void render() {
        // Third-person view (optional)
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);
        GL11.glColor3f(0.0f, 1.0f, 0.0f);
        Shapes.cube(size);
        GL11.glPopMatrix();
    }

    public void handleMouseInput(float xpos, float ypos) {
        if (firstMouse) {
            lastX = xpos;
            lastY = ypos;
            firstMouse = false;
            return;
        }

        float xoffset = xpos - lastX;
        float yoffset = lastY - ypos;
        lastX = xpos;
        lastY = ypos;

        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;

        camera.rotate(yoffset, xoffset);
    }
} 