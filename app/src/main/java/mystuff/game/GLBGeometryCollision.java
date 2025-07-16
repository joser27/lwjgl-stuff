package mystuff.game;

import mystuff.utils.GLBLoader.MeshInfo;
import mystuff.utils.DebugRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import mystuff.utils.Shapes;
import org.lwjgl.opengl.GL11;

/**
 * Geometry-based collision detection for GLB models.
 * Uses actual vertex/triangle data instead of bounding boxes for precise collision.
 * Optimized with spatial partitioning and distance-based culling for better performance.
 */
public class GLBGeometryCollision {
    
    public static class Triangle {
        public float[] v1, v2, v3; // Vertices in world space
        public float[] normal;     // Triangle normal
        public BoundingBox bounds; // Individual triangle bounds for quick culling
        public float centerX, centerY, centerZ; // Triangle center for distance calculations
        
        public Triangle(float[] v1, float[] v2, float[] v3) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            calculateNormal(v1, v2, v3);
            
            // Calculate triangle bounds and center
            calculateBoundsAndCenter();
        }
        
        private void calculateNormal(float[] v1, float[] v2, float[] v3) {
            // Calculate two edge vectors
            float[] edge1 = {v2[0] - v1[0], v2[1] - v1[1], v2[2] - v1[2]};
            float[] edge2 = {v3[0] - v1[0], v3[1] - v1[1], v3[2] - v1[2]};
            
            // Cross product to get normal
            float[] normal = {
                edge1[1] * edge2[2] - edge1[2] * edge2[1],
                edge1[2] * edge2[0] - edge1[0] * edge2[2],
                edge1[0] * edge2[1] - edge1[1] * edge2[0]
            };
            
            // Normalize
            float length = (float) Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
            if (length > 0) {
                normal[0] /= length;
                normal[1] /= length;
                normal[2] /= length;
            }
            
            this.normal = normal;
        }
        
        private void calculateBoundsAndCenter() {
            // Calculate bounds
            float minX = Math.min(Math.min(v1[0], v2[0]), v3[0]);
            float maxX = Math.max(Math.max(v1[0], v2[0]), v3[0]);
            float minY = Math.min(Math.min(v1[1], v2[1]), v3[1]);
            float maxY = Math.max(Math.max(v1[1], v2[1]), v3[1]);
            float minZ = Math.min(Math.min(v1[2], v2[2]), v3[2]);
            float maxZ = Math.max(Math.max(v1[2], v2[2]), v3[2]);
            
            this.bounds = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
            
            // Calculate center
            this.centerX = (v1[0] + v2[0] + v3[0]) / 3.0f;
            this.centerY = (v1[1] + v2[1] + v3[1]) / 3.0f;
            this.centerZ = (v1[2] + v2[2] + v3[2]) / 3.0f;
        }
        
        /**
         * Calculate squared distance from triangle center to a point
         */
        public float getSquaredDistanceTo(float x, float y, float z) {
            float dx = centerX - x;
            float dy = centerY - y;
            float dz = centerZ - z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
    
    // Chunky triangle soup spatial partitioning
    private static class ChunkGrid {
        private static final float CHUNK_SIZE = 8.0f;
        private final Map<Long, Set<Triangle>> chunkMap = new HashMap<>();
        private final float minX, minY, minZ;
        private final int gridSizeX, gridSizeY, gridSizeZ;

        public ChunkGrid(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.gridSizeX = (int)Math.ceil((maxX - minX) / CHUNK_SIZE);
            this.gridSizeY = (int)Math.ceil((maxY - minY) / CHUNK_SIZE);
            this.gridSizeZ = (int)Math.ceil((maxZ - minZ) / CHUNK_SIZE);
        }

        private long chunkKey(int x, int y, int z) {
            return (((long)x) << 40) | (((long)y) << 20) | (long)z;
        }

        private int chunkCoord(float v, float min) {
            return (int)Math.floor((v - min) / CHUNK_SIZE);
        }

        public void addTriangle(Triangle tri) {
            int minXc = chunkCoord(tri.bounds.getMinX(), minX);
            int maxXc = chunkCoord(tri.bounds.getMaxX(), minX);
            int minYc = chunkCoord(tri.bounds.getMinY(), minY);
            int maxYc = chunkCoord(tri.bounds.getMaxY(), minY);
            int minZc = chunkCoord(tri.bounds.getMinZ(), minZ);
            int maxZc = chunkCoord(tri.bounds.getMaxZ(), minZ);
            for (int x = minXc; x <= maxXc; x++) {
                for (int y = minYc; y <= maxYc; y++) {
                    for (int z = minZc; z <= maxZc; z++) {
                        long key = chunkKey(x, y, z);
                        chunkMap.computeIfAbsent(key, k -> new HashSet<>()).add(tri);
                    }
                }
            }
        }

        public Set<Triangle> getTrianglesInAABB(BoundingBox box) {
            int minXc = chunkCoord(box.getMinX(), minX);
            int maxXc = chunkCoord(box.getMaxX(), minX);
            int minYc = chunkCoord(box.getMinY(), minY);
            int maxYc = chunkCoord(box.getMaxY(), minY);
            int minZc = chunkCoord(box.getMinZ(), minZ);
            int maxZc = chunkCoord(box.getMaxZ(), minZ);
            Set<Triangle> result = new HashSet<>();
            int chunkCount = 0;
            for (int x = minXc; x <= maxXc; x++) {
                for (int y = minYc; y <= maxYc; y++) {
                    for (int z = minZc; z <= maxZc; z++) {
                        long key = chunkKey(x, y, z);
                        Set<Triangle> chunk = chunkMap.get(key);
                        if (chunk != null) {
                            result.addAll(chunk);
                            chunkCount++;
                        }
                    }
                }
            }
            return result;
        }
    }

    private List<Triangle> collisionTriangles;
    private ChunkGrid chunkGrid;
    private float modelX, modelY, modelZ;
    private float modelScale;
    private BoundingBox overallBounds; // For quick culling
    
    // Performance optimization parameters
    private static final float MAX_COLLISION_DISTANCE = 5.0f; // Maximum distance to check triangles
    private static final float MAX_COLLISION_DISTANCE_SQUARED = MAX_COLLISION_DISTANCE * MAX_COLLISION_DISTANCE;
    private static final int MAX_TRIANGLES_TO_CHECK = 20; // Aggressively limit number of triangles to check per collision
    
    // Statistics for debugging
    private int totalCollisionChecks = 0;
    private int trianglesCheckedThisFrame = 0;
    private int trianglesCulledThisFrame = 0;
    
    public GLBGeometryCollision(float x, float y, float z, float scale) {
        this.modelX = x;
        this.modelY = y;
        this.modelZ = z;
        this.modelScale = scale;
        this.collisionTriangles = new ArrayList<>();
    }
    
    /**
     * Add geometry data for collision detection
     */
    public void addGeometryData(float[] vertices, int[] indices, MeshInfo meshInfo) {
        if (vertices == null || indices == null || meshInfo == null) {
            return;
        }
        
        DebugRenderer.getInstance().addMessage("Adding geometry collision for mesh: " + meshInfo.name, 2.0f);
        
        // Process triangles for this mesh
        int endIndex = Math.min(meshInfo.startIndex + meshInfo.indexCount, indices.length);
        int triangleCount = 0;
        
        for (int i = meshInfo.startIndex; i < endIndex; i += 3) {
            if (i + 2 < indices.length) {
                int idx1 = indices[i];
                int idx2 = indices[i + 1];
                int idx3 = indices[i + 2];
                
                if (idx1 * 3 + 2 < vertices.length && 
                    idx2 * 3 + 2 < vertices.length && 
                    idx3 * 3 + 2 < vertices.length) {
                    
                    // Get vertices in world space
                    float[] v1 = transformVertex(vertices, idx1);
                    float[] v2 = transformVertex(vertices, idx2);
                    float[] v3 = transformVertex(vertices, idx3);
                    
                    // Create collision triangle
                    Triangle triangle = new Triangle(v1, v2, v3);
                    collisionTriangles.add(triangle);
                    triangleCount++;
                }
            }
        }
        
        DebugRenderer.getInstance().addMessage("  Added " + triangleCount + " collision triangles", 2.0f);
    }
    
    /**
     * Transform vertex from model space to world space
     */
    private float[] transformVertex(float[] vertices, int vertexIndex) {
        float x = vertices[vertexIndex * 3] * modelScale + modelX;
        float y = vertices[vertexIndex * 3 + 1] * modelScale + modelY;
        float z = vertices[vertexIndex * 3 + 2] * modelScale + modelZ;
        return new float[]{x, y, z};
    }
    
    /**
     * Check collision with player bounding box (optimized version)
     */
    public boolean checkCollision(BoundingBox playerBox) {
        if (playerBox == null || collisionTriangles.isEmpty()) {
            return false;
        }
        
        totalCollisionChecks++;
        trianglesCheckedThisFrame = 0;
        trianglesCulledThisFrame = 0;
        
        if (overallBounds != null && !playerBox.intersects(overallBounds)) {
            return false;
        }
        
        float[] playerCenter = playerBox.getCenter();
        float playerX = playerCenter[0];
        float playerY = playerCenter[1];
        float playerZ = playerCenter[2];
        
        Set<Triangle> candidates = chunkGrid.getTrianglesInAABB(playerBox);
        
        int trianglesChecked = 0;
        int trianglesCulled = 0;
        int trianglesCheckedThisFrameLocal = 0;
        for (Triangle triangle : candidates) {
            trianglesCheckedThisFrameLocal++;
            trianglesChecked++;
            
            float distanceSquared = triangle.getSquaredDistanceTo(playerX, playerY, playerZ);
            if (distanceSquared > MAX_COLLISION_DISTANCE_SQUARED) {
                trianglesCulled++;
                continue;
            }
            
            if (!playerBox.intersects(triangle.bounds)) {
                trianglesCulled++;
                continue;
            }
            
            if (triangleIntersectsBox(triangle, playerBox)) {
                trianglesCheckedThisFrame = trianglesChecked;
                trianglesCulledThisFrame = trianglesCulled;
                return true;
            }
            
            if (trianglesCheckedThisFrameLocal >= MAX_TRIANGLES_TO_CHECK) {
                break;
            }
        }
        trianglesCheckedThisFrame = trianglesChecked;
        trianglesCulledThisFrame = trianglesCulled;
        return false;
    }
    
    /**
     * Check if a triangle intersects with a bounding box
     */
    private boolean triangleIntersectsBox(Triangle triangle, BoundingBox box) {
        // Check if any vertex is inside the box
        if (box.contains(triangle.v1[0], triangle.v1[1], triangle.v1[2]) ||
            box.contains(triangle.v2[0], triangle.v2[1], triangle.v2[2]) ||
            box.contains(triangle.v3[0], triangle.v3[1], triangle.v3[2])) {
            return true;
        }
        
        // Check if triangle bounds overlap box bounds
        return triangle.bounds.intersects(box);
    }
    
    /**
     * Build overall bounding box and spatial grid for quick culling
     */
    public void buildOverallBounds() {
        if (collisionTriangles.isEmpty()) {
            return;
        }
        
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;
        
        for (Triangle triangle : collisionTriangles) {
            for (float[] vertex : new float[][]{triangle.v1, triangle.v2, triangle.v3}) {
                minX = Math.min(minX, vertex[0]);
                maxX = Math.max(maxX, vertex[0]);
                minY = Math.min(minY, vertex[1]);
                maxY = Math.max(maxY, vertex[1]);
                minZ = Math.min(minZ, vertex[2]);
                maxZ = Math.max(maxZ, vertex[2]);
            }
        }
        
        overallBounds = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        
        // Build chunky soup grid
        chunkGrid = new ChunkGrid(minX, minY, minZ, maxX, maxY, maxZ);
        for (Triangle triangle : collisionTriangles) {
            chunkGrid.addTriangle(triangle);
        }
    }
    
    /**
     * Get collision info for debugging
     */
    public String getDebugInfo() {
        String gridInfo = chunkGrid != null ? 
            String.format("Grid: %dx%d cells", chunkGrid.gridSizeX, chunkGrid.gridSizeY) : "No grid";
        return String.format("GLBGeometryCollision: %d triangles, %d checks, %s", 
            collisionTriangles.size(), totalCollisionChecks, gridInfo);
    }
    
    private String formatBoundingBox(BoundingBox box) {
        if (box == null) return "null";
        return String.format("%.2f,%.2f,%.2f to %.2f,%.2f,%.2f", 
            box.getMinX(), box.getMinY(), box.getMinZ(),
            box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }
    
    public int getTriangleCount() {
        return collisionTriangles.size();
    }
    
    /**
     * Reset performance statistics
     */
    public void resetStats() {
        totalCollisionChecks = 0;
        trianglesCheckedThisFrame = 0;
        trianglesCulledThisFrame = 0;
    }
    
    /**
     * Get performance statistics
     */
    public int getTotalCollisionChecks() {
        return totalCollisionChecks;
    }
    
    public int getTrianglesChecked() {
        return trianglesCheckedThisFrame;
    }
    
    public int getTrianglesCulled() {
        return trianglesCulledThisFrame;
    }
    
    public float getCullingPercentage() {
        return totalCollisionChecks > 0 ? (totalCollisionChecks * 100.0f / totalCollisionChecks) : 0.0f;
    }
    
    /**
     * Render all non-empty chunk boundaries for debugging.
     * Call this from your debug render loop, e.g. in HouseMap.render() or a debug overlay.
     */
    public void renderChunkGrid() {
        if (chunkGrid == null) return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glLineWidth(2.0f);
        GL11.glColor3f(1.0f, 1.0f, 0.0f); // yellow
        for (Long key : chunkGrid.chunkMap.keySet()) {
            int x = (int) (key >> 40);
            int y = (int) ((key >> 20) & 0xFFFFF);
            int z = (int) (key & 0xFFFFF);
            float chunkMinX = chunkGrid.minX + x * ChunkGrid.CHUNK_SIZE;
            float chunkMinY = chunkGrid.minY + y * ChunkGrid.CHUNK_SIZE;
            float chunkMinZ = chunkGrid.minZ + z * ChunkGrid.CHUNK_SIZE;
            Shapes.wireCuboid(chunkMinX, chunkMinY, chunkMinZ, ChunkGrid.CHUNK_SIZE, ChunkGrid.CHUNK_SIZE, ChunkGrid.CHUNK_SIZE);
        }
        GL11.glPopAttrib();
    }
    

} 