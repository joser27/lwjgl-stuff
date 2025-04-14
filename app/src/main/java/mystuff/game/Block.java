package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.engine.Window;
import mystuff.utils.Shapes;
import mystuff.utils.TextureLoader;
import org.lwjgl.opengl.GL11;
import mystuff.utils.Debug;

public class Block {
    private float x, y, z;
    private BlockType type;
    private BoundingBox boundingBox;
    private static int dirtTexture = -1;
    private static int stoneTexture = -1;
    private static int grassTexture = -1;

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

        // Load textures if not already loaded
        if (dirtTexture == -1) {
            System.out.println("Loading block textures...");
            try {
                // Load dirt texture
                dirtTexture = TextureLoader.loadTexture("resources/textures/dirt.png");
                if (dirtTexture != -1) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirtTexture);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                    System.out.println("Successfully loaded dirt texture with ID: " + dirtTexture);
                }

                // Load stone texture
                stoneTexture = TextureLoader.loadTexture("resources/textures/stone.png");
                if (stoneTexture != -1) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, stoneTexture);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                    System.out.println("Successfully loaded stone texture with ID: " + stoneTexture);
                }

                // Load grass texture
                grassTexture = TextureLoader.loadTexture("resources/textures/grass.png");
                if (grassTexture != -1) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassTexture);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                    System.out.println("Successfully loaded grass texture with ID: " + grassTexture);
                }
                
                // Check if any textures failed to load
                if (dirtTexture == -1 || stoneTexture == -1 || grassTexture == -1) {
                    System.err.println("Warning: Some textures failed to load. Using fallback colors.");
                }
            } catch (Exception e) {
                System.err.println("Error loading textures: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void update(Window window, float deltaTime) {
        // Blocks don't need to update currently
    }

    public void render() {
        if (type == BlockType.AIR) return;  // Don't render air blocks

        // Save current OpenGL state
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        
        // Move to block position
        GL11.glTranslatef(x, y, z);

        // Enable texturing
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // Bind appropriate texture based on block type
        int textureID = -1;
        switch (type) {
            case STONE:
                textureID = stoneTexture;
                break;
            case DIRT:
                textureID = dirtTexture;
                break;
            case GRASS:
                textureID = grassTexture;
                break;
        }

        // Set color to white to show texture's true colors
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        if (textureID != -1) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        } else {
            // Fallback colors if texture loading failed
            switch (type) {
                case STONE:
                    GL11.glColor3f(0.5f, 0.5f, 0.5f); // Gray
                    break;
                case DIRT:
                    GL11.glColor3f(0.6f, 0.4f, 0.2f); // Brown
                    break;
                case GRASS:
                    GL11.glColor3f(0.0f, 0.8f, 0.0f); // Green
                    break;
            }
        }

        float size = World.BLOCK_SIZE / 2;  // Half size for centered rendering

        // Front face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex3f(-size, -size, size);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex3f(size, -size, size);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex3f(size, size, size);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex3f(-size, size, size);
        GL11.glEnd();

        // Back face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex3f(-size, -size, -size);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex3f(-size, size, -size);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex3f(size, size, -size);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex3f(size, -size, -size);
        GL11.glEnd();

        // Top face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex3f(-size, size, -size);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex3f(-size, size, size);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex3f(size, size, size);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex3f(size, size, -size);
        GL11.glEnd();

        // Bottom face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex3f(-size, -size, -size);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex3f(size, -size, -size);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex3f(size, -size, size);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex3f(-size, -size, size);
        GL11.glEnd();

        // Right face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex3f(size, -size, -size);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex3f(size, size, -size);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex3f(size, size, size);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex3f(size, -size, size);
        GL11.glEnd();

        // Left face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex3f(-size, -size, -size);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex3f(-size, -size, size);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex3f(-size, size, size);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex3f(-size, size, -size);
        GL11.glEnd();

        // Restore OpenGL state
        GL11.glPopAttrib();
        GL11.glPopMatrix();
        
        if (Debug.showBoundingBoxes() && type != BlockType.AIR) {
            GL11.glPushMatrix();
            GL11.glTranslatef(boundingBox.getCenterX(), boundingBox.getCenterY(), boundingBox.getCenterZ());
            GL11.glColor3f(0.0f, 1.0f, 0.0f);  // Green for block bounding box
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);  // Wireframe mode
            float width = boundingBox.getWidth();
            float height = boundingBox.getHeight();
            float depth = boundingBox.getDepth();
            Shapes.cuboid(width, height, depth);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);  // Back to fill mode
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);  // Reset color
            GL11.glPopMatrix();
        }

        // Add block info display if enabled
        if (Debug.showBlockInfo()) {
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y + World.BLOCK_SIZE, z);
            // Render block type and coordinates
            // Note: You'll need to implement text rendering here
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

    public void cleanup() {
        // Delete textures if they were loaded
        if (dirtTexture != -1) {
            GL11.glDeleteTextures(dirtTexture);
            dirtTexture = -1;
        }
        if (stoneTexture != -1) {
            GL11.glDeleteTextures(stoneTexture);
            stoneTexture = -1;
        }
        if (grassTexture != -1) {
            GL11.glDeleteTextures(grassTexture);
            grassTexture = -1;
        }
    }
}

