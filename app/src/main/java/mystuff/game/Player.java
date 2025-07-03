package mystuff.game;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import mystuff.engine.GameObject;
import mystuff.engine.Window;
import mystuff.engine.Camera;
import mystuff.utils.TextureLoader;
import mystuff.utils.Debug;
import mystuff.utils.KeyboardManager;

public class Player extends GameObject {
    private float speed = 5.0f;
    private float sprintSpeed = 16.0f; // Speed when sprinting
    private boolean isSprinting = false;
    private float size = 10.0f;
    private Camera camera;  // Reference to the camera
    private float mouseSensitivity = 0.2f;
    private boolean firstMouse = true;
    private float lastX = 400, lastY = 300;
    private float velocity = 0.0f;
    private float gravity = -25.0f; // Increased gravity for better feel
    private World world;  // Reference to the world
    private boolean isOnGround = false;
    private float jumpForce = 12.0f;  // Increased jump force for better feel
    private boolean debugMode = true;  // Add debug mode
    private boolean wasSpacePressed = false;  // Track space key state
    private static final float GROUND_CHECK_DISTANCE = 0.1f;  // Increased for better ground detection
    private static final float MAX_VELOCITY = 30.0f;  // Increased max velocity
    
    // Player bounding box for entity collision
    private BoundingBox boundingBox;
    
    // No-clip mode for camera
    private boolean noClipMode = false;
    private boolean wasNoClipMode = false; // Track previous state
    private float cameraSpeed = 0.5f;  // Adjusted for delta-time independent movement in no-clip mode
    
    // Store last position before entering no-clip mode
    private float lastNormalX, lastNormalY, lastNormalZ;

    private static int playerTexture = -1;
    private static final float TEXTURE_SCALE = 1280.0f;  // Your texture width

    private PlayerRenderer renderer;

    public static final float PLAYER_WIDTH = 1.0f;  // Slightly narrower than rendered size
    public static final float PLAYER_HEIGHT = 2.0f; // Player is taller than wide (2x)
    public static final float PLAYER_DEPTH = 1.0f;  // Same as width

    public Player(float x, float y, float z, Camera camera, World world) {
        super(x, y, z);
        this.camera = camera;
        this.world = world;
        // Set initial camera position to player position at eye level (slightly lower than before)
        camera.setPosition(x, y + (PLAYER_HEIGHT * 0.75f), z); // Eye level at approximately head height
        
        // Create player's bounding box for entity collision
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

        // Initialize renderer
        this.renderer = new PlayerRenderer();
        this.renderer.init();
    }

    @Override
    public void update(Window window, float deltaTime) {
        // Add debug mode toggle with F3
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_F3)) {
            Debug.toggleDebugMode();
            Debug.toggleBoundingBoxes();
            Debug.togglePlayerInfo();
        }

        // Handle keyboard input for movement
        handleKeyboardInput(window, deltaTime);

        // Update bounding box position
        updateBoundingBox();
        
        // Heightmap collision and movement
        if (!noClipMode) {
            updateHeightmapCollision(deltaTime);
        }

        if (Debug.showPlayerInfo()) {
            System.out.printf("Position: (%.2f, %.2f, %.2f) Velocity: %.2f OnGround: %b NoClip: %b%n", 
                x, y, z, velocity, isOnGround, noClipMode);
            
            // Show world border info
            float[] bounds = world.getWorldBorderBounds();
            System.out.printf("World Border: X[%.1f, %.1f] Z[%.1f, %.1f]%n", 
                bounds[0], bounds[1], bounds[2], bounds[3]);
        }
    }
    
    /**
     * Updates the bounding box to match the player's position
     */
    private void updateBoundingBox() {
        boundingBox = BoundingBox.fromCenterAndSize(
            x, y, z, 
            PLAYER_WIDTH, PLAYER_HEIGHT, PLAYER_DEPTH
        );
    }
    
    /**
     * Handle keyboard input for movement
     */
    private void handleKeyboardInput(Window window, float deltaTime) {
        float currentSpeed = getCurrentSpeed();
        
        // Handle sprinting
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            setSprinting(true);
        } else {
            setSprinting(false);
        }
        
        // Handle jumping
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_SPACE) && isOnGround && !wasSpacePressed) {
            velocity = jumpForce;
            isOnGround = false;
            if (Debug.showPlayerInfo()) {
                System.out.println("JUMP! Velocity set to: " + jumpForce);
            }
        }
        wasSpacePressed = KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_SPACE);
        
        // Handle movement based on camera direction
        float moveX = 0, moveZ = 0;
        
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_W)) {
            moveZ -= 1; // Forward (negative Z in OpenGL)
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_S)) {
            moveZ += 1; // Backward (positive Z in OpenGL)
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_A)) {
            moveX -= 1; // Left (negative X)
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_D)) {
            moveX += 1; // Right (positive X)
        }
        
        // Normalize movement vector
        if (moveX != 0 || moveZ != 0) {
            float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
            moveX /= length;
            moveZ /= length;
        }
        
        // Apply movement based on camera direction
        float yaw = camera.getYaw();
        float yawRad = (float) Math.toRadians(-yaw); // Invert yaw for correct direction
        
        float forwardX = (float) Math.sin(yawRad);
        float forwardZ = (float) Math.cos(yawRad);
        float rightX = (float) Math.sin(yawRad + Math.PI / 2);
        float rightZ = (float) Math.cos(yawRad + Math.PI / 2);
        
        // Calculate final movement
        float finalMoveX = (moveX * rightX + moveZ * forwardX) * currentSpeed * deltaTime;
        float finalMoveZ = (moveX * rightZ + moveZ * forwardZ) * currentSpeed * deltaTime;
        
        // Apply movement
        x += finalMoveX;
        z += finalMoveZ;
        
        // Apply world border constraints
        if (!noClipMode) {
            float[] borderPos = world.applyWorldBorderForce(x, z);
            x = borderPos[0];
            z = borderPos[1];
        }
        
        // Note: Camera position will be updated in updateHeightmapCollision
        // to ensure it's at the correct height after physics calculations
    }
    
    /**
     * Handle heightmap-based collision and movement
     */
    private void updateHeightmapCollision(float deltaTime) {
        // Get ground height at current position
        float groundHeight = world.getHeightAt(x, z);
        
        // Debug output to see what's happening
        if (Debug.showPlayerInfo()) {
            System.out.printf("Player Y: %.2f, Ground Y: %.2f, Velocity: %.2f%n", y, groundHeight, velocity);
        }
        
        // Apply gravity first
        velocity += gravity * deltaTime;
        velocity = Math.max(velocity, -MAX_VELOCITY); // Limit fall speed
        
        // Apply velocity to get new position
        float newY = y + velocity * deltaTime;
        
        // Check if player would hit the ground
        if (newY <= groundHeight) {
            // Player hit the ground
            y = groundHeight;
            velocity = 0;
            isOnGround = true;
        } else {
            // Player is in the air
            y = newY;
            isOnGround = false;
        }
        
        // Update camera position to follow player
        if (!noClipMode) {
            camera.setPosition(x, y + (PLAYER_HEIGHT * 0.75f), z);
        }
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
    
    public void setNoClipMode(boolean noClipMode) {
        this.noClipMode = noClipMode;
    }
    
    /**
     * Toggle no-clip mode and handle camera position updates
     */
    public void toggleNoClipMode() {
        wasNoClipMode = noClipMode; // Store current state before changing
        noClipMode = !noClipMode;
        
        // If we're entering no-clip mode, update camera position to player's eye level
        if (!wasNoClipMode && noClipMode) {
            camera.setPosition(x, y + (PLAYER_HEIGHT * 0.75f), z);
        }
        
        System.out.println("N key pressed! No-clip mode: " + (noClipMode ? "ON" : "OFF"));
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public float getCameraSpeed() {
        return cameraSpeed;
    }
    
    public float getJumpForce() {
        return jumpForce;
    }
    
    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }
    
    public float getVelocity() {
        return velocity;
    }
    
    public void setOnGround(boolean onGround) {
        this.isOnGround = onGround;
    }
    
    public boolean isOnGround() {
        return isOnGround;
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    public float getSprintSpeed() {
        return sprintSpeed;
    }
    
    public boolean isSprinting() {
        return isSprinting;
    }
    
    public void setSprinting(boolean sprinting) {
        this.isSprinting = sprinting;
    }
    
    public float getCurrentSpeed() {
        return isSprinting ? sprintSpeed : speed;
    }
    
    public int getCollisionCheckRadius() {
        return 0;
    }
    
    public void setCollisionCheckRadius(int radius) {
        // No-op
    }
} 