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
    // Movement speeds
    private float speed = 5.0f;
    private float sprintSpeed = 16.0f;
    private float noClipSpeed = 10.0f;
    private float noClipSprintSpeed = 50.0f;
    private boolean isSprinting = false;
    
    // Camera and input
    private Camera camera;
    private float mouseSensitivity = 0.2f;
    private boolean firstMouse = true;
    private float lastX = 400, lastY = 300;
    
    // Physics
    private float velocity = 0.0f;
    private float gravity = -25.0f;
    private static final float GROUND_LEVEL = 17.0f;
    private boolean isOnGround = false;
    private float jumpForce = 12.0f;
    private boolean wasSpacePressed = false;
    private static final float MAX_VELOCITY = 30.0f;
    
    // Collision
    private BoundingBox boundingBox;
    
    // No-clip mode
    private boolean noClipMode = false;
    private boolean wasNoClipMode = false;
    private float lastNormalX, lastNormalY, lastNormalZ;
    private float noClipCameraX, noClipCameraY, noClipCameraZ;

    // Rendering
    private static int playerTexture = -1;
    private PlayerRenderer renderer;

    // Player dimensions
    public static final float PLAYER_WIDTH = 1.0f;
    public static final float PLAYER_HEIGHT = 2.0f;
    public static final float PLAYER_DEPTH = 1.0f;
    private static final float CAMERA_HEIGHT_OFFSET = 0.95f;

    public Player(float x, float y, float z, Camera camera, World world) {
        super(x, y, z);
        this.camera = camera;
        camera.setPosition(x, y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
        
        updateBoundingBox();

        // Load player texture
        if (playerTexture == -1) {
            playerTexture = TextureLoader.loadTexture("textures/Wolf_Body.jpg");
            if (playerTexture == -1) {
                System.err.println("Failed to load player texture!");
            } else {
                System.out.println("Successfully loaded player texture with ID: " + playerTexture);
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

    @Override
    public void update(Window window, float deltaTime) {
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_F3)) {
            Debug.toggleDebugMode();
            Debug.toggleBoundingBoxes();
            Debug.togglePlayerInfo();
        }

        handleKeyboardInput(window, deltaTime);
        updateBoundingBox();
        
        if (!noClipMode) {
            updateHeightmapCollision(deltaTime);
        }

        if (Debug.showPlayerInfo()) {
            if (noClipMode) {
                System.out.printf("SPIRIT MODE - Player: (%.2f, %.2f, %.2f) Camera: (%.2f, %.2f, %.2f)%n", 
                    x, y, z, noClipCameraX, noClipCameraY, noClipCameraZ);
                System.out.println("*** SPIRIT MODE ACTIVE - Press N to return to player ***");
            } else {
                System.out.printf("Position: (%.2f, %.2f, %.2f) Velocity: %.2f OnGround: %b%n", 
                    x, y, z, velocity, isOnGround);
            }
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
        
        // Handle movement
        float moveX = 0, moveZ = 0, moveY = 0;
        
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_W)) {
            if (noClipMode) {
                moveZ = 1;
            } else {
                moveZ -= 1;
            }
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_S)) {
            if (!noClipMode) {
                moveZ += 1;
            }
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_A)) {
            if (!noClipMode) {
                moveX -= 1;
            }
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_D)) {
            if (!noClipMode) {
                moveX += 1;
            }
        }
        
        // Vertical movement in no-clip mode
        if (noClipMode) {
            if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_SPACE)) {
                moveY += 1;
            }
            if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL)) {
                moveY -= 1;
            }
        }
        
        // Apply movement
        if (noClipMode) {
            applyNoClipMovement(moveX, moveY, moveZ, currentSpeed, deltaTime);
        } else {
            applyNormalMovement(moveX, moveZ, currentSpeed, deltaTime);
        }
    }
    
    /**
     * Apply 3D movement in no-clip mode based on camera orientation
     */
    private void applyNoClipMovement(float moveX, float moveY, float moveZ, float speed, float deltaTime) {
        // Get camera orientation
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        
        // Convert to radians - invert yaw to fix direction
        float yawRad = (float) Math.toRadians(-yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        
        // Calculate forward vector (where camera is looking)
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);
        
        // Forward vector - this is the direction the camera faces
        float forwardX = -sinYaw * cosPitch;
        float forwardY = -sinPitch;
        float forwardZ = -cosYaw * cosPitch;
        
        // Calculate movement based on input
        float finalMoveX = 0;
        float finalMoveY = 0;
        float finalMoveZ = 0;
        
        // W key moves in camera direction
        if (moveZ > 0) {
            finalMoveX = forwardX * speed * deltaTime;
            finalMoveY = forwardY * speed * deltaTime;
            finalMoveZ = forwardZ * speed * deltaTime;
        }
        
        // Space/Shift for vertical movement
        if (moveY != 0) {
            finalMoveY += moveY * speed * deltaTime;
        }
        
        // Apply movement to no-clip camera position
        noClipCameraX += finalMoveX;
        noClipCameraY += finalMoveY;
        noClipCameraZ += finalMoveZ;
        
        // Update camera position
        camera.setPosition(noClipCameraX, noClipCameraY, noClipCameraZ);
    }
    
    private void applyNormalMovement(float moveX, float moveZ, float speed, float deltaTime) {
        if (moveX != 0 || moveZ != 0) {
            float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
            moveX /= length;
            moveZ /= length;
        }
        
        float yaw = camera.getYaw();
        float yawRad = (float) Math.toRadians(-yaw);
        
        float forwardX = (float) Math.sin(yawRad);
        float forwardZ = (float) Math.cos(yawRad);
        float rightX = (float) Math.sin(yawRad + Math.PI / 2);
        float rightZ = (float) Math.cos(yawRad + Math.PI / 2);
        
        float finalMoveX = (moveX * rightX + moveZ * forwardX) * speed * deltaTime;
        float finalMoveZ = (moveX * rightZ + moveZ * forwardZ) * speed * deltaTime;
        
        x += finalMoveX;
        z += finalMoveZ;
    }
    
    private void updateHeightmapCollision(float deltaTime) {
        if (noClipMode) {
            return;
        }
        
        float groundHeight = GROUND_LEVEL;
        
        if (Debug.showPlayerInfo()) {
            System.out.printf("Player Y: %.2f, Ground Y: %.2f, Velocity: %.2f%n", y, groundHeight, velocity);
        }
        
        velocity += gravity * deltaTime;
        velocity = Math.max(velocity, -MAX_VELOCITY);
        
        float newY = y + velocity * deltaTime;
        float feetY = newY - (PLAYER_HEIGHT * 0.5f);
        
        if (feetY <= groundHeight) {
            y = groundHeight + (PLAYER_HEIGHT * 0.5f);
            velocity = 0;
            isOnGround = true;
        } else {
            y = newY;
            isOnGround = false;
        }
        
        camera.setPosition(x, y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
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
    
    public void toggleNoClipMode() {
        wasNoClipMode = noClipMode;
        noClipMode = !noClipMode;
        
        if (!wasNoClipMode && noClipMode) {
            lastNormalX = x;
            lastNormalY = y;
            lastNormalZ = z;
            
            noClipCameraX = x;
            noClipCameraY = y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET);
            noClipCameraZ = z;
            camera.setPosition(noClipCameraX, noClipCameraY, noClipCameraZ);
            
            System.out.println("Entering SPIRIT MODE - Player body stays in place, camera can fly freely");
        } else if (wasNoClipMode && !noClipMode) {
            camera.setPosition(x, y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
            System.out.println("Exiting SPIRIT MODE - Returning to normal gameplay");
        }
        
        System.out.println("N key pressed! No-clip mode: " + (noClipMode ? "ON" : "OFF"));
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public float getCameraSpeed() {
        return 0.5f;
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
        if (noClipMode) {
            return isSprinting ? noClipSprintSpeed : noClipSpeed;
        } else {
            return isSprinting ? sprintSpeed : speed;
        }
    }
    
    public int getCollisionCheckRadius() {
        return 0;
    }
    
    public void setCollisionCheckRadius(int radius) {
        // No-op
    }
} 