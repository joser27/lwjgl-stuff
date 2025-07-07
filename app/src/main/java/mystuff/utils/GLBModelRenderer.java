package mystuff.utils;

import mystuff.utils.GLBLoader.ModelData;
import static org.lwjgl.opengl.GL11.*;

public class GLBModelRenderer {
    
    private ModelData modelData;
    private int textureId = -1;
    
    public GLBModelRenderer(String glbFilePath) {
        this.modelData = GLBLoader.loadGLBModel(glbFilePath);
        if (modelData == null) {
            System.err.println("Failed to load GLB model: " + glbFilePath);
        }
    }
    
    public GLBModelRenderer(String glbFilePath, String texturePath) {
        this.modelData = GLBLoader.loadGLBModel(glbFilePath);
        if (modelData == null) {
            System.err.println("Failed to load GLB model: " + glbFilePath);
        } else {
            // Load texture if provided
            textureId = TextureLoader.loadTexture(texturePath);
            if (textureId == -1) {
                System.err.println("Failed to load texture: " + texturePath);
            }
        }
    }
    
    public void render() {
        if (modelData == null) {
            return;
        }
        
        // Setup OpenGL rendering states
        setupRenderingStates();
        
        // Setup lighting
        setupLighting();
        
        // Enable texturing if available
        if (textureId != -1) {
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, textureId);
        }
        
        // Set color to white for proper texture rendering
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Only render front faces to avoid the "mirror" effect
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        
        glBegin(GL_TRIANGLES);
        for (int i = 0; i < modelData.indices.length; i += 3) {
            renderTriangle(modelData.indices[i], modelData.indices[i + 1], modelData.indices[i + 2]);
        }
        glEnd();
        
        // Cleanup OpenGL states
        cleanupRenderingStates();
    }
    
    private void setupRenderingStates() {
        // Enable depth testing with proper function
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        
        // Enable face culling with proper winding
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        
        // Use smooth shading for better appearance
        glShadeModel(GL_SMOOTH);
        
        // Enable polygon offset to prevent z-fighting
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.0f, 1.0f);
    }
    
    private void setupLighting() {
        // Enable lighting
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);
        glEnable(GL_COLOR_MATERIAL);
        
        // Configure light properties
        float[] lightPosition = {100.0f, 100.0f, 100.0f, 1.0f};
        float[] lightAmbient = {0.3f, 0.3f, 0.3f, 1.0f};
        float[] lightDiffuse = {0.8f, 0.8f, 0.8f, 1.0f};
        float[] lightSpecular = {0.5f, 0.5f, 0.5f, 1.0f};
        
        glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);
        glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
        glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
        glLightfv(GL_LIGHT0, GL_SPECULAR, lightSpecular);
    }
    
    private void cleanupRenderingStates() {
        // Disable texturing
        glDisable(GL_TEXTURE_2D);
        
        // Restore OpenGL states
        glDisable(GL_LIGHTING);
        glDisable(GL_LIGHT0);
        glDisable(GL_COLOR_MATERIAL);
        glDisable(GL_POLYGON_OFFSET_FILL);
    }
    
    private void renderTriangle(int index1, int index2, int index3) {
        // Render first vertex
        if (textureId != -1) {
            if (modelData.texCoords.length > index1 * 2 + 1) {
                glTexCoord2f(modelData.texCoords[index1 * 2], 1.0f - modelData.texCoords[index1 * 2 + 1]);
            } else {
                glTexCoord2f(0.0f, 0.0f);
            }
        }
        if (modelData.normals.length > index1 * 3 + 2) {
            glNormal3f(modelData.normals[index1 * 3], modelData.normals[index1 * 3 + 1], modelData.normals[index1 * 3 + 2]);
        }
        glVertex3f(modelData.vertices[index1 * 3], modelData.vertices[index1 * 3 + 1], modelData.vertices[index1 * 3 + 2]);
        
        // Render second vertex
        if (textureId != -1) {
            if (modelData.texCoords.length > index2 * 2 + 1) {
                glTexCoord2f(modelData.texCoords[index2 * 2], 1.0f - modelData.texCoords[index2 * 2 + 1]);
            } else {
                glTexCoord2f(1.0f, 0.0f);
            }
        }
        if (modelData.normals.length > index2 * 3 + 2) {
            glNormal3f(modelData.normals[index2 * 3], modelData.normals[index2 * 3 + 1], modelData.normals[index2 * 3 + 2]);
        }
        glVertex3f(modelData.vertices[index2 * 3], modelData.vertices[index2 * 3 + 1], modelData.vertices[index2 * 3 + 2]);
        
        // Render third vertex
        if (textureId != -1) {
            if (modelData.texCoords.length > index3 * 2 + 1) {
                glTexCoord2f(modelData.texCoords[index3 * 2], 1.0f - modelData.texCoords[index3 * 2 + 1]);
            } else {
                glTexCoord2f(0.5f, 1.0f);
            }
        }
        if (modelData.normals.length > index3 * 3 + 2) {
            glNormal3f(modelData.normals[index3 * 3], modelData.normals[index3 * 3 + 1], modelData.normals[index3 * 3 + 2]);
        }
        glVertex3f(modelData.vertices[index3 * 3], modelData.vertices[index3 * 3 + 1], modelData.vertices[index3 * 3 + 2]);
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