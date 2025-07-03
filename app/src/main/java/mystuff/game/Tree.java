package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.utils.TextureLoader;
import static org.lwjgl.opengl.GL11.*;

public abstract class Tree extends GameObject {
    protected static final float TREE_WIDTH = 18.0f;   // Width of tree billboard (3x larger)
    protected static final float TREE_HEIGHT = 24.0f;  // Height of tree billboard (3x larger)
    protected static int treeTexture = -1;

    public Tree(float x, float y, float z) {
        super(x, y, z);
        loadTexture();
    }
    
    protected static void loadTexture() {
        if (treeTexture == -1) {
            treeTexture = TextureLoader.loadTexture(getTexturePath());
            if (treeTexture != -1) {
                System.out.println("Tree texture loaded: " + treeTexture);
            } else {
                System.err.println("Failed to load tree texture: " + getTexturePath());
            }
        }
    }
    
    /**
     * Override this method to specify the texture path for each tree type
     */
    protected static String getTexturePath() {
        return "resources/textures/bigtree.png"; // Default texture
    }

    @Override
    public void update(mystuff.engine.Window window, float deltaTime) {
        // No updates needed for static trees
    }

    @Override
    public void render() {
        if (treeTexture == -1) return;
        
        // Save state
        glPushMatrix();
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Position
        glTranslatef(x, y, z);
        
        // Enable texture
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, treeTexture);
        
        // Use alpha testing for sharp transparency (no blending issues)
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f); // Pixels above 10% opacity are visible
        
        // White color (don't tint the texture)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Disable face culling
        glDisable(GL_CULL_FACE);
        
        // Render the cross-pattern billboards
        renderBillboards();
        
        // Restore state
        glPopAttrib();
        glPopMatrix();
    }
    
    /**
     * Render the cross-pattern billboards. Can be overridden for different tree shapes.
     */
    protected void renderBillboards() {
        // Render first billboard (facing X direction)
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 1.0f); glVertex3f(-TREE_WIDTH/2, 0, 0);
        glTexCoord2f(1.0f, 1.0f); glVertex3f(TREE_WIDTH/2, 0, 0);
        glTexCoord2f(1.0f, 0.0f); glVertex3f(TREE_WIDTH/2, TREE_HEIGHT, 0);
        glTexCoord2f(0.0f, 0.0f); glVertex3f(-TREE_WIDTH/2, TREE_HEIGHT, 0);
        glEnd();
        
        // Render second billboard (facing Z direction, perpendicular to first)
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 1.0f); glVertex3f(0, 0, -TREE_WIDTH/2);
        glTexCoord2f(1.0f, 1.0f); glVertex3f(0, 0, TREE_WIDTH/2);
        glTexCoord2f(1.0f, 0.0f); glVertex3f(0, TREE_HEIGHT, TREE_WIDTH/2);
        glTexCoord2f(0.0f, 0.0f); glVertex3f(0, TREE_HEIGHT, -TREE_WIDTH/2);
        glEnd();
    }

    public void cleanup() {
        // No cleanup for individual trees
    }
    
    public static void cleanupSharedResources() {
        if (treeTexture != -1) {
            glDeleteTextures(treeTexture);
            treeTexture = -1;
        }
    }
}
