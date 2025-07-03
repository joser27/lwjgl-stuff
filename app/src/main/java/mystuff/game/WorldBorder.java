package mystuff.game;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class WorldBorder {
    private int vaoId;
    private int vboId;
    private int eboId;
    private int vertexCount;
    
    // Border properties
    private float minX, maxX, minZ, maxZ;
    private float height = 50.0f; // Border wall height
    private float thickness = 2.0f; // Border wall thickness
    
    // Border color (light gray with transparency)
    private float[] borderColor = {0.8f, 0.8f, 0.8f, 0.3f}; // Light gray, 30% opacity
    
    public WorldBorder(float minX, float maxX, float minZ, float maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        init();
    }
    
    private void init() {
        // Create vertices for 4 walls around the border
        float[] vertices = createBorderVertices();
        int[] indices = createBorderIndices();
        
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);
        
        // Create VBO
        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        
        FloatBuffer verticesBuffer = MemoryUtil.memAllocFloat(vertices.length);
        verticesBuffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(verticesBuffer);
        
        // Position attribute
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        
        // Color attribute
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        
        // Create EBO
        eboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        
        IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indices.length);
        indicesBuffer.put(indices).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(indicesBuffer);
        
        vertexCount = indices.length;
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }
    
    private float[] createBorderVertices() {
        // Create 4 walls: North, South, East, West
        // Each vertex has: x, y, z, r, g, b
        return new float[] {
            // North wall (facing positive Z)
            minX, 0, maxZ, borderColor[0], borderColor[1], borderColor[2],
            maxX, 0, maxZ, borderColor[0], borderColor[1], borderColor[2],
            maxX, height, maxZ, borderColor[0], borderColor[1], borderColor[2],
            minX, height, maxZ, borderColor[0], borderColor[1], borderColor[2],
            
            // South wall (facing negative Z)
            minX, 0, minZ, borderColor[0], borderColor[1], borderColor[2],
            maxX, 0, minZ, borderColor[0], borderColor[1], borderColor[2],
            maxX, height, minZ, borderColor[0], borderColor[1], borderColor[2],
            minX, height, minZ, borderColor[0], borderColor[1], borderColor[2],
            
            // East wall (facing positive X)
            maxX, 0, minZ, borderColor[0], borderColor[1], borderColor[2],
            maxX, 0, maxZ, borderColor[0], borderColor[1], borderColor[2],
            maxX, height, maxZ, borderColor[0], borderColor[1], borderColor[2],
            maxX, height, minZ, borderColor[0], borderColor[1], borderColor[2],
            
            // West wall (facing negative X)
            minX, 0, minZ, borderColor[0], borderColor[1], borderColor[2],
            minX, 0, maxZ, borderColor[0], borderColor[1], borderColor[2],
            minX, height, maxZ, borderColor[0], borderColor[1], borderColor[2],
            minX, height, minZ, borderColor[0], borderColor[1], borderColor[2]
        };
    }
    
    private int[] createBorderIndices() {
        // Create indices for 4 walls (each wall is 2 triangles)
        return new int[] {
            // North wall
            0, 1, 2, 2, 3, 0,
            // South wall
            4, 5, 6, 6, 7, 4,
            // East wall
            8, 9, 10, 10, 11, 8,
            // West wall
            12, 13, 14, 14, 15, 12
        };
    }
    
    public void render() {
        // Enable blending for transparency
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Disable depth writing for transparent objects
        GL11.glDepthMask(false);
        
        // Set alpha value for transparency
        GL11.glColor4f(1.0f, 1.0f, 1.0f, borderColor[3]);
        
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
        
        // Restore depth writing
        GL11.glDepthMask(true);
        
        // Restore color
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        GL11.glDisable(GL11.GL_BLEND);
    }
    
    public void cleanup() {
        GL30.glDeleteVertexArrays(vaoId);
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(eboId);
    }
    
    public void setBorderColor(float r, float g, float b, float a) {
        borderColor[0] = r;
        borderColor[1] = g;
        borderColor[2] = b;
        borderColor[3] = a;
    }
    
    public void setHeight(float height) {
        this.height = height;
    }
} 