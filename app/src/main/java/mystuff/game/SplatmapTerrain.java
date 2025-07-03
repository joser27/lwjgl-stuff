package mystuff.game;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import mystuff.utils.TextureLoader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

public class SplatmapTerrain {
    private float[][] heightMap;
    private int width, height;
    private float scale = 1.0f;
    private float heightScale = 10.0f;
    
    // Textures
    private int grassTexture = -1;
    private int dirtTexture = -1;
    private int stoneTexture = -1;
    private int splatmapTexture = -1;
    
    // Terrain generation
    private Random random;
    private float noiseScale = 0.02f;
    
    // OpenGL objects for rendering
    private int vaoId;
    private int vboId;
    private int eboId;
    private int vertexCount;
    
    // Shader program for splatmap blending
    private int shaderProgram;
    
    public SplatmapTerrain(int width, int height) {
        this.width = width;
        this.height = height;
        this.heightMap = new float[width][height];
        this.random = new Random(42); // Fixed seed for consistent terrain
        
        generateTerrain();
        loadTextures();
        createShader();
        createMesh();
    }
    
    /**
     * Generate terrain using simple noise (same as before)
     */
    private void generateTerrain() {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                // Simple noise generation
                float noise = generateNoise(x * noiseScale, z * noiseScale);
                
                // Add some variation
                float variation = (float) Math.sin(x * 0.1f) * (float) Math.cos(z * 0.1f) * 0.5f;
                
                // Base height + noise + variation
                heightMap[x][z] = 10.0f + noise * heightScale + variation;
            }
        }
    }
    
    /**
     * Simple noise generation
     */
    private float generateNoise(float x, float z) {
        // Simple 2D noise using sine waves
        float noise = 0;
        float amplitude = 1.0f;
        float frequency = 1.0f;
        
        for (int i = 0; i < 4; i++) {
            noise += amplitude * (float) Math.sin(x * frequency) * (float) Math.cos(z * frequency);
            amplitude *= 0.5f;
            frequency *= 2.0f;
        }
        
        return noise * 0.5f + 0.5f; // Normalize to 0-1
    }
    
    /**
     * Load terrain textures including splatmap
     */
    private void loadTextures() {
        System.out.println("Loading terrain textures...");
        
        // Load individual textures
        grassTexture = TextureLoader.loadTexture("resources/textures/grass.jpg");
        dirtTexture = TextureLoader.loadTexture("resources/textures/dirt.jpg");
        stoneTexture = TextureLoader.loadTexture("resources/textures/stone.jpg");
        splatmapTexture = TextureLoader.loadTexture("resources/textures/splatmap.png");
        
        System.out.println("Texture loading results:");
        System.out.println("  Grass texture ID: " + grassTexture);
        System.out.println("  Dirt texture ID: " + dirtTexture);
        System.out.println("  Stone texture ID: " + stoneTexture);
        System.out.println("  Splatmap texture ID: " + splatmapTexture);
        
        if (grassTexture == -1) System.err.println("Failed to load grass texture");
        if (dirtTexture == -1) System.err.println("Failed to load dirt texture");
        if (stoneTexture == -1) System.err.println("Failed to load stone texture");
        if (splatmapTexture == -1) System.err.println("Failed to load splatmap texture");
        
        // Set texture parameters for better quality
        setupTexture(grassTexture, "grass");
        setupTexture(dirtTexture, "dirt");
        setupTexture(stoneTexture, "stone");
        setupTexture(splatmapTexture, "splatmap");
    }
    
    /**
     * Setup texture parameters
     */
    private void setupTexture(int textureId, String name) {
        if (textureId != -1) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            System.out.println("Loaded " + name + " texture with ID: " + textureId);
        }
    }
    
    /**
     * Create simple shader for splatmap blending
     */
    private void createShader() {
        // For now, we'll use the existing OpenGL immediate mode
        // In a full implementation, you'd create vertex/fragment shaders here
        shaderProgram = 0; // Placeholder
    }
    
    /**
     * Create mesh data for terrain
     */
    private void createMesh() {
        // For now, we'll use immediate mode rendering
        // In a full implementation, you'd create VBOs/VAOs here
        vaoId = 0; // Placeholder
    }
    
    /**
     * Get height at world coordinates (same as before)
     */
    public float getHeightAt(float worldX, float worldZ) {
        int x = (int) (worldX / scale);
        int z = (int) (worldZ / scale);
        
        if (x < 0 || x >= width - 1 || z < 0 || z >= height - 1) {
            return 10.0f; // Default height outside terrain
        }
        
        // Bilinear interpolation for smooth height
        float fx = (worldX / scale) - x;
        float fz = (worldZ / scale) - z;
        
        float h00 = heightMap[x][z];
        float h10 = heightMap[x + 1][z];
        float h01 = heightMap[x][z + 1];
        float h11 = heightMap[x + 1][z + 1];
        
        float h0 = h00 * (1 - fx) + h10 * fx;
        float h1 = h01 * (1 - fx) + h11 * fx;
        
        return h0 * (1 - fz) + h1 * fz;
    }
    
    /**
     * Get normal vector at world coordinates (same as before)
     */
    public float[] getNormalAt(float worldX, float worldZ) {
        float delta = 1.0f;
        
        float h1 = getHeightAt(worldX - delta, worldZ);
        float h2 = getHeightAt(worldX + delta, worldZ);
        float h3 = getHeightAt(worldX, worldZ - delta);
        float h4 = getHeightAt(worldX, worldZ + delta);
        
        float dx = (h2 - h1) / (2 * delta);
        float dz = (h4 - h3) / (2 * delta);
        
        // Normal vector: (-dx, 1, -dz) normalized
        float length = (float) Math.sqrt(dx * dx + 1 + dz * dz);
        return new float[]{-dx / length, 1.0f / length, -dz / length};
    }
    
    /**
     * Render the terrain with splatmap blending
     */
    public void render(float playerX, float playerZ, float renderDistance) {
        if (grassTexture == -1 || dirtTexture == -1 || stoneTexture == -1 || splatmapTexture == -1) {
            System.err.println("Some textures failed to load, using fallback rendering");
            renderFallback(playerX, playerZ, renderDistance);
            return;
        }
        
        // Enable texture units for multi-texturing
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        
        // Bind textures to different texture units
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassTexture);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirtTexture);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, stoneTexture);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, splatmapTexture);
        
        // Calculate visible terrain bounds
        int startX = Math.max(0, (int) ((playerX - renderDistance) / scale));
        int endX = Math.min(width - 1, (int) ((playerX + renderDistance) / scale));
        int startZ = Math.max(0, (int) ((playerZ - renderDistance) / scale));
        int endZ = Math.min(height - 1, (int) ((playerZ + renderDistance) / scale));
        
        // Render terrain with splatmap blending
        renderTerrainWithSplatmap(startX, endX, startZ, endZ);
        
        // Reset texture unit
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
    
    /**
     * Render terrain using splatmap for texture blending
     */
    private void renderTerrainWithSplatmap(int startX, int endX, int startZ, int endZ) {

        // For now, just use grass texture to see actual terrain
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassTexture);
        
        // Explicitly set color to white to avoid tinting other objects
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        
        // Set texture environment to modulate mode (default) for proper texture rendering
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        
        // Set texture filtering to ensure proper sampling
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

        // Remove forced color to see actual texture
        // GL11.glColor3f(1, 0, 0); // Commented out to see splatmap

        GL11.glBegin(GL11.GL_TRIANGLES);

        for (int x = startX; x < endX; x++) {
            for (int z = startZ; z < endZ; z++) {
                float x1 = x * scale;
                float z1 = z * scale;
                float x2 = (x + 1) * scale;
                float z2 = (z + 1) * scale;

                float y1 = heightMap[x][z];
                float y2 = heightMap[x + 1][z];
                float y3 = heightMap[x][z + 1];
                float y4 = heightMap[x + 1][z + 1];

                // Texture coordinates for grass texture
                float texScale = 10.0f; // Repeat grass texture 10 times
                float u = (float) x / (width - 1) * texScale;
                float v = (float) z / (height - 1) * texScale;
                float u2 = (float) (x + 1) / (width - 1) * texScale;
                float v2 = (float) (z + 1) / (height - 1) * texScale;



                // First triangle
                GL11.glTexCoord2f(u, v);
                GL11.glVertex3f(x1, y1, z1);
                GL11.glTexCoord2f(u2, v);
                GL11.glVertex3f(x2, y2, z1);
                GL11.glTexCoord2f(u, v2);
                GL11.glVertex3f(x1, y3, z2);

                // Second triangle
                GL11.glTexCoord2f(u2, v);
                GL11.glVertex3f(x2, y2, z1);
                GL11.glTexCoord2f(u2, v2);
                GL11.glVertex3f(x2, y4, z2);
                GL11.glTexCoord2f(u, v2);
                GL11.glVertex3f(x1, y3, z2);
            }
        }

        GL11.glEnd();
        
        // Disable texturing and reset color to white for other objects
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor3f(1, 1, 1);
    }
    
    /**
     * Select texture based on height (fallback method)
     */
    private int selectTextureByHeight(float height) {
        if (height < 12.0f) {
            return grassTexture;
        } else if (height < 15.0f) {
            return dirtTexture;
        } else {
            return stoneTexture;
        }
    }
    
    /**
     * Debug method to print texture selection
     */
    private void debugTextureSelection(float height, int textureId) {
        String textureName = "unknown";
        if (textureId == grassTexture) textureName = "grass";
        else if (textureId == dirtTexture) textureName = "dirt";
        else if (textureId == stoneTexture) textureName = "stone";
        
        System.out.printf("Height: %.2f -> Texture: %s (ID: %d)%n", height, textureName, textureId);
    }
    
    /**
     * Fallback rendering if textures fail to load
     */
    private void renderFallback(float playerX, float playerZ, float renderDistance) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (grassTexture != -1) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassTexture);
        }
        
        // Simple rendering without splatmap
        int startX = Math.max(0, (int) ((playerX - renderDistance) / scale));
        int endX = Math.min(width - 1, (int) ((playerX + renderDistance) / scale));
        int startZ = Math.max(0, (int) ((playerZ - renderDistance) / scale));
        int endZ = Math.min(height - 1, (int) ((playerZ + renderDistance) / scale));
        
        GL11.glBegin(GL11.GL_TRIANGLES);
        
        for (int x = startX; x < endX; x++) {
            for (int z = startZ; z < endZ; z++) {
                float x1 = x * scale;
                float z1 = z * scale;
                float x2 = (x + 1) * scale;
                float z2 = (z + 1) * scale;
                
                float y1 = heightMap[x][z];
                float y2 = heightMap[x + 1][z];
                float y3 = heightMap[x][z + 1];
                float y4 = heightMap[x + 1][z + 1];
                
                // First triangle
                GL11.glTexCoord2f(0, 0);
                GL11.glVertex3f(x1, y1, z1);
                GL11.glTexCoord2f(1, 0);
                GL11.glVertex3f(x2, y2, z1);
                GL11.glTexCoord2f(0, 1);
                GL11.glVertex3f(x1, y3, z2);
                
                // Second triangle
                GL11.glTexCoord2f(1, 0);
                GL11.glVertex3f(x2, y2, z1);
                GL11.glTexCoord2f(1, 1);
                GL11.glVertex3f(x2, y4, z2);
                GL11.glTexCoord2f(0, 1);
                GL11.glVertex3f(x1, y3, z2);
            }
        }
        
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
    
    /**
     * Check if a position is on the ground (same as before)
     */
    public boolean isOnGround(float x, float y, float z, float tolerance) {
        float groundHeight = getHeightAt(x, z);
        return Math.abs(y - groundHeight) <= tolerance;
    }
    
    /**
     * Get terrain bounds (same as before)
     */
    public float getMinX() { return 0; }
    public float getMaxX() { return width * scale; }
    public float getMinZ() { return 0; }
    public float getMaxZ() { return height * scale; }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (grassTexture != -1) {
            GL11.glDeleteTextures(grassTexture);
        }
        if (dirtTexture != -1) {
            GL11.glDeleteTextures(dirtTexture);
        }
        if (stoneTexture != -1) {
            GL11.glDeleteTextures(stoneTexture);
        }
        if (splatmapTexture != -1) {
            GL11.glDeleteTextures(splatmapTexture);
        }
        
        if (vaoId != 0) {
            GL30.glDeleteVertexArrays(vaoId);
        }
        if (vboId != 0) {
            GL15.glDeleteBuffers(vboId);
        }
        if (eboId != 0) {
            GL15.glDeleteBuffers(eboId);
        }
    }
} 