package mystuff.utils;

import mystuff.utils.GLBLoader.ModelData;
import mystuff.utils.GLBLoader.MaterialInfo;
import mystuff.utils.GLBLoader.MeshInfo;
import static org.lwjgl.opengl.GL11.*;

public class GLBModelRenderer {
    
    private ModelData modelData;
    private int[] textureIds; // Array of texture IDs for each material
    private TextureMatcher.MaterialInfo[] materialInfos; 
    private int fallbackTextureId = -1;
    
    public GLBModelRenderer(String glbFilePath) {
        this.modelData = GLBLoader.loadGLBModel(glbFilePath);
        if (modelData == null) {
            System.err.println("Failed to load GLB model: " + glbFilePath);
        } else {
            // Load materials with enhanced type detection
            loadMaterialsFromModel();
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
            
            // Load materials with enhanced type detection
            loadMaterialsFromModel();
        }
    }
    
    private void loadMaterialsFromModel() {
        if (modelData.materials == null || modelData.materials.length == 0) {
            System.out.println("No materials found, using fallback material for entire model");
            textureIds = new int[] { fallbackTextureId };
            materialInfos = new TextureMatcher.MaterialInfo[] { 
                new TextureMatcher.MaterialInfo("textures/missing_texture.jpg") 
            };
            return;
        }
        
        System.out.println("Loading materials for " + modelData.materials.length + " materials...");
        
        // Initialize TextureMatcher
        TextureMatcher.initializeTextureCache();
        
        textureIds = new int[modelData.materials.length];
        materialInfos = new TextureMatcher.MaterialInfo[modelData.materials.length];
        int successCount = 0;
        int fallbackCount = 0;
        int specialCount = 0;
        
        for (int i = 0; i < modelData.materials.length; i++) {
            MaterialInfo material = modelData.materials[i];
            
            // Get enhanced material information
            TextureMatcher.MaterialInfo matInfo = TextureMatcher.getMaterialInfo(material.cleanName);
            materialInfos[i] = matInfo;
            
            // Load texture if material uses one
            if (matInfo.texturePath != null) {
                int textureId = TextureLoader.loadTexture(matInfo.texturePath);
                if (textureId != -1) {
                    textureIds[i] = textureId;
                    if (!matInfo.texturePath.equals("textures/missing_texture.jpg")) {
                        successCount++;
                    } else {
                        fallbackCount++;
                    }
                } else {
                    textureIds[i] = fallbackTextureId;
                    fallbackCount++;
                    System.err.println("  Failed to load texture for material " + i + ": " + material.name);
                }
            } else {
                // Non-textured material (color, glass, etc.)
                textureIds[i] = -1;
                specialCount++;
            }
            
            // Log material type for debugging
            System.out.println("  Material " + i + ": \"" + material.name + "\" -> " + 
                             matInfo.type + " (texture: " + (matInfo.texturePath != null ? "YES" : "NO") + ")");
        }
        
        System.out.println("Material loading complete:");
        System.out.println("  Textured materials: " + successCount);
        System.out.println("  Fallback textures: " + fallbackCount);
        System.out.println("  Special materials: " + specialCount);
        System.out.println("  Total materials: " + modelData.materials.length);
    }
    
    public void render() {
        if (modelData == null) {
            return;
        }
        
        // Setup basic OpenGL rendering states
        setupBasicRenderingStates();
        
        // Setup lighting
        setupLighting();
        
        // Set default color to white
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Disable face culling for complex GLB models (furniture, interiors)
        // This prevents missing faces on chairs, ovens, and other complex geometry
        glDisable(GL_CULL_FACE);
        
        // Render by meshes to support multiple material types
        if (modelData.meshes != null && modelData.meshes.length > 0) {
            renderMeshesWithMaterials();
        } else {
            // Fallback: render all triangles with first material
            renderAllTrianglesWithMaterial();
        }
        
        // Cleanup OpenGL states
        cleanupRenderingStates();
    }
    
    private void renderMeshesWithMaterials() {
        for (int meshIndex = 0; meshIndex < modelData.meshes.length; meshIndex++) {
            MeshInfo mesh = modelData.meshes[meshIndex];
            
            // Validate mesh data
            if (mesh.startIndex < 0 || mesh.indexCount <= 0 || 
                mesh.startIndex + mesh.indexCount > modelData.indices.length) {
                System.err.println("Invalid mesh " + meshIndex + ": startIndex=" + mesh.startIndex + 
                                 ", indexCount=" + mesh.indexCount + ", total indices=" + modelData.indices.length);
                continue;
            }
            
            // Get material info for this mesh
            TextureMatcher.MaterialInfo matInfo = getMaterialInfoForMesh(mesh.materialIndex);
            
            // Setup material-specific rendering
            setupMaterialRendering(matInfo, mesh.materialIndex);
            
            // Render this mesh's triangles
            glBegin(GL_TRIANGLES);
            for (int i = mesh.startIndex; i < mesh.startIndex + mesh.indexCount; i += 3) {
                if (i + 2 < modelData.indices.length) {
                    int idx1 = modelData.indices[i];
                    int idx2 = modelData.indices[i + 1];
                    int idx3 = modelData.indices[i + 2];
                    renderTriangle(idx1, idx2, idx3, matInfo);
                }
            }
            glEnd();
            
            // Cleanup material-specific rendering
            cleanupMaterialRendering(matInfo);
        }
    }
    
    private void renderAllTrianglesWithMaterial() {
        // Use first material if available
        TextureMatcher.MaterialInfo matInfo = (materialInfos != null && materialInfos.length > 0) ? 
                                            materialInfos[0] : 
                                            new TextureMatcher.MaterialInfo("textures/missing_texture.jpg");
        
        setupMaterialRendering(matInfo, 0);
        
        glBegin(GL_TRIANGLES);
        for (int i = 0; i < modelData.indices.length; i += 3) {
            if (i + 2 < modelData.indices.length) {
                renderTriangle(modelData.indices[i], modelData.indices[i + 1], modelData.indices[i + 2], matInfo);
            }
        }
        glEnd();
        
        cleanupMaterialRendering(matInfo);
    }
    
    private void setupMaterialRendering(TextureMatcher.MaterialInfo matInfo, int materialIndex) {
        switch (matInfo.type) {
            case GLASS:
                setupGlassRendering(matInfo, materialIndex);
                break;
            case MIRROR:
                setupMirrorRendering(matInfo, materialIndex);
                break;
            case COLOR:
                setupColorRendering(matInfo);
                break;
            case EMISSIVE:
                setupEmissiveRendering(matInfo, materialIndex);
                break;
            case TEXTURED:
            default:
                setupTexturedRendering(matInfo, materialIndex);
                break;
        }
    }
    
    private void setupGlassRendering(TextureMatcher.MaterialInfo matInfo, int materialIndex) {
        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Reduce depth writing for better transparency
        glDepthMask(false);
        
        // Set transparent color
        glColor4f(matInfo.color[0], matInfo.color[1], matInfo.color[2], matInfo.alpha);
        
        // Use texture if available
        if (matInfo.texturePath != null) {
            int textureId = getTextureForMaterial(materialIndex);
            if (textureId != -1) {
                glEnable(GL_TEXTURE_2D);
                glBindTexture(GL_TEXTURE_2D, textureId);
            }
        } else {
            glDisable(GL_TEXTURE_2D);
        }
    }
    
    private void setupMirrorRendering(TextureMatcher.MaterialInfo matInfo, int materialIndex) {
        // High specular reflection
        glEnable(GL_LIGHTING);
        float[] specular = {1.0f, 1.0f, 1.0f, 1.0f};
        glMaterialfv(GL_FRONT, GL_SPECULAR, specular);
        glMaterialf(GL_FRONT, GL_SHININESS, 128.0f); // Shininess is a single float value
        
        // Set reflective color
        glColor4f(matInfo.color[0], matInfo.color[1], matInfo.color[2], 1.0f);
        
        // Use texture if available, otherwise rely on material properties
        if (matInfo.texturePath != null) {
            int textureId = getTextureForMaterial(materialIndex);
            if (textureId != -1) {
                glEnable(GL_TEXTURE_2D);
                glBindTexture(GL_TEXTURE_2D, textureId);
            }
        } else {
            glDisable(GL_TEXTURE_2D);
        }
    }
    
    private void setupColorRendering(TextureMatcher.MaterialInfo matInfo) {
        // Disable texturing, use solid color
        glDisable(GL_TEXTURE_2D);
        glColor4f(matInfo.color[0], matInfo.color[1], matInfo.color[2], matInfo.alpha);
    }
    
    private void setupEmissiveRendering(TextureMatcher.MaterialInfo matInfo, int materialIndex) {
        // Bright emissive lighting
        float[] emission = {matInfo.color[0] * 0.3f, matInfo.color[1] * 0.3f, matInfo.color[2] * 0.3f, 1.0f};
        glMaterialfv(GL_FRONT, GL_EMISSION, emission);
        
        // Bright color
        glColor4f(matInfo.color[0], matInfo.color[1], matInfo.color[2], 1.0f);
        
        // Use texture if available
        if (matInfo.texturePath != null) {
            int textureId = getTextureForMaterial(materialIndex);
            if (textureId != -1) {
                glEnable(GL_TEXTURE_2D);
                glBindTexture(GL_TEXTURE_2D, textureId);
            }
        } else {
            glDisable(GL_TEXTURE_2D);
        }
    }
    
    private void setupTexturedRendering(TextureMatcher.MaterialInfo matInfo, int materialIndex) {
        // Standard textured rendering
        glColor4f(1.0f, 1.0f, 1.0f, matInfo.alpha);
        
        int textureId = getTextureForMaterial(materialIndex);
        if (textureId != -1) {
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, textureId);
        } else {
            glDisable(GL_TEXTURE_2D);
        }
    }
    
    private void cleanupMaterialRendering(TextureMatcher.MaterialInfo matInfo) {
        // Reset material properties to default
        glDepthMask(true);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Reset material properties
        float[] defaultEmission = {0.0f, 0.0f, 0.0f, 1.0f};
        float[] defaultSpecular = {0.0f, 0.0f, 0.0f, 1.0f};
        glMaterialfv(GL_FRONT, GL_EMISSION, defaultEmission);
        glMaterialfv(GL_FRONT, GL_SPECULAR, defaultSpecular);
        glMaterialf(GL_FRONT, GL_SHININESS, 0.0f); // Shininess is a single float value
    }
    
    private TextureMatcher.MaterialInfo getMaterialInfoForMesh(int materialIndex) {
        if (materialInfos == null || materialIndex < 0 || materialIndex >= materialInfos.length) {
            return new TextureMatcher.MaterialInfo("textures/missing_texture.jpg");
        }
        return materialInfos[materialIndex];
    }
    
    private int getTextureForMaterial(int materialIndex) {
        if (textureIds == null || materialIndex < 0 || materialIndex >= textureIds.length) {
            return fallbackTextureId;
        }
        
        int textureId = textureIds[materialIndex];
        return textureId != -1 ? textureId : fallbackTextureId;
    }
    
    private void setupBasicRenderingStates() {
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
    
    private void renderTriangle(int index1, int index2, int index3, TextureMatcher.MaterialInfo matInfo) {
        // Render first vertex
        if (matInfo.texturePath != null && modelData.texCoords.length > index1 * 2 + 1) {
            glTexCoord2f(modelData.texCoords[index1 * 2], 1.0f - modelData.texCoords[index1 * 2 + 1]);
        }
        if (modelData.normals.length > index1 * 3 + 2) {
            glNormal3f(modelData.normals[index1 * 3], modelData.normals[index1 * 3 + 1], modelData.normals[index1 * 3 + 2]);
        }
        glVertex3f(modelData.vertices[index1 * 3], modelData.vertices[index1 * 3 + 1], modelData.vertices[index1 * 3 + 2]);
        
        // Render second vertex
        if (matInfo.texturePath != null && modelData.texCoords.length > index2 * 2 + 1) {
            glTexCoord2f(modelData.texCoords[index2 * 2], 1.0f - modelData.texCoords[index2 * 2 + 1]);
        }
        if (modelData.normals.length > index2 * 3 + 2) {
            glNormal3f(modelData.normals[index2 * 3], modelData.normals[index2 * 3 + 1], modelData.normals[index2 * 3 + 2]);
        }
        glVertex3f(modelData.vertices[index2 * 3], modelData.vertices[index2 * 3 + 1], modelData.vertices[index2 * 3 + 2]);
        
        // Render third vertex
        if (matInfo.texturePath != null && modelData.texCoords.length > index3 * 2 + 1) {
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
    
    // === COLLISION DETECTION SUPPORT ===
    
    /**
     * Get mesh information for geometry-based collision detection
     */
    public GLBLoader.MeshInfo[] getMeshes() {
        return modelData != null ? modelData.meshes : null;
    }
    
    /**
     * Get vertex data for collision calculations
     */
    public float[] getVertices() {
        return modelData != null ? modelData.vertices : null;
    }
    
    /**
     * Get index data for collision calculations
     */
    public int[] getIndices() {
        return modelData != null ? modelData.indices : null;
    }
    
    /**
     * Get material information for mesh identification
     */
    public GLBLoader.MaterialInfo[] getMaterials() {
        return modelData != null ? modelData.materials : null;
    }
    
    /**
     * Check if model has mesh data available for geometry-based collision detection
     */
    public boolean hasMeshData() {
        return modelData != null && 
               modelData.meshes != null && 
               modelData.vertices != null && 
               modelData.indices != null;
    }
    
    /**
     * Get the name of a mesh by index
     */
    public String getMeshName(int meshIndex) {
        if (modelData == null || modelData.meshes == null || 
            meshIndex < 0 || meshIndex >= modelData.meshes.length) {
            return "unknown_mesh_" + meshIndex;
        }
        
        GLBLoader.MeshInfo mesh = modelData.meshes[meshIndex];
        String name = mesh.name;
        
        // If mesh name is null/empty, try to get material name
        if (name == null || name.trim().isEmpty()) {
            if (modelData.materials != null && 
                mesh.materialIndex >= 0 && 
                mesh.materialIndex < modelData.materials.length) {
                name = modelData.materials[mesh.materialIndex].cleanName;
            }
        }
        
        // Fallback to numbered mesh
        if (name == null || name.trim().isEmpty()) {
            name = "mesh_" + meshIndex;
        }
        
        return name;
    }
} 