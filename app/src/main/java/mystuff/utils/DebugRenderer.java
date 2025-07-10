package mystuff.utils;

import static org.lwjgl.opengl.GL11.*;
import java.util.ArrayList;
import java.util.List;
import mystuff.game.Player;
import mystuff.game.CollisionManager;
import mystuff.utils.FontLoader;

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
        
        // Collision visualization info
        renderText("Collision Shapes: Green=Player Capsule, Red=World Triangles", 10, y, 1.0f, 1.0f, 0.0f);
        y += lineHeight;
        renderText("Press B to toggle collision shape visualization", 10, y, 0.5f, 0.5f, 0.5f);
        y += lineHeight;
        
        // Restore OpenGL state
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        
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