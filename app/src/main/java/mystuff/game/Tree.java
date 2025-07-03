package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.utils.TextureLoader;
import static org.lwjgl.opengl.GL11.*;

public class Tree extends GameObject {
    private static final float TREE_WIDTH = 8.0f;   // Width of tree billboard
    private static final float TREE_HEIGHT = 12.0f; // Height of tree billboard
    private static int treeTexture = -1;
    private static final long TEXTURE_RETRY_DELAY = 1000; // 1 second
    private static long lastTextureAttempt = 0;

    public Tree(float x, float y, float z) {
        super(x, y, z);
        loadTextureIfNeeded();
    }
    
    private void loadTextureIfNeeded() {
        if (treeTexture == -1) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTextureAttempt > TEXTURE_RETRY_DELAY) {
                lastTextureAttempt = currentTime;
                
                // Try to load a simpler tree texture, fallback to existing ones
                String[] texturePaths = {
                    "resources/textures/Leaves.png",
                    "resources/textures/cedar_leaves.png", 
                    "resources/textures/bigtree.png"
                };
                
                for (String path : texturePaths) {
                    treeTexture = TextureLoader.loadTexture(path);
                    if (treeTexture != -1) {
                        System.out.println("Tree texture loaded: " + path);
                        glBindTexture(GL_TEXTURE_2D, treeTexture);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
                        break;
                    }
                }
                
                if (treeTexture == -1) {
                    System.err.println("Failed to load any tree texture!");
                }
            }
        }
    }

    @Override
    public void update(mystuff.engine.Window window, float deltaTime) {
        // Trees don't need to update
    }

    private void renderBillboardQuad(float angle) {
        glPushMatrix();
        
        // Rotate around Y axis for cross pattern
        glRotatef(angle, 0, 1, 0);
        
        // Draw the quad from ground up
        glBegin(GL_QUADS);
        // Bottom left
        glTexCoord2f(0.0f, 0.0f);
        glVertex3f(-TREE_WIDTH/2, 0, 0);
        // Bottom right
        glTexCoord2f(1.0f, 0.0f);
        glVertex3f(TREE_WIDTH/2, 0, 0);
        // Top right
        glTexCoord2f(1.0f, 1.0f);
        glVertex3f(TREE_WIDTH/2, TREE_HEIGHT, 0);
        // Top left
        glTexCoord2f(0.0f, 1.0f);
        glVertex3f(-TREE_WIDTH/2, TREE_HEIGHT, 0);
        glEnd();
        
        glPopMatrix();
    }

    @Override
    public void render() {
        loadTextureIfNeeded();
        if (treeTexture == -1) {
            // Fallback: render a simple colored rectangle if no texture
            renderFallbackTree();
            return;
        }

        // Save OpenGL state
        glPushMatrix();
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Move to tree position
        glTranslatef(x, y, z);
        
        // Use alpha testing only for clean, sharp transparency
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.5f);  // Sharp cutoff - pixels are either fully opaque or invisible
        
        // Enable texturing
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, treeTexture);
        
        // Set white color for proper texture rendering
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Disable backface culling so we can see both sides
        glDisable(GL_CULL_FACE);
        
        // Render 2 intersecting billboards (cross pattern)
        renderBillboardQuad(0.0f);   // First plane
        renderBillboardQuad(90.0f);  // Second plane perpendicular to first
        
        // Restore OpenGL state
        glPopAttrib();
        glPopMatrix();
    }
    
    private void renderFallbackTree() {
        // Simple fallback tree if texture fails
        glPushMatrix();
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        glTranslatef(x, y, z);
        glDisable(GL_TEXTURE_2D);
        
        // Brown trunk
        glColor3f(0.5f, 0.3f, 0.1f);
        glPushMatrix();
        glTranslatef(0, TREE_HEIGHT * 0.3f, 0);
        glScalef(0.5f, TREE_HEIGHT * 0.6f, 0.5f);
        // Simple cube for trunk
        glBegin(GL_QUADS);
        // Front face
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        // Back face
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glEnd();
        glPopMatrix();
        
        // Green leaves (simple cross)
        glColor3f(0.0f, 0.7f, 0.0f);
        glDisable(GL_CULL_FACE);
        
        // Cross pattern for leaves
        float leafY = TREE_HEIGHT * 0.7f;
        glBegin(GL_QUADS);
        // First plane
        glVertex3f(-TREE_WIDTH/2, leafY, 0);
        glVertex3f(TREE_WIDTH/2, leafY, 0);
        glVertex3f(TREE_WIDTH/2, TREE_HEIGHT, 0);
        glVertex3f(-TREE_WIDTH/2, TREE_HEIGHT, 0);
        // Second plane
        glVertex3f(0, leafY, -TREE_WIDTH/2);
        glVertex3f(0, leafY, TREE_WIDTH/2);
        glVertex3f(0, TREE_HEIGHT, TREE_WIDTH/2);
        glVertex3f(0, TREE_HEIGHT, -TREE_WIDTH/2);
        glEnd();
        
        glPopAttrib();
        glPopMatrix();
    }

    public void cleanup() {
        // Don't clean up static texture here as it's shared between all trees
    }
    
    public static void cleanupSharedResources() {
        if (treeTexture != -1) {
            glDeleteTextures(treeTexture);
            treeTexture = -1;
        }
    }
}
