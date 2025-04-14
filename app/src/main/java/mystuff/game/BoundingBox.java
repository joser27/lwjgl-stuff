package mystuff.game;

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
     * Creates a bounding box from a center point and dimensions
     */
    public static BoundingBox fromCenterAndSize(float centerX, float centerY, float centerZ, 
                                               float width, float height, float depth) {
        float halfWidth = width / 2;
        float halfHeight = height / 2;
        float halfDepth = depth / 2;
        
        return new BoundingBox(
            centerX - halfWidth, centerY - halfHeight, centerZ - halfDepth,
            centerX + halfWidth, centerY + halfHeight, centerZ + halfDepth
        );
    }
    
    /**
     * Check if this bounding box intersects with another bounding box
     */
    public boolean intersects(BoundingBox other) {
        return maxX > other.minX && minX < other.maxX &&
               maxY > other.minY && minY < other.maxY &&
               maxZ > other.minZ && minZ < other.maxZ;
    }
    
    /**
     * Specialized test to check if this box is standing on another box
     * Uses a small threshold to account for floating point imprecision
     */
    public boolean isOnTopOf(BoundingBox other, float threshold) {
        // Check if horizontally overlapping
        boolean horizontalOverlap = maxX > other.minX && minX < other.maxX &&
                                    maxZ > other.minZ && minZ < other.maxZ;
        
        // Check if bottom of this box is at or just above the top of other box
        boolean verticalContact = Math.abs(minY - other.maxY) <= threshold;
        
        return horizontalOverlap && verticalContact;
    }
    
    /**
     * Check for horizontal collision only (ignoring Y-axis)
     */
    public boolean intersectsHorizontally(BoundingBox other) {
        return maxX > other.minX && minX < other.maxX &&
               maxZ > other.minZ && minZ < other.maxZ;
    }
    
    /**
     * Move this bounding box by the specified amounts
     */
    public void translate(float dx, float dy, float dz) {
        minX += dx;
        maxX += dx;
        minY += dy;
        maxY += dy;
        minZ += dz;
        maxZ += dz;
    }
    
    /**
     * Create a copy of this bounding box translated by the given amount
     */
    public BoundingBox getTranslated(float dx, float dy, float dz) {
        return new BoundingBox(
            minX + dx, minY + dy, minZ + dz,
            maxX + dx, maxY + dy, maxZ + dz
        );
    }
    
    /**
     * Get the penetration depth between this box and another on all axes
     * @return Array of [x, y, z] penetration depths (positive values indicate overlap)
     */
    public float[] getPenetrationDepth(BoundingBox other) {
        float overlapX = Math.min(maxX - other.minX, other.maxX - minX);
        float overlapY = Math.min(maxY - other.minY, other.maxY - minY);
        float overlapZ = Math.min(maxZ - other.minZ, other.maxZ - minZ);
        
        return new float[] {overlapX, overlapY, overlapZ};
    }
    
    // Getters and setters
    public float getMinX() { return minX; }
    public float getMinY() { return minY; }
    public float getMinZ() { return minZ; }
    public float getMaxX() { return maxX; }
    public float getMaxY() { return maxY; }
    public float getMaxZ() { return maxZ; }
    
    public float getWidth() { return maxX - minX; }
    public float getHeight() { return maxY - minY; }
    public float getDepth() { return maxZ - minZ; }
    
    public float getCenterX() { return (minX + maxX) / 2; }
    public float getCenterY() { return (minY + maxY) / 2; }
    public float getCenterZ() { return (minZ + maxZ) / 2; }
}
