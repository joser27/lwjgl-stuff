package mystuff.game;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import mystuff.engine.GameObject;
import mystuff.engine.Window;
import mystuff.engine.Camera;
import mystuff.utils.Debug;
import mystuff.utils.DebugRenderer;
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
    private boolean isOnGround = false;
    private float jumpForce = 12.0f;
    private boolean wasSpacePressed = false;
    private static final float MAX_VELOCITY = 30.0f;
    
    // Simple ground detection and step-up system
    private static final float GROUND_LEVEL = 17.0f;
    private static final float MAX_STEP_HEIGHT = 0.3f; // Maximum height to step up
    private static final float STEP_UP_DISTANCE = 0.1f; // How far forward to test for step-up
    private static final float COLLISION_RESPONSE_FACTOR = 0.8f; // How much to slide along surfaces
    
    // Collision
    private BoundingBox boundingBox;
    private CapsuleCollision capsuleCollision;
    private World world; // Reference to world for collision checks
    private boolean collisionEnabled = true; // Temporary toggle for testing
    private boolean useCapsuleCollision = true; // Toggle between box and capsule collision
    
    // No-clip mode
    private boolean noClipMode = false;
    private boolean wasNoClipMode = false;
    private float lastNormalX, lastNormalY, lastNormalZ;
    private float noClipCameraX, noClipCameraY, noClipCameraZ;

    // Rendering
    private PlayerRenderer renderer;

    // Player dimensions
    public static final float PLAYER_WIDTH = 0.5f;
    public static final float PLAYER_HEIGHT = 0.5f;
    public static final float PLAYER_DEPTH = 0.5f;
    private static final float CAMERA_HEIGHT_OFFSET = 1.0f;
    
    // Capsule collision dimensions (bean-shaped for better movement)
    public static final float CAPSULE_RADIUS = 0.2f;  // Skinnier radius for more precise movement
    public static final float CAPSULE_HEIGHT = 0.6f;  // Taller for better coverage

    public Player(float x, float y, float z, Camera camera, World world) {
        super(x, y, z);
        this.camera = camera;
        this.world = world; // Store world reference for collision checks
        camera.setPosition(x, y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
        
        updateBoundingBox();
        updateCapsuleCollision();

        this.renderer = new PlayerRenderer();
        this.renderer.init();
    }

    @Override
    public void update(Window window, float deltaTime) {
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_F3)) {
            Debug.toggleDebugMode();
            Debug.togglePlayerInfo();
        }
        
        // Temporary collision toggle for testing
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_C)) {
            collisionEnabled = !collisionEnabled;
            DebugRenderer.getInstance().addMessage("Collision detection: " + (collisionEnabled ? "ENABLED" : "DISABLED"), 3.0f);
        }
        
        // Show detailed collision debug info
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_V)) {
            DebugRenderer.getInstance().addMessage("=== DETAILED COLLISION DEBUG INFO ===", 5.0f);
            DebugRenderer.getInstance().addMessage(CollisionManager.getInstance().getDebugInfo(), 5.0f);
            DebugRenderer.getInstance().addMessage("=== END COLLISION DEBUG INFO ===", 5.0f);
        }
        
        // Toggle between box and capsule collision
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_B)) {
            useCapsuleCollision = !useCapsuleCollision;
            DebugRenderer.getInstance().addMessage("Collision type: " + (useCapsuleCollision ? "CAPSULE (Bean-shaped)" : "BOX (Cube)"), 3.0f);
        }

        handleKeyboardInput(window, deltaTime);
        updateBoundingBox();
        updateCapsuleCollision();
        
        if (!noClipMode) {
            updateHeightmapCollision(deltaTime);
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
     * Updates the capsule collision to match the player's position
     */
    private void updateCapsuleCollision() {
        capsuleCollision = CapsuleCollision.fromCenterAndSize(
            x, y, z,
            CAPSULE_RADIUS, CAPSULE_HEIGHT
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
        
        // Apply movement with step-up system and collision response
        applyMovementWithStepUp(finalMoveX, finalMoveZ);
    }

    /**
     * Apply movement with simple collision detection and step-up
     */
    private void applyMovementWithStepUp(float moveX, float moveZ) {
        float newX = x + moveX;
        float newZ = z + moveZ;
        
        // First, try normal movement at current height
        if (!wouldCollide(newX, y, newZ)) {
            x = newX;
            z = newZ;
            return;
        }
        
        // If normal movement fails, try step-up
        float stepUpY = tryStepUpWithRaycast(newX, newZ);
        if (stepUpY > y) {
            x = newX;
            z = newZ;
            y = stepUpY;
            return;
        }
        
        // If step-up fails, try sliding along surfaces
        float[] slideResult = trySliding(moveX, moveZ);
        if (slideResult != null) {
            x += slideResult[0];
            z += slideResult[1];
            return;
        }
        
        // If all else fails, try moving in individual axes
        if (!wouldCollide(x + moveX, y, z)) {
            x += moveX;
        } else if (!wouldCollide(x, y, z + moveZ)) {
            z += moveZ;
        }
    }

    /**
     * Try to step up over small obstacles
     */
    private float tryStepUpWithRaycast(float targetX, float targetZ) {
        // Simple step-up: try moving to target position at gradually higher heights
        for (float testHeight = 0.1f; testHeight <= MAX_STEP_HEIGHT; testHeight += 0.05f) {
            float testY = y + testHeight;
            
            // Check if we can move to the target position at this height
            if (!wouldCollide(targetX, testY, targetZ)) {
                return testY;
            }
        }
        
        return y; // No step-up possible
    }

    /**
     * Try sliding along collision surfaces
     */
    private float[] trySliding(float moveX, float moveZ) {
        // Try sliding along X axis
        if (Math.abs(moveX) > 0.001f && !wouldCollide(x + moveX * COLLISION_RESPONSE_FACTOR, y, z)) {
            return new float[]{moveX * COLLISION_RESPONSE_FACTOR, 0};
        }
        
        // Try sliding along Z axis
        if (Math.abs(moveZ) > 0.001f && !wouldCollide(x, y, z + moveZ * COLLISION_RESPONSE_FACTOR)) {
            return new float[]{0, moveZ * COLLISION_RESPONSE_FACTOR};
        }
        
        // Try diagonal sliding
        float diagonalX = moveX * COLLISION_RESPONSE_FACTOR * 0.7f;
        float diagonalZ = moveZ * COLLISION_RESPONSE_FACTOR * 0.7f;
        if (!wouldCollide(x + diagonalX, y, z + diagonalZ)) {
            return new float[]{diagonalX, diagonalZ};
        }
        
        return null; // No sliding possible
    }
    
    /**
     * Check if player would collide with world objects at given position
     */
    private boolean wouldCollide(float newX, float newY, float newZ) {
        // Skip collision detection if disabled
        if (!collisionEnabled) {
            return false;
        }
        
        if (useCapsuleCollision) {
            // Use capsule collision for smoother movement
            CapsuleCollision tempCapsule = CapsuleCollision.fromCenterAndSize(newX, newY, newZ, CAPSULE_RADIUS, CAPSULE_HEIGHT);
            return checkCapsuleCollision(tempCapsule);
        } else {
            // Use bounding box collision (legacy)
            BoundingBox tempBox = BoundingBox.fromCenterAndSize(newX, newY, newZ, PLAYER_WIDTH, PLAYER_HEIGHT, PLAYER_DEPTH);
            return CollisionManager.getInstance().checkCollision(tempBox);
        }
    }
    
    /**
     * Check capsule collision with world objects (optimized)
     */
    private boolean checkCapsuleCollision(CapsuleCollision capsule) {
        // Use the capsule's bounding box for collision detection
        // This is much faster than complex capsule geometry checks
        BoundingBox capsuleBox = capsule.getBoundingBox();
        return CollisionManager.getInstance().checkCollision(capsuleBox);
    }
    
    /**
     * Check collision with all collidable entities
     */
    private boolean checkEntityCollisions(BoundingBox playerBox) {
        // Use CollisionManager for collision detection
        return CollisionManager.getInstance().checkCollision(playerBox);
    }
    
    private void updateHeightmapCollision(float deltaTime) {
        if (noClipMode) {
            return;
        }
        
        // Apply gravity
        velocity += gravity * deltaTime;
        velocity = Math.max(velocity, -MAX_VELOCITY);
        
        // Calculate new Y position
        float newY = y + velocity * deltaTime;
        
        // Simple ground detection
        float groundHeight = GROUND_LEVEL;
        float capsuleBottom = newY - CAPSULE_HEIGHT / 2.0f;
        
        if (capsuleBottom <= groundHeight) {
            // On ground
            y = groundHeight + CAPSULE_HEIGHT / 2.0f;
            velocity = 0;
            isOnGround = true;
        } else {
            // In air
            y = newY;
            isOnGround = false;
        }
        
        // Update camera position
        camera.setPosition(x, y + (CAPSULE_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
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
    
    public CapsuleCollision getCapsuleCollision() {
        return capsuleCollision;
    }
    
    public boolean isUsingCapsuleCollision() {
        return useCapsuleCollision;
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
            
            DebugRenderer.getInstance().addMessage("Entering SPIRIT MODE - Player body stays in place, camera can fly freely", 3.0f);
        } else if (wasNoClipMode && !noClipMode) {
            camera.setPosition(x, y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
            DebugRenderer.getInstance().addMessage("Exiting SPIRIT MODE - Returning to normal gameplay", 3.0f);
        }
        
        DebugRenderer.getInstance().addMessage("N key pressed! No-clip mode: " + (noClipMode ? "ON" : "OFF"), 2.0f);
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