package mystuff.game;

import mystuff.utils.GLBLoader.MeshInfo;
import mystuff.utils.DebugRenderer;
import java.util.ArrayList;
import java.util.List;

/**
 * Geometry-based collision detection for GLB models.
 * Uses actual vertex/triangle data instead of bounding boxes for precise collision.
 */
public class GLBGeometryCollision {
    
    public static class Triangle {
        public float[] v1, v2, v3; // Vertices in world space
        public float[] normal;     // Triangle normal
        
        public Triangle(float[] v1, float[] v2, float[] v3) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.normal = calculateNormal(v1, v2, v3);
        }
        
        private float[] calculateNormal(float[] v1, float[] v2, float[] v3) {
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
            
            return normal;
        }
    }
    
    private List<Triangle> collisionTriangles;
    private float modelX, modelY, modelZ;
    private float modelScale;
    private BoundingBox overallBounds; // For quick culling
    
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
     * Check collision with player bounding box
     */
    public boolean checkCollision(BoundingBox playerBox) {
        if (playerBox == null || collisionTriangles.isEmpty()) {
            return false;
        }
        
        // Quick bounds check first
        if (overallBounds != null && !playerBox.intersects(overallBounds)) {
            return false;
        }
        
        // Check against individual triangles
        for (Triangle triangle : collisionTriangles) {
            if (triangleIntersectsBox(triangle, playerBox)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a triangle intersects with a bounding box
     */
    private boolean triangleIntersectsBox(Triangle triangle, BoundingBox box) {
        // Simple AABB-triangle intersection test
        // This is a simplified version - for production you'd want more sophisticated collision
        
        // Check if any vertex is inside the box
        if (box.contains(triangle.v1[0], triangle.v1[1], triangle.v1[2]) ||
            box.contains(triangle.v2[0], triangle.v2[1], triangle.v2[2]) ||
            box.contains(triangle.v3[0], triangle.v3[1], triangle.v3[2])) {
            return true;
        }
        
        // Check if triangle edges intersect box faces
        // (Simplified - just check if triangle bounds overlap box bounds)
        float minX = Math.min(Math.min(triangle.v1[0], triangle.v2[0]), triangle.v3[0]);
        float maxX = Math.max(Math.max(triangle.v1[0], triangle.v2[0]), triangle.v3[0]);
        float minY = Math.min(Math.min(triangle.v1[1], triangle.v2[1]), triangle.v3[1]);
        float maxY = Math.max(Math.max(triangle.v1[1], triangle.v2[1]), triangle.v3[1]);
        float minZ = Math.min(Math.min(triangle.v1[2], triangle.v2[2]), triangle.v3[2]);
        float maxZ = Math.max(Math.max(triangle.v1[2], triangle.v2[2]), triangle.v3[2]);
        
        return !(maxX < box.getMinX() || minX > box.getMaxX() ||
                maxY < box.getMinY() || minY > box.getMaxY() ||
                maxZ < box.getMinZ() || minZ > box.getMaxZ());
    }
    
    /**
     * Build overall bounding box for quick culling
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
        DebugRenderer.getInstance().addMessage("Overall collision bounds: " + formatBoundingBox(overallBounds), 3.0f);
    }
    
    /**
     * Get collision info for debugging
     */
    public String getDebugInfo() {
        return "GLBGeometryCollision: " + collisionTriangles.size() + " triangles";
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
     * Get the collision triangles for rendering
     */
    public List<Triangle> getCollisionTriangles() {
        return collisionTriangles;
    }
    
    /**
     * Get the overall bounding box for rendering
     */
    public BoundingBox getOverallBounds() {
        return overallBounds;
    }
} 