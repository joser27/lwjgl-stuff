package mystuff.game;

import static org.lwjgl.opengl.GL11.*;

/**
 * Capsule collider for more natural character collision detection.
 * A capsule is essentially a cylinder with hemispherical caps on both ends.
 * This provides smoother movement against walls and better collision for humanoid characters.
 */
public class CapsuleCollider {
    private float centerX, centerY, centerZ; // Center of the capsule
    private float radius; // Radius of the capsule
    private float height; // Total height of the capsule (including both hemispheres)
    private float cylinderHeight; // Height of the cylindrical part (excluding hemispheres)
    
    /**
     * Create a capsule collider
     * @param centerX X position of capsule center
     * @param centerY Y position of capsule center  
     * @param centerZ Z position of capsule center
     * @param radius Radius of the capsule
     * @param height Total height of the capsule (including hemispheres)
     */
    public CapsuleCollider(float centerX, float centerY, float centerZ, float radius, float height) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.height = height;
        this.cylinderHeight = height - (2 * radius); // Subtract the two hemisphere radii
    }
    
    /**
     * Create a capsule collider from player dimensions
     */
    public static CapsuleCollider fromPlayerDimensions(float centerX, float centerY, float centerZ, 
                                                      float playerWidth, float playerHeight) {
        // Use half the width as radius, full height as capsule height
        float radius = playerWidth * 0.4f; // Slightly smaller than half width for better movement
        return new CapsuleCollider(centerX, centerY, centerZ, radius, playerHeight);
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
     * Check if this capsule intersects with a triangle - OPTIMIZED VERSION
     */
    public boolean intersectsTriangle(float[] v1, float[] v2, float[] v3) {
        // Quick bounding sphere check first - early exit for distant triangles
        float[] triangleCenter = {
            (v1[0] + v2[0] + v3[0]) / 3f,
            (v1[1] + v2[1] + v3[1]) / 3f,
            (v1[2] + v2[2] + v3[2]) / 3f
        };
        
        float distToCenterSq = (triangleCenter[0] - centerX) * (triangleCenter[0] - centerX) +
                              (triangleCenter[1] - centerY) * (triangleCenter[1] - centerY) +
                              (triangleCenter[2] - centerZ) * (triangleCenter[2] - centerZ);
        
        // If triangle center is way outside capsule, skip expensive calculations
        float maxDistance = radius + height/2 + getTriangleMaxRadius(v1, v2, v3, triangleCenter);
        if (distToCenterSq > maxDistance * maxDistance) {
            return false;
        }
        
        // Simplified capsule-triangle test: check if triangle intersects capsule's cylinder
        return triangleIntersectsCylinder(v1, v2, v3);
    }
    
    /**
     * Fast approximate triangle radius calculation
     */
    private float getTriangleMaxRadius(float[] v1, float[] v2, float[] v3, float[] center) {
        float d1 = distance3D(v1, center);
        float d2 = distance3D(v2, center);
        float d3 = distance3D(v3, center);
        return Math.max(d1, Math.max(d2, d3));
    }
    
    /**
     * Optimized cylinder-triangle intersection test
     */
    private boolean triangleIntersectsCylinder(float[] v1, float[] v2, float[] v3) {
        float cylinderTop = centerY + cylinderHeight/2;
        float cylinderBottom = centerY - cylinderHeight/2;
        
        // Check if any triangle vertex is inside cylinder
        if (pointInCylinder(v1, cylinderBottom, cylinderTop) ||
            pointInCylinder(v2, cylinderBottom, cylinderTop) ||
            pointInCylinder(v3, cylinderBottom, cylinderTop)) {
            return true;
        }
        
        // Check if triangle edges intersect cylinder (simplified)
        return edgeIntersectsCylinder(v1, v2, cylinderBottom, cylinderTop) ||
               edgeIntersectsCylinder(v2, v3, cylinderBottom, cylinderTop) ||
               edgeIntersectsCylinder(v3, v1, cylinderBottom, cylinderTop);
    }
    
    /**
     * Check if point is inside cylinder
     */
    private boolean pointInCylinder(float[] point, float bottom, float top) {
        if (point[1] < bottom || point[1] > top) return false;
        
        float dx = point[0] - centerX;
        float dz = point[2] - centerZ;
        return (dx * dx + dz * dz) <= (radius * radius);
    }
    
    /**
     * Check if edge intersects cylinder (simplified)
     */
    private boolean edgeIntersectsCylinder(float[] start, float[] end, float bottom, float top) {
        // Check if edge spans the cylinder height
        float minY = Math.min(start[1], end[1]);
        float maxY = Math.max(start[1], end[1]);
        
        if (maxY < bottom || minY > top) return false;
        
        // Simple 2D distance check to cylinder axis
        float startDx = start[0] - centerX;
        float startDz = start[2] - centerZ;
        float endDx = end[0] - centerX;
        float endDz = end[2] - centerZ;
        
        float startDistSq = startDx * startDx + startDz * startDz;
        float endDistSq = endDx * endDx + endDz * endDz;
        
        // If either endpoint is inside cylinder radius
        if (startDistSq <= radius * radius || endDistSq <= radius * radius) {
            return true;
        }
        
        // Check closest point on edge to cylinder axis (simplified)
        float edgeDx = endDx - startDx;
        float edgeDz = endDz - startDz;
        float t = -(startDx * edgeDx + startDz * edgeDz) / (edgeDx * edgeDx + edgeDz * edgeDz);
        t = Math.max(0, Math.min(1, t));
        
        float closestDx = startDx + t * edgeDx;
        float closestDz = startDz + t * edgeDz;
        
        return (closestDx * closestDx + closestDz * closestDz) <= (radius * radius);
    }
    
    /**
     * Check if this capsule intersects with a bounding box (for compatibility)
     */
    public boolean intersectsBoundingBox(BoundingBox box) {
        // Find closest point on the bounding box to the capsule's center line
        float[] capsuleStart = {centerX, centerY - cylinderHeight/2, centerZ};
        float[] capsuleEnd = {centerX, centerY + cylinderHeight/2, centerZ};
        
        // Clamp the capsule line segment to the bounding box
        float clampedX = Math.max(box.getMinX(), Math.min(box.getMaxX(), centerX));
        float clampedZ = Math.max(box.getMinZ(), Math.min(box.getMaxZ(), centerZ));
        
        // Check distance from capsule line to closest point on box
        for (float y = capsuleStart[1]; y <= capsuleEnd[1]; y += 0.1f) {
            float clampedY = Math.max(box.getMinY(), Math.min(box.getMaxY(), y));
            float[] boxPoint = {clampedX, clampedY, clampedZ};
            float[] linePoint = {centerX, y, centerZ};
            
            if (distance3D(boxPoint, linePoint) <= radius) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the bounding box that encompasses this capsule (for quick culling)
     */
    public BoundingBox getBoundingBox() {
        return new BoundingBox(
            centerX - radius, centerY - height/2, centerZ - radius,
            centerX + radius, centerY + height/2, centerZ + radius
        );
    }
    
    /**
     * Check if a point is inside this capsule - SIMPLIFIED VERSION
     */
    public boolean contains(float x, float y, float z) {
        float cylinderTop = centerY + cylinderHeight/2;
        float cylinderBottom = centerY - cylinderHeight/2;
        
        // Check if point is within cylinder height
        if (y >= cylinderBottom && y <= cylinderTop) {
            // Check if point is within cylinder radius
            float dx = x - centerX;
            float dz = z - centerZ;
            return (dx * dx + dz * dz) <= (radius * radius);
        }
        
        // Check hemisphere caps
        float[] topCenter = {centerX, cylinderTop, centerZ};
        float[] bottomCenter = {centerX, cylinderBottom, centerZ};
        float[] point = {x, y, z};
        
        if (y > cylinderTop) {
            // Check top hemisphere
            return distance3D(point, topCenter) <= radius;
        } else {
            // Check bottom hemisphere  
            return distance3D(point, bottomCenter) <= radius;
        }
    }
    

    

    
    /**
     * Calculate dot product of two 3D vectors
     */
    private float dotProduct(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
    
    /**
     * Calculate 3D distance between two points
     */
    private float distance3D(float[] a, float[] b) {
        float dx = a[0] - b[0];
        float dy = a[1] - b[1];
        float dz = a[2] - b[2];
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    // Getters
    public float getCenterX() { return centerX; }
    public float getCenterY() { return centerY; }
    public float getCenterZ() { return centerZ; }
    public float getRadius() { return radius; }
    public float getHeight() { return height; }
    public float getCylinderHeight() { return cylinderHeight; }
    
    /**
     * Get debug information about this capsule
     */
    public String getDebugInfo() {
        return String.format("Capsule[center=(%.2f,%.2f,%.2f), radius=%.2f, height=%.2f]",
                centerX, centerY, centerZ, radius, height);
    }
    
    /**
     * Render the capsule collider as wireframe for debug visualization
     */
    public void renderDebugWireframe() {
        // Save current OpenGL state
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Setup wireframe rendering
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(2.0f);
        
        // Set bright green color for collision shape
        glColor3f(0.0f, 1.0f, 0.0f);
        
        glPushMatrix();
        glTranslatef(centerX, centerY, centerZ);
        
        // Render cylinder part
        renderCylinderWireframe();
        
        // Render top hemisphere
        glPushMatrix();
        glTranslatef(0, cylinderHeight/2, 0);
        renderHemisphereWireframe(true); // top hemisphere
        glPopMatrix();
        
        // Render bottom hemisphere 
        glPushMatrix();
        glTranslatef(0, -cylinderHeight/2, 0);
        renderHemisphereWireframe(false); // bottom hemisphere
        glPopMatrix();
        
        glPopMatrix();
        
        // Restore OpenGL state
        glPopAttrib();
    }
    
    /**
     * Render wireframe cylinder
     */
    private void renderCylinderWireframe() {
        int segments = 16;
        float angleStep = (float)(2 * Math.PI / segments);
        
        // Vertical lines
        for (int i = 0; i < segments; i++) {
            float angle = i * angleStep;
            float x = radius * (float)Math.cos(angle);
            float z = radius * (float)Math.sin(angle);
            
            glBegin(GL_LINES);
            glVertex3f(x, -cylinderHeight/2, z);
            glVertex3f(x, cylinderHeight/2, z);
            glEnd();
        }
        
        // Top circle
        glBegin(GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            float angle = i * angleStep;
            float x = radius * (float)Math.cos(angle);
            float z = radius * (float)Math.sin(angle);
            glVertex3f(x, cylinderHeight/2, z);
        }
        glEnd();
        
        // Bottom circle
        glBegin(GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            float angle = i * angleStep;
            float x = radius * (float)Math.cos(angle);
            float z = radius * (float)Math.sin(angle);
            glVertex3f(x, -cylinderHeight/2, z);
        }
        glEnd();
    }
    
    /**
     * Render wireframe hemisphere
     */
    private void renderHemisphereWireframe(boolean isTop) {
        int segments = 16;
        int rings = 8;
        
        float angleStep = (float)(2 * Math.PI / segments);
        float ringStep = (float)(Math.PI / 2 / rings);
        
        // Render meridian lines (longitude)
        for (int i = 0; i < segments; i++) {
            float angle = i * angleStep;
            glBegin(GL_LINE_STRIP);
            
            for (int j = 0; j <= rings; j++) {
                float ringAngle = j * ringStep;
                if (!isTop) ringAngle = -ringAngle; // flip for bottom hemisphere
                
                float y = radius * (float)Math.sin(ringAngle);
                float ringRadius = radius * (float)Math.cos(ringAngle);
                float x = ringRadius * (float)Math.cos(angle);
                float z = ringRadius * (float)Math.sin(angle);
                
                glVertex3f(x, y, z);
            }
            glEnd();
        }
        
        // Render parallel lines (latitude)
        for (int j = 1; j < rings; j++) {
            float ringAngle = j * ringStep;
            if (!isTop) ringAngle = -ringAngle;
            
            float y = radius * (float)Math.sin(ringAngle);
            float ringRadius = radius * (float)Math.cos(ringAngle);
            
            glBegin(GL_LINE_LOOP);
            for (int i = 0; i < segments; i++) {
                float angle = i * angleStep;
                float x = ringRadius * (float)Math.cos(angle);
                float z = ringRadius * (float)Math.sin(angle);
                glVertex3f(x, y, z);
            }
            glEnd();
        }
    }
} 