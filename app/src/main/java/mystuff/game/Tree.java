package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.utils.TextureLoader;
import static org.lwjgl.opengl.GL11.*;

public class Tree extends GameObject {
    private static final float TREE_WIDTH = 6.0f;   // Width of tree billboard
    private static final float TREE_HEIGHT = 8.0f;  // Height of tree billboard
    private static int treeTexture = -1;
    private static boolean textureLoaded = false;

    public Tree(float x, float y, float z) {
        super(x, y, z);
        loadTexture();
    }
    
    private static void loadTexture() {
        if (!textureLoaded) {
            System.out.println("Loading billboard tree texture...");
            treeTexture = TextureLoader.loadTexture("resources/textures/tree.png");
            if (treeTexture != -1) {
                System.out.println("Tree billboard texture loaded successfully!");
                glBindTexture(GL_TEXTURE_2D, treeTexture);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
                textureLoaded = true;
            } else {
                System.err.println("Failed to load tree.png - using fallback rendering");
            }
        }
    }

    @Override
    public void update(mystuff.engine.Window window, float deltaTime) {
        // Static trees don't need updates
    }

    private void renderBillboard(float angle) {
        glPushMatrix();
        glRotatef(angle, 0, 1, 0);  // Rotate around Y-axis
        
        // Draw billboard quad from ground up (flipped texture coordinates)
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 1.0f); glVertex3f(-TREE_WIDTH/2, 0, 0);           // Bottom left
        glTexCoord2f(1.0f, 1.0f); glVertex3f(TREE_WIDTH/2, 0, 0);            // Bottom right  
        glTexCoord2f(1.0f, 0.0f); glVertex3f(TREE_WIDTH/2, TREE_HEIGHT, 0);  // Top right
        glTexCoord2f(0.0f, 0.0f); glVertex3f(-TREE_WIDTH/2, TREE_HEIGHT, 0); // Top left
        glEnd();
        
        glPopMatrix();
    }

    @Override
    public void render() {
        loadTexture(); // Ensure texture is loaded
        
        glPushMatrix();
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Position the tree
        glTranslatef(x, y, z);
        
        if (treeTexture != -1) {
            // Render with texture
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, treeTexture);
            
            // Sharp alpha testing for clean edges
            glEnable(GL_ALPHA_TEST);
            glAlphaFunc(GL_GREATER, 0.5f);
            
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // White for proper texture colors
        } else {
            // Fallback: simple colored tree
            glDisable(GL_TEXTURE_2D);
            glColor3f(0.0f, 0.6f, 0.0f); // Green
        }
        
        // Disable culling so we can see both sides of the billboard
        glDisable(GL_CULL_FACE);
        
        // Render cross-pattern (2 intersecting billboards like 7 Days to Die)
        renderBillboard(0.0f);   // First billboard
        renderBillboard(90.0f);  // Second billboard perpendicular to first
        
        glPopAttrib();
        glPopMatrix();
    }

    public void cleanup() {
        // Individual trees don't clean up the shared texture
    }
    
    public static void cleanupSharedResources() {
        if (treeTexture != -1) {
            glDeleteTextures(treeTexture);
            treeTexture = -1;
            textureLoaded = false;
            System.out.println("Tree texture resources cleaned up");
        }
    }
}
