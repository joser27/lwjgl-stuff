package mystuff.game;

import org.lwjgl.opengl.GL11;
import mystuff.engine.Camera;
import mystuff.utils.Debug;
import java.util.*;

public class World {
    private HeightmapTerrain terrain;
    private Map<ChunkKey, List<Tree>> chunkTrees; // Trees organized by chunk
    private Camera camera;
    private Player player;
    private static final int RENDER_DISTANCE = 8;
    private static final float TERRAIN_SIZE = 1000.0f; // Size of terrain

    public World(Camera camera) {
        this.camera = camera;
        this.chunkTrees = new HashMap<>();
        
        // Initialize heightmap terrain
        int terrainWidth = (int) (TERRAIN_SIZE / 2.0f); // 500x500 heightmap
        int terrainHeight = (int) (TERRAIN_SIZE / 2.0f);
        this.terrain = new HeightmapTerrain(terrainWidth, terrainHeight);
        
        generateWorld();
    }

    private void generateWorld() {
        // Procedural tree generation based on heightmap
        generateProceduralTrees();

        if (Debug.showPlayerInfo()) {
            System.out.println("Heightmap terrain generated with procedural trees");
        }
    }
    
    /**
     * Generate trees procedurally based on terrain features
     */
    private void generateProceduralTrees() {
        Random random = new Random(42); // Fixed seed for consistent generation
        
        // Get terrain bounds
        float minX = terrain.getMinX();
        float maxX = terrain.getMaxX();
        float minZ = terrain.getMinZ();
        float maxZ = terrain.getMaxZ();
        
        // Generate trees across the terrain
        for (float x = minX; x < maxX; x += 15.0f) { // 15 units spacing
            for (float z = minZ; z < maxZ; z += 15.0f) {
                // Skip some positions for natural spacing
                if (random.nextFloat() > 0.3f) continue; // 30% chance of tree
                
                // Get ground height at this position
                float groundHeight = terrain.getHeightAt(x, z);
                
                // Don't place trees on very steep slopes or underwater
                float[] normal = terrain.getNormalAt(x, z);
                float slope = 1.0f - normal[1]; // 0 = flat, 1 = vertical
                
                if (slope > 0.7f || groundHeight < 8.0f) continue; // Skip steep slopes and low areas
                
                // Choose tree type based on height and position
                TreeType treeType = chooseTreeType(x, z, groundHeight, random);
                
                // Add some random offset for natural placement
                float offsetX = (random.nextFloat() - 0.5f) * 5.0f;
                float offsetZ = (random.nextFloat() - 0.5f) * 5.0f;
                
                addTreeToChunk((int)(x + offsetX), (int)groundHeight, (int)(z + offsetZ), treeType);
            }
        }
        
        // Generate dense forests in specific areas
        generateForest(100, 100, 50, TreeType.PINE, random);
        generateForest(200, 150, 40, TreeType.OAK, random);
        generateForest(300, 80, 60, TreeType.BROADLEAF, random);
    }
    
    /**
     * Choose tree type based on position and height
     */
    private TreeType chooseTreeType(float x, float z, float height, Random random) {
        // Pine trees prefer higher elevations
        if (height > 15.0f && random.nextFloat() > 0.6f) {
            return TreeType.PINE;
        }
        
        // Oak trees are common in middle elevations
        if (height > 10.0f && random.nextFloat() > 0.4f) {
            return TreeType.OAK;
        }
        
        // Broadleaf trees prefer lower elevations
        return TreeType.BROADLEAF;
    }
    
    /**
     * Generate a dense forest in a specific area
     */
    private void generateForest(float centerX, float centerZ, int radius, TreeType type, Random random) {
        for (int i = 0; i < radius * 2; i++) {
            float angle = random.nextFloat() * 2 * (float)Math.PI;
            float distance = random.nextFloat() * radius;
            
            float x = centerX + (float)Math.cos(angle) * distance;
            float z = centerZ + (float)Math.sin(angle) * distance;
            
            // Check if within terrain bounds
            if (x < terrain.getMinX() || x > terrain.getMaxX() || 
                z < terrain.getMinZ() || z > terrain.getMaxZ()) {
                continue;
            }
            
            float groundHeight = terrain.getHeightAt(x, z);
            float[] normal = terrain.getNormalAt(x, z);
            float slope = 1.0f - normal[1];
            
            if (slope < 0.7f && groundHeight > 8.0f) {
                addTreeToChunk((int)x, (int)groundHeight, (int)z, type);
            }
        }
    }

    public void render(Camera camera) {
        // Update camera frustum
        camera.update();
        
        // Save OpenGL state
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        // Use player position for culling if available, otherwise use camera
        float cullingX = (player != null) ? player.getX() : camera.getX();
        float cullingY = (player != null) ? player.getY() : camera.getY();
        float cullingZ = (player != null) ? player.getZ() : camera.getZ();
        
        // Render terrain
        if (terrain != null) {
            terrain.render(cullingX, cullingZ, RENDER_DISTANCE * 32.0f); // 32 units per chunk
        }
        
        // Render transparent objects last
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Render trees organized by chunks
        for (Map.Entry<ChunkKey, List<Tree>> entry : chunkTrees.entrySet()) {
            ChunkKey key = entry.getKey();
            List<Tree> treesInChunk = entry.getValue();
            
            // Calculate distance to chunk center
            float chunkX = key.x * 32.0f; // 32 units per chunk
            float chunkY = key.y * 32.0f;
            float chunkZ = key.z * 32.0f;
            
            float dx = chunkX - cullingX;
            float dy = chunkY - cullingY;
            float dz = chunkZ - cullingZ;
            float distanceSquared = dx*dx + dy*dy + dz*dz;
            float treeRenderDistanceSquared = (RENDER_DISTANCE * 32.0f * 2) * (RENDER_DISTANCE * 32.0f * 2);
            
            // Only render trees in nearby chunks
            if (distanceSquared <= treeRenderDistanceSquared) {
                for (Tree tree : treesInChunk) {
                    float treeX = tree.getX();
                    float treeY = tree.getY();
                    float treeZ = tree.getZ();
                    
                    // Use the same frustum culling method as terrain blocks
                    // Calculate tree bounding box (approximate size for pine trees)
                    float treeWidth = 24.0f;  // Maximum tree width
                    float treeHeight = 70.0f; // Maximum tree height
                    float treeDepth = 24.0f;  // Same as width for cross-pattern trees
                    
                    boolean treeInFrustum = isBoxInViewFromPosition(
                        treeX - treeWidth/2, treeY, treeZ - treeDepth/2,
                        treeWidth, treeHeight, treeDepth,
                        cullingX, cullingY, cullingZ,
                        camera.getPitch(), camera.getYaw()
                    );
                    
                    if (treeInFrustum) {
                        tree.render();
                    }
                }
            }
        }
        
        GL11.glDisable(GL11.GL_BLEND);
        
        // Restore OpenGL state
        GL11.glPopAttrib();
    }

    public void update(float deltaTime) {
        // Update trees
        for (List<Tree> treesInChunk : chunkTrees.values()) {
            for (Tree tree : treesInChunk) {
                tree.update(null, deltaTime); // Tree doesn't need window reference
            }
        }
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public float getHeightAt(float x, float z) {
        if (terrain != null) {
            return terrain.getHeightAt(x, z);
        }
        return 10.0f; // Default height
    }

    public boolean isOnGround(float x, float y, float z, float tolerance) {
        if (terrain != null) {
            return terrain.isOnGround(x, y, z, tolerance);
        }
        return Math.abs(y - 10.0f) <= tolerance; // Default ground check
    }

    public float[] getNormalAt(float x, float z) {
        if (terrain != null) {
            return terrain.getNormalAt(x, z);
        }
        return new float[]{0, 1, 0}; // Default normal (flat ground)
    }

    public float getMinX() { 
        return terrain != null ? terrain.getMinX() : 0; 
    }
    
    public float getMaxX() { 
        return terrain != null ? terrain.getMaxX() : TERRAIN_SIZE; 
    }
    
    public float getMinZ() { 
        return terrain != null ? terrain.getMinZ() : 0; 
    }
    
    public float getMaxZ() { 
        return terrain != null ? terrain.getMaxZ() : TERRAIN_SIZE; 
    }

    public void cleanup() {
        // Cleanup terrain
        if (terrain != null) {
            terrain.cleanup();
        }
        
        // Cleanup trees
        for (List<Tree> treesInChunk : chunkTrees.values()) {
            for (Tree tree : treesInChunk) {
                tree.cleanup();
            }
        }
        chunkTrees.clear();
    }

    // Tree management methods
    private void addTreeToChunk(int x, int y, int z, TreeType type) {
        ChunkKey key = new ChunkKey(x / 32, y / 32, z / 32);
        chunkTrees.computeIfAbsent(key, k -> new ArrayList<>());
        
        Tree tree = createTree(type, x, y, z);
        if (tree != null) {
            chunkTrees.get(key).add(tree);
        }
    }

    private Tree createTree(TreeType type, int x, int y, int z) {
        switch (type) {
            case OAK:
                return new OakTree(x, y, z);
            case BROADLEAF:
                return new BroadleafTree(x, y, z);
            case PINE:
                return new PineTree(x, y, z);
            default:
                return new OakTree(x, y, z); // Default to oak tree
        }
    }

    // Frustum culling helper method
    private boolean isBoxInViewFromPosition(float boxX, float boxY, float boxZ, 
                                          float boxWidth, float boxHeight, float boxDepth,
                                          float viewX, float viewY, float viewZ, 
                                          float pitch, float yaw) {
        // Simple distance-based culling for now
        float dx = boxX + boxWidth/2 - viewX;
        float dy = boxY + boxHeight/2 - viewY;
        float dz = boxZ + boxDepth/2 - viewZ;
        float distanceSquared = dx*dx + dy*dy + dz*dz;
        
        // Check if box is within reasonable viewing distance
        float maxDistance = 200.0f; // Adjust based on your needs
        return distanceSquared <= maxDistance * maxDistance;
    }

    // ChunkKey class for organizing trees
    private static class ChunkKey {
        final int x, y, z;
        
        ChunkKey(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ChunkKey chunkKey = (ChunkKey) obj;
            return x == chunkKey.x && y == chunkKey.y && z == chunkKey.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }
}
