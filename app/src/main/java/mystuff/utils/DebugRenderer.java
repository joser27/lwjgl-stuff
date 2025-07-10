package mystuff.utils;

import static org.lwjgl.opengl.GL11.*;
import java.util.ArrayList;
import java.util.List;
import mystuff.game.Player;
import mystuff.game.CollisionManager;
import mystuff.utils.FontLoader;
import mystuff.game.BoundingBox;
import mystuff.game.CapsuleCollision;
import mystuff.game.GLBGeometryCollision;
import java.util.List;

/**
 * In-game debug information renderer
 * Displays debug text and information directly in the game window
 */
public class DebugRenderer {
    private static DebugRenderer instance;
    private List<DebugMessage> messages;
    private float yOffset = 20.0f;
    private float lineHeight = 20.0f;
    private int maxMessages = 20;
    
    public static class DebugMessage {
        public String text;
        public float duration;
        public float timeRemaining;
        public boolean isError;
        
        public DebugMessage(String text, float duration, boolean isError) {
            this.text = text;
            this.duration = duration;
            this.timeRemaining = duration;
            this.isError = isError;
        }
    }
    
    private DebugRenderer() {
        messages = new ArrayList<>();
    }
    
    public static DebugRenderer getInstance() {
        if (instance == null) {
            instance = new DebugRenderer();
        }
        return instance;
    }
    
    /**
     * Add a debug message to display in-game
     */
    public void addMessage(String text, float duration) {
        addMessage(text, duration, false);
    }
    
    /**
     * Add an error message to display in-game
     */
    public void addError(String text, float duration) {
        addMessage(text, duration, true);
    }
    
    /**
     * Add a debug message to display in-game
     */
    public void addMessage(String text, float duration, boolean isError) {
        // Remove old messages if we have too many
        if (messages.size() >= maxMessages) {
            messages.remove(0);
        }
        
        messages.add(new DebugMessage(text, duration, isError));
    }
    
    /**
     * Update message timers
     */
    public void update(float deltaTime) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            DebugMessage msg = messages.get(i);
            msg.timeRemaining -= deltaTime;
            
            if (msg.timeRemaining <= 0) {
                messages.remove(i);
            }
        }
    }
    
    /**
     * Render debug information in-game
     */
    public void render(int windowWidth, int windowHeight) {
        if (!Debug.isDebugMode()) {
            return;
        }
        
        // Save current OpenGL state
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Setup 2D orthographic projection for UI rendering
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
        
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        
        // Disable depth testing for UI
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_LIGHTING);
        
        // Force fill mode for UI rendering (not affected by wireframe mode)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        
        // Render debug messages
        float currentY = yOffset;
        for (DebugMessage msg : messages) {
            if (msg.timeRemaining > 0) {
                renderText(msg.text, 10, currentY, msg.isError ? 1.0f : 0.0f, msg.isError ? 0.0f : 1.0f, 0.0f);
                currentY += lineHeight;
            }
        }
        
        // Restore OpenGL state
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        
        glPopAttrib();
    }
    
    /**
     * Render player info in-game
     */
    public void renderPlayerInfo(Player player, int windowWidth, int windowHeight) {
        if (!Debug.isDebugMode() || player == null) {
            return;
        }
        
        // Get camera from the game (we'll need to pass this in)
        // For now, we'll just show player-specific info
        
        // Save current OpenGL state
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Setup 2D orthographic projection
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
        
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_LIGHTING);
        
        // Force fill mode for UI rendering (not affected by wireframe mode)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        
        float y = 350.0f; // Start lower to avoid overlap with Game class debug info
        
        // Player position
        String posText = String.format("Position: (%.2f, %.2f, %.2f)", 
            player.getX(), player.getY(), player.getZ());
        renderText(posText, 10, y, 1.0f, 1.0f, 1.0f);
        y += lineHeight;
        
        // Player velocity
        String velText = String.format("Velocity: %.2f", player.getVelocity());
        renderText(velText, 10, y, 1.0f, 1.0f, 1.0f);
        y += lineHeight;
        
        // On ground status
        String groundText = "On Ground: " + (player.isOnGround() ? "YES" : "NO");
        renderText(groundText, 10, y, 1.0f, 1.0f, 1.0f);
        y += lineHeight;
        
        // No-clip mode
        if (player.isNoClipMode()) {
            String noClipText = "*** SPIRIT MODE ACTIVE ***";
            renderText(noClipText, 10, y, 1.0f, 0.0f, 0.0f);
            y += lineHeight;
        }
        
        // Sprint status
        if (player.isSprinting()) {
            String sprintText = "SPRINTING";
            renderText(sprintText, 10, y, 0.0f, 1.0f, 0.0f);
            y += lineHeight;
        }
        
        // Collision type
        String collisionText = "Collision: " + (player.isUsingCapsuleCollision() ? "CAPSULE" : "BOX");
        renderText(collisionText, 10, y, 1.0f, 1.0f, 1.0f);
        y += lineHeight;
        
        // Show capsule dimensions when using capsule collision
        if (player.isUsingCapsuleCollision()) {
            CapsuleCollision capsule = player.getCapsuleCollision();
            if (capsule != null) {
                String capsuleText = String.format("Capsule: R=%.2f, H=%.2f", 
                    capsule.getRadius(), capsule.getHeight());
                renderText(capsuleText, 10, y, 0.8f, 0.8f, 1.0f);
                y += lineHeight;
            }
        }
        
        // Show ground detection info
        String groundStatusText = "On Ground: " + (player.isOnGround() ? "YES" : "NO");
        renderText(groundStatusText, 10, y, 1.0f, 1.0f, 1.0f);
        y += lineHeight;
        
        // Restore OpenGL state
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        
        glPopAttrib();
    }
    
    /**
     * Render collision debug info in-game
     */
    public void renderCollisionInfo(CollisionManager collisionManager, int windowWidth, int windowHeight) {
        if (!Debug.isDebugMode() || collisionManager == null) {
            return;
        }
        
        // Save current OpenGL state
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Setup 2D orthographic projection
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
        
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_LIGHTING);
        
        // Force fill mode for UI rendering (not affected by wireframe mode)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        
        float y = windowHeight - 150.0f; // Move up slightly to avoid overlap
        
        // Collision stats
        String statsText = collisionManager.getCollisionStats();
        renderText(statsText, 10, y, 0.0f, 1.0f, 0.0f);
        y += lineHeight;
        
        // Restore OpenGL state
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        
        glPopAttrib();
    }
    
    /**
     * Render collision boxes for debugging
     */
    public void renderCollisionBox(BoundingBox box, float r, float g, float b) {
        if (!Debug.isDebugMode() || box == null) {
            return;
        }
        
        // Save OpenGL state
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Disable lighting and textures for wireframe rendering
        glDisable(GL_LIGHTING);
        glDisable(GL_TEXTURE_2D);
        
        // Set wireframe mode
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        
        // Set color
        glColor3f(r, g, b);
        
        // Set line width for better visibility
        glLineWidth(2.0f);
        
        // Get box dimensions
        float minX = box.getMinX();
        float minY = box.getMinY();
        float minZ = box.getMinZ();
        float maxX = box.getMaxX();
        float maxY = box.getMaxY();
        float maxZ = box.getMaxZ();
        
        // Draw wireframe cube
        glBegin(GL_LINES);
        
        // Bottom face
        glVertex3f(minX, minY, minZ); glVertex3f(maxX, minY, minZ);
        glVertex3f(minX, minY, minZ); glVertex3f(minX, minY, maxZ);
        glVertex3f(maxX, minY, minZ); glVertex3f(maxX, minY, maxZ);
        glVertex3f(minX, minY, maxZ); glVertex3f(maxX, minY, maxZ);
        
        // Top face
        glVertex3f(minX, maxY, minZ); glVertex3f(maxX, maxY, minZ);
        glVertex3f(minX, maxY, minZ); glVertex3f(minX, maxY, maxZ);
        glVertex3f(maxX, maxY, minZ); glVertex3f(maxX, maxY, maxZ);
        glVertex3f(minX, maxY, maxZ); glVertex3f(maxX, maxY, maxZ);
        
        // Vertical edges
        glVertex3f(minX, minY, minZ); glVertex3f(minX, maxY, minZ);
        glVertex3f(maxX, minY, minZ); glVertex3f(maxX, maxY, minZ);
        glVertex3f(minX, minY, maxZ); glVertex3f(minX, maxY, maxZ);
        glVertex3f(maxX, minY, maxZ); glVertex3f(maxX, maxY, maxZ);
        
        glEnd();
        
        // Draw center point
        float[] center = box.getCenter();
        glPointSize(5.0f);
        glBegin(GL_POINTS);
        glVertex3f(center[0], center[1], center[2]);
        glEnd();
        
        // Restore OpenGL state
        glPopAttrib();
    }
    
    /**
     * Render capsule collision for debugging
     */
    public void renderCapsuleCollision(CapsuleCollision capsule, float r, float g, float b) {
        if (!Debug.isDebugMode() || capsule == null) {
            return;
        }
        
        // Save OpenGL state
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Disable lighting and textures for wireframe rendering
        glDisable(GL_LIGHTING);
        glDisable(GL_TEXTURE_2D);
        
        // Set wireframe mode
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        
        // Set color
        glColor3f(r, g, b);
        
        // Set line width for better visibility
        glLineWidth(2.0f);
        
        float[] center = capsule.getCenter();
        float radius = capsule.getRadius();
        float height = capsule.getHeight();
        float halfHeight = height / 2.0f;
        
        // Draw the cylindrical part (vertical lines)
        int segments = 16;
        float angleStep = (float) (2.0 * Math.PI / segments);
        
        glBegin(GL_LINES);
        
        // Draw vertical lines for the cylinder
        for (int i = 0; i < segments; i++) {
            float angle = i * angleStep;
            float x = center[0] + radius * (float) Math.cos(angle);
            float z = center[2] + radius * (float) Math.sin(angle);
            
            // Bottom to top of cylinder
            glVertex3f(x, center[1] - halfHeight, z);
            glVertex3f(x, center[1] + halfHeight, z);
        }
        
        // Draw horizontal circles for top and bottom of cylinder
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) * angleStep;
            
            float x1 = center[0] + radius * (float) Math.cos(angle1);
            float z1 = center[2] + radius * (float) Math.sin(angle1);
            float x2 = center[0] + radius * (float) Math.cos(angle2);
            float z2 = center[2] + radius * (float) Math.sin(angle2);
            
            // Bottom circle
            glVertex3f(x1, center[1] - halfHeight, z1);
            glVertex3f(x2, center[1] - halfHeight, z2);
            
            // Top circle
            glVertex3f(x1, center[1] + halfHeight, z1);
            glVertex3f(x2, center[1] + halfHeight, z2);
        }
        
        glEnd();
        
        // Draw hemispherical caps (top and bottom)
        drawHemisphere(center[0], center[1] + halfHeight, center[2], radius, segments, true);  // Top cap
        drawHemisphere(center[0], center[1] - halfHeight, center[2], radius, segments, false); // Bottom cap
        
        // Draw center point
        glPointSize(5.0f);
        glBegin(GL_POINTS);
        glVertex3f(center[0], center[1], center[2]);
        glEnd();
        
        // Restore OpenGL state
        glPopAttrib();
    }
    
    /**
     * Draw a hemisphere (half sphere) for capsule caps
     */
    private void drawHemisphere(float centerX, float centerY, float centerZ, float radius, int segments, boolean isTop) {
        int rings = segments / 2;
        float ringStep = (float) (Math.PI / 2.0 / rings);
        float angleStep = (float) (2.0 * Math.PI / segments);
        
        glBegin(GL_LINES);
        
        // Draw rings (horizontal circles at different heights)
        for (int ring = 1; ring <= rings; ring++) {
            float ringAngle = ring * ringStep;
            float ringRadius = radius * (float) Math.cos(ringAngle);
            float ringY = centerY + (isTop ? 1 : -1) * radius * (float) Math.sin(ringAngle);
            
            for (int i = 0; i < segments; i++) {
                float angle1 = i * angleStep;
                float angle2 = (i + 1) * angleStep;
                
                float x1 = centerX + ringRadius * (float) Math.cos(angle1);
                float z1 = centerZ + ringRadius * (float) Math.sin(angle1);
                float x2 = centerX + ringRadius * (float) Math.cos(angle2);
                float z2 = centerZ + ringRadius * (float) Math.sin(angle2);
                
                glVertex3f(x1, ringY, z1);
                glVertex3f(x2, ringY, z2);
            }
        }
        
        // Draw vertical lines from center to edge
        for (int i = 0; i < segments; i++) {
            float angle = i * angleStep;
            float x = centerX + radius * (float) Math.cos(angle);
            float z = centerZ + radius * (float) Math.sin(angle);
            
            glVertex3f(centerX, centerY, centerZ);
            glVertex3f(x, centerY, z);
        }
        
        glEnd();
    }
    
    /**
     * Render player collision box specifically
     */
    public void renderPlayerCollisionBox(Player player) {
        if (!Debug.isDebugMode() || player == null) {
            return;
        }
        
        // Determine collision state and color
        float r, g, b;
        boolean hasCollision = false;
        
        if (player.isNoClipMode()) {
            r = 1.0f; g = 0.0f; b = 0.0f; // Red for no-clip
        } else {
            // Check collision based on current collision type
            if (player.isUsingCapsuleCollision()) {
                CapsuleCollision capsule = player.getCapsuleCollision();
                if (capsule != null) {
                    hasCollision = CollisionManager.getInstance().checkCollision(capsule.getBoundingBox());
                }
            } else {
                BoundingBox playerBox = player.getBoundingBox();
                if (playerBox != null) {
                    hasCollision = CollisionManager.getInstance().checkCollision(playerBox);
                }
            }
            
            if (hasCollision) {
                r = 1.0f; g = 0.5f; b = 0.0f; // Orange for collision
            } else {
                r = 0.0f; g = 1.0f; b = 0.0f; // Green for no collision
            }
        }
        
        // Render the appropriate collision shape
        if (player.isUsingCapsuleCollision()) {
            CapsuleCollision capsule = player.getCapsuleCollision();
            if (capsule != null) {
                renderCapsuleCollision(capsule, r, g, b);
            }
        } else {
            BoundingBox playerBox = player.getBoundingBox();
            if (playerBox != null) {
                renderCollisionBox(playerBox, r, g, b);
            }
        }
    }
    
    /**
     * Render GLB collision geometry with solid surfaces for better visibility
     */
    public void renderGLBCollisionGeometry(List<GLBGeometryCollision> geometryCollisions) {
        if (!Debug.isDebugMode() || geometryCollisions == null || geometryCollisions.isEmpty()) {
            return;
        }
        
        // Save OpenGL state
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Enable lighting for better depth perception
        glEnable(GL_LIGHTING);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_TEXTURE_2D);
        
        // Set material properties for collision visualization
        glColor3f(1.0f, 0.2f, 0.2f); // Red color for collision geometry
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Render each GLB collision geometry
        for (GLBGeometryCollision geometryCollision : geometryCollisions) {
            renderSingleGLBCollision(geometryCollision);
        }
        
        // Restore OpenGL state
        glPopAttrib();
    }
    
    /**
     * Render a single GLB collision geometry
     */
    private void renderSingleGLBCollision(GLBGeometryCollision geometryCollision) {
        if (geometryCollision == null) {
            return;
        }
        
        // Get collision triangles
        List<GLBGeometryCollision.Triangle> triangles = geometryCollision.getCollisionTriangles();
        if (triangles == null || triangles.isEmpty()) {
            return;
        }
        
        // Render triangles as solid surfaces
        glBegin(GL_TRIANGLES);
        for (GLBGeometryCollision.Triangle triangle : triangles) {
            // Calculate lighting based on triangle normal
            float[] normal = triangle.normal;
            glNormal3f(normal[0], normal[1], normal[2]);
            
            // Render triangle vertices
            glVertex3f(triangle.v1[0], triangle.v1[1], triangle.v1[2]);
            glVertex3f(triangle.v2[0], triangle.v2[1], triangle.v2[2]);
            glVertex3f(triangle.v3[0], triangle.v3[1], triangle.v3[2]);
        }
        glEnd();
        
        // Also render wireframe outline for better definition
        glDisable(GL_LIGHTING);
        glColor3f(0.8f, 0.0f, 0.0f); // Darker red for wireframe
        glLineWidth(1.0f);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        
        glBegin(GL_TRIANGLES);
        for (GLBGeometryCollision.Triangle triangle : triangles) {
            glVertex3f(triangle.v1[0], triangle.v1[1], triangle.v1[2]);
            glVertex3f(triangle.v2[0], triangle.v2[1], triangle.v2[2]);
            glVertex3f(triangle.v3[0], triangle.v3[1], triangle.v3[2]);
        }
        glEnd();
        
        // Reset to fill mode
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }
    
    /**
     * Render collision bounding boxes for all GLB models
     */
    public void renderGLBCollisionBounds(List<GLBGeometryCollision> geometryCollisions) {
        if (!Debug.isDebugMode() || geometryCollisions == null || geometryCollisions.isEmpty()) {
            return;
        }
        
        // Save OpenGL state
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Disable lighting and textures
        glDisable(GL_LIGHTING);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_DEPTH_TEST);
        
        // Set wireframe mode
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(2.0f);
        
        // Render bounding box for each GLB collision
        for (GLBGeometryCollision geometryCollision : geometryCollisions) {
            if (geometryCollision != null) {
                BoundingBox bounds = geometryCollision.getOverallBounds();
                if (bounds != null) {
                    // Use orange color for GLB collision bounds
                    glColor3f(1.0f, 0.5f, 0.0f);
                    renderCollisionBox(bounds, 1.0f, 0.5f, 0.0f);
                }
            }
        }
        
        // Restore OpenGL state
        glPopAttrib();
    }
    
    /**
     * Simple text rendering using FontLoader
     */
    private void renderText(String text, float x, float y, float r, float g, float b) {
        // Set color
        glColor3f(r, g, b);
        
        // Use FontLoader for proper text rendering
        FontLoader.renderText(text, (int)x, (int)y);
        
        // Reset color
        glColor3f(1.0f, 1.0f, 1.0f);
    }
    
    /**
     * Clear all debug messages
     */
    public void clearMessages() {
        messages.clear();
    }
} 