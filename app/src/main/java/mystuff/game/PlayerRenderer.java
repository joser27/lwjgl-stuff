package mystuff.game;

import static org.lwjgl.opengl.GL11.*;
import mystuff.utils.Debug;
import mystuff.utils.DebugRenderer;
import mystuff.utils.OBJModelRenderer;

public class PlayerRenderer {
    // OBJ model support - using beggar model
    private static OBJModelRenderer playerModel = null;
    
    // Model dimensions and scale
    private static final float PLAYER_SCALE = 1.1f;
    private static final float LEG_HEIGHT = 0.75f;
    
    public void init() {
        // Load beggar model for player (using first frame of walk animation)
        if (playerModel == null) {
            DebugRenderer.getInstance().addMessage("Loading beggar model for player from: animations/beggar/walk/walk0001.obj", 3.0f);
            playerModel = new OBJModelRenderer("animations/beggar/walk/walk0001.obj", "textures/beggar1st_albedo.png");
            if (playerModel.isLoaded()) {
                DebugRenderer.getInstance().addMessage("Player model (beggar) loaded successfully!", 3.0f);
                DebugRenderer.getInstance().addMessage("Vertex count: " + playerModel.getVertexCount(), 3.0f);
                
                // Debug: Print model bounds
                float[] bounds = playerModel.getModelBounds();
                if (bounds != null) {
                    DebugRenderer.getInstance().addMessage(String.format("Model bounds: X[%.3f, %.3f] Y[%.3f, %.3f] Z[%.3f, %.3f]",
                        bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]), 3.0f);
                    DebugRenderer.getInstance().addMessage(String.format("Model size: %.3f x %.3f x %.3f",
                        bounds[1] - bounds[0], bounds[3] - bounds[2], bounds[5] - bounds[4]), 3.0f);
                    DebugRenderer.getInstance().addMessage(String.format("Model Y offset needed: %.3f", -bounds[2]), 3.0f);
                }
            } else {
                DebugRenderer.getInstance().addError("Failed to load player model!", 5.0f);
            }
        }
    }

    public void render(Player player, float yaw, float pitch) {
        // Only render player model if model is loaded AND player is in no-clip mode (third-person view)
        if (playerModel == null || !playerModel.isLoaded() || !player.isNoClipMode()) return;

        glPushMatrix();
        
        // Move to player position and adjust height to align with floor
        // Move the model down significantly to place it on the ground
        glTranslatef(player.getX(), player.getY() - PLAYER_SCALE * 0.8f, player.getZ());
        
        // Apply player rotation to face camera direction
        glRotatef(-yaw, 0, 1, 0);  // Rotate around Y axis (left/right)
        
        // Render the beggar model
        if (playerModel != null && playerModel.isLoaded()) {
            // Disable face culling for better visibility
            glDisable(GL_CULL_FACE);
            
            // Set normal color
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            
            // Render the model with appropriate scale
            playerModel.render(PLAYER_SCALE);
            
            // Re-enable face culling
            glEnable(GL_CULL_FACE);
        } else {
            DebugRenderer.getInstance().addMessage("Player model not loaded, rendering fallback cube", 2.0f);
            // Fallback: simple colored cube if model fails to load
            glDisable(GL_TEXTURE_2D);
            glColor3f(0.8f, 0.6f, 0.4f); // Skin tone color
            
            glBegin(GL_QUADS);
            // Simple cube as fallback
            float size = PLAYER_SCALE;
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
        // Cleanup is handled by OBJModelRenderer
        if (playerModel != null) {
            playerModel.cleanup();
            playerModel = null;
        }
    }
} 