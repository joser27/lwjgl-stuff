package mystuff.game;

import org.lwjgl.opengl.GL11;
import mystuff.utils.TextureLoader;
import java.util.Random;

public class HeightmapTerrain {
    private float[][] heightMap;
    private int width, height;
    private float scale = 1.0f;
    private float heightScale = 10.0f;
    private int textureSize = 64; // Size of terrain texture
    
    // Textures
    private int grassTexture = -1;
    private int dirtTexture = -1;
    private int stoneTexture = -1;
    
    // Terrain generation
    private Random random;
    private float noiseScale = 0.02f;
    
    public HeightmapTerrain(int width, int height) {
        this.width = width;
        this.height = height;
        this.heightMap = new float[width][height];
        this.random = new Random(42); // Fixed seed for consistent terrain
        
        generateTerrain();
        loadTextures();
    }
    
    /**
     * Generate terrain using simple noise
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
     * Load terrain textures
     */
    private void loadTextures() {
        grassTexture = TextureLoader.loadTexture("resources/textures/grass.png");
        dirtTexture = TextureLoader.loadTexture("resources/textures/dirt.png");
        stoneTexture = TextureLoader.loadTexture("resources/textures/stone.png");
        
        if (grassTexture == -1) System.err.println("Failed to load grass texture");
        if (dirtTexture == -1) System.err.println("Failed to load dirt texture");
        if (stoneTexture == -1) System.err.println("Failed to load stone texture");
    }
    
    /**
     * Get height at world coordinates
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
     * Get normal vector at world coordinates (for lighting)
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
     * Render the terrain
     */
    public void render(float playerX, float playerZ, float renderDistance) {
        if (grassTexture == -1) return;
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassTexture);
        
        // Calculate visible terrain bounds
        int startX = Math.max(0, (int) ((playerX - renderDistance) / scale));
        int endX = Math.min(width - 1, (int) ((playerX + renderDistance) / scale));
        int startZ = Math.max(0, (int) ((playerZ - renderDistance) / scale));
        int endZ = Math.min(height - 1, (int) ((playerZ + renderDistance) / scale));
        
        // Render terrain as triangles
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
     * Check if a position is on the ground
     */
    public boolean isOnGround(float x, float y, float z, float tolerance) {
        float groundHeight = getHeightAt(x, z);
        return Math.abs(y - groundHeight) <= tolerance;
    }
    
    /**
     * Get terrain bounds
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
            grassTexture = -1;
        }
        if (dirtTexture != -1) {
            GL11.glDeleteTextures(dirtTexture);
            dirtTexture = -1;
        }
        if (stoneTexture != -1) {
            GL11.glDeleteTextures(stoneTexture);
            stoneTexture = -1;
        }
    }
} 