package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.utils.GLBModelRenderer;
import org.lwjgl.opengl.GL11;

public class HouseMap extends GameObject {
    private static final float HOUSE_SCALE = 1.0f; // Scale of the house model
    private static GLBModelRenderer houseModel = null;
    private static boolean triedLoad = false;
    private static String HOUSE_GLB = "models/Quequis_House.glb";
    

    public HouseMap(float x, float y, float z) {
        super(x, y, z);
        if (!triedLoad) {
            // Try to load the GLB house model
            // GLB models have embedded textures, so no separate texture path needed
            houseModel = new GLBModelRenderer(HOUSE_GLB);
            triedLoad = true;
            if (houseModel.isLoaded()) {
                System.out.println("House OBJ loaded! Vertices: " + houseModel.getVertexCount());
                float[] bounds = houseModel.getModelBounds();
                if (bounds != null) {
                    System.out.printf("House bounds: X[%.3f, %.3f] Y[%.3f, %.3f] Z[%.3f, %.3f]%n",
                        bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
                    System.out.printf("House size: %.3f x %.3f x %.3f%n",
                        bounds[1] - bounds[0], bounds[3] - bounds[2], bounds[5] - bounds[4]);
                }
            } else {
                System.err.println("Failed to load house.glb: " + HOUSE_GLB);
            }
        }
    }

    @Override
    public void render() {
        if (houseModel != null && houseModel.isLoaded()) {
            GL11.glPushMatrix();
            
            // Position the house
            GL11.glTranslatef(getX(), getY(), getZ());
            
            // You might need to adjust rotation based on how the model is oriented
            // GL11.glRotatef(0.0f, 1.0f, 0.0f, 0.0f); // Adjust if needed
            
            // Render the house model
            houseModel.render(HOUSE_SCALE);
            
            GL11.glPopMatrix();
        } else {
            // Fallback rendering if model fails to load
            renderFallbackCube();
        }
    }
    
    private void renderFallbackCube() {
        GL11.glPushMatrix();
        GL11.glTranslatef(getX(), getY(), getZ());
        GL11.glColor3f(0.6f, 0.4f, 0.2f); // Brown color for house
        
        // Draw a simple house-like cube
        float size = 10.0f;
        GL11.glBegin(GL11.GL_QUADS);
        // Front face
        GL11.glVertex3f(-size, 0, size);
        GL11.glVertex3f(size, 0, size);
        GL11.glVertex3f(size, size*2, size);
        GL11.glVertex3f(-size, size*2, size);
        // Back face
        GL11.glVertex3f(-size, 0, -size);
        GL11.glVertex3f(-size, size*2, -size);
        GL11.glVertex3f(size, size*2, -size);
        GL11.glVertex3f(size, 0, -size);
        // Top face
        GL11.glVertex3f(-size, size*2, -size);
        GL11.glVertex3f(-size, size*2, size);
        GL11.glVertex3f(size, size*2, size);
        GL11.glVertex3f(size, size*2, -size);
        // Bottom face
        GL11.glVertex3f(-size, 0, -size);
        GL11.glVertex3f(size, 0, -size);
        GL11.glVertex3f(size, 0, size);
        GL11.glVertex3f(-size, 0, size);
        // Right face
        GL11.glVertex3f(size, 0, -size);
        GL11.glVertex3f(size, size*2, -size);
        GL11.glVertex3f(size, size*2, size);
        GL11.glVertex3f(size, 0, size);
        // Left face
        GL11.glVertex3f(-size, 0, -size);
        GL11.glVertex3f(-size, 0, size);
        GL11.glVertex3f(-size, size*2, size);
        GL11.glVertex3f(-size, size*2, -size);
        GL11.glEnd();
        
        GL11.glColor3f(1.0f, 1.0f, 1.0f); // Reset color
        GL11.glPopMatrix();
    }

    public void cleanup() {
        if (houseModel != null) {
            houseModel.cleanup();
        }
    }

    @Override
    public void update(mystuff.engine.Window window, float deltaTime) {
        // No update logic needed for static house
    }
}
