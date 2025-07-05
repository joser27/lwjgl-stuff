package mystuff.game;

import static org.lwjgl.opengl.GL11.*;
import mystuff.utils.TextureLoader;
import mystuff.utils.Debug;
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
                System.out.println("Player texture loaded successfully!");
            } else {
                System.err.println("Failed to load player texture!");
            }
        }
        
        // Load OBJ player model
        if (playerModel == null) {
            System.out.println("Attempting to load wolf model from: models/Wolf_obj.obj");
            playerModel = new OBJModelRenderer("models/Wolf_obj.obj");
            if (playerModel.isLoaded()) {
                System.out.println("OBJ player model (Wolf) loaded successfully!");
                System.out.println("Vertex count: " + playerModel.getVertexCount());
                
                // Debug: Print model bounds
                float[] bounds = playerModel.getModelBounds();
                if (bounds != null) {
                    System.out.printf("Model bounds: X[%.3f, %.3f] Y[%.3f, %.3f] Z[%.3f, %.3f]%n",
                        bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
                    System.out.printf("Model size: %.3f x %.3f x %.3f%n",
                        bounds[1] - bounds[0], bounds[3] - bounds[2], bounds[5] - bounds[4]);
                }
            } else {
                System.err.println("Failed to load OBJ player model!");
            }
        }
    }

    public void render(Player player, float yaw, float pitch) {
        // Only render player model if texture is loaded
        if (playerTexture == -1) return;

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
            System.out.println("Wolf model not loaded, rendering fallback cube");
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
        
        // Render bounding box if debug mode is enabled
        if (Debug.showBoundingBoxes()) {
            BoundingBox bb = player.getBoundingBox();
            glPushMatrix();
            // Reset position since we're already at player's position
            glColor3f(1.0f, 0.0f, 0.0f);  // Red for player bounding box
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);  // Wireframe mode
            
            // Draw simple wireframe box
            float[] size = bb.getSize();
            float width = size[0];
            float height = size[1];
            float depth = size[2];
            
            // Draw the 12 edges of the bounding box
            glBegin(GL_LINES);
            // Front face
            glVertex3f(-width/2, -height/2, depth/2); glVertex3f(width/2, -height/2, depth/2);
            glVertex3f(width/2, -height/2, depth/2); glVertex3f(width/2, height/2, depth/2);
            glVertex3f(width/2, height/2, depth/2); glVertex3f(-width/2, height/2, depth/2);
            glVertex3f(-width/2, height/2, depth/2); glVertex3f(-width/2, -height/2, depth/2);
            // Back face
            glVertex3f(-width/2, -height/2, -depth/2); glVertex3f(width/2, -height/2, -depth/2);
            glVertex3f(width/2, -height/2, -depth/2); glVertex3f(width/2, height/2, -depth/2);
            glVertex3f(width/2, height/2, -depth/2); glVertex3f(-width/2, height/2, -depth/2);
            glVertex3f(-width/2, height/2, -depth/2); glVertex3f(-width/2, -height/2, -depth/2);
            // Connecting edges
            glVertex3f(-width/2, -height/2, depth/2); glVertex3f(-width/2, -height/2, -depth/2);
            glVertex3f(width/2, -height/2, depth/2); glVertex3f(width/2, -height/2, -depth/2);
            glVertex3f(width/2, height/2, depth/2); glVertex3f(width/2, height/2, -depth/2);
            glVertex3f(-width/2, height/2, depth/2); glVertex3f(-width/2, height/2, -depth/2);
            glEnd();
            
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);  // Back to fill mode
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);  // Reset color
            glPopMatrix();
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