package mystuff.utils;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import static org.lwjgl.assimp.Assimp.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class GLBLoader {
    
    // Enhanced ModelData class to support materials
    public static class ModelData {
        public float[] vertices;
        public float[] texCoords;
        public float[] normals;
        public int[] indices;
        public int vertexCount;
        
        // Material support
        public MaterialInfo[] materials;
        public MeshInfo[] meshes;
    }
    
    public static class MaterialInfo {
        public String name;
        public String cleanName; // Cleaned version for texture matching
        public float[] baseColorFactor; // Base color factor from GLB material (RGBA)
        public boolean hasTexture; // Whether this material has a texture
        public float metallicFactor; // Metallic factor for PBR materials
        public float roughnessFactor; // Roughness factor for PBR materials
        public float alphaCutoff; // Alpha cutoff for transparency
        
        public MaterialInfo(String name) {
            this.name = name;
            this.cleanName = cleanMaterialName(name);
            this.baseColorFactor = new float[]{1.0f, 1.0f, 1.0f, 1.0f}; // Default white
            this.hasTexture = false;
            this.metallicFactor = 0.0f;
            this.roughnessFactor = 1.0f;
            this.alphaCutoff = 0.5f;
        }
        
        public MaterialInfo(String name, float[] baseColorFactor, boolean hasTexture) {
            this.name = name;
            this.cleanName = cleanMaterialName(name);
            this.baseColorFactor = baseColorFactor != null ? baseColorFactor : new float[]{1.0f, 1.0f, 1.0f, 1.0f};
            this.hasTexture = hasTexture;
            this.metallicFactor = 0.0f;
            this.roughnessFactor = 1.0f;
            this.alphaCutoff = 0.5f;
        }
        
        public MaterialInfo(String name, float[] baseColorFactor, boolean hasTexture, 
                          float metallicFactor, float roughnessFactor, float alphaCutoff) {
            this.name = name;
            this.cleanName = cleanMaterialName(name);
            this.baseColorFactor = baseColorFactor != null ? baseColorFactor : new float[]{1.0f, 1.0f, 1.0f, 1.0f};
            this.hasTexture = hasTexture;
            this.metallicFactor = metallicFactor;
            this.roughnessFactor = roughnessFactor;
            this.alphaCutoff = alphaCutoff;
        }
        
        // Clean material name for better texture matching
        private static String cleanMaterialName(String name) {
            if (name == null) return "default";
            
            // Remove common prefixes/suffixes and normalize
            String cleaned = name.toLowerCase()
                .replaceAll("^material_?", "")
                .replaceAll("^mat_?", "")
                .replaceAll("^m_", "")
                .replaceAll("_?\\d+$", "") // Remove trailing numbers
                .replaceAll("[^a-z0-9_]", "_") // Replace special chars with underscore
                .replaceAll("_+", "_") // Collapse multiple underscores
                .replaceAll("^_|_$", ""); // Remove leading/trailing underscores
            
            return cleaned.isEmpty() ? "default" : cleaned;
        }
    }
    
    public static class MeshInfo {
        public String name;
        public int materialIndex;
        public int startIndex;
        public int indexCount;
        
        public MeshInfo(String name, int materialIndex, int startIndex, int indexCount) {
            this.name = name;
            this.materialIndex = materialIndex;
            this.startIndex = startIndex;
            this.indexCount = indexCount;
        }
    }
    
    public static ModelData loadGLBModel(String filePath) {
        try {
            // Load the GLB file from resources
            InputStream inputStream = GLBLoader.class.getClassLoader().getResourceAsStream(filePath);
            if (inputStream == null) {
                System.err.println("Could not find GLB file: " + filePath);
                return null;
            }
            
            // Read the file data
            byte[] data = inputStream.readAllBytes();
            inputStream.close();
            
            System.out.println("Loading GLB file: " + filePath);
            System.out.println("  File size: " + data.length + " bytes");
            
            // Write to temporary file for Assimp to read
            java.io.File tempFile = java.io.File.createTempFile("temp_model", ".glb");
            tempFile.deleteOnExit();
            
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                fos.write(data);
            }
            
            // Load with Assimp using the same flags as OBJ loading
            AIScene scene = aiImportFile(tempFile.getAbsolutePath(), 
                aiProcess_Triangulate | aiProcess_GenNormals | aiProcess_CalcTangentSpace | 
                aiProcess_JoinIdenticalVertices | aiProcess_PreTransformVertices);
            
            if (scene == null) {
                System.err.println("Failed to load GLB with Assimp: " + aiGetErrorString());
                tempFile.delete();
                return null;
            }
            
            // Convert Assimp scene to enhanced ModelData
            ModelData modelData = convertAssimpScene(scene);
            
            // Cleanup
            aiReleaseImport(scene);
            tempFile.delete();
            
            if (modelData != null) {
                System.out.println("GLB model loaded successfully: " + filePath);
                System.out.println("  Vertices: " + modelData.vertices.length / 3);
                System.out.println("  Faces: " + modelData.indices.length / 3);
                System.out.println("  Materials: " + (modelData.materials != null ? modelData.materials.length : 0));
                System.out.println("  Meshes: " + (modelData.meshes != null ? modelData.meshes.length : 0));
                
                // Print material names for debugging
                if (modelData.materials != null) {
                    System.out.println("  Material names:");
                    for (int i = 0; i < modelData.materials.length; i++) {
                        MaterialInfo mat = modelData.materials[i];
                        System.out.println("    " + i + ": \"" + mat.name + "\" -> \"" + mat.cleanName + "\"");
                    }
                }
            }
            
            return modelData;
            
        } catch (IOException e) {
            System.err.println("Error loading GLB file: " + filePath);
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error loading GLB file: " + filePath);
            e.printStackTrace();
            return null;
        }
    }
    
    private static ModelData convertAssimpScene(AIScene scene) {
        // Get mesh count
        int numMeshes = scene.mNumMeshes();
        if (numMeshes == 0) {
            System.err.println("No meshes found in GLB file");
            return null;
        }
        
        // Extract materials first
        MaterialInfo[] materials = extractMaterials(scene);
        
        // Process all meshes and combine them into one ModelData
        PointerBuffer meshes = scene.mMeshes();
        
        List<Float> verticesList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>(); 
        List<Float> texCoordsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();
        List<MeshInfo> meshList = new ArrayList<>();
        
        int vertexOffset = 0;
        
        for (int i = 0; i < numMeshes; i++) {
            AIMesh mesh = AIMesh.create(meshes.get(i));
            
            int vertexCount = mesh.mNumVertices();
            int startIndex = indicesList.size();
            
            // Extract vertices
            AIVector3D.Buffer vertices = mesh.mVertices();
            for (int v = 0; v < vertexCount; v++) {
                AIVector3D vertex = vertices.get(v);
                verticesList.add(vertex.x());
                verticesList.add(vertex.y());
                verticesList.add(vertex.z());
            }
            
            // Extract normals
            AIVector3D.Buffer normals = mesh.mNormals();
            if (normals != null) {
                for (int v = 0; v < vertexCount; v++) {
                    AIVector3D normal = normals.get(v);
                    normalsList.add(normal.x());
                    normalsList.add(normal.y());
                    normalsList.add(normal.z());
                }
            } else {
                // Add default normals if none exist
                for (int v = 0; v < vertexCount; v++) {
                    normalsList.add(0.0f);
                    normalsList.add(1.0f);
                    normalsList.add(0.0f);
                }
            }
            
            // Extract texture coordinates 
            AIVector3D.Buffer texCoords = mesh.mTextureCoords(0);
            if (texCoords != null) {
                for (int v = 0; v < vertexCount; v++) {
                    AIVector3D texCoord = texCoords.get(v);
                    texCoordsList.add(texCoord.x());
                    texCoordsList.add(texCoord.y());
                }
            } else {
                // Add default texture coordinates if none exist
                for (int v = 0; v < vertexCount; v++) {
                    texCoordsList.add(0.0f);
                    texCoordsList.add(0.0f);
                }
            }
            
            // Extract faces/indices
            AIFace.Buffer faces = mesh.mFaces();
            int numFaces = mesh.mNumFaces();
            
            for (int f = 0; f < numFaces; f++) {
                AIFace face = faces.get(f);
                IntBuffer indices = face.mIndices();
                int numIndices = face.mNumIndices();
                
                // Only process triangles
                if (numIndices == 3) {
                    for (int idx = 0; idx < numIndices; idx++) {
                        // Add vertex offset to make indices global across all meshes
                        indicesList.add(indices.get(idx) + vertexOffset);
                    }
                }
            }
            
            // Create mesh info
            int indexCount = indicesList.size() - startIndex;
            MeshInfo meshInfo = new MeshInfo("Mesh_" + i, mesh.mMaterialIndex(), startIndex, indexCount);
            meshList.add(meshInfo);
            
            vertexOffset += vertexCount;
        }
        
        // Convert lists to arrays
        ModelData modelData = new ModelData();
        modelData.vertices = listToArray(verticesList);
        modelData.normals = listToArray(normalsList);
        modelData.texCoords = listToArray(texCoordsList);
        modelData.indices = listToIntArray(indicesList);
        modelData.vertexCount = modelData.indices.length;
        modelData.materials = materials;
        modelData.meshes = meshList.toArray(new MeshInfo[0]);
        
        return modelData;
    }
    
    private static MaterialInfo[] extractMaterials(AIScene scene) {
        int numMaterials = scene.mNumMaterials();
        if (numMaterials == 0) {
            System.out.println("No materials found in GLB file, creating default material");
            return new MaterialInfo[] { new MaterialInfo("default") };
        }
        
        System.out.println("Extracting " + numMaterials + " materials from GLB file");
        
        PointerBuffer materialPtrs = scene.mMaterials();
        MaterialInfo[] materials = new MaterialInfo[numMaterials];
        
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(materialPtrs.get(i));
            
            // Get material name
            AIString nameString = AIString.calloc();
            String materialName = "Material_" + i;
            
            if (aiGetMaterialString(aiMaterial, AI_MATKEY_NAME, 0, 0, nameString) == aiReturn_SUCCESS) {
                String extractedName = nameString.dataString();
                if (extractedName != null && !extractedName.trim().isEmpty()) {
                    materialName = extractedName.trim();
                }
            }
            nameString.free();
            
            // Extract base color factor (diffuse color)
            float[] baseColorFactor = extractBaseColorFactor(aiMaterial);
            
            // Check if material has textures
            boolean hasTexture = checkMaterialHasTexture(aiMaterial);
            
            // Extract additional material properties
            float metallicFactor = extractMetallicFactor(aiMaterial);
            float roughnessFactor = extractRoughnessFactor(aiMaterial);
            float alphaCutoff = extractAlphaCutoff(aiMaterial);
            
            materials[i] = new MaterialInfo(materialName, baseColorFactor, hasTexture, 
                                          metallicFactor, roughnessFactor, alphaCutoff);
        }
        
        return materials;
    }
    
    private static float[] extractBaseColorFactor(AIMaterial aiMaterial) {
        float[] color = new float[4];
        int[] pMax = new int[1];
        
        // Try to get base color factor (diffuse color)
        if (aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_COLOR_DIFFUSE, 0, 0, color, pMax) == aiReturn_SUCCESS) {
            return normalizeColor(color);
        }
        
        // Fallback: try to get base color factor from PBR properties
        if (aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_BASE_COLOR, 0, 0, color, pMax) == aiReturn_SUCCESS) {
            return normalizeColor(color);
        }
        
        // Try to get ambient color as fallback
        if (aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_COLOR_AMBIENT, 0, 0, color, pMax) == aiReturn_SUCCESS) {
            return normalizeColor(color);
        }
        
        // Try to get specular color as fallback
        if (aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_COLOR_SPECULAR, 0, 0, color, pMax) == aiReturn_SUCCESS) {
            return normalizeColor(color);
        }
        
        // Default to white if no color found
        return new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    }
    
    private static float[] normalizeColor(float[] color) {
        // Ensure minimum brightness and proper alpha
        float r = Math.max(0.2f, color[0]); // Minimum 20% brightness
        float g = Math.max(0.2f, color[1]);
        float b = Math.max(0.2f, color[2]);
        float a = Math.max(0.8f, color[3]); // Minimum 80% alpha for visibility
        
        // If all colors are very dark, brighten them up
        if (r < 0.3f && g < 0.3f && b < 0.3f) {
            r = Math.min(1.0f, r * 3.0f); // Brighten by 3x
            g = Math.min(1.0f, g * 3.0f);
            b = Math.min(1.0f, b * 3.0f);
        }
        
        return new float[]{r, g, b, a};
    }
    
    private static boolean checkMaterialHasTexture(AIMaterial aiMaterial) {
        // Check for diffuse texture
        if (aiGetMaterialTextureCount(aiMaterial, aiTextureType_DIFFUSE) > 0) {
            return true;
        }
        
        // Check for base color texture (PBR)
        if (aiGetMaterialTextureCount(aiMaterial, aiTextureType_BASE_COLOR) > 0) {
            return true;
        }
        
        // Check for other common texture types
        if (aiGetMaterialTextureCount(aiMaterial, aiTextureType_NORMALS) > 0) {
            return true;
        }
        
        return false;
    }
    
    private static float extractMetallicFactor(AIMaterial aiMaterial) {
        float[] metallic = new float[1];
        int[] pMax = new int[1];
        
        // Try to get metallic factor from PBR properties
        if (aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_METALLIC_FACTOR, 0, 0, metallic, pMax) == aiReturn_SUCCESS) {
            return metallic[0];
        }
        
        // Try to get metallic factor from legacy properties
        if (aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_SHININESS, 0, 0, metallic, pMax) == aiReturn_SUCCESS) {
            // Convert shininess to metallic factor (rough approximation)
            return metallic[0] > 50.0f ? 1.0f : 0.0f;
        }
        
        return 0.0f; // Default to non-metallic
    }
    
    private static float extractRoughnessFactor(AIMaterial aiMaterial) {
        float[] roughness = new float[1];
        int[] pMax = new int[1];
        
        // Try to get roughness factor from PBR properties
        if (aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_ROUGHNESS_FACTOR, 0, 0, roughness, pMax) == aiReturn_SUCCESS) {
            return roughness[0];
        }
        
        // Try to get roughness from shininess (inverse relationship)
        if (aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_SHININESS, 0, 0, roughness, pMax) == aiReturn_SUCCESS) {
            // Convert shininess to roughness (rough approximation)
            return Math.max(0.0f, 1.0f - (roughness[0] / 128.0f));
        }
        
        return 1.0f; // Default to rough
    }
    
    private static float extractAlphaCutoff(AIMaterial aiMaterial) {
        float[] alphaCutoff = new float[1];
        int[] pMax = new int[1];
        
        // Try to get alpha cutoff
        if (aiGetMaterialFloatArray(aiMaterial, AI_MATKEY_GLTF_ALPHACUTOFF, 0, 0, alphaCutoff, pMax) == aiReturn_SUCCESS) {
            return alphaCutoff[0];
        }
        
        return 0.5f; // Default alpha cutoff
    }
    
    private static float[] listToArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    
    private static int[] listToIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
} 