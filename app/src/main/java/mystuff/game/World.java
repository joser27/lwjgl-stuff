package mystuff.game;

import org.lwjgl.opengl.GL11;
import mystuff.utils.Shapes;
import java.util.ArrayList;
import java.util.List;

public class World {
    // World constants
    public static final int BLOCK_SIZE = 2;  // Size of each block (reduced from 16)
    public static final int CHUNK_SIZE = 16;  // Size of a chunk in blocks
    public static final int WORLD_SIZE = 8;   // World size in chunks
    
    private List<Block> blocks;

    public World() {
        blocks = new ArrayList<>();
        generateWorld();
    }

    private void generateWorld() {
        // Create a flat ground plane of blocks
        int worldSizeInBlocks = CHUNK_SIZE * WORLD_SIZE;
        
        // Start from 0,0,0 and extend in positive directions
        for(int x = 0; x < worldSizeInBlocks; x++) {
            for(int z = 0; z < worldSizeInBlocks; z++) {
                // Convert grid coordinates to world coordinates
                float worldX = x * BLOCK_SIZE;
                float worldZ = z * BLOCK_SIZE;
                
                // Create ground blocks
                blocks.add(new Block(worldX, -BLOCK_SIZE, worldZ));
                
                // Create border walls
                if(x == 0 || x == worldSizeInBlocks-1 || z == 0 || z == worldSizeInBlocks-1) {
                    for(int y = 0; y < 4; y++) { // 4 blocks high walls
                        blocks.add(new Block(worldX, y * BLOCK_SIZE, worldZ));
                    }
                }
            }
        }
    }

    public void render() {
        // Render blocks
        for(Block block : blocks) {
            block.render();
        }
        
        // Render grid
        renderGrid();
    }

    private void renderGrid() {
        int worldSizeInBlocks = CHUNK_SIZE * WORLD_SIZE;
        float worldSize = worldSizeInBlocks * BLOCK_SIZE;
        
        GL11.glPushMatrix();
        
        // Set grid color (white, semi-transparent)
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.2f);
        
        // Enable blending for transparency
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Disable depth testing temporarily for grid lines
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        // Draw X grid lines
        GL11.glBegin(GL11.GL_LINES);
        for(int i = 0; i <= worldSizeInBlocks; i++) {
            float x = i * BLOCK_SIZE;
            GL11.glVertex3f(x, 0, 0);
            GL11.glVertex3f(x, 0, worldSize);
        }
        GL11.glEnd();
        
        // Draw Z grid lines
        GL11.glBegin(GL11.GL_LINES);
        for(int i = 0; i <= worldSizeInBlocks; i++) {
            float z = i * BLOCK_SIZE;
            GL11.glVertex3f(0, 0, z);
            GL11.glVertex3f(worldSize, 0, z);
        }
        GL11.glEnd();
        
        // Draw origin marker (red lines)
        GL11.glColor3f(1.0f, 0.0f, 0.0f);
        GL11.glBegin(GL11.GL_LINES);
        // X axis
        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(BLOCK_SIZE, 0, 0);
        // Y axis
        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(0, BLOCK_SIZE, 0);
        // Z axis
        GL11.glVertex3f(0, 0, 0);
        GL11.glVertex3f(0, 0, BLOCK_SIZE);
        GL11.glEnd();
        
        // Restore OpenGL state
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        
        GL11.glPopMatrix();
    }
}

class Block {
    private float x, y, z;
    private float size;

    public Block(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = World.BLOCK_SIZE / 2.0f; // Half size because our cube method uses half-extents
    }

    public void render() {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);
        
        // Set block color (different colors based on height for variety)
        if(y < 0) {
            GL11.glColor3f(0.5f, 0.35f, 0.05f); // Brown for ground
        } else {
            GL11.glColor3f(0.7f, 0.7f, 0.7f); // Gray for walls
        }
        
        Shapes.cube(size);
        
        GL11.glPopMatrix();
    }
}
