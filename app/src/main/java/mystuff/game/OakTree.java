package mystuff.game;

import mystuff.utils.TextureLoader;
import static org.lwjgl.opengl.GL11.*;

public class OakTree extends Tree {
    // Oak tree specific properties
    private static final float OAK_WIDTH = 30.0f;
    private static final float OAK_HEIGHT = 36.0f;
    private static int oakTexture = -1;
    
    public OakTree(float x, float y, float z) {
        super(x, y, z);
        loadOakTexture();
    }
    
    private static void loadOakTexture() {
        if (oakTexture == -1) {
            oakTexture = TextureLoader.loadTexture("textures/oak_tree.png");
            if (oakTexture != -1) {
                System.out.println("Oak tree texture loaded: " + oakTexture);
            } else {
                System.err.println("Failed to load oak tree texture");
            }
        }
    }
    
    @Override
    public void render() {
        if (oakTexture == -1) return;
        
        // Save state
        glPushMatrix();
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Position
        glTranslatef(x, y, z);
        
        // Enable texture
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, oakTexture);
        
        // Use alpha testing for sharp transparency (no blending issues)
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f); // Pixels above 10% opacity are visible
        
        // White color (don't tint the texture)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Disable face culling
        glDisable(GL_CULL_FACE);
        
        // Render oak-specific billboards
        renderOakBillboards();
        
        // Restore state
        glPopAttrib();
        glPopMatrix();
    }
    
    private void renderOakBillboards() {
        // Use oak-specific dimensions
        float width = OAK_WIDTH;
        float height = OAK_HEIGHT;
        
        // Render first billboard (facing X direction)
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 1.0f); glVertex3f(-width/2, 0, 0);
        glTexCoord2f(1.0f, 1.0f); glVertex3f(width/2, 0, 0);
        glTexCoord2f(1.0f, 0.0f); glVertex3f(width/2, height, 0);
        glTexCoord2f(0.0f, 0.0f); glVertex3f(-width/2, height, 0);
        glEnd();
        
        // Render second billboard (facing Z direction, perpendicular to first)
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 1.0f); glVertex3f(0, 0, -width/2);
        glTexCoord2f(1.0f, 1.0f); glVertex3f(0, 0, width/2);
        glTexCoord2f(1.0f, 0.0f); glVertex3f(0, height, width/2);
        glTexCoord2f(0.0f, 0.0f); glVertex3f(0, height, -width/2);
        glEnd();
    }
    
    public static void cleanupOakResources() {
        if (oakTexture != -1) {
            glDeleteTextures(oakTexture);
            oakTexture = -1;
        }
    }
} 