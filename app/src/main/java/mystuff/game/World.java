package mystuff.game;

import org.lwjgl.opengl.GL11;
import mystuff.utils.Shapes;
import java.util.ArrayList;
import java.util.List;

public class World {
    // World constants
    public static final int BLOCK_SIZE = 2;  // Size of each block (reduced from 16)
    public static final int CHUNK_SIZE = 16;  // Size of a chunk in blocks
    public static final int WORLD_SIZE = 8;   // World size in chunks
    private static final float GROUND_THRESHOLD = 0.1f; // Threshold for ground detection
    
    private Block[][][] blocks;
    private final int worldSizeInBlocks;

    public World() {
        worldSizeInBlocks = CHUNK_SIZE * WORLD_SIZE;
        blocks = new Block[worldSizeInBlocks][worldSizeInBlocks][worldSizeInBlocks];
        generateWorld();
    }

    private void generateWorld() {
        // Create a flat ground plane of blocks
        for(int x = 0; x < worldSizeInBlocks; x++) {
            for(int z = 0; z < worldSizeInBlocks; z++) {
                // Convert grid coordinates to world coordinates
                float worldX = x * BLOCK_SIZE;
                float worldZ = z * BLOCK_SIZE;
                
                // Place ground at y=0 instead of -BLOCK_SIZE
                if((x + z) % 2 == 0) {
                    blocks[x][0][z] = new Block(worldX, 0, worldZ, BlockType.DIRT);
                } else {
                    blocks[x][0][z] = new Block(worldX, 0, worldZ, BlockType.GRASS);
                }
            }
        }
    }

    public void render() {
        // Render blocks
        for(int x = 0; x < worldSizeInBlocks; x++) {
            for(int y = 0; y < worldSizeInBlocks; y++) {
                for(int z = 0; z < worldSizeInBlocks; z++) {
                    Block block = blocks[x][y][z];
                    if(block != null) {
                        block.render();
                    }
                }
            }
        }
    }

    // Convert world coordinates to grid coordinates
    public int[] worldToGridCoords(float x, float y, float z) {
        return new int[] {
            Math.max(0, Math.min(worldSizeInBlocks - 1, (int) Math.floor(x / BLOCK_SIZE))),
            Math.max(0, Math.min(worldSizeInBlocks - 1, (int) Math.floor(y / BLOCK_SIZE))),
            Math.max(0, Math.min(worldSizeInBlocks - 1, (int) Math.floor(z / BLOCK_SIZE)))
        };
    }

    // Check if a point is inside the world bounds
    public boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < worldSizeInBlocks &&
               y >= 0 && y < worldSizeInBlocks &&
               z >= 0 && z < worldSizeInBlocks;
    }

    // Get block at grid coordinates
    public Block getBlockAt(int x, int y, int z) {
        if (!isInBounds(x, y, z)) return null;
        return blocks[x][y][z];
    }

    // Get block at world coordinates
    public Block getBlockAtWorldCoords(float x, float y, float z) {
        int[] gridCoords = worldToGridCoords(x, y, z);
        return getBlockAt(gridCoords[0], gridCoords[1], gridCoords[2]);
    }

    // Check if a point collides with any block
    public boolean checkCollision(float x, float y, float z) {
        int[] gridCoords = worldToGridCoords(x, y, z);
        Block block = getBlockAt(gridCoords[0], gridCoords[1], gridCoords[2]);
        return block != null && block.getType() != BlockType.AIR;
    }

    // Get all blocks that could potentially intersect with the given bounding box
    private List<Block> getPotentialCollisionBlocks(BoundingBox box) {
        List<Block> potentialBlocks = new ArrayList<>();
        
        // Get grid coordinates for the box bounds
        int[] minGrid = worldToGridCoords(box.getMinX(), box.getMinY(), box.getMinZ());
        int[] maxGrid = worldToGridCoords(box.getMaxX(), box.getMaxY(), box.getMaxZ());
        
        // Check all blocks in the box's bounds
        for (int gridX = minGrid[0]; gridX <= maxGrid[0]; gridX++) {
            for (int gridY = minGrid[1]; gridY <= maxGrid[1]; gridY++) {
                for (int gridZ = minGrid[2]; gridZ <= maxGrid[2]; gridZ++) {
                    Block block = getBlockAt(gridX, gridY, gridZ);
                    if (block != null && block.getType() != BlockType.AIR) {
                        potentialBlocks.add(block);
                    }
                }
            }
        }
        
        return potentialBlocks;
    }

    // Check if a bounding box collides with any blocks
    public boolean checkBoundingBoxCollision(BoundingBox box) {
        List<Block> potentialBlocks = getPotentialCollisionBlocks(box);
        
        for (Block block : potentialBlocks) {
            if (box.intersects(block.getBoundingBox())) {
                return true;
            }
        }
        
        return false;
    }
    
    // Check if player is standing on ground
    public boolean checkGroundCollision(Player player) {
        BoundingBox playerBox = player.getBoundingBox();
        List<Block> potentialBlocks = getPotentialCollisionBlocks(
            new BoundingBox(
                playerBox.getMinX(), playerBox.getMinY() - GROUND_THRESHOLD, playerBox.getMinZ(),
                playerBox.getMaxX(), playerBox.getMinY() + GROUND_THRESHOLD, playerBox.getMaxZ()
            )
        );
        
        // Check if player is standing on any block
        for (Block block : potentialBlocks) {
            if (playerBox.isOnTopOf(block.getBoundingBox(), GROUND_THRESHOLD)) {
                return true;
            }
        }
        
        return false;
    }
    
    // Find the ground level (Y coordinate of the top of the highest block below the player)
    public float findGroundLevel(float x, float y, float z) {
        // Convert to grid coordinates
        int gridX = (int) Math.floor(x / BLOCK_SIZE);
        int gridZ = (int) Math.floor(z / BLOCK_SIZE);
        
        // Ensure we're in bounds
        if (!isInBounds(gridX, 0, gridZ)) {
            return Float.MIN_VALUE;
        }
        
        // Calculate current grid Y position
        int currentGridY = (int) Math.floor(y / BLOCK_SIZE);
        
        // Start at current player Y position and search down
        // This prevents teleporting back to starting ground
        for (int gridY = currentGridY; gridY >= 0; gridY--) {
            Block block = getBlockAt(gridX, gridY, gridZ);
            if (block != null && block.getType() != BlockType.AIR) {
                // Return the top of the block
                return block.getY() + (BLOCK_SIZE / 2.0f);
            }
        }
        
        // If no ground found, return minimum value
        return Float.MIN_VALUE;
    }

    // Legacy box collision check (kept for backward compatibility)
    public boolean checkBoxCollision(float x, float y, float z, float width, float height, float depth) {
        // Create a bounding box for this check
        BoundingBox box = BoundingBox.fromCenterAndSize(x, y, z, width, height, depth);
        return checkBoundingBoxCollision(box);
    }
}
