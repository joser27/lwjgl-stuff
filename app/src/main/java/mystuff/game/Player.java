package mystuff.game;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import mystuff.engine.GameObject;
import mystuff.engine.Window;
import mystuff.engine.Camera;
import mystuff.utils.Shapes;
import mystuff.utils.TextureLoader;
import java.util.List;
import mystuff.utils.Debug;
import mystuff.utils.KeyboardManager;

public class Player extends GameObject {
    private float speed = 5.0f;
    private float size = 10.0f;
    private Camera camera;  // Reference to the camera
    private float mouseSensitivity = 0.2f;
    private boolean firstMouse = true;
    private float lastX = 400, lastY = 300;
    private float velocity = 0.0f;
    private float gravity = -9.0f;
    private World world;  // Reference to the world
    private boolean isOnGround = false;
    private float jumpForce = 8.0f;  // Increased for better Minecraft-like jump
    private boolean debugMode = true;  // Add debug mode
    private boolean wasSpacePressed = false;  // Track space key state
    private static final float GROUND_CHECK_DISTANCE = 0.05f;  // How far below to check for ground
    private static final float MAX_VELOCITY = 20.0f;  // Reduced maximum velocity
    private BoundingBox boundingBox; // Player's bounding box
    
    // Player dimensions for bounding box
    private static final float PLAYER_WIDTH = World.BLOCK_SIZE;  // Slightly narrower than rendered size
    private static final float PLAYER_HEIGHT = World.BLOCK_SIZE*2; // Player is taller than wide (2x)
    private static final float PLAYER_DEPTH = World.BLOCK_SIZE;  // Same as width
    
    // Store last grounded position to prevent teleporting
    private float lastGroundY = 0;
    
    // No-clip mode for camera
    private boolean noClipMode = false;
    private boolean wasNPressed = false;
    private float cameraSpeed = 10.0f;  // Increased for faster no-clip movement

    private static int playerTexture = -1;
    private static final float TEXTURE_SCALE = 1280.0f;  // Your texture width

    private PlayerRenderer renderer;

    public Player(float x, float y, float z, Camera camera, World world) {
        super(x, y, z);
        this.camera = camera;
        this.world = world;
        this.lastGroundY = y;
        // Set initial camera position to player position at eye level (slightly lower than before)
        camera.setPosition(x, y + (PLAYER_HEIGHT * 0.75f), z); // Eye level at approximately head height
        
        // Create player's bounding box
        updateBoundingBox();

        // Load player texture if not already loaded
        if (playerTexture == -1) {
            playerTexture = TextureLoader.loadTexture("resources/textures/player.png");
            if (playerTexture == -1) {
                System.err.println("Failed to load player texture!");
            } else {
                System.out.println("Successfully loaded player texture with ID: " + playerTexture);
                // Set texture parameters for smoother rendering
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, playerTexture);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            }
        }

        this.renderer = new PlayerRenderer();
        this.renderer.init();
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
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_N)) {
            noClipMode = !noClipMode;
            if (Debug.showPlayerInfo()) System.out.println("No-clip mode: " + (noClipMode ? "ON" : "OFF"));
        }
        
        // Add debug mode toggle with F3
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_F3)) {
            Debug.toggleDebugMode();
            Debug.toggleBoundingBoxes();
            Debug.togglePlayerInfo();
        }

        if (noClipMode) {
            updateCameraNoClip(window, deltaTime);
        } else {
            updatePlayerPhysics(window, deltaTime);
        }

        if (Debug.showPlayerInfo()) {
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

        float dx = 0, dy = 0, dz = 0;

        // Forward/Backward
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_W)) {
            dx += forwardX * cameraSpeed * deltaTime;
            dz -= forwardZ * cameraSpeed * deltaTime;
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_S)) {
            dx -= forwardX * cameraSpeed * deltaTime;
            dz += forwardZ * cameraSpeed * deltaTime;
        }

        // Strafe Left/Right
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_A)) {
            dx -= rightX * cameraSpeed * deltaTime;
            dz -= rightZ * cameraSpeed * deltaTime;
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_D)) {
            dx += rightX * cameraSpeed * deltaTime;
            dz += rightZ * cameraSpeed * deltaTime;
        }
        
        // Up/Down
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_SPACE)) {
            dy += cameraSpeed * deltaTime;
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            dy -= cameraSpeed * deltaTime;
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
        // Check for reset key
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_R)) {
            x = 5.0f;
            y = 5.0f;
            z = 5.0f;
        }

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
        boolean isSpacePressed = KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_SPACE);
        if (isSpacePressed && !wasSpacePressed && isOnGround) {
            velocity = jumpForce;
            isOnGround = false;
            if (Debug.showPlayerInfo()) System.out.println("Jump initiated! Velocity: " + velocity);
        }
        wasSpacePressed = isSpacePressed;

        // Apply gravity and vertical movement
        if (!isOnGround) {
            velocity += gravity * deltaTime;
            // Clamp velocity to maximum speed
            velocity = Math.max(Math.min(velocity, MAX_VELOCITY), -MAX_VELOCITY);
            if (Debug.showPlayerInfo() && Math.abs(velocity) >= MAX_VELOCITY) {
                System.out.println("Velocity clamped at: " + velocity);
            }
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
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_W)) {
            dx += forwardX * speed * deltaTime;
            dz -= forwardZ * speed * deltaTime;
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_S)) {
            dx -= forwardX * speed * deltaTime;
            dz += forwardZ * speed * deltaTime;
        }

        // Strafe Left/Right
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_A)) {
            dx -= rightX * speed * deltaTime;
            dz -= rightZ * speed * deltaTime;
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_D)) {
            dx += rightX * speed * deltaTime;
            dz += rightZ * speed * deltaTime;
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

        // Update camera position to follow player at proper eye level
        camera.setPosition(x, y + (PLAYER_HEIGHT * 0.75f), z);
    }

    @Override
    public void render() {
        renderer.render(this, camera.getYaw(), camera.getPitch());
    }

    public void cleanup() {
        if (renderer != null) {
            renderer.cleanup();
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
    
    public boolean isNoClipMode() {
        return noClipMode;
    }
} 