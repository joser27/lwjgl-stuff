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
    private float sprintSpeed = 8.0f; // Speed when sprinting
    private boolean isSprinting = false;
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
    public static final float PLAYER_WIDTH = World.BLOCK_SIZE;  // Slightly narrower than rendered size
    public static final float PLAYER_HEIGHT = World.BLOCK_SIZE*2; // Player is taller than wide (2x)
    public static final float PLAYER_DEPTH = World.BLOCK_SIZE;  // Same as width
    
    // Collision detection radius (in chunks)
    private int collisionCheckRadius = 1;
    
    // Store last grounded position to prevent teleporting
    private float lastGroundY = 0;
    
    // No-clip mode for camera
    private boolean noClipMode = false;
    private boolean wasNPressed = false;
    private float cameraSpeed = 0.5f;  // Adjusted for delta-time independent movement in no-clip mode
    
    // Store last position before entering no-clip mode
    private float lastNormalX, lastNormalY, lastNormalZ;

    private static int playerTexture = -1;
    private static final float TEXTURE_SCALE = 1280.0f;  // Your texture width

    private PlayerRenderer renderer;
    private PlayerPhysics physics;

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

        // Initialize physics and renderer
        this.physics = new PlayerPhysics();
        this.renderer = new PlayerRenderer();
        this.renderer.init();
    }
    
    /**
     * Updates the bounding box to match the player's position
     */
    public void updateBoundingBox() {
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
        boolean wasNoClipMode = noClipMode;
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_N)) {
            noClipMode = !noClipMode;
            
            // If we're entering no-clip mode, update camera position to player's eye level
            if (!wasNoClipMode && noClipMode) {
                camera.setPosition(x, y + (PLAYER_HEIGHT * 0.75f), z);
            }
            
            if (Debug.showPlayerInfo()) System.out.println("No-clip mode: " + (noClipMode ? "ON" : "OFF"));
        }
        
        // Add debug mode toggle with F3
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_F3)) {
            Debug.toggleDebugMode();
            Debug.toggleBoundingBoxes();
            Debug.togglePlayerInfo();
        }

        // Delegate physics updates to the PlayerPhysics class
        physics.updatePhysics(this, window, deltaTime, camera, world, noClipMode, Debug.showPlayerInfo());

        if (Debug.showPlayerInfo()) {
            System.out.printf("Position: (%.2f, %.2f, %.2f) Velocity: %.2f OnGround: %b NoClip: %b%n", 
                x, y, z, velocity, isOnGround, noClipMode);
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
    
    /**
     * Gets the current active movement speed based on whether sprinting is active
     */
    public float getCurrentSpeed() {
        return isSprinting ? sprintSpeed : speed;
    }
    
    /**
     * Gets the collision check radius (in chunks)
     */
    public int getCollisionCheckRadius() {
        return collisionCheckRadius;
    }
    
    /**
     * Sets the collision check radius (in chunks)
     */
    public void setCollisionCheckRadius(int radius) {
        // Ensure radius is at least 1 to include current chunk
        this.collisionCheckRadius = Math.max(1, radius);
    }
} 