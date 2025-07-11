package mystuff.game;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import mystuff.engine.GameObject;
import mystuff.engine.Window;
import mystuff.engine.Camera;
import mystuff.utils.TextureLoader;
import mystuff.utils.Debug;
import mystuff.utils.DebugRenderer;
import mystuff.utils.KeyboardManager;
import mystuff.utils.Shapes;

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
    private static final float GROUND_CHECK_EPSILON = 0.02f; // Increased buffer
    private float lastGroundY = 0.0f; // Track last stable ground position
    
    // Step-up parameters
    private static final float MAX_STEP_HEIGHT = 0.35f;
    private static final float STEP_CHECK_DISTANCE = 0.05f;
    private static final float STEP_SMOOTHING = 8.0f;
    private float currentStepOffset = 0.0f;
    private boolean isSteppingDown = false; // Track if we're in a step-down motion

    // Collision
    private BoundingBox boundingBox;
    private World world; // Reference to world for collision checks
    private boolean collisionEnabled = true; // Temporary toggle for testing
    
    // No-clip mode
    private boolean noClipMode = false;
    private boolean wasNoClipMode = false;
    private float lastNormalX, lastNormalY, lastNormalZ;
    private float noClipCameraX, noClipCameraY, noClipCameraZ;

    // Rendering
    private static int playerTexture = -1;
    private PlayerRenderer renderer;

    // Player dimensions
    public static final float PLAYER_WIDTH = 0.3f;
    public static final float PLAYER_HEIGHT = 1.0f;
    public static final float PLAYER_DEPTH = 0.3f;
    private static final float CAMERA_HEIGHT_OFFSET = 1.0f;

    public Player(float x, float y, float z, Camera camera, World world) {
        super(x, y, z);
        this.camera = camera;
        this.world = world; // Store world reference for collision checks
        camera.setPosition(x, y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
        
        updateBoundingBox();

        // Load player texture
        if (playerTexture == -1) {
            playerTexture = TextureLoader.loadTexture("textures/Wolf_Body.jpg");
            if (playerTexture == -1) {
                DebugRenderer.getInstance().addError("Failed to load player texture!", 5.0f);
            } else {
                DebugRenderer.getInstance().addMessage("Successfully loaded player texture with ID: " + playerTexture, 3.0f);
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

        handleKeyboardInput(window, deltaTime);
        updateBoundingBox();
        
        if (!noClipMode) {
            updateHeightmapCollision(deltaTime);
        }


    }
    
    /**
     * Updates the bounding box to match the player's position
     */
    private void updateBoundingBox() {
        // Safety check - ensure we have valid coordinates
        if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) {
            DebugRenderer.getInstance().addError("Player position contains NaN values!", 5.0f);
            x = 0; y = 0; z = 0;
        }
        
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
        
        // Store original position
        float originalX = x;
        float originalY = y;
        float originalZ = z;
        
        // Try moving in both directions
        x += finalMoveX;
        z += finalMoveZ;
        updateBoundingBox();
        
        if (CollisionManager.getInstance().checkCollision(boundingBox)) {
            // Collision occurred - try step up
            if (tryStepUp(finalMoveX, finalMoveZ)) {
                // Step up successful - update camera
                camera.setPosition(x, y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
                return;
            }
            
            // Step up failed - try moving in X direction only
            x = originalX + finalMoveX;
            z = originalZ;
            y = originalY;
            updateBoundingBox();
            
            if (CollisionManager.getInstance().checkCollision(boundingBox)) {
                // X movement failed - try Z direction only
                x = originalX;
                z = originalZ + finalMoveZ;
                updateBoundingBox();
                
                if (CollisionManager.getInstance().checkCollision(boundingBox)) {
                    // Both directions failed - revert to original position
                    x = originalX;
                    z = originalZ;
                    updateBoundingBox();
                }
            }
        }
        
        // Smooth step down when walking off edges
        if (isOnGround) {
            tryStepDown();
        }
    }
    
    /**
     * Attempts to step up over a small obstacle
     */
    private boolean tryStepUp(float moveX, float moveZ) {
        if (!isOnGround) {
            return false; // Only step up when on ground
        }
        
        // Safety check - don't step up if we're already too high
        if (y > lastGroundY + MAX_STEP_HEIGHT * 2) {
            return false;
        }
        
        // Try stepping up by increments
        float stepIncrement = MAX_STEP_HEIGHT / 4;
        for (float stepHeight = stepIncrement; stepHeight <= MAX_STEP_HEIGHT; stepHeight += stepIncrement) {
            // Move up by step height
            y += stepHeight;
            updateBoundingBox();
            
            // Check if this position is valid
            if (!CollisionManager.getInstance().checkCollision(boundingBox)) {
                // Found a valid step up height
                currentStepOffset = stepHeight;
                
                // Debug message
                if (Debug.isDebugMode()) {
                    DebugRenderer.getInstance().addMessage(
                        String.format("Step up successful: +%.2f at Y: %.3f", stepHeight, y), 1.0f);
                }
                
                return true;
            }
            
            // Step up didn't work - restore position
            y -= stepHeight;
            updateBoundingBox();
        }
        
        // Debug message for failed step up
        if (Debug.isDebugMode()) {
            DebugRenderer.getInstance().addMessage(
                String.format("Step up failed at Y: %.3f", y), 0.5f);
        }
        
        return false;
    }
    
    /**
     * Check if we need to step down when walking off edges
     */
    private void tryStepDown() {
        if (!isOnGround) {
            return; // Only check when on ground
        }
        
        // Check if there's ground directly below us
        float originalY = y;
        y -= GROUND_CHECK_EPSILON;
        updateBoundingBox();
        
        if (CollisionManager.getInstance().checkCollision(boundingBox)) {
            // Still on ground, restore position
            y = originalY;
            updateBoundingBox();
            return;
        }
        
        // No ground directly below - look for ground within step range
        float maxStepDown = MAX_STEP_HEIGHT;
        float testY = y;
        boolean foundGround = false;
        
        // Search downward for ground
        while (testY > y - maxStepDown) {
            testY -= GROUND_CHECK_EPSILON;
            y = testY;
            updateBoundingBox();
            
            if (CollisionManager.getInstance().checkCollision(boundingBox)) {
                // Found ground - position player on it
                y = testY + GROUND_CHECK_EPSILON;
                lastGroundY = y;
                foundGround = true;
                
                if (Debug.isDebugMode()) {
                    DebugRenderer.getInstance().addMessage(
                        String.format("Step down: %.2f at Y: %.3f", 
                        originalY - y, y), 1.0f);
                }
                break;
            }
        }
        
        if (!foundGround) {
            // No ground found within step range - we're falling
            y = originalY;
            isOnGround = false;
            velocity = 0; // Start falling from current position
            
            if (Debug.isDebugMode()) {
                DebugRenderer.getInstance().addMessage("Started falling", 1.0f);
            }
        }
        
        updateBoundingBox();
        camera.setPosition(x, y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
    }
    
    /**
     * Check if player would collide with world objects at given position
     */
    private boolean wouldCollide(float newX, float newY, float newZ) {
        // Skip collision detection if disabled
        if (!collisionEnabled) {
            return false;
        }
        
        // Create temporary bounding box for collision check
        BoundingBox tempBox = BoundingBox.fromCenterAndSize(newX, newY, newZ, PLAYER_WIDTH, PLAYER_HEIGHT, PLAYER_DEPTH);
        boolean collision = CollisionManager.getInstance().checkCollision(tempBox);
        

        
        return collision;
    }
    
    /**
     * Check collision with all collidable entities
     */
    private boolean checkEntityCollisions(BoundingBox playerBox) {
        // Use CollisionManager for collision detection
        return CollisionManager.getInstance().checkCollision(playerBox);
    }
    
    /**
     * Check for ceiling collisions when moving upward
     */
    private void checkCeilingCollision(float newY, float originalY) {
        // Safety check - don't check if movement is too small
        if (Math.abs(newY - originalY) < 0.001f) {
            y = newY;
            return;
        }
        
        // Check the entire movement path BEFORE moving
        float stepSize = 0.01f;
        float safeY = originalY;
        
        // Find the highest safe position
        for (float testY = originalY; testY <= newY; testY += stepSize) {
            y = testY;
            updateBoundingBox();
            
            if (CollisionManager.getInstance().checkCollision(boundingBox)) {
                // Found collision - stop at previous safe position
                y = safeY;
                velocity = 0;
                isOnGround = false;
                
                if (Debug.isDebugMode()) {
                    DebugRenderer.getInstance().addMessage(
                        String.format("Ceiling collision prevented at Y: %.3f", y), 1.0f);
                }
                break;
            } else {
                // This position is safe
                safeY = testY;
            }
        }
        
        // If we made it through the loop without collision, move to target
        if (safeY == newY) {
            y = newY;
        }
        
        updateBoundingBox();
        camera.setPosition(x, y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
    }
    
    /**
     * Check for floor collisions when moving downward
     */
    private void checkFloorCollision(float newY, float originalY) {
        // Safety check - don't check if movement is too small
        if (Math.abs(newY - originalY) < 0.001f) {
            y = newY;
            return;
        }
        
        // Check the entire movement path BEFORE moving
        float stepSize = 0.01f;
        float safeY = originalY;
        
        // Find the lowest safe position
        for (float testY = originalY; testY >= newY; testY -= stepSize) {
            y = testY;
            updateBoundingBox();
            
            if (CollisionManager.getInstance().checkCollision(boundingBox)) {
                // Found collision - stop at previous safe position
                y = safeY;
                velocity = 0;
                isOnGround = true;
                lastGroundY = y;
                
                if (Debug.isDebugMode()) {
                    DebugRenderer.getInstance().addMessage(
                        String.format("Floor collision prevented at Y: %.3f", y), 1.0f);
                }
                break;
            } else {
                // This position is safe
                safeY = testY;
            }
        }
        
        // If we made it through the loop without collision, move to target
        if (safeY == newY) {
            y = newY;
        }
        
        updateBoundingBox();
        camera.setPosition(x, y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
    }

    private void updateHeightmapCollision(float deltaTime) {
        if (noClipMode) {
            return;
        }

        // Store original position
        float originalY = y;

        // Apply gravity only if truly in air
        if (!isOnGround) {
            velocity += gravity * deltaTime;
            velocity = Math.max(velocity, -MAX_VELOCITY);
            
            // Calculate new Y position
            float newY = y + velocity * deltaTime;
            
            // Limit maximum movement to prevent tunneling
            float maxMovement = 1.0f; // Reduced from 2.0f for more precise collision
            if (Math.abs(newY - y) > maxMovement) {
                if (newY > y) {
                    newY = y + maxMovement;
                } else {
                    newY = y - maxMovement;
                }
            }
            
            // Check for collisions along the movement path
            if (velocity > 0) {
                // Moving upward - check for ceiling collisions
                checkCeilingCollision(newY, originalY);
            } else {
                // Moving downward - check for floor collisions
                checkFloorCollision(newY, originalY);
            }
        }
        
        // Update bounding box at new position
        updateBoundingBox();
        
        // Ground check for when not moving
        if (velocity == 0) {
            boolean wasOnGround = isOnGround;
            checkGround();
            
            // Debug visualization
            if (Debug.isDebugMode()) {
                if (isOnGround != wasOnGround) {
                    DebugRenderer.getInstance().addMessage(
                        String.format("Ground state: %s -> %s at Y: %.3f", 
                        wasOnGround, isOnGround, y), 0.5f);
                }
            }
        }
    }

    /**
     * Dedicated ground check method to centralize ground detection logic
     */
    private void checkGround() {
        // Safety check - ensure we have valid collision manager
        if (CollisionManager.getInstance() == null) {
            DebugRenderer.getInstance().addError("CollisionManager is null!", 5.0f);
            return;
        }
        
        // Check directly below with epsilon
        float originalY = y;
        y -= GROUND_CHECK_EPSILON;
        updateBoundingBox();
        
        if (CollisionManager.getInstance().checkCollision(boundingBox)) {
            // Found ground
            y = originalY;
            if (!isOnGround) {
                lastGroundY = y;
                if (Debug.isDebugMode()) {
                    DebugRenderer.getInstance().addMessage("Landed on ground", 1.0f);
                }
            }
            isOnGround = true;
            velocity = 0;
        } else {
            // No ground found - we're in the air
            y = originalY;
            isOnGround = false;
        }
        
        updateBoundingBox();
        
        // Update camera
        camera.setPosition(x, y + (PLAYER_HEIGHT * CAMERA_HEIGHT_OFFSET), z);
    }

    @Override
    public void render() {
        renderer.render(this, camera.getYaw(), camera.getPitch());
        
        // Render bounding box in debug mode
        if (Debug.isDebugMode()) {
            renderBoundingBox();
        }
    }
    
    /**
     * Renders the player's bounding box as a wireframe for debugging
     */
    private void renderBoundingBox() {
        // Save OpenGL state
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        
        // Disable textures and lighting for wireframe
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        
        // Set wireframe mode
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        
        // Draw main bounding box in cyan
        GL11.glColor4f(0.0f, 1.0f, 1.0f, 1.0f);
        GL11.glLineWidth(2.0f);
        GL11.glTranslatef(x, y, z);
        Shapes.cuboid(PLAYER_WIDTH, PLAYER_HEIGHT, PLAYER_DEPTH);
        
        // Draw ground check visualization in yellow
        GL11.glColor4f(1.0f, 1.0f, 0.0f, 1.0f);
        GL11.glLineWidth(1.0f);
        GL11.glTranslatef(0, -GROUND_CHECK_EPSILON, 0);
        Shapes.cuboid(PLAYER_WIDTH, 0.02f, PLAYER_DEPTH);
        
        // Restore OpenGL state
        GL11.glPopMatrix();
        GL11.glPopAttrib();
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