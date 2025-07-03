package mystuff.game;

/**
 * Simple Axis-Aligned Bounding Box for entity collision detection
 */
public class BoundingBox {
    private float minX, minY, minZ;
    private float maxX, maxY, maxZ;
    
    public BoundingBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }
    
    /**
     * Create a bounding box from center point and size
     */
    public static BoundingBox fromCenterAndSize(float centerX, float centerY, float centerZ, 
                                               float width, float height, float depth) {
        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;
        float halfDepth = depth / 2.0f;
        
        return new BoundingBox(
            centerX - halfWidth, centerY - halfHeight, centerZ - halfDepth,
            centerX + halfWidth, centerY + halfHeight, centerZ + halfDepth
        );
    }
    
    /**
     * Check if this bounding box intersects with another
     */
    public boolean intersects(BoundingBox other) {
        return !(maxX < other.minX || minX > other.maxX ||
                maxY < other.minY || minY > other.maxY ||
                maxZ < other.minZ || minZ > other.maxZ);
    }
    
    /**
     * Check if a point is inside this bounding box
     */
    public boolean contains(float x, float y, float z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
    
    /**
     * Get the center point of this bounding box
     */
    public float[] getCenter() {
        return new float[]{
            (minX + maxX) / 2.0f,
            (minY + maxY) / 2.0f,
            (minZ + maxZ) / 2.0f
        };
    }
    
    /**
     * Get the size of this bounding box
     */
    public float[] getSize() {
        return new float[]{
            maxX - minX,
            maxY - minY,
            maxZ - minZ
        };
    }
    
    // Getters
    public float getMinX() { return minX; }
    public float getMinY() { return minY; }
    public float getMinZ() { return minZ; }
    public float getMaxX() { return maxX; }
    public float getMaxY() { return maxY; }
    public float getMaxZ() { return maxZ; }
    
    /**
     * Update the bounding box position (keep same size)
     */
    public void setPosition(float centerX, float centerY, float centerZ) {
        float[] size = getSize();
        float halfWidth = size[0] / 2.0f;
        float halfHeight = size[1] / 2.0f;
        float halfDepth = size[2] / 2.0f;
        
        minX = centerX - halfWidth;
        minY = centerY - halfHeight;
        minZ = centerZ - halfDepth;
        maxX = centerX + halfWidth;
        maxY = centerY + halfHeight;
        maxZ = centerZ + halfDepth;
    }
} 