package mystuff.game;

import mystuff.utils.Shapes;
import mystuff.utils.TextureLoader;
import static org.lwjgl.opengl.GL11.*;

public class Leaves {
    private static final int NUM_CROSS_QUADS = 4;   // Number of crossing billboard quads
    private static final float LEAF_WIDTH = 30.0f;   // Width of leaf billboards
    private static final float LEAF_HEIGHT = 40.0f;  // Height of leaf billboards
    private static int leavesTexture = -1;
    
    public Leaves() {
        if (leavesTexture == -1) {
            System.out.println("Attempting to load leaves texture...");
            leavesTexture = TextureLoader.loadTexture("resources/textures/bigtree.png");
            if (leavesTexture != -1) {
                System.out.println("Successfully loaded leaves texture with ID: " + leavesTexture);
                glBindTexture(GL_TEXTURE_2D, leavesTexture);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
            } else {
                System.err.println("Failed to load leaves texture!");
            }
        }
    }

    private void renderLeafQuad(float width, float height, float angle, float yOffset, java.util.Random random) {
        glPushMatrix();
        
        // Add vertical offset for layering
        glTranslatef(0, yOffset, 0);
        
        // Rotate around Y axis for crossed billboard effect
        glRotatef(angle, 0, 1, 0);
        
        // Add slight random tilt for variation
        float tiltX = (random.nextFloat() * 10.0f - 5.0f);
        float tiltZ = (random.nextFloat() * 10.0f - 5.0f);
        glRotatef(tiltX, 1, 0, 0);
        glRotatef(tiltZ, 0, 0, 1);
        
        // Draw the leaf quad
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 0.0f);
        glVertex3f(-width/2, 0, 0);
        glTexCoord2f(1.0f, 0.0f);
        glVertex3f(width/2, 0, 0);
        glTexCoord2f(1.0f, 1.0f);
        glVertex3f(width/2, height, 0);
        glTexCoord2f(0.0f, 1.0f);
        glVertex3f(-width/2, height, 0);
        glEnd();
        
        glPopMatrix();
    }

    public void render(float x, float y, float z) {
        if (leavesTexture == -1) {
            System.err.println("Warning: Leaves texture not loaded!");
            return;
        }

        // Create random generator with seed based on tree position
        long seed = (long)(x * 1000 + z * 100);
        java.util.Random random = new java.util.Random(seed);
        
        // Setup for leaves
        glPushMatrix();
        glTranslatef(x, y, z);  // Position at the specified coordinates
        
        // Save current OpenGL state
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        
        // Enable alpha testing to discard fully transparent pixels
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f);
        
        // Enable texturing and blending
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Bind texture and set color to white (no tint)
        glBindTexture(GL_TEXTURE_2D, leavesTexture);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Temporarily disable depth writing but keep depth testing
        glDepthMask(false);
        
        // Calculate angle step for evenly distributed quads
        float angleStep = 180.0f / NUM_CROSS_QUADS;
        
        // Render crossed billboards in two layers
        for (int layer = 0; layer < 2; layer++) {
            float yOffset = layer * LEAF_HEIGHT * 0.5f;
            for (int i = 0; i < NUM_CROSS_QUADS; i++) {
                float angle = i * angleStep + (layer * 45.0f);  // Offset angles between layers
                // Render both front and back faces of each quad
                renderLeafQuad(LEAF_WIDTH, LEAF_HEIGHT, angle, yOffset, random);
                renderLeafQuad(LEAF_WIDTH * 0.8f, LEAF_HEIGHT * 0.9f, angle + 90, yOffset + LEAF_HEIGHT * 0.25f, random);
            }
        }
        
        // Restore depth writing
        glDepthMask(true);
        
        // Restore previous OpenGL state
        glPopAttrib();
        glPopMatrix();
    }

    public void cleanup() {
        if (leavesTexture != -1) {
            glDeleteTextures(leavesTexture);
            leavesTexture = -1;
        }
    }
} 