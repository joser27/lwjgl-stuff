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

    private void generateAir() {
        // Initialize all blocks to air
        for(int x = 0; x < WORLD_WIDTH; x++) {
            for(int y = 0; y < WORLD_HEIGHT; y++) {
                for(int z = 0; z < WORLD_DEPTH; z++) {
                    float worldX = x * BLOCK_SIZE;
                    float worldY = y * BLOCK_SIZE;
                    float worldZ = z * BLOCK_SIZE;
                    blocks[x][y][z] = new Block(worldX, worldY, worldZ, BlockType.AIR);
                }
            }
        }
    }
    private void generateWorld() {
        for(int x = 0; x < WORLD_WIDTH; x++) {
            for(int y = 0; y < WORLD_HEIGHT; y++) {
                for(int z = 0; z < WORLD_DEPTH; z++) {
                    float worldX = x * BLOCK_SIZE;
                    float worldY = y * BLOCK_SIZE;
                    float worldZ = z * BLOCK_SIZE;
                    if (y == 0) {
                        blocks[x][y][z] = new Block(worldX, worldY, worldZ, BlockType.DIRT);
                    } 
                }
            }
        }
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
}
