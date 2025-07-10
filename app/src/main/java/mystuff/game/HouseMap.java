package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.utils.GLBModelRenderer;
import mystuff.utils.GLBLoader.MeshInfo;
import mystuff.utils.Debug;
import mystuff.utils.DebugRenderer;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;
import java.util.List;

public class HouseMap extends GameObject {
    private static final float HOUSE_SCALE = 1f;
    private static GLBModelRenderer houseModel = null;
    private static boolean triedLoad = false;
    private static String HOUSE_GLB = "models/Tacos.glb";
    
    // Geometry-based collision detection
    private GLBGeometryCollision geometryCollision;
    
    public HouseMap(float x, float y, float z) {
        super(x, y, z);
        if (!triedLoad) {
            // Load the GLB house model with automatic texture matching
            // The new system will automatically match materials to textures in textures/house/
            // missing_texture.jpg will be used as fallback for unmatched materials
            houseModel = new GLBModelRenderer(HOUSE_GLB, "textures/missing_texture.jpg");
            triedLoad = true;
            if (houseModel.isLoaded()) {
                DebugRenderer.getInstance().addMessage("House GLB loaded with automatic texture matching!", 3.0f);
                DebugRenderer.getInstance().addMessage("  Vertices: " + houseModel.getVertexCount(), 3.0f);
                float[] bounds = houseModel.getModelBounds();
                if (bounds != null) {
                    System.out.printf("  House bounds: X[%.3f, %.3f] Y[%.3f, %.3f] Z[%.3f, %.3f]%n",
                        bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
                }
            } else {
                DebugRenderer.getInstance().addError("Failed to load house GLB model", 5.0f);
            }
        }
        
        // Create geometry-based collision system
        setupGeometryCollision();
        
        // Register with CollisionManager for player collision detection
        if (geometryCollision != null) {
            CollisionManager.getInstance().addGLBGeometryCollision(geometryCollision);
        }
    }
    
    private void setupGeometryCollision() {
        if (houseModel == null || !houseModel.isLoaded() || !houseModel.hasMeshData()) {
            DebugRenderer.getInstance().addError("Cannot setup geometry collision - model not loaded or no mesh data", 5.0f);
            return;
        }
        
        // Create geometry collision system
        geometryCollision = new GLBGeometryCollision(getX(), getY(), getZ(), HOUSE_SCALE);
        
        // Get mesh data from the GLB model
        MeshInfo[] meshes = houseModel.getMeshes();
        float[] vertices = houseModel.getVertices();
        int[] indices = houseModel.getIndices();
        
        if (meshes == null || vertices == null || indices == null) {
            DebugRenderer.getInstance().addError("Missing mesh data for geometry collision detection", 5.0f);
            return;
        }
        
        DebugRenderer.getInstance().addMessage("Setting up geometry collision for " + meshes.length + " meshes...", 3.0f);
        
        // Process each mesh to extract triangle geometry
        for (int i = 0; i < meshes.length; i++) {
            MeshInfo mesh = meshes[i];
            String meshName = houseModel.getMeshName(i);
            
            // Add geometry data for this mesh (uses actual triangles like wireframe rendering)
            geometryCollision.addGeometryData(vertices, indices, mesh);
        }
        
        // Build overall bounds for quick culling
        geometryCollision.buildOverallBounds();
        
        DebugRenderer.getInstance().addMessage("Geometry collision setup complete: " + geometryCollision.getTriangleCount() + " triangles", 3.0f);
    }
    
    /**
     * Check if player collides with this house (legacy method - now uses capsule collision)
     */
    @Deprecated
    public boolean checkCollision(Player player) {
        if (geometryCollision == null || player.getCapsuleCollider() == null) {
            return false;
        }
        return geometryCollision.checkCapsuleCollision(player.getCapsuleCollider());
    }
    
    /**
     * Get the geometry collision system for this house
     */
    public GLBGeometryCollision getGeometryCollision() {
        return geometryCollision;
    }

    @Override
    public void render() {
        if (houseModel != null && houseModel.isLoaded()) {
            GL11.glPushMatrix();
            
            // Position the house
            GL11.glTranslatef(getX(), getY(), getZ());
            
            // Apply rotation to orient the house properly
            GL11.glRotatef(0.0f, 1.0f, 0.0f, 0.0f); // No X rotation
            GL11.glRotatef(0.0f, 0.0f, 1.0f, 0.0f); // No Y rotation  
            GL11.glRotatef(0.0f, 0.0f, 0.0f, 1.0f); // No Z rotation
            
            // Render the house model with proper scaling
            houseModel.render(HOUSE_SCALE);
            
            GL11.glPopMatrix();
        } else {
            // Fallback rendering if model fails to load
            renderFallbackCube();
        }
        
        // Render collision geometry in debug mode
        if (geometryCollision != null) {
            geometryCollision.renderDebugWireframe();
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
        // Unregister from CollisionManager
        if (geometryCollision != null) {
            CollisionManager.getInstance().removeGLBGeometryCollision(geometryCollision);
        }
        
        if (houseModel != null) {
            houseModel.cleanup();
        }
    }

    @Override
    public void update(mystuff.engine.Window window, float deltaTime) {
        // No update logic needed for static house
    }
}
