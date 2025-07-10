package mystuff.game;

/**
 * Capsule (bean-shaped) collision detection for smooth movement over stairs and obstacles.
 * A capsule is a cylinder with hemispherical caps on both ends.
 */
public class CapsuleCollision {
    private float centerX, centerY, centerZ;
    private float radius;
    private float height;
    private float halfHeight;
    
    public CapsuleCollision(float centerX, float centerY, float centerZ, float radius, float height) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.height = height;
        this.halfHeight = height / 2.0f;
    }
    
    /**
     * Create a capsule from center point, radius, and height
     */
    public static CapsuleCollision fromCenterAndSize(float centerX, float centerY, float centerZ, 
                                                    float radius, float height) {
        return new CapsuleCollision(centerX, centerY, centerZ, radius, height);
    }
    
    /**
     * Check if this capsule intersects with a bounding box
     */
    public boolean intersects(BoundingBox box) {
        if (box == null) {
            return false;
        }
        
        // Get the closest point on the box to the capsule center
        float closestX = Math.max(box.getMinX(), Math.min(centerX, box.getMaxX()));
        float closestY = Math.max(box.getMinY(), Math.min(centerY, box.getMaxY()));
        float closestZ = Math.max(box.getMinZ(), Math.min(centerZ, box.getMaxZ()));
        
        // Calculate distance from capsule center to closest point
        float dx = centerX - closestX;
        float dy = centerY - closestY;
        float dz = centerZ - closestZ;
        float distanceSquared = dx * dx + dy * dy + dz * dz;
        
        // Check if the closest point is within the capsule radius
        if (distanceSquared <= radius * radius) {
            return true;
        }
        
        // Check if the capsule's cylindrical part intersects with the box
        // This is a simplified check - for more accuracy, you'd need more complex geometry
        return checkCylinderIntersection(box);
    }
    
    /**
     * Check if the cylindrical part of the capsule intersects with the box
     */
    private boolean checkCylinderIntersection(BoundingBox box) {
        // Check if the capsule's cylindrical axis intersects with the box
        float capsuleTop = centerY + halfHeight;
        float capsuleBottom = centerY - halfHeight;
        
        // Check vertical overlap
        if (capsuleBottom > box.getMaxY() || capsuleTop < box.getMinY()) {
            return false;
        }
        
        // Check horizontal distance from capsule axis to box
        float horizontalDistance = getHorizontalDistanceToBox(box);
        return horizontalDistance <= radius;
    }
    
    /**
     * Get the horizontal distance from the capsule's central axis to the box
     */
    private float getHorizontalDistanceToBox(BoundingBox box) {
        // Find the closest point on the box's horizontal projection to the capsule center
        float closestX = Math.max(box.getMinX(), Math.min(centerX, box.getMaxX()));
        float closestZ = Math.max(box.getMinZ(), Math.min(centerZ, box.getMaxZ()));
        
        float dx = centerX - closestX;
        float dz = centerZ - closestZ;
        
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Check if a point is inside this capsule
     */
    public boolean contains(float x, float y, float z) {
        // Calculate horizontal distance from capsule axis
        float dx = x - centerX;
        float dz = z - centerZ;
        float horizontalDistance = (float) Math.sqrt(dx * dx + dz * dz);
        
        // Check if point is within the cylindrical part
        if (horizontalDistance <= radius && y >= centerY - halfHeight && y <= centerY + halfHeight) {
            return true;
        }
        
        // Check if point is within the top hemisphere
        if (y > centerY + halfHeight) {
            float dy = y - (centerY + halfHeight);
            float distanceSquared = dx * dx + dy * dy + dz * dz;
            return distanceSquared <= radius * radius;
        }
        
        // Check if point is within the bottom hemisphere
        if (y < centerY - halfHeight) {
            float dy = y - (centerY - halfHeight);
            float distanceSquared = dx * dx + dy * dy + dz * dz;
            return distanceSquared <= radius * radius;
        }
        
        return false;
    }
    
    /**
     * Get the center point of this capsule
     */
    public float[] getCenter() {
        return new float[]{centerX, centerY, centerZ};
    }
    
    /**
     * Get the radius of this capsule
     */
    public float getRadius() {
        return radius;
    }
    
    /**
     * Get the height of this capsule
     */
    public float getHeight() {
        return height;
    }
    
    /**
     * Update the capsule position
     */
    public void setPosition(float centerX, float centerY, float centerZ) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
    }
    
    /**
     * Get a bounding box that encompasses this capsule (for quick culling)
     */
    public BoundingBox getBoundingBox() {
        return new BoundingBox(
            centerX - radius, centerY - halfHeight, centerZ - radius,
            centerX + radius, centerY + halfHeight, centerZ + radius
        );
    }
    
    /**
     * Get debug info for this capsule
     */
    public String getDebugInfo() {
        return String.format("Capsule: center(%.2f, %.2f, %.2f), radius=%.2f, height=%.2f", 
                           centerX, centerY, centerZ, radius, height);
    }
} 