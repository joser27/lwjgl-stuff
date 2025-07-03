package mystuff.game;

import mystuff.utils.TextureLoader;
import static org.lwjgl.opengl.GL11.*;

public class BroadleafTree extends Tree {
    // Broadleaf tree specific properties
    private static final float BROADLEAF_WIDTH = 14.0f;   // Smaller than oak for compact broad leaves
    private static final float BROADLEAF_HEIGHT = 18.0f;  // Shorter than oak
    private static int broadleafTexture = -1;
    
    public BroadleafTree(float x, float y, float z) {
        super(x, y, z);
        loadBroadleafTexture();
    }
    
    private static void loadBroadleafTexture() {
        if (broadleafTexture == -1) {
            broadleafTexture = TextureLoader.loadTexture("resources/textures/broadleaf_tree.png");
            if (broadleafTexture != -1) {
                System.out.println("Broadleaf tree texture loaded: " + broadleafTexture);
            } else {
                System.err.println("Failed to load broadleaf tree texture");
            }
        }
    }
    
    @Override
    public void render() {
        if (broadleafTexture == -1) return;
        
        // Save state
        glPushMatrix();
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Position
        glTranslatef(x, y, z);
        
        // Enable texture
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, broadleafTexture);
        
        // Use alpha testing for sharp transparency (no blending issues)
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f); // Pixels above 10% opacity are visible
        
        // White color (don't tint the texture)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Disable face culling
        glDisable(GL_CULL_FACE);
        
        // Render broadleaf-specific billboards
        renderBroadleafBillboards();
        
        // Restore state
        glPopAttrib();
        glPopMatrix();
    }
    
    private void renderBroadleafBillboards() {
        // Use broadleaf-specific dimensions
        float width = BROADLEAF_WIDTH;
        float height = BROADLEAF_HEIGHT;
        
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
    
    public static void cleanupBroadleafResources() {
        if (broadleafTexture != -1) {
            glDeleteTextures(broadleafTexture);
            broadleafTexture = -1;
        }
    }
} 