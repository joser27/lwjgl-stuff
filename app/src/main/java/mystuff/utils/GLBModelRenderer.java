package mystuff.utils;

import mystuff.utils.GLBLoader.ModelData;
import mystuff.utils.GLBLoader.MaterialInfo;
import mystuff.utils.GLBLoader.MeshInfo;
import static org.lwjgl.opengl.GL11.*;

public class GLBModelRenderer {
    
    private ModelData modelData;
    private int[] textureIds; // Array of texture IDs for each material
    private int fallbackTextureId = -1;
    
    public GLBModelRenderer(String glbFilePath) {
        this.modelData = GLBLoader.loadGLBModel(glbFilePath);
        if (modelData == null) {
            System.err.println("Failed to load GLB model: " + glbFilePath);
        } else {
            // Load textures automatically using smart matching
            loadTexturesFromMaterials();
        }
    }
    
    public GLBModelRenderer(String glbFilePath, String fallbackTexturePath) {
        this.modelData = GLBLoader.loadGLBModel(glbFilePath);
        if (modelData == null) {
            System.err.println("Failed to load GLB model: " + glbFilePath);
        } else {
            // Load fallback texture first
            fallbackTextureId = TextureLoader.loadTexture(fallbackTexturePath);
            if (fallbackTextureId == -1) {
                System.err.println("Failed to load fallback texture: " + fallbackTexturePath);
            }
            
            // Load textures automatically using smart matching
            loadTexturesFromMaterials();
        }
    }
    
    private void loadTexturesFromMaterials() {
        if (modelData.materials == null || modelData.materials.length == 0) {
            System.out.println("No materials found, using fallback texture for entire model");
            textureIds = new int[] { fallbackTextureId };
            return;
        }
        
        System.out.println("Loading textures for " + modelData.materials.length + " materials...");
        
        // Initialize TextureMatcher
        TextureMatcher.initializeTextureCache();
        
        textureIds = new int[modelData.materials.length];
        int successCount = 0;
        int fallbackCount = 0;
        
        for (int i = 0; i < modelData.materials.length; i++) {
            MaterialInfo material = modelData.materials[i];
            
            // Find best matching texture
            String texturePath = TextureMatcher.findBestTexture(material.cleanName);
            
            // Load the texture
            int textureId = TextureLoader.loadTexture(texturePath);
            if (textureId != -1) {
                textureIds[i] = textureId;
                if (!texturePath.equals("textures/missing_texture.jpg")) {
                    successCount++;
                } else {
                    fallbackCount++;
                }
            } else {
                // Use fallback texture if available
                textureIds[i] = fallbackTextureId;
                fallbackCount++;
                System.err.println("  Failed to load texture for material " + i + ": " + material.name);
            }
        }
        
        System.out.println("Texture loading complete:");
        System.out.println("  Successful matches: " + successCount);
        System.out.println("  Fallback textures: " + fallbackCount);
        System.out.println("  Total materials: " + modelData.materials.length);
    }
    
    public void render() {
        if (modelData == null) {
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
        
        // Render by meshes to support multiple textures
        if (modelData.meshes != null && modelData.meshes.length > 0) {
            renderMeshes();
        } else {
            // Fallback: render all triangles with first texture
            renderAllTriangles();
        }
        
        // Cleanup OpenGL states
        cleanupRenderingStates();
    }
    
    private void renderMeshes() {
        for (int meshIndex = 0; meshIndex < modelData.meshes.length; meshIndex++) {
            MeshInfo mesh = modelData.meshes[meshIndex];
            
            // Validate mesh data
            if (mesh.startIndex < 0 || mesh.indexCount <= 0 || 
                mesh.startIndex + mesh.indexCount > modelData.indices.length) {
                System.err.println("Invalid mesh " + meshIndex + ": startIndex=" + mesh.startIndex + 
                                 ", indexCount=" + mesh.indexCount + ", total indices=" + modelData.indices.length);
                continue;
            }
            
            // Get texture for this mesh's material
            int textureId = getTextureForMaterial(mesh.materialIndex);
            
            // Enable texture if available
            if (textureId != -1) {
                glEnable(GL_TEXTURE_2D);
                glBindTexture(GL_TEXTURE_2D, textureId);
            }
            
            // Render this mesh's triangles
            glBegin(GL_TRIANGLES);
            for (int i = mesh.startIndex; i < mesh.startIndex + mesh.indexCount; i += 3) {
                if (i + 2 < modelData.indices.length) {
                    int idx1 = modelData.indices[i];
                    int idx2 = modelData.indices[i + 1];
                    int idx3 = modelData.indices[i + 2];
                    renderTriangle(idx1, idx2, idx3, textureId != -1);
                }
            }
            glEnd();
            
            // Disable texture
            if (textureId != -1) {
                glDisable(GL_TEXTURE_2D);
            }
        }
    }
    
    private void renderAllTriangles() {
        // Enable first texture if available
        int textureId = (textureIds != null && textureIds.length > 0) ? textureIds[0] : -1;
        if (textureId != -1) {
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, textureId);
        }
        
        glBegin(GL_TRIANGLES);
        for (int i = 0; i < modelData.indices.length; i += 3) {
            if (i + 2 < modelData.indices.length) {
                renderTriangle(modelData.indices[i], modelData.indices[i + 1], modelData.indices[i + 2], textureId != -1);
            }
        }
        glEnd();
        
        if (textureId != -1) {
            glDisable(GL_TEXTURE_2D);
        }
    }
    
    private int getTextureForMaterial(int materialIndex) {
        if (textureIds == null || materialIndex < 0 || materialIndex >= textureIds.length) {
            return fallbackTextureId;
        }
        
        int textureId = textureIds[materialIndex];
        return textureId != -1 ? textureId : fallbackTextureId;
    }
    
    private void setupRenderingStates() {
        // Enable depth testing with proper function
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        
        // Enable alpha blending for transparency support
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Enable alpha testing to discard fully transparent pixels
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f); // Discard pixels with alpha < 0.1
        
        // Keep depth writing enabled for proper depth sorting
        glDepthMask(true);
        
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
        
        // Disable alpha blending and testing
        glDisable(GL_BLEND);
        glDisable(GL_ALPHA_TEST);
        
        // Restore OpenGL states
        glDisable(GL_LIGHTING);
        glDisable(GL_LIGHT0);
        glDisable(GL_COLOR_MATERIAL);
        glDisable(GL_POLYGON_OFFSET_FILL);
    }
    
    private void renderTriangle(int index1, int index2, int index3, boolean useTexture) {
        // Render first vertex
        if (useTexture && modelData.texCoords.length > index1 * 2 + 1) {
            glTexCoord2f(modelData.texCoords[index1 * 2], 1.0f - modelData.texCoords[index1 * 2 + 1]);
        }
        if (modelData.normals.length > index1 * 3 + 2) {
            glNormal3f(modelData.normals[index1 * 3], modelData.normals[index1 * 3 + 1], modelData.normals[index1 * 3 + 2]);
        }
        glVertex3f(modelData.vertices[index1 * 3], modelData.vertices[index1 * 3 + 1], modelData.vertices[index1 * 3 + 2]);
        
        // Render second vertex
        if (useTexture && modelData.texCoords.length > index2 * 2 + 1) {
            glTexCoord2f(modelData.texCoords[index2 * 2], 1.0f - modelData.texCoords[index2 * 2 + 1]);
        }
        if (modelData.normals.length > index2 * 3 + 2) {
            glNormal3f(modelData.normals[index2 * 3], modelData.normals[index2 * 3 + 1], modelData.normals[index2 * 3 + 2]);
        }
        glVertex3f(modelData.vertices[index2 * 3], modelData.vertices[index2 * 3 + 1], modelData.vertices[index2 * 3 + 2]);
        
        // Render third vertex
        if (useTexture && modelData.texCoords.length > index3 * 2 + 1) {
            glTexCoord2f(modelData.texCoords[index3 * 2], 1.0f - modelData.texCoords[index3 * 2 + 1]);
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
        // Clean up all textures
        if (textureIds != null) {
            for (int textureId : textureIds) {
                if (textureId != -1) {
                    glDeleteTextures(textureId);
                }
            }
        }
        if (fallbackTextureId != -1) {
            glDeleteTextures(fallbackTextureId);
        }
    }
} 