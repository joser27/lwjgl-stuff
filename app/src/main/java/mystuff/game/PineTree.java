package mystuff.game;

import mystuff.utils.TextureLoader;
import static org.lwjgl.opengl.GL11.*;

public class PineTree extends Tree {
    // Pine tree specific properties
    private static final float PINE_WIDTH = 24.0f;   // Narrower than oak for pine needles
    private static final float PINE_HEIGHT = 70.0f;  // Taller than oak for pine trees
    private static int pineTexture = -1;
    
    public PineTree(float x, float y, float z) {
        super(x, y, z);
        loadPineTexture();
    }
    
    private static void loadPineTexture() {
        if (pineTexture == -1) {
            pineTexture = TextureLoader.loadTexture("resources/textures/Pine_Tree.png");
            if (pineTexture != -1) {
                System.out.println("Pine tree texture loaded: " + pineTexture);
            } else {
                System.err.println("Failed to load pine tree texture");
            }
        }
    }
    
    @Override
    public void render() {
        if (pineTexture == -1) return;
        
        // Save state
        glPushMatrix();
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Position
        glTranslatef(x, y, z);
        
        // Enable texture
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, pineTexture);
        
        // Use alpha testing for sharp transparency (no blending issues)
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f); // Pixels above 10% opacity are visible
        
        // White color (don't tint the texture)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Disable face culling
        glDisable(GL_CULL_FACE);
        
        // Render pine-specific billboards
        renderPineBillboards();
        
        // Restore state
        glPopAttrib();
        glPopMatrix();
    }
    
    private void renderPineBillboards() {
        // Use pine-specific dimensions
        float width = PINE_WIDTH;
        float height = PINE_HEIGHT;
        
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
    
    public static void cleanupPineResources() {
        if (pineTexture != -1) {
            glDeleteTextures(pineTexture);
            pineTexture = -1;
        }
    }
}
