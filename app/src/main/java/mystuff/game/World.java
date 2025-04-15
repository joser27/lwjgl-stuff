package mystuff.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mystuff.engine.Window;
import org.lwjgl.opengl.GL11;
import mystuff.engine.Camera;

public class World {
    // World constants
    public static final float BLOCK_SIZE = 1.0f;  // Size of each block
    private static final int WORLD_WIDTH = 200;    // Width of the world in blocks
    private static final int WORLD_HEIGHT = 200;   // Height of the world in blocks
    private static final int WORLD_DEPTH = 200;    // Depth of the world in blocks
    
    private Map<ChunkKey, Chunk> chunks;
    private List<Tree> trees;  // Add list to store trees
    private Camera camera;  // Add camera field
    private static final int RENDER_DISTANCE = 4; // Number of chunks to render in each direction

    public World(Camera camera) {
        this.camera = camera;
        this.chunks = new HashMap<>();
        this.trees = new ArrayList<>();  // Initialize tree list
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
        
        // Convert grid coordinates to chunk coordinates
        int chunkX = Chunk.worldToChunkCoord(x * BLOCK_SIZE);
        int chunkY = Chunk.worldToChunkCoord(y * BLOCK_SIZE);
        int chunkZ = Chunk.worldToChunkCoord(z * BLOCK_SIZE);
        
        // Get or create the chunk
        ChunkKey key = new ChunkKey(chunkX, chunkY, chunkZ);
        Chunk chunk = chunks.computeIfAbsent(key, k -> new Chunk(this, chunkX, chunkY, chunkZ));
        
        // Convert to local chunk coordinates
        int localX = Chunk.worldToLocalCoord(x * BLOCK_SIZE);
        int localY = Chunk.worldToLocalCoord(y * BLOCK_SIZE);
        int localZ = Chunk.worldToLocalCoord(z * BLOCK_SIZE);
        
        // Set the block in the chunk
        chunk.setBlock(localX, localY, localZ, type);
        return true;
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
        // Calculate which chunks are in render distance
        int playerChunkX = Chunk.worldToChunkCoord(camera.getX());
        int playerChunkY = Chunk.worldToChunkCoord(camera.getY());
        int playerChunkZ = Chunk.worldToChunkCoord(camera.getZ());

        // First render all opaque blocks in visible chunks
        for (Chunk chunk : chunks.values()) {
            // Skip chunks outside render distance
            if (Math.abs(chunk.getChunkX() - playerChunkX) > RENDER_DISTANCE ||
                Math.abs(chunk.getChunkY() - playerChunkY) > RENDER_DISTANCE ||
                Math.abs(chunk.getChunkZ() - playerChunkZ) > RENDER_DISTANCE) {
                continue;
            }
            chunk.render();
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
    
    // Get block at world coordinates
    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= WORLD_WIDTH || 
            y < 0 || y >= WORLD_HEIGHT || 
            z < 0 || z >= WORLD_DEPTH) {
            return null;
        }

        int chunkX = Chunk.worldToChunkCoord(x * BLOCK_SIZE);
        int chunkY = Chunk.worldToChunkCoord(y * BLOCK_SIZE);
        int chunkZ = Chunk.worldToChunkCoord(z * BLOCK_SIZE);
        
        ChunkKey key = new ChunkKey(chunkX, chunkY, chunkZ);
        Chunk chunk = chunks.get(key);
        if (chunk == null) return null;
        
        int localX = Chunk.worldToLocalCoord(x * BLOCK_SIZE);
        int localY = Chunk.worldToLocalCoord(y * BLOCK_SIZE);
        int localZ = Chunk.worldToLocalCoord(z * BLOCK_SIZE);
        
        return chunk.getBlock(localX, localY, localZ);
    }

    // Get all blocks in the world (for collision detection)
    public List<Block> getAllBlocks() {
        List<Block> allBlocks = new ArrayList<>();
        for (Chunk chunk : chunks.values()) {
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
                for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                    for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block != null && block.getType() != BlockType.AIR) {
                            allBlocks.add(block);
                        }
                    }
                }
            }
        }
        return allBlocks;
    }

    public boolean removeBlock(int x, int y, int z) {
        return setBlock(x, y, z, BlockType.AIR);
    }

    public void cleanup() {
        // Cleanup chunks
        for (Chunk chunk : chunks.values()) {
            chunk.cleanup();
        }
        chunks.clear();

        // Cleanup trees
        for (Tree tree : trees) {
            tree.cleanup();
        }
    }
    
    // Inner class to use as key for chunk map
    private static class ChunkKey {
        private final int x, y, z;
        
        public ChunkKey(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkKey key = (ChunkKey) o;
            return x == key.x && y == key.y && z == key.z;
        }
        
        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }

    // Get chunk at chunk coordinates
    public Chunk getChunk(int chunkX, int chunkY, int chunkZ) {
        return chunks.get(new ChunkKey(chunkX, chunkY, chunkZ));
    }
}
