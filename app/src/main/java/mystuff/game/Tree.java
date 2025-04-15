package mystuff.game;

import mystuff.utils.Shapes;
import mystuff.engine.GameObject;
import mystuff.utils.TextureLoader;
import static org.lwjgl.opengl.GL11.*;

public class Tree extends GameObject {
    private static final float TREE_HEIGHT = 20.0f;
    private static final float TREE_WIDTH = 2.0f;
    private static final float TREE_DEPTH = 2.0f;
    private static final int SIDES = 8;
    private static int treeTexture = -1;
    private Leaves leaves; 

    public Tree(float x, float y, float z) {
        super(x, y, z);
        if (treeTexture == -1) {
            treeTexture = TextureLoader.loadTexture("resources/textures/Wood.png");
            if (treeTexture != -1) {
                glBindTexture(GL_TEXTURE_2D, treeTexture);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                System.out.println("Tree texture loaded successfully!");
            } else {
                System.err.println("Failed to load tree texture!");
            }
        }
        leaves = new Leaves();
    }

    @Override
    public void update(mystuff.engine.Window window, float deltaTime) {
        // Trees don't need to update for now
    }

    @Override
    public void render() {
        if (treeTexture == -1) return;

        glPushMatrix();
        
        // Move to tree position and offset down by half width to align with ground
        glTranslatef(x, y - TREE_WIDTH/3, z);
        
        // Enable texturing
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, treeTexture);
        
        // Set color to white to render texture properly
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        float radius = TREE_WIDTH / 2;
        float angleStep = (float) (2 * Math.PI / SIDES);
        
        // Render each side of the octagonal trunk
        for (int i = 0; i < SIDES; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) * angleStep;
            
            float x1 = (float) (radius * Math.cos(angle1));
            float z1 = (float) (radius * Math.sin(angle1));
            float x2 = (float) (radius * Math.cos(angle2));
            float z2 = (float) (radius * Math.sin(angle2));
            
            // Draw the side face
            glBegin(GL_QUADS);
            glTexCoord2f(i / (float)SIDES, 1);       glVertex3f(x1, 0, z1);
            glTexCoord2f((i + 1) / (float)SIDES, 1); glVertex3f(x2, 0, z2);
            glTexCoord2f((i + 1) / (float)SIDES, 0); glVertex3f(x2, TREE_HEIGHT, z2);
            glTexCoord2f(i / (float)SIDES, 0);       glVertex3f(x1, TREE_HEIGHT, z1);
            glEnd();
        }

        // Cleanup trunk rendering
        glDisable(GL_TEXTURE_2D);
        glPopMatrix();

        // Render leaves starting from about 1/4 up the trunk
        float leavesStartHeight = TREE_HEIGHT * 0.25f;
        leaves.render(x, y + leavesStartHeight - TREE_WIDTH/3, z);
    }

    public void cleanup() {
        if (treeTexture != -1) {
            glDeleteTextures(treeTexture);
            treeTexture = -1;
        }
        if (leaves != null) {
            leaves.cleanup();
        }
    }
}
