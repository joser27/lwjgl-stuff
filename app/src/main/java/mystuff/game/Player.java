package mystuff.game;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import mystuff.engine.GameObject;
import mystuff.engine.Window;
import mystuff.engine.Camera;
import mystuff.utils.Shapes;
import java.util.List;

public class Player extends GameObject {
    private float speed = 0.1f;
    private float size = 1.0f;
    private Camera camera;  // Reference to the camera
    private float mouseSensitivity = 0.1f;
    private boolean firstMouse = true;
    private float lastX = 400, lastY = 300;
    private float velocity = 0.0f;
    private float gravity = -10.0f;
    private World world;  // Reference to the world
    private boolean isOnGround = false;
    private float jumpForce = 10.0f;  // Increased jump force
    private boolean debugMode = true;  // Add debug mode
    private boolean wasSpacePressed = false;  // Track space key state
    private static final float GROUND_CHECK_DISTANCE = 0.05f;  // How far below to check for ground
    private BoundingBox boundingBox; // Player's bounding box
    
    // Player dimensions for bounding box
    private static final float PLAYER_WIDTH = 1.8f;  // Slightly narrower than rendered size
    private static final float PLAYER_HEIGHT = 3.6f; // Player is taller than wide (2x)
    private static final float PLAYER_DEPTH = 1.8f;  // Same as width
    
    // Store last grounded position to prevent teleporting
    private float lastGroundY = 0;
    
    // No-clip mode for camera
    private boolean noClipMode = false;
    private boolean wasNPressed = false;
    private float cameraSpeed = 0.2f;  // Faster camera movement in no-clip mode

    public Player(float x, float y, float z, Camera camera, World world) {
        super(x, y, z);
        this.camera = camera;
        this.world = world;
        this.lastGroundY = y;
        // Set initial camera position to player position
        camera.setPosition(x, y + size, z); // Eye level is above player position
        
        // Create player's bounding box
        updateBoundingBox();
    }
    
    /**
     * Updates the bounding box to match the player's position
     */
    private void updateBoundingBox() {
        // Create a bounding box that's slightly smaller than the rendered player
        // for better collision detection
        boundingBox = BoundingBox.fromCenterAndSize(
            x, y, z, 
            PLAYER_WIDTH, PLAYER_HEIGHT, PLAYER_DEPTH
        );
    }

    @Override
    public void update(Window window, float deltaTime) {
        // Check for no-clip mode toggle
        boolean isNPressed = GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_N) == GLFW.GLFW_PRESS;
        if (isNPressed && !wasNPressed) {
            noClipMode = !noClipMode;
            if (debugMode) System.out.println("No-clip mode: " + (noClipMode ? "ON" : "OFF"));
        }
        wasNPressed = isNPressed;
        
        if (noClipMode) {
            // No-clip camera mode - move camera independently
            updateCameraNoClip(window, deltaTime);
        } else {
            // Normal mode - update player physics and camera follows player
            updatePlayerPhysics(window, deltaTime);
        }

        if (debugMode) {
            System.out.printf("Position: (%.2f, %.2f, %.2f) Velocity: %.2f OnGround: %b NoClip: %b%n", 
                x, y, z, velocity, isOnGround, noClipMode);
        }
    }
    
    /**
     * Updates camera position in no-clip mode
     */
    private void updateCameraNoClip(Window window, float deltaTime) {
        // Calculate movement direction based on camera yaw
        float yaw = (float) Math.toRadians(camera.getYaw());
        
        // Forward vector points where the camera is looking
        float forwardX = (float) Math.sin(yaw);
        float forwardZ = (float) Math.cos(yaw);
        
        // Right vector is perpendicular to forward
        float rightX = (float) Math.cos(yaw);
        float rightZ = (float) Math.sin(yaw);
        
        // Up vector is always up in world space
        float upX = 0;
        float upY = 1;
        float upZ = 0;

        float dx = 0, dy = 0, dz = 0;

        // Forward/Backward
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            dx += forwardX * cameraSpeed;
            dz -= forwardZ * cameraSpeed;
        }
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            dx -= forwardX * cameraSpeed;
            dz += forwardZ * cameraSpeed;
        }

        // Strafe Left/Right
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            dx -= rightX * cameraSpeed;
            dz -= rightZ * cameraSpeed;
        }
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            dx += rightX * cameraSpeed;
            dz += rightZ * cameraSpeed;
        }
        
        // Up/Down
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            dy += cameraSpeed;
        }
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            dy -= cameraSpeed;
        }
        
        // Move camera directly without collision
        camera.setPosition(
            camera.getX() + dx,
            camera.getY() + dy,
            camera.getZ() + dz
        );
    }
    
    /**
     * Updates player position and physics (normal mode)
     */
    private void updatePlayerPhysics(Window window, float deltaTime) {
        // Get all blocks from the world
        List<Block> blocks = world.getAllBlocks();
        
        // Check if player is standing on ground by checking collision with blocks
        isOnGround = false;
        for (Block block : blocks) {
            BoundingBox blockBB = block.getBoundingBox();
            if (boundingBox.isOnTopOf(blockBB, 0.1f)) {
                isOnGround = true;
                break;
            }
        }
        
        // Track the last ground position when we're on ground
        if (isOnGround) {
            lastGroundY = y;
        }
        
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
        } else {
            // Reset velocity when on ground
            velocity = 0;
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

        // Calculate vertical movement for this frame
        float dy = velocity * deltaTime;
        
        // Test movement in each axis separately to allow sliding along walls
        // Create temporary bounding boxes for testing movement
        BoundingBox xMovedBox = boundingBox.getTranslated(dx, 0, 0);
        BoundingBox zMovedBox = boundingBox.getTranslated(0, 0, dz);
        BoundingBox yMovedBox = boundingBox.getTranslated(0, dy, 0);
        
        // Check collisions for X movement
        boolean xCollision = false;
        for (Block block : blocks) {
            if (xMovedBox.intersects(block.getBoundingBox())) {
                xCollision = true;
                break;
            }
        }
        if (!xCollision) {
            x += dx;
        }
        
        // Check collisions for Z movement
        boolean zCollision = false;
        for (Block block : blocks) {
            if (zMovedBox.intersects(block.getBoundingBox())) {
                zCollision = true;
                break;
            }
        }
        if (!zCollision) {
            z += dz;
        }
        
        // Check collisions for Y movement
        boolean yCollision = false;
        float groundLevel = 0; // Default ground level is 0
        for (Block block : blocks) {
            BoundingBox blockBB = block.getBoundingBox();
            if (yMovedBox.intersects(blockBB)) {
                yCollision = true;
                if (velocity < 0) {
                    // If moving down, we hit ground
                    // Set ground level to the top of the block
                    groundLevel = blockBB.getMaxY();
                }
                break;
            }
        }
        
        if (!yCollision) {
            // No collision, apply vertical movement
            y += dy;
        } else {
            // We hit something
            if (velocity < 0) {
                // If moving down, we hit ground
                velocity = 0;
                isOnGround = true;
                
                // Position player so bottom of bounding box is at ground level
                // Add half player height to set center position
                y = groundLevel + (PLAYER_HEIGHT / 2);
            } else {
                // If moving up, we hit ceiling
                velocity = 0;
            }
        }
        
        // Update the bounding box to the new position
        updateBoundingBox();

        // Update camera position to follow player
        camera.setPosition(x, y + size, z);
    }

    @Override
    public void render() {        
        // Debug rendering of the bounding box (when in debug mode)
        if (debugMode) {
            GL11.glPushMatrix();
            GL11.glTranslatef(boundingBox.getCenterX(), boundingBox.getCenterY(), boundingBox.getCenterZ());
            GL11.glColor3f(1.0f, 0.0f, 0.0f);  // Red for bounding box
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);  // Wireframe mode
            Shapes.cuboid(PLAYER_WIDTH, PLAYER_HEIGHT, PLAYER_DEPTH);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);  // Back to fill mode
            GL11.glPopMatrix();
        }
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
    
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }
} 