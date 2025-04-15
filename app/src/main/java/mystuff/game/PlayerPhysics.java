package mystuff.game;

import org.lwjgl.glfw.GLFW;
import mystuff.engine.Window;
import mystuff.engine.Camera;
import mystuff.utils.KeyboardManager;
import java.util.List;

/**
 * Handles all physics and movement for the player
 */
public class PlayerPhysics {
    // Physics constants
    private static final float GROUND_CHECK_DISTANCE = 0.05f;
    private static final float MAX_VELOCITY = 20.0f;
    private static final float GRAVITY = -20.0f;
    
    // State variables
    private float velocity = 0.0f;
    private boolean isOnGround = false;
    private float lastGroundY = 0;
    private boolean wasSpacePressed = false;
    private boolean wasShiftPressed = false;

    /**
     * Main update method that delegates to the appropriate physics handler
     */
    public void updatePhysics(Player player, Window window, float deltaTime, Camera camera, World world, boolean noClipMode, boolean debugMode) {
        if (noClipMode) {
            updateCameraNoClip(player, window, deltaTime, camera);
        } else {
            updatePlayerPhysics(player, window, deltaTime, camera, world, debugMode);
        }
    }

    /**
     * Updates camera position in no-clip (flying) mode
     */
    private void updateCameraNoClip(Player player, Window window, float deltaTime, Camera camera) {
        // Calculate movement direction based on camera yaw
        float yaw = (float) Math.toRadians(camera.getYaw());
        
        // Forward vector points where the camera is looking
        float forwardX = (float) Math.sin(yaw);
        float forwardZ = (float) Math.cos(yaw);
        
        // Right vector is perpendicular to forward
        float rightX = (float) Math.cos(yaw);
        float rightZ = (float) Math.sin(yaw);

        // In no-clip mode, left control can be used for sprint since shift is used for down movement
        boolean isSprintPressed = KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL);
        player.setSprinting(isSprintPressed);
        
        float dx = 0, dy = 0, dz = 0;
        // Apply sprint multiplier in no-clip mode
        float speedMultiplier = isSprintPressed ? 2.0f : 1.0f;
        // Apply proper scaling for movement in no-clip mode
        // Use deltaTime scaled by 50 to get the right feel with the small cameraSpeed value
        float cameraSpeed = player.getCameraSpeed() * speedMultiplier * (deltaTime * 50);

        // Forward/Backward
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_W)) {
            dx += forwardX * cameraSpeed;
            dz -= forwardZ * cameraSpeed;
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_S)) {
            dx -= forwardX * cameraSpeed;
            dz += forwardZ * cameraSpeed;
        }

        // Strafe Left/Right
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_A)) {
            dx -= rightX * cameraSpeed;
            dz -= rightZ * cameraSpeed;
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_D)) {
            dx += rightX * cameraSpeed;
            dz += rightZ * cameraSpeed;
        }
        
        // Up/Down
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_SPACE)) {
            dy += cameraSpeed;
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            dy -= cameraSpeed;
        }
        
        // Calculate new camera position
        float newCameraX = camera.getX() + dx;
        float newCameraY = camera.getY() + dy;
        float newCameraZ = camera.getZ() + dz;
        
        // Update camera position directly (only the camera moves in no-clip mode)
        camera.setPosition(newCameraX, newCameraY, newCameraZ);
        
        // Do NOT update player position - leave the body where it was
        // This creates the "spectator" effect where the body stays in place
        // while the camera/view can move freely
        
        // No need to update bounding box since player position is not changing
    }

    /**
     * Updates player position with collision detection and physics
     */
    private void updatePlayerPhysics(Player player, Window window, float deltaTime, Camera camera, World world, boolean debugMode) {
        // Performance timing - start
        long startTime = System.nanoTime();
        
        // Get only nearby blocks for collision detection instead of all blocks
        // This significantly improves performance by checking only a limited number of blocks
        List<Block> blocks = world.getNearbyBlocksForCollision(
            player.getX(), player.getY(), player.getZ(), 
            player.getCollisionCheckRadius()
        );
        BoundingBox playerBB = player.getBoundingBox();
        
        // Record block fetch time
        long blockFetchTime = System.nanoTime() - startTime;
        
        // Check for reset key
        if (KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_R)) {
            player.setPosition(5.0f, 5.0f, 5.0f);
            player.updateBoundingBox();
            return;
        }
        
        // Check if player is standing on ground
        isOnGround = false;
        for (Block block : blocks) {
            BoundingBox blockBB = block.getBoundingBox();
            if (playerBB.isOnTopOf(blockBB, GROUND_CHECK_DISTANCE)) {
                isOnGround = true;
                break;
            }
        }
        
        // Update player's ground state
        player.setOnGround(isOnGround);
        
        // Track the last ground position when we're on ground
        if (isOnGround) {
            lastGroundY = player.getY();
        }
        
        // Handle jumping
        boolean isSpacePressed = KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_SPACE);
        if (isSpacePressed && !wasSpacePressed && isOnGround) {
            velocity = player.getJumpForce();
            isOnGround = false;
            player.setOnGround(false);
            if (debugMode) System.out.println("Jump initiated! Velocity: " + velocity);
        }
        wasSpacePressed = isSpacePressed;

        // Apply gravity and vertical movement
        if (!isOnGround) {
            velocity += GRAVITY * deltaTime;
            // Clamp velocity to maximum speed
            velocity = Math.max(Math.min(velocity, MAX_VELOCITY), -MAX_VELOCITY);
            if (debugMode && Math.abs(velocity) >= MAX_VELOCITY) {
                System.out.println("Velocity clamped at: " + velocity);
            }
        } else {
            // Reset velocity when on ground
            velocity = 0;
        }
        
        // Update the player's velocity
        player.setVelocity(velocity);
        
        // Calculate movement direction based on camera yaw
        float yaw = (float) Math.toRadians(camera.getYaw());
        float forwardX = (float) Math.sin(yaw);
        float forwardZ = (float) Math.cos(yaw);
        float rightX = (float) Math.cos(yaw);
        float rightZ = (float) Math.sin(yaw);

        // Handle sprinting (only when on ground and pressing forward)
        boolean isShiftPressed = KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT);
        boolean isForwardPressed = KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_W);
        
        // Toggle sprint mode with shift key
        if (isOnGround && isForwardPressed) {
            player.setSprinting(isShiftPressed);
        } else if (!isForwardPressed) {
            // Stop sprinting if not moving forward
            player.setSprinting(false);
        }
        
        float dx = 0, dz = 0;
        float moveSpeed = player.getCurrentSpeed() * deltaTime;

        // Forward/Backward
        if (isForwardPressed) {
            dx += forwardX * moveSpeed;
            dz -= forwardZ * moveSpeed;
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_S)) {
            dx -= forwardX * moveSpeed;
            dz += forwardZ * moveSpeed;
        }

        // Strafe Left/Right
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_A)) {
            dx -= rightX * moveSpeed;
            dz -= rightZ * moveSpeed;
        }
        if (KeyboardManager.isKeyPressed(GLFW.GLFW_KEY_D)) {
            dx += rightX * moveSpeed;
            dz += rightZ * moveSpeed;
        }

        // Calculate vertical movement for this frame
        float dy = velocity * deltaTime;
        
        // Test movement in each axis separately to allow sliding along walls
        // Create temporary bounding boxes for testing movement
        BoundingBox xMovedBox = playerBB.getTranslated(dx, 0, 0);
        BoundingBox zMovedBox = playerBB.getTranslated(0, 0, dz);
        BoundingBox yMovedBox = playerBB.getTranslated(0, dy, 0);
        
        // Check collisions for X movement
        boolean xCollision = false;
        for (Block block : blocks) {
            if (xMovedBox.intersects(block.getBoundingBox())) {
                xCollision = true;
                break;
            }
        }
        if (!xCollision) {
            player.setPosition(player.getX() + dx, player.getY(), player.getZ());
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
            player.setPosition(player.getX(), player.getY(), player.getZ() + dz);
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
            player.setPosition(player.getX(), player.getY() + dy, player.getZ());
        } else {
            // We hit something
            if (velocity < 0) {
                // If moving down, we hit ground
                velocity = 0;
                isOnGround = true;
                player.setOnGround(true);
                
                // Position player so bottom of bounding box is at ground level
                // Add half player height to set center position
                player.setPosition(player.getX(), groundLevel + (Player.PLAYER_HEIGHT / 2), player.getZ());
            } else {
                // If moving up, we hit ceiling
                velocity = 0;
            }
            player.setVelocity(velocity);
        }
        
        // Update the player's bounding box to match the new position
        player.updateBoundingBox();

        // Update camera position to follow player at proper eye level
        camera.setPosition(player.getX(), player.getY() + (Player.PLAYER_HEIGHT * 0.75f), player.getZ());
        
        // Performance timing - end
        long totalTime = System.nanoTime() - startTime;
        if (debugMode) {
            System.out.printf("Physics performance: Block fetch: %.2fms, Total physics: %.2fms, Blocks checked: %d%n", 
                blockFetchTime / 1_000_000.0, totalTime / 1_000_000.0, blocks.size());
        }
    }
} 