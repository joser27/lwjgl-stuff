package mystuff.utils;

import mystuff.utils.GLBLoader.GLBModelData;
import mystuff.utils.GLBLoader.GLBMaterial;
import mystuff.utils.GLBLoader.GLBMesh;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class GLBModelRenderer {
    
    private GLBModelData modelData;
    private int[] textureIds;
    
    public GLBModelRenderer(String glbFilePath) {
        this.modelData = GLBLoader.loadGLBModel(glbFilePath);
        if (modelData == null) {
            System.err.println("Failed to load GLB model: " + glbFilePath);
        } else {
            // Load textures from embedded materials
            loadTextures();
        }
    }
    
    private void loadTextures() {
        if (modelData.materials == null || modelData.materials.length == 0) {
            // Initialize empty array to avoid null pointer exceptions
            textureIds = new int[0];
            return;
        }
        
        textureIds = new int[modelData.materials.length];
        
        for (int i = 0; i < modelData.materials.length; i++) {
            GLBMaterial material = modelData.materials[i];
            
            if (material.diffuseTexture != null) {
                // TODO: STB has issues with embedded GLB textures - temporarily disable
                System.out.println("Material " + i + " (" + material.name + ") has texture data (" + 
                                 material.diffuseTexture.length + " bytes) - texture loading disabled");
                textureIds[i] = -1;
            } else {
                textureIds[i] = -1;
            }
        }
    }
    
    private int createTextureFromBytes(byte[] imageData) {
        ByteBuffer buffer = null;
        try {
            // Use STB to decode the image data
            IntBuffer width = IntBuffer.allocate(1);
            IntBuffer height = IntBuffer.allocate(1);
            IntBuffer channels = IntBuffer.allocate(1);
            
            // Create a safe direct buffer for STB
            buffer = org.lwjgl.system.MemoryUtil.memAlloc(imageData.length);
            buffer.put(imageData);
            buffer.flip();
            
            ByteBuffer decodedImage = stbi_load_from_memory(buffer, width, height, channels, 4);
            
            if (decodedImage == null) {
                System.err.println("Failed to decode embedded texture: " + stbi_failure_reason());
                return -1;
            }
            
            int w = width.get(0);
            int h = height.get(0);
            
            // Create OpenGL texture
            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            
            // Set texture parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            
            // Upload texture data
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, decodedImage);
            
            // Generate mipmaps
            glGenerateMipmap(GL_TEXTURE_2D);
            
            // Free STB memory
            stbi_image_free(decodedImage);
            
            glBindTexture(GL_TEXTURE_2D, 0);
            
            return textureId;
            
        } catch (Exception e) {
            System.err.println("Error creating texture from bytes: " + e.getMessage());
            e.printStackTrace();
            return -1;
        } finally {
            // Always free the input buffer
            if (buffer != null) {
                org.lwjgl.system.MemoryUtil.memFree(buffer);
            }
        }
    }
    
    public void render() {
        if (modelData == null) {
            // Render fallback cube if GLB failed to load
            renderFallbackCube();
            return;
        }
        
        // Setup OpenGL rendering states
        setupRenderingStates();
        
        // Setup lighting
        setupLighting();
        
        // Set color to white for proper texture rendering
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Only render front faces to avoid the "mirror" effect
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        
        // Render all meshes
        if (modelData.meshes != null && modelData.meshes.length > 0) {
            renderMeshes();
        } else {
            // If no mesh data, render all vertices as triangles
            renderAllVertices();
        }
        
        // Cleanup OpenGL states
        cleanupRenderingStates();
    }
    
    private void renderMeshes() {
        for (GLBMesh mesh : modelData.meshes) {
            // Set up material/texture for this mesh
            if (mesh.materialIndex >= 0 && mesh.materialIndex < textureIds.length) {
                int textureId = textureIds[mesh.materialIndex];
                if (textureId != -1) {
                    glEnable(GL_TEXTURE_2D);
                    glBindTexture(GL_TEXTURE_2D, textureId);
                }
            }
            
            // Render this mesh's triangles
            glBegin(GL_TRIANGLES);
            for (int i = mesh.startIndex; i < mesh.startIndex + mesh.indexCount; i += 3) {
                if (i + 2 < modelData.indices.length) {
                    renderTriangle(modelData.indices[i], modelData.indices[i + 1], modelData.indices[i + 2]);
                }
            }
            glEnd();
            
            // Disable texture for next mesh
            if (glIsEnabled(GL_TEXTURE_2D)) {
                glDisable(GL_TEXTURE_2D);
            }
        }
    }
    
    private void renderAllVertices() {
        if (modelData.indices == null || modelData.indices.length == 0) {
            return;
        }
        
        glBegin(GL_TRIANGLES);
        for (int i = 0; i < modelData.indices.length; i += 3) {
            if (i + 2 < modelData.indices.length) {
                renderTriangle(modelData.indices[i], modelData.indices[i + 1], modelData.indices[i + 2]);
            }
        }
        glEnd();
    }
    
    private void renderTriangle(int index1, int index2, int index3) {
        // Render first vertex
        if (modelData.texCoords != null && modelData.texCoords.length > index1 * 2 + 1) {
            glTexCoord2f(modelData.texCoords[index1 * 2], 1.0f - modelData.texCoords[index1 * 2 + 1]);
        }
        if (modelData.normals != null && modelData.normals.length > index1 * 3 + 2) {
            glNormal3f(modelData.normals[index1 * 3], modelData.normals[index1 * 3 + 1], modelData.normals[index1 * 3 + 2]);
        }
        if (modelData.vertices != null && modelData.vertices.length > index1 * 3 + 2) {
            glVertex3f(modelData.vertices[index1 * 3], modelData.vertices[index1 * 3 + 1], modelData.vertices[index1 * 3 + 2]);
        }
        
        // Render second vertex
        if (modelData.texCoords != null && modelData.texCoords.length > index2 * 2 + 1) {
            glTexCoord2f(modelData.texCoords[index2 * 2], 1.0f - modelData.texCoords[index2 * 2 + 1]);
        }
        if (modelData.normals != null && modelData.normals.length > index2 * 3 + 2) {
            glNormal3f(modelData.normals[index2 * 3], modelData.normals[index2 * 3 + 1], modelData.normals[index2 * 3 + 2]);
        }
        if (modelData.vertices != null && modelData.vertices.length > index2 * 3 + 2) {
            glVertex3f(modelData.vertices[index2 * 3], modelData.vertices[index2 * 3 + 1], modelData.vertices[index2 * 3 + 2]);
        }
        
        // Render third vertex
        if (modelData.texCoords != null && modelData.texCoords.length > index3 * 2 + 1) {
            glTexCoord2f(modelData.texCoords[index3 * 2], 1.0f - modelData.texCoords[index3 * 2 + 1]);
        }
        if (modelData.normals != null && modelData.normals.length > index3 * 3 + 2) {
            glNormal3f(modelData.normals[index3 * 3], modelData.normals[index3 * 3 + 1], modelData.normals[index3 * 3 + 2]);
        }
        if (modelData.vertices != null && modelData.vertices.length > index3 * 3 + 2) {
            glVertex3f(modelData.vertices[index3 * 3], modelData.vertices[index3 * 3 + 1], modelData.vertices[index3 * 3 + 2]);
        }
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
    
    private void renderFallbackCube() {
        // Render a simple colored cube if GLB loading fails
        glPushMatrix();
        glColor3f(0.8f, 0.6f, 0.2f); // Gold color to indicate GLB model
        
        float size = 10.0f;
        glBegin(GL_QUADS);
        // Front face
        glVertex3f(-size, -size, size);
        glVertex3f(size, -size, size);
        glVertex3f(size, size, size);
        glVertex3f(-size, size, size);
        // Back face
        glVertex3f(-size, -size, -size);
        glVertex3f(-size, size, -size);
        glVertex3f(size, size, -size);
        glVertex3f(size, -size, -size);
        // Top face
        glVertex3f(-size, size, -size);
        glVertex3f(-size, size, size);
        glVertex3f(size, size, size);
        glVertex3f(size, size, -size);
        // Bottom face
        glVertex3f(-size, -size, -size);
        glVertex3f(size, -size, -size);
        glVertex3f(size, -size, size);
        glVertex3f(-size, -size, size);
        // Right face
        glVertex3f(size, -size, -size);
        glVertex3f(size, size, -size);
        glVertex3f(size, size, size);
        glVertex3f(size, -size, size);
        // Left face
        glVertex3f(-size, -size, -size);
        glVertex3f(-size, -size, size);
        glVertex3f(-size, size, size);
        glVertex3f(-size, size, -size);
        glEnd();
        
        glColor3f(1.0f, 1.0f, 1.0f); // Reset color
        glPopMatrix();
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
        // Clean up textures if needed
        if (textureIds != null) {
            for (int textureId : textureIds) {
                if (textureId != -1) {
                    glDeleteTextures(textureId);
                }
            }
        }
    }
} 