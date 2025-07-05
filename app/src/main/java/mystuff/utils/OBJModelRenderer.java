package mystuff.utils;

import static org.lwjgl.opengl.GL11.*;

public class OBJModelRenderer {
    
    private OBJLoader.ModelData modelData;
    private int textureId = -1;
    
    public OBJModelRenderer(String objFilePath) {
        this.modelData = OBJLoader.loadOBJModel(objFilePath);
        if (modelData == null) {
            System.err.println("Failed to load OBJ model: " + objFilePath);
        }
    }
    
    public OBJModelRenderer(String objFilePath, String textureFilePath) {
        this.modelData = OBJLoader.loadOBJModel(objFilePath);
        if (modelData == null) {
            System.err.println("Failed to load OBJ model: " + objFilePath);
        } else {
            this.textureId = TextureLoader.loadTexture(textureFilePath);
            if (textureId == -1) {
                System.err.println("Failed to load texture: " + textureFilePath);
            }
        }
    }
    
    public void render() {
        if (modelData == null) {
            return;
        }
        
        // Enable texturing if we have a texture
        if (textureId != -1) {
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, textureId);
        }
        
        // Set color to white for proper texture rendering
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Render the model using immediate mode
        glBegin(GL_TRIANGLES);
        
        for (int i = 0; i < modelData.indices.length; i += 3) {
            int index1 = modelData.indices[i];
            int index2 = modelData.indices[i + 1];
            int index3 = modelData.indices[i + 2];
            
            // First vertex
            if (textureId != -1 && modelData.texCoords.length > index1 * 2 + 1) {
                glTexCoord2f(modelData.texCoords[index1 * 2], modelData.texCoords[index1 * 2 + 1]);
            }
            if (modelData.normals.length > index1 * 3 + 2) {
                glNormal3f(modelData.normals[index1 * 3], modelData.normals[index1 * 3 + 1], modelData.normals[index1 * 3 + 2]);
            }
            glVertex3f(modelData.vertices[index1 * 3], modelData.vertices[index1 * 3 + 1], modelData.vertices[index1 * 3 + 2]);
            
            // Second vertex
            if (textureId != -1 && modelData.texCoords.length > index2 * 2 + 1) {
                glTexCoord2f(modelData.texCoords[index2 * 2], modelData.texCoords[index2 * 2 + 1]);
            }
            if (modelData.normals.length > index2 * 3 + 2) {
                glNormal3f(modelData.normals[index2 * 3], modelData.normals[index2 * 3 + 1], modelData.normals[index2 * 3 + 2]);
            }
            glVertex3f(modelData.vertices[index2 * 3], modelData.vertices[index2 * 3 + 1], modelData.vertices[index2 * 3 + 2]);
            
            // Third vertex
            if (textureId != -1 && modelData.texCoords.length > index3 * 2 + 1) {
                glTexCoord2f(modelData.texCoords[index3 * 2], modelData.texCoords[index3 * 2 + 1]);
            }
            if (modelData.normals.length > index3 * 3 + 2) {
                glNormal3f(modelData.normals[index3 * 3], modelData.normals[index3 * 3 + 1], modelData.normals[index3 * 3 + 2]);
            }
            glVertex3f(modelData.vertices[index3 * 3], modelData.vertices[index3 * 3 + 1], modelData.vertices[index3 * 3 + 2]);
        }
        
        glEnd();
        
        // Disable texturing
        if (textureId != -1) {
            glDisable(GL_TEXTURE_2D);
        }
    }
    
    public void render(float scale) {
        glPushMatrix();
        glScalef(scale, scale, scale);
        render();
        glPopMatrix();
    }
    
    public void render(float scaleX, float scaleY, float scaleZ) {
        glPushMatrix();
        glScalef(scaleX, scaleY, scaleZ);
        render();
        glPopMatrix();
    }
    
    public boolean isLoaded() {
        return modelData != null;
    }
    
    public int getVertexCount() {
        return modelData != null ? modelData.vertexCount : 0;
    }
    
    public float[] getModelBounds() {
        if (modelData == null || modelData.vertices == null || modelData.vertices.length == 0) {
            return null;
        }
        
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;
        
        for (int i = 0; i < modelData.vertices.length; i += 3) {
            float x = modelData.vertices[i];
            float y = modelData.vertices[i + 1];
            float z = modelData.vertices[i + 2];
            
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        
        return new float[]{minX, maxX, minY, maxY, minZ, maxZ};
    }
    
    public void cleanup() {
        // Clean up texture if needed
        if (textureId != -1) {
            glDeleteTextures(textureId);
        }
    }
} 