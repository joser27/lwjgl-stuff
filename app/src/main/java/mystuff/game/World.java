package mystuff.game;

import java.util.ArrayList;
import java.util.List;
import mystuff.engine.Window;
import org.lwjgl.opengl.GL11;
import mystuff.engine.Camera;

public class World {
    // World constants
    public static final float BLOCK_SIZE = 1.0f;  // Size of each block
    private static final int WORLD_WIDTH = 128;    // Width of the world in blocks
    private static final int WORLD_HEIGHT = 128;   // Height of the world in blocks
    private static final int WORLD_DEPTH = 128;    // Depth of the world in blocks
    
    private Block[][][] blocks;
    private List<Tree> trees;  // Add list to store trees
    private Camera camera;  // Add camera field

    public World(Camera camera) {
        this.camera = camera;
        blocks = new Block[WORLD_WIDTH][WORLD_HEIGHT][WORLD_DEPTH];
        trees = new ArrayList<>();  // Initialize tree list
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

        // Add some trees
        trees.add(new Tree(5, 1, 5));  // Tree at (5,1,5)
        trees.add(new Tree(10, 1, 10));  // Tree at (10,1,10)
        trees.add(new Tree(3, 1, 12));  // Tree at (3,1,12)
        trees.add(new Tree(22, 1, 12));  // Tree at (3,1,12)

        // Add a stone block as before
        setBlock(3, 5, 3, BlockType.STONE);
        setBlock(4, 4, 3, BlockType.STONE);
        setBlock(3, 3, 3, BlockType.STONE);
    }

    public void update(Window window, float deltaTime) {
        // Update trees if needed
        for (Tree tree : trees) {
            tree.update(window, deltaTime);
        }
    }

    public void render() {
        // First render all opaque blocks
        for(int x = 0; x < WORLD_WIDTH; x++) {
            for(int y = 0; y < WORLD_HEIGHT; y++) {
                for(int z = 0; z < WORLD_DEPTH; z++) {
                    Block block = blocks[x][y][z];
                    if (block != null && block.getType() != BlockType.AIR) {
                        block.render();
                    }
                }
            }
        }

        // Now render transparent objects (leaves) after all opaque objects
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);

        // Sort trees by distance from camera for proper transparency
        float cameraX = camera.getX();
        float cameraY = camera.getY(); 
        float cameraZ = camera.getZ();
        
        trees.sort((t1, t2) -> {
            float dx1 = t1.getX() - cameraX;
            float dy1 = t1.getY() - cameraY;
            float dz1 = t1.getZ() - cameraZ;
            float dist1 = dx1 * dx1 + dy1 * dy1 + dz1 * dz1;

            float dx2 = t2.getX() - cameraX;
            float dy2 = t2.getY() - cameraY;
            float dz2 = t2.getZ() - cameraZ;
            float dist2 = dx2 * dx2 + dy2 * dy2 + dz2 * dz2;

            // Sort back to front
            return Float.compare(dist2, dist1);
        });

        // Render trees (they handle their own transparency)
        for (Tree tree : trees) {
            tree.render();
        }

        // Restore OpenGL state
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
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
        // Cleanup blocks
        for (Block[][] blockLayer : blocks) {
            for (Block[] blockRow : blockLayer) {
                for (Block block : blockRow) {
                    if (block != null) {
                        block.cleanup();
                    }
                }
            }
        }

        // Cleanup trees
        for (Tree tree : trees) {
            tree.cleanup();
        }
    }
}
