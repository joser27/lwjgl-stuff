package mystuff.game;

import org.lwjgl.glfw.GLFW;
import mystuff.engine.Window;
import mystuff.engine.Camera;
import java.util.List;

public class PlayerPhysics {
    private static final float GROUND_CHECK_DISTANCE = 0.05f;
    private float speed = 0.1f;
    private float gravity = -20.0f;
    private float jumpForce = 10.0f;
    private float cameraSpeed = 0.2f;
    private float velocity = 0.0f;
    private boolean isOnGround = false;
    private float lastGroundY = 0;
    private boolean wasSpacePressed = false;

    public void updatePhysics(Player player, Window window, float deltaTime, Camera camera, World world, boolean noClipMode, boolean debugMode) {
        if (noClipMode) {
            updateCameraNoClip(window, deltaTime, camera);
        } else {
            updatePlayerPhysics(player, window, deltaTime, camera, world, debugMode);
        }
    }

    private void updateCameraNoClip(Window window, float deltaTime, Camera camera) {
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

    private void updatePlayerPhysics(Player player, Window window, float deltaTime, Camera camera, World world, boolean debugMode) {
        // Get all blocks from the world
        List<Block> blocks = world.getAllBlocks();
        BoundingBox playerBB = player.getBoundingBox();
        
        // Check if player is standing on ground
        isOnGround = false;
        for (Block block : blocks) {
            BoundingBox blockBB = block.getBoundingBox();
            if (playerBB.isOnTopOf(blockBB, 0.1f)) {
                isOnGround = true;
                break;
            }
        }
        
        // Track the last ground position when we're on ground
        if (isOnGround) {
            lastGroundY = player.getY();
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
            velocity = 0;
        }
        
        // Calculate movement direction based on camera yaw
        float yaw = (float) Math.toRadians(camera.getYaw());
        float forwardX = (float) Math.sin(yaw);
        float forwardZ = (float) Math.cos(yaw);
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

        float dy = velocity * deltaTime;
        
        // Test movement in each axis separately
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
        float groundLevel = 0;
        for (Block block : blocks) {
            BoundingBox blockBB = block.getBoundingBox();
            if (yMovedBox.intersects(blockBB)) {
                yCollision = true;
                if (velocity < 0) {
                    groundLevel = blockBB.getMaxY();
                }
                break;
            }
        }
        
        if (!yCollision) {
            player.setPosition(player.getX(), player.getY() + dy, player.getZ());
        } else {
            if (velocity < 0) {
                velocity = 0;
                isOnGround = true;
                player.setPosition(player.getX(), groundLevel + (player.getBoundingBox().getHeight() / 2), player.getZ());
            } else {
                velocity = 0;
            }
        }

        // Update camera position to follow player at proper eye level (75% of player height)
        camera.setPosition(player.getX(), player.getY() + (player.getBoundingBox().getHeight() * 0.75f), player.getZ());
    }
} 