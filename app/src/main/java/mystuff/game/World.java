package mystuff.game;

import java.util.ArrayList;
import java.util.List;
import mystuff.engine.Window;

public class World {
    // World constants
    public static final float BLOCK_SIZE = 1.0f;  // Size of each block
    private static final int WORLD_WIDTH = 16;    // Width of the world in blocks
    private static final int WORLD_HEIGHT = 16;   // Height of the world in blocks
    private static final int WORLD_DEPTH = 16;    // Depth of the world in blocks
    
    private Block[][][] blocks;

    public World() {
        blocks = new Block[WORLD_WIDTH][WORLD_HEIGHT][WORLD_DEPTH];
        generateAir();
        generateWorld();
    }

    /**
     * Sets a block at the specified grid coordinates
     * @param x Grid X coordinate
     * @param y Grid Y coordinate
     * @param z Grid Z coordinate
     * @param type Type of block to set
     * @return true if coordinates are valid and block was set, false otherwise
     */
    public boolean setBlock(int x, int y, int z, BlockType type) {
        // Check if coordinates are within bounds
        if (x < 0 || x >= WORLD_WIDTH || 
            y < 0 || y >= WORLD_HEIGHT || 
            z < 0 || z >= WORLD_DEPTH) {
            return false;
        }
        
        // Convert grid coordinates to world coordinates
        float worldX = x * BLOCK_SIZE;
        float worldY = y * BLOCK_SIZE;
        float worldZ = z * BLOCK_SIZE;
        
        // Create and set the block
        blocks[x][y][z] = new Block(worldX, worldY, worldZ, type);
        return true;
    }

    private void generateAir() {
        // Initialize all blocks to air
        for(int x = 0; x < WORLD_WIDTH; x++) {
            for(int y = 0; y < WORLD_HEIGHT; y++) {
                for(int z = 0; z < WORLD_DEPTH; z++) {
                    setBlock(x, y, z, BlockType.AIR);
                }
            }
        }
    }

    private void generateWorld() {
        // Create ground layer
        for(int x = 0; x < WORLD_WIDTH; x++) {
            for(int z = 0; z < WORLD_DEPTH; z++) {
                setBlock(x, 0, z, BlockType.DIRT);
            }
        }

        // Add a stone block as before
        setBlock(3, 5, 3, BlockType.STONE);
        setBlock(4, 4, 3, BlockType.STONE);
        setBlock(3, 3, 3, BlockType.STONE);
    }

    public void update(Window window, float deltaTime) {
        
    }

    public void render() {
        // Render blocks
        for(int x = 0; x < WORLD_WIDTH; x++) {
            for(int y = 0; y < WORLD_HEIGHT; y++) {
                for(int z = 0; z < WORLD_DEPTH; z++) {
                    Block block = blocks[x][y][z];
                    block.render();
                }
            }
        }
    }
    
    // Get all blocks in the world
    public List<Block> getAllBlocks() {
        List<Block> allBlocks = new ArrayList<>();
        for(int x = 0; x < WORLD_WIDTH; x++) {
            for(int y = 0; y < WORLD_HEIGHT; y++) {
                for(int z = 0; z < WORLD_DEPTH; z++) {
                    Block block = blocks[x][y][z];
                    if(block != null && block.getType() != BlockType.AIR) {
                        allBlocks.add(block);
                    }
                }
            }
        }
        return allBlocks;
    }

    /**
     * Gets a block at the specified grid coordinates
     * @param x Grid X coordinate
     * @param y Grid Y coordinate
     * @param z Grid Z coordinate
     * @return The block at the specified position, or null if coordinates are invalid
     */
    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= WORLD_WIDTH || 
            y < 0 || y >= WORLD_HEIGHT || 
            z < 0 || z >= WORLD_DEPTH) {
            return null;
        }
        return blocks[x][y][z];
    }

    /**
     * Removes a block at the specified grid coordinates by replacing it with an air block
     * @param x Grid X coordinate
     * @param y Grid Y coordinate
     * @param z Grid Z coordinate
     * @return true if block was removed, false if coordinates were invalid
     */
    public boolean removeBlock(int x, int y, int z) {
        if (x < 0 || x >= WORLD_WIDTH || 
            y < 0 || y >= WORLD_HEIGHT || 
            z < 0 || z >= WORLD_DEPTH) {
            return false;
        }
        
        // Convert grid coordinates to world coordinates for the air block
        float worldX = x * BLOCK_SIZE;
        float worldY = y * BLOCK_SIZE;
        float worldZ = z * BLOCK_SIZE;
        
        // Replace with air block
        blocks[x][y][z] = new Block(worldX, worldY, worldZ, BlockType.AIR);
        return true;
    }

    public void cleanup() {
        // Cleanup any resources held by blocks
        for (Block[][] blockLayer : blocks) {
            for (Block[] blockRow : blockLayer) {
                for (Block block : blockRow) {
                    if (block != null) {
                        block.cleanup();
                    }
                }
            }
        }
    }
}
