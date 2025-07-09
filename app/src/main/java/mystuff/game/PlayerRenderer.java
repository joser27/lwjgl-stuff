package mystuff.game;

import static org.lwjgl.opengl.GL11.*;
import mystuff.utils.TextureLoader;
import mystuff.utils.Debug;
import mystuff.utils.DebugRenderer;
import mystuff.utils.OBJModelRenderer;

public class PlayerRenderer {
    private static int playerTexture = -1;
    
    // OBJ model support
    private static OBJModelRenderer playerModel = null;
    
    // Model dimensions (only used for fallback cube)
    private static final float HEAD_SIZE = 0.5f;
    private static final float LEG_HEIGHT = 0.75f;
    
    public void init() {
        if (playerTexture == -1) {
            playerTexture = TextureLoader.loadTexture("textures/player.png");
            if (playerTexture != -1) {
                glBindTexture(GL_TEXTURE_2D, playerTexture);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                DebugRenderer.getInstance().addMessage("Player texture loaded successfully!", 3.0f);
            } else {
                DebugRenderer.getInstance().addError("Failed to load player texture!", 5.0f);
            }
        }
        
        // Load OBJ player model
        if (playerModel == null) {
            DebugRenderer.getInstance().addMessage("Attempting to load wolf model from: models/Wolf_obj.obj", 3.0f);
            playerModel = new OBJModelRenderer("models/Wolf_obj.obj");
            if (playerModel.isLoaded()) {
                DebugRenderer.getInstance().addMessage("OBJ player model (Wolf) loaded successfully!", 3.0f);
                DebugRenderer.getInstance().addMessage("Vertex count: " + playerModel.getVertexCount(), 3.0f);
                
                // Debug: Print model bounds
                float[] bounds = playerModel.getModelBounds();
                if (bounds != null) {
                    DebugRenderer.getInstance().addMessage(String.format("Model bounds: X[%.3f, %.3f] Y[%.3f, %.3f] Z[%.3f, %.3f]",
                        bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]), 3.0f);
                    DebugRenderer.getInstance().addMessage(String.format("Model size: %.3f x %.3f x %.3f",
                        bounds[1] - bounds[0], bounds[3] - bounds[2], bounds[5] - bounds[4]), 3.0f);
                }
            } else {
                DebugRenderer.getInstance().addError("Failed to load OBJ player model!", 5.0f);
            }
        }
    }

    public void render(Player player, float yaw, float pitch) {
        // Only render player model if texture is loaded AND player is in no-clip mode (third-person view)
        if (playerTexture == -1 || !player.isNoClipMode()) return;

        glPushMatrix();
        
        // Move to player position and adjust height to make feet touch ground
        glTranslatef(player.getX(), player.getY() - LEG_HEIGHT/3, player.getZ());
        
        // Enable texturing
        glEnable(GL_TEXTURE_2D);
        
        // Render the wolf model
        if (playerModel != null && playerModel.isLoaded()) {
            // System.out.println("Rendering wolf model with " + playerModel.getVertexCount() + " vertices");
            // Render the entire wolf model
            glPushMatrix();
            
            // Disable face culling to see if model is inside out
            glDisable(GL_CULL_FACE);
            
            // Set bright color to make sure model is visible
            glColor4f(1.0f, 0.0f, 0.0f, 1.0f); // Bright red
            
            // Render at player position (we're already at player position from glTranslatef above)
            // No additional translation needed since we're already positioned
            
            // Center the model vertically (model Y goes from 0 to 0.578, so center at 0.289)
            glTranslatef(0, 0.289f, 0);
            
            // Apply player rotation
            glRotatef(-yaw, 0, 1, 0);  // Rotate around Y axis (left/right)
            glRotatef(-pitch, 1, 0, 0);  // Invert pitch rotation for natural up/down movement
            
            // Much larger scale since the model is very small (0.24 x 0.58 x 1.02)
            playerModel.render(20.0f); // Scale up significantly to make it visible
            glPopMatrix();
            
            // Re-enable face culling
            glEnable(GL_CULL_FACE);
            
            // Reset color
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            DebugRenderer.getInstance().addMessage("Wolf model not loaded, rendering fallback cube", 2.0f);
            // Fallback: simple colored cube if wolf model fails to load
            glDisable(GL_TEXTURE_2D);
            glColor3f(0.8f, 0.6f, 0.4f); // Skin tone color
            
            glBegin(GL_QUADS);
            // Simple cube as fallback
            float size = HEAD_SIZE;
            // Front
            glVertex3f(-size, -size, size); glVertex3f(size, -size, size); glVertex3f(size, size, size); glVertex3f(-size, size, size);
            // Back
            glVertex3f(-size, -size, -size); glVertex3f(size, -size, -size); glVertex3f(size, size, -size); glVertex3f(-size, size, -size);
            // Left
            glVertex3f(-size, -size, -size); glVertex3f(-size, -size, size); glVertex3f(-size, size, size); glVertex3f(-size, size, -size);
            // Right
            glVertex3f(size, -size, -size); glVertex3f(size, -size, size); glVertex3f(size, size, size); glVertex3f(size, size, -size);
            // Top
            glVertex3f(-size, size, -size); glVertex3f(size, size, -size); glVertex3f(size, size, size); glVertex3f(-size, size, size);
            // Bottom
            glVertex3f(-size, -size, -size); glVertex3f(size, -size, -size); glVertex3f(size, -size, size); glVertex3f(-size, -size, size);
            glEnd();
            
            glEnable(GL_TEXTURE_2D);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        }
        

        
        glPopMatrix();
    }
    
    public void cleanup() {
        if (playerTexture != -1) {
            glDeleteTextures(playerTexture);
            playerTexture = -1;
        }
    }
} 