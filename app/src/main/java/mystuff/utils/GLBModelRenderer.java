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
    private int whiteTextureId = -1;
    
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
        String fallbackTexture = "textures/missing_texture.jpg";
        int fallbackTextureId = TextureLoader.loadTexture(fallbackTexture);
        if (fallbackTextureId == -1) {
            System.err.println("Failed to load fallback texture: " + fallbackTexture);
        } else {
            System.out.println("Fallback texture loaded successfully with ID: " + fallbackTextureId);
        }
        
        // List all materials that need textures (only first 10 to avoid spam)
        System.out.println("=== Materials needing textures (showing first 10) ===");
        for (int i = 0; i < Math.min(10, modelData.materials.length); i++) {
            GLBMaterial material = modelData.materials[i];
            System.out.println("Material " + i + ": " + material.name + " (looking for: " + material.name.toLowerCase() + ".png/.jpg)");
        }
        if (modelData.materials.length > 10) {
            System.out.println("... and " + (modelData.materials.length - 10) + " more materials");
        }
        System.out.println("==================================================");
        
        int loadedCount = 0;
        int fallbackCount = 0;
        
        for (int i = 0; i < modelData.materials.length; i++) {
            GLBMaterial material = modelData.materials[i];
            int textureId = -1;
            
            // Skip embedded textures - only use external textures
            String texturePath = "textures/house/" + material.name.toLowerCase() + ".png";
            textureId = TextureLoader.loadTexture(texturePath);
            if (textureId == -1) {
                // Try .jpg extension
                texturePath = "textures/house/" + material.name.toLowerCase() + ".jpg";
                textureId = TextureLoader.loadTexture(texturePath);
            }
            if (textureId != -1) {
                loadedCount++;
                if (i < 10) { // Only log first 10 successful loads
                    System.out.println("Material " + i + " (" + material.name + ") loaded external texture: " + texturePath + " (ID: " + textureId + ")");
                }
            } else {
                if (i < 10) { // Only log first 10 missing textures
                    System.out.println("Material " + i + " (" + material.name + ") - no external texture found, using fallback");
                }
            }
            
            // Always use fallback if no texture found
            if (textureId == -1) {
                if (fallbackTextureId != -1) {
                    textureId = fallbackTextureId;
                    fallbackCount++;
                    if (i < 10) { // Only log first 10 fallback uses
                        System.out.println("  -> Using fallback texture (ID: " + fallbackTextureId + ") for material " + i);
                    }
                } else {
                    System.err.println("  -> No fallback texture available for material " + i);
                }
            }
            textureIds[i] = textureId;
        }
        
        // Summary of texture loading
        System.out.println("=== Texture Loading Summary ===");
        System.out.println("Total materials: " + textureIds.length);
        System.out.println("External textures loaded: " + loadedCount);
        System.out.println("Using fallback texture: " + fallbackCount);
        System.out.println("===============================");
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
    
    private void createWhiteTexture() {
        if (whiteTextureId != -1) return;
        // Create a 1x1 white pixel
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(4);
        buffer.put((byte)255).put((byte)255).put((byte)255).put((byte)255);
        buffer.flip();
        whiteTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, whiteTextureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glBindTexture(GL_TEXTURE_2D, 0);
        System.out.println("Created 1x1 white texture for debug mode, ID: " + whiteTextureId);
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
        createWhiteTexture();
        for (int meshIndex = 0; meshIndex < modelData.meshes.length; meshIndex++) {
            GLBMesh mesh = modelData.meshes[meshIndex];
            
            // Validate mesh indices
            if (mesh.startIndex < 0 || mesh.indexCount <= 0 || 
                mesh.startIndex + mesh.indexCount > modelData.indices.length) {
                System.err.println("Invalid mesh " + meshIndex + ": startIndex=" + mesh.startIndex + 
                                 ", indexCount=" + mesh.indexCount + ", total indices=" + modelData.indices.length);
                continue;
            }
            
            // Force use of white texture for all meshes
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, whiteTextureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            
            // Render this mesh's triangles
            glBegin(GL_TRIANGLES);
            for (int i = mesh.startIndex; i < mesh.startIndex + mesh.indexCount; i += 3) {
                if (i + 2 < modelData.indices.length) {
                    int idx1 = modelData.indices[i];
                    int idx2 = modelData.indices[i + 1];
                    int idx3 = modelData.indices[i + 2];
                    renderTriangle(idx1, idx2, idx3);
                }
            }
            glEnd();
            
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
        // Validate indices first
        if (index1 < 0 || index2 < 0 || index3 < 0 || 
            modelData.vertices == null || modelData.vertices.length == 0) {
            return;
        }
        
        int maxVertexIndex = (modelData.vertices.length / 3) - 1;
        if (index1 > maxVertexIndex || index2 > maxVertexIndex || index3 > maxVertexIndex) {
            return;
        }
        
        // Render first vertex
        if (modelData.texCoords != null && modelData.texCoords.length > index1 * 2 + 1) {
            // Flip V coordinate for OpenGL
            glTexCoord2f(modelData.texCoords[index1 * 2], 1.0f - modelData.texCoords[index1 * 2 + 1]);
        }
        if (modelData.normals != null && modelData.normals.length > index1 * 3 + 2) {
            glNormal3f(modelData.normals[index1 * 3], modelData.normals[index1 * 3 + 1], modelData.normals[index1 * 3 + 2]);
        }
        glVertex3f(modelData.vertices[index1 * 3], modelData.vertices[index1 * 3 + 1], modelData.vertices[index1 * 3 + 2]);
        
        // Render second vertex
        if (modelData.texCoords != null && modelData.texCoords.length > index2 * 2 + 1) {
            // Flip V coordinate for OpenGL
            glTexCoord2f(modelData.texCoords[index2 * 2], 1.0f - modelData.texCoords[index2 * 2 + 1]);
        }
        if (modelData.normals != null && modelData.normals.length > index2 * 3 + 2) {
            glNormal3f(modelData.normals[index2 * 3], modelData.normals[index2 * 3 + 1], modelData.normals[index2 * 3 + 2]);
        }
        glVertex3f(modelData.vertices[index2 * 3], modelData.vertices[index2 * 3 + 1], modelData.vertices[index2 * 3 + 2]);
        
        // Render third vertex
        if (modelData.texCoords != null && modelData.texCoords.length > index3 * 2 + 1) {
            // Flip V coordinate for OpenGL
            glTexCoord2f(modelData.texCoords[index3 * 2], 1.0f - modelData.texCoords[index3 * 2 + 1]);
        }
        if (modelData.normals != null && modelData.normals.length > index3 * 3 + 2) {
            glNormal3f(modelData.normals[index3 * 3], modelData.normals[index3 * 3 + 1], modelData.normals[index3 * 3 + 2]);
        }
        glVertex3f(modelData.vertices[index3 * 3], modelData.vertices[index3 * 3 + 1], modelData.vertices[index3 * 3 + 2]);
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