package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.engine.Window;
import mystuff.utils.GLBModelRenderer;
import mystuff.utils.GLBLoader.MeshInfo;
import mystuff.utils.Debug;
import mystuff.utils.DebugRenderer;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;

/**
 * Generic GLB object class for easy addition of GLB models to the game.
 * Automatically handles texture matching, collision detection, and rendering.
 */
public class GLBObject extends GameObject {
    private static final float DEFAULT_SCALE = 1.0f;
    
    // Static model cache to avoid reloading the same models
    private static GLBModelRenderer modelCache = null;
    private static String lastModelPath = null;
    private static boolean triedLoad = false;
    
    // Instance-specific properties
    private String modelPath;
    private String textureFolder;
    private float scale;
    private float rotationX, rotationY, rotationZ;
    
    // Geometry-based collision detection
    private GLBGeometryCollision geometryCollision;
    
    /**
     * Create a GLB object with automatic texture matching
     * @param x X position
     * @param y Y position  
     * @param z Z position
     * @param modelPath Path to the GLB file (e.g., "models/wooden_stairs_21.glb")
     * @param textureFolder Path to texture folder (e.g., "textures/wooden_stairs_21/")
     */
    public GLBObject(float x, float y, float z, String modelPath, String textureFolder) {
        this(x, y, z, modelPath, textureFolder, DEFAULT_SCALE, 0, 0, 0);
    }
    
    /**
     * Create a GLB object with custom scale and rotation
     */
    public GLBObject(float x, float y, float z, String modelPath, String textureFolder, 
                    float scale, float rotationX, float rotationY, float rotationZ) {
        super(x, y, z);
        this.modelPath = modelPath;
        this.textureFolder = textureFolder;
        this.scale = scale;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
        
        loadModel();
        setupGeometryCollision();
        
        // Register with CollisionManager for player collision detection
        if (geometryCollision != null) {
            CollisionManager.getInstance().addGLBGeometryCollision(geometryCollision);
        }
    }
    
    /**
     * Load the GLB model with automatic texture matching
     */
    private void loadModel() {
        // Check if we need to load a new model
        if (!triedLoad || !modelPath.equals(lastModelPath)) {
            // Load the GLB model with automatic texture matching
            modelCache = new GLBModelRenderer(modelPath, "textures/missing_texture.jpg");
            lastModelPath = modelPath;
            triedLoad = true;
            
            if (modelCache.isLoaded()) {
                DebugRenderer.getInstance().addMessage("GLB loaded: " + modelPath, 3.0f);
                DebugRenderer.getInstance().addMessage("  Vertices: " + modelCache.getVertexCount(), 3.0f);
                float[] bounds = modelCache.getModelBounds();
                if (bounds != null) {
                    System.out.printf("  Model bounds: X[%.3f, %.3f] Y[%.3f, %.3f] Z[%.3f, %.3f]%n",
                        bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
                }
            } else {
                DebugRenderer.getInstance().addError("Failed to load GLB model: " + modelPath, 5.0f);
            }
        }
    }
    
    /**
     * Setup geometry-based collision detection
     */
    private void setupGeometryCollision() {
        if (modelCache == null || !modelCache.isLoaded() || !modelCache.hasMeshData()) {
            DebugRenderer.getInstance().addError("Cannot setup geometry collision - model not loaded or no mesh data", 5.0f);
            return;
        }
        
        // Create geometry collision system
        geometryCollision = new GLBGeometryCollision(getX(), getY(), getZ(), scale);
        
        // Get mesh data from the GLB model
        MeshInfo[] meshes = modelCache.getMeshes();
        float[] vertices = modelCache.getVertices();
        int[] indices = modelCache.getIndices();
        
        if (meshes == null || vertices == null || indices == null) {
            DebugRenderer.getInstance().addError("Missing mesh data for geometry collision detection", 5.0f);
            return;
        }
        
        DebugRenderer.getInstance().addMessage("Setting up geometry collision for " + meshes.length + " meshes...", 3.0f);
        
        // Process each mesh to extract triangle geometry
        for (int i = 0; i < meshes.length; i++) {
            MeshInfo mesh = meshes[i];
            String meshName = modelCache.getMeshName(i);
            
            // Add geometry data for this mesh
            geometryCollision.addGeometryData(vertices, indices, mesh);
        }
        
        // Build overall bounds for quick culling
        geometryCollision.buildOverallBounds();
        
        DebugRenderer.getInstance().addMessage("Geometry collision setup complete: " + geometryCollision.getTriangleCount() + " triangles", 3.0f);
    }
    
    /**
     * Get the geometry collision system for this object
     */
    public GLBGeometryCollision getGeometryCollision() {
        return geometryCollision;
    }
    
    /**
     * Get the model path
     */
    public String getModelPath() {
        return modelPath;
    }
    
    /**
     * Get the texture folder
     */
    public String getTextureFolder() {
        return textureFolder;
    }
    
    /**
     * Get the scale
     */
    public float getScale() {
        return scale;
    }
    
    /**
     * Get rotation values
     */
    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }
    
    /**
     * Set rotation values
     */
    public void setRotation(float x, float y, float z) {
        this.rotationX = x;
        this.rotationY = y;
        this.rotationZ = z;
    }
    
    /**
     * Set scale
     */
    public void setScale(float scale) {
        this.scale = scale;
    }
    
    @Override
    public void update(Window window, float deltaTime) {
        // GLB objects are static, so no update needed
        // Override this method if you need animated GLB objects
    }

    @Override
    public void render() {
        if (modelCache != null && modelCache.isLoaded()) {
            GL11.glPushMatrix();
            
            // Position the object
            GL11.glTranslatef(getX(), getY(), getZ());
            
            // Apply rotation
            GL11.glRotatef(rotationX, 1.0f, 0.0f, 0.0f);
            GL11.glRotatef(rotationY, 0.0f, 1.0f, 0.0f);
            GL11.glRotatef(rotationZ, 0.0f, 0.0f, 1.0f);
            
            // Render the model with proper scaling
            modelCache.render(scale);
            
            GL11.glPopMatrix();
        } else {
            // Fallback rendering if model fails to load
            renderFallbackCube();
        }
    }
    
    /**
     * Fallback rendering if model fails to load
     */
    private void renderFallbackCube() {
        GL11.glPushMatrix();
        GL11.glTranslatef(getX(), getY(), getZ());
        GL11.glColor3f(1.0f, 0.0f, 0.0f); // Red for error
        
        // Draw a simple cube
        GL11.glBegin(GL11.GL_QUADS);
        // Front face
        GL11.glVertex3f(-0.5f, -0.5f, 0.5f);
        GL11.glVertex3f(0.5f, -0.5f, 0.5f);
        GL11.glVertex3f(0.5f, 0.5f, 0.5f);
        GL11.glVertex3f(-0.5f, 0.5f, 0.5f);
        // Back face
        GL11.glVertex3f(-0.5f, -0.5f, -0.5f);
        GL11.glVertex3f(-0.5f, 0.5f, -0.5f);
        GL11.glVertex3f(0.5f, 0.5f, -0.5f);
        GL11.glVertex3f(0.5f, -0.5f, -0.5f);
        // Top face
        GL11.glVertex3f(-0.5f, 0.5f, -0.5f);
        GL11.glVertex3f(-0.5f, 0.5f, 0.5f);
        GL11.glVertex3f(0.5f, 0.5f, 0.5f);
        GL11.glVertex3f(0.5f, 0.5f, -0.5f);
        // Bottom face
        GL11.glVertex3f(-0.5f, -0.5f, -0.5f);
        GL11.glVertex3f(0.5f, -0.5f, -0.5f);
        GL11.glVertex3f(0.5f, -0.5f, 0.5f);
        GL11.glVertex3f(-0.5f, -0.5f, 0.5f);
        // Right face
        GL11.glVertex3f(0.5f, -0.5f, -0.5f);
        GL11.glVertex3f(0.5f, 0.5f, -0.5f);
        GL11.glVertex3f(0.5f, 0.5f, 0.5f);
        GL11.glVertex3f(0.5f, -0.5f, 0.5f);
        // Left face
        GL11.glVertex3f(-0.5f, -0.5f, -0.5f);
        GL11.glVertex3f(-0.5f, -0.5f, 0.5f);
        GL11.glVertex3f(-0.5f, 0.5f, 0.5f);
        GL11.glVertex3f(-0.5f, 0.5f, -0.5f);
        GL11.glEnd();
        
        GL11.glColor3f(1.0f, 1.0f, 1.0f); // Reset color
        GL11.glPopMatrix();
    }
} 