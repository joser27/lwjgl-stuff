package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.engine.Window;
import mystuff.utils.Shapes;
import org.lwjgl.opengl.GL11;

public class Block {
    private float x, y, z;
    private BlockType type;
    private BoundingBox boundingBox;

    public Block(float x, float y, float z, BlockType type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        
        // Create a bounding box centered at the block's position
        float halfSize = World.BLOCK_SIZE / 2;
        this.boundingBox = new BoundingBox(
            x - halfSize, y - halfSize, z - halfSize,
            x + halfSize, y + halfSize, z + halfSize
        );
    }

    public void update(Window window, float deltaTime) {
        // Blocks don't need to update currently
    }

    public void render() {
        if (type == BlockType.AIR) return;  // Don't render air blocks

        // Save current OpenGL state
        GL11.glPushMatrix();
        
        // Move to block position
        GL11.glTranslatef(x, y, z);

        // Set color based on block type
        switch (type) {
            case AIR:
                // Air is invisible, no color neededError
                break;
            case STONE:
                // Grey color for stone
                GL11.glColor3f(0.5f, 0.5f, 0.5f);
                break;
            case DIRT:
                // Brown color for dirt
                GL11.glColor3f(0.55f, 0.27f, 0.07f);
                break;
            case GRASS:
                // Green color for grass
                GL11.glColor3f(0.1f, 0.6f, 0.1f);
                break;
        }

        // Render the block at full BLOCK_SIZE
        Shapes.cube(World.BLOCK_SIZE); 

        // Restore OpenGL state
        GL11.glPopMatrix();
        
        boolean debugMode = true; // You could make this a field or get it from somewhere else
        if (debugMode && type != BlockType.AIR) {
            GL11.glPushMatrix();
            GL11.glTranslatef(boundingBox.getCenterX(), boundingBox.getCenterY(), boundingBox.getCenterZ());
            GL11.glColor3f(0.0f, 1.0f, 0.0f);  // Green for block bounding box
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);  // Wireframe mode
            float width = boundingBox.getWidth();
            float height = boundingBox.getHeight();
            float depth = boundingBox.getDepth();
            Shapes.cuboid(width, height, depth);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);  // Back to fill mode
            GL11.glPopMatrix();
        }
    }

    public float getX() { 
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public BlockType getType() {
        return type;
    }
    
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }
}

