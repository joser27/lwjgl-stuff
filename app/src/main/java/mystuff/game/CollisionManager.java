package mystuff.game;

import java.util.ArrayList;
import java.util.List;
import mystuff.utils.DebugRenderer;

/**
 * Centralized collision detection manager for the game.
 * Manages geometry-based collision detection for GLB models.
 */
public class CollisionManager {
    
    // Geometry-based collision detection for GLB models
    private List<GLBGeometryCollision> geometryCollisions;
    private static CollisionManager instance;
    
    private CollisionManager() {
        geometryCollisions = new ArrayList<>();
    }
    
    public static CollisionManager getInstance() {
        if (instance == null) {
            instance = new CollisionManager();
        }
        return instance;
    }
    
    /**
     * Add a geometry-based collision system for GLB models
     */
    public void addGLBGeometryCollision(GLBGeometryCollision geometryCollision) {
        if (geometryCollision != null && !geometryCollisions.contains(geometryCollision)) {
            geometryCollisions.add(geometryCollision);
            DebugRenderer.getInstance().addMessage("Added geometry collision: " + geometryCollision.getDebugInfo(), 3.0f);
        }
    }
    
    /**
     * Remove a geometry-based collision system
     */
    public void removeGLBGeometryCollision(GLBGeometryCollision geometryCollision) {
        if (geometryCollision != null) {
            geometryCollisions.remove(geometryCollision);
            DebugRenderer.getInstance().addMessage("Removed geometry collision: " + geometryCollision.getDebugInfo(), 3.0f);
        }
    }
    
    /**
     * Check collision with all registered geometry collisions
     */
    public boolean checkGeometryCollision(BoundingBox playerBox) {
        if (playerBox == null) {
            return false;
        }
        
        for (GLBGeometryCollision geometryCollision : geometryCollisions) {
            if (geometryCollision.checkCollision(playerBox)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check collision with all registered collision systems
     */
    public boolean checkCollision(BoundingBox playerBox) {
        return checkGeometryCollision(playerBox);
    }
    
    /**
     * Get debug info for all collision systems
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("CollisionManager Debug Info:\n");
        info.append("  Geometry collisions: ").append(geometryCollisions.size()).append("\n");
        
        for (int i = 0; i < geometryCollisions.size(); i++) {
            info.append("    Geometry ").append(i).append(": ").append(geometryCollisions.get(i).getDebugInfo()).append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Clear all registered collision systems
     */
    public void clearAll() {
        geometryCollisions.clear();
        DebugRenderer.getInstance().addMessage("Cleared all collision systems", 3.0f);
    }
    
    /**
     * Get collision statistics for debugging
     */
    public String getCollisionStats() {
        int totalTriangles = 0;
        
        for (GLBGeometryCollision geometryCollision : geometryCollisions) {
            totalTriangles += geometryCollision.getTriangleCount();
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("CollisionManager: ").append(geometryCollisions.size()).append(" GLB models, ")
             .append(totalTriangles).append(" total triangles");
        
        return stats.toString();
    }
    
    /**
     * Get all GLB geometry collisions for rendering
     */
    public List<GLBGeometryCollision> getGeometryCollisions() {
        return geometryCollisions;
    }
} 