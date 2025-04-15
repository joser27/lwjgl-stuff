package mystuff.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mystuff.engine.Window;
import org.lwjgl.opengl.GL11;
import mystuff.engine.Camera;
import mystuff.utils.Debug;
import mystuff.engine.Frustum;

public class World {
    // World constants
    public static final float BLOCK_SIZE = 1.0f;  // Size of each block
    private static final int WORLD_WIDTH = 200;    // Width of the world in blocks
    private static final int WORLD_HEIGHT = 200;   // Height of the world in blocks
    private static final int WORLD_DEPTH = 200;    // Depth of the world in blocks
    
    private Map<ChunkKey, Chunk> chunks;
    private List<Tree> trees;
    private Camera camera;
    private Player player;
    private static final int RENDER_DISTANCE = 4;
    private static final float CLOSE_DISTANCE = 32.0f; // Distance threshold for color change

    public World(Camera camera) {
        this.camera = camera;
        this.chunks = new HashMap<>();
        this.trees = new ArrayList<>();
        generateWorld();
    }

    // Add method to set player reference
    public void setPlayer(Player player) {
        this.player = player;
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
        int groundHeight = 10; // Height of the flat world
        
        // Generate a flat world of dirt blocks
        for(int x = 0; x < WORLD_WIDTH; x++) {
            for(int z = 0; z < WORLD_DEPTH; z++) {
                // Create columns of blocks
                for(int y = 0; y < groundHeight; y++) {
                    if (y == groundHeight - 1) {
                        setBlock(x, y, z, BlockType.DIRT); // Top layer is dirt
                    }
                }
            }
        }

        if (Debug.showPlayerInfo()) {
            System.out.println("Flat dirt world generated with dimensions: " + 
                             WORLD_WIDTH + "x" + groundHeight + "x" + WORLD_DEPTH);
        }
    }

    public void update(Window window, float deltaTime) {
        // Update trees if needed
        for (Tree tree : trees) {
            tree.update(window, deltaTime);
        }
    }

    public void render(Camera camera) {
        // Update camera frustum
        camera.update();
        
        int chunksInView = 0;
        int chunksInFrustum = 0;
        int totalChunks = chunks.size();
        
        // Save OpenGL state
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        // Use player position for culling if available, otherwise use camera
        float cullingX = (player != null) ? player.getX() : camera.getX();
        float cullingY = (player != null) ? player.getY() : camera.getY();
        float cullingZ = (player != null) ? player.getZ() : camera.getZ();
        
        // Render opaque blocks first
        for (Chunk chunk : chunks.values()) {
            // Get chunk bounds
            float chunkX = chunk.getChunkX() * Chunk.CHUNK_SIZE;
            float chunkY = chunk.getChunkY() * Chunk.CHUNK_SIZE;
            float chunkZ = chunk.getChunkZ() * Chunk.CHUNK_SIZE;
            
            // Calculate distance from player (not camera) to chunk center
            float dx = chunkX + Chunk.CHUNK_SIZE/2 - cullingX;
            float dy = chunkY + Chunk.CHUNK_SIZE/2 - cullingY;
            float dz = chunkZ + Chunk.CHUNK_SIZE/2 - cullingZ;
            float distanceSquared = dx*dx + dy*dy + dz*dz;
            float renderDistanceSquared = (RENDER_DISTANCE * Chunk.CHUNK_SIZE) * (RENDER_DISTANCE * Chunk.CHUNK_SIZE);
            
            // Check if chunk is in view frustum relative to player position
            boolean inFrustum = isBoxInViewFromPosition(
                chunkX, chunkY, chunkZ,
                Chunk.CHUNK_SIZE, Chunk.CHUNK_SIZE, Chunk.CHUNK_SIZE,
                cullingX, cullingY, cullingZ,
                camera.getPitch(), camera.getYaw()
            );
            
            if (inFrustum) {
                chunksInFrustum++;
                // Only render if within render distance
                if (distanceSquared <= renderDistanceSquared) {
                    chunksInView++;
                    chunk.render();
                }
            }
            
            // Debug visualization when debug mode is on
            if (Debug.showBoundingBoxes()) {
                GL11.glPushAttrib(GL11.GL_CURRENT_BIT | GL11.GL_POLYGON_BIT);
                GL11.glPushMatrix();
                GL11.glTranslatef(chunkX + Chunk.CHUNK_SIZE/2, chunkY + Chunk.CHUNK_SIZE/2, chunkZ + Chunk.CHUNK_SIZE/2);
                
                float distance = (float)Math.sqrt(distanceSquared);
                
                if (inFrustum) {
                    if (distance <= CLOSE_DISTANCE) {
                        // Green for close chunks in frustum
                        GL11.glColor3f(0.0f, 1.0f, 0.0f);
                    } else if (distanceSquared <= renderDistanceSquared) {
                        // Yellow for far chunks in frustum but within render distance
                        GL11.glColor3f(1.0f, 1.0f, 0.0f);
                    } else {
                        // Red for chunks in frustum but beyond render distance
                        GL11.glColor3f(1.0f, 0.0f, 0.0f);
                    }
                } else {
                    // Blue for chunks outside frustum
                    GL11.glColor3f(0.0f, 0.0f, 1.0f);
                }
                
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
                mystuff.utils.Shapes.cuboid(Chunk.CHUNK_SIZE, Chunk.CHUNK_SIZE, Chunk.CHUNK_SIZE);
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
        }
        
        if (Debug.showPlayerInfo()) {
            System.out.printf("Chunks rendered: %d/%d (%.1f%%), In frustum: %d/%d (%.1f%%)%n", 
                chunksInView, totalChunks, (chunksInView * 100.0f) / totalChunks,
                chunksInFrustum, totalChunks, (chunksInFrustum * 100.0f) / totalChunks);
        }
        
        // Render transparent objects last
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        for (Tree tree : trees) {
            float treeX = tree.getX();
            float treeY = tree.getY();
            float treeZ = tree.getZ();
            
            // Check if tree is in view frustum before rendering
            if (camera.isBoxInView(treeX, treeY, treeZ, 1, 5, 1)) {
                tree.render();
            }
        }
        
        GL11.glDisable(GL11.GL_BLEND);
        
        // Restore OpenGL state
        GL11.glPopAttrib();
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

    // Helper method to check if a box is in view from a specific position
    private boolean isBoxInViewFromPosition(
        float boxX, float boxY, float boxZ,
        float width, float height, float depth,
        float viewX, float viewY, float viewZ,
        float pitch, float yaw
    ) {
        // Create temporary matrices for the view from player position
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glRotatef(pitch, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(yaw, 0.0f, 1.0f, 0.0f);
        GL11.glTranslatef(-viewX, -viewY, -viewZ);
        
        // Get the modelview matrix from player's perspective
        float[] modelViewMatrix = new float[16];
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix);
        
        // Get the current projection matrix
        float[] projectionMatrix = new float[16];
        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projectionMatrix);
        
        // Restore the original matrix
        GL11.glPopMatrix();
        
        // Create a temporary frustum for this check
        Frustum tempFrustum = new Frustum();
        tempFrustum.update(projectionMatrix, modelViewMatrix);
        
        return tempFrustum.isBoxInFrustum(boxX, boxY, boxZ, width, height, depth);
    }
}
