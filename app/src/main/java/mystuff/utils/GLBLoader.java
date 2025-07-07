package mystuff.utils;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import static org.lwjgl.assimp.Assimp.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class GLBLoader {
    
    public static class GLBModelData {
        public float[] vertices;
        public float[] normals;
        public float[] texCoords;
        public int[] indices;
        public int vertexCount;
        public GLBMaterial[] materials;
        public GLBMesh[] meshes;
        
        public GLBModelData() {
            // Default constructor
        }
    }
    
    public static class GLBMaterial {
        public String name;
        public byte[] diffuseTexture;
        public String textureFormat;
        public float[] baseColorFactor = {1.0f, 1.0f, 1.0f, 1.0f};
        public float metallicFactor = 1.0f;
        public float roughnessFactor = 1.0f;
        
        public GLBMaterial(String name) {
            this.name = name;
        }
        
        public GLBMaterial(String name, byte[] diffuseTexture) {
            this.name = name;
            this.diffuseTexture = diffuseTexture;
        }
    }
    
    public static class GLBMesh {
        public String name;
        public int materialIndex;
        public int startIndex;
        public int indexCount;
        
        public GLBMesh(String name, int materialIndex, int startIndex, int indexCount) {
            this.name = name;
            this.materialIndex = materialIndex;
            this.startIndex = startIndex;
            this.indexCount = indexCount;
        }
    }
    
    public static GLBModelData loadGLBModel(String filePath) {
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
            
            // Load with Assimp
            AIScene scene = aiImportFile(tempFile.getAbsolutePath(), 
                aiProcess_Triangulate | aiProcess_FlipUVs | aiProcess_GenNormals);
            
            if (scene == null) {
                System.err.println("Failed to load GLB with Assimp: " + aiGetErrorString());
                tempFile.delete();
                return null;
            }
            
            // Convert Assimp scene to our format
            GLBModelData modelData = convertAssimpScene(scene);
            
            // Cleanup
            aiReleaseImport(scene);
            tempFile.delete();
            
            System.out.println("GLB model loaded successfully: " + filePath);
            System.out.println("  Vertices: " + modelData.vertexCount);
            System.out.println("  Materials: " + (modelData.materials != null ? modelData.materials.length : 0));
            System.out.println("  Meshes: " + (modelData.meshes != null ? modelData.meshes.length : 0));
            
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
    
    private static GLBModelData convertAssimpScene(AIScene scene) {
        GLBModelData modelData = new GLBModelData();
        
        // Get mesh count
        int numMeshes = scene.mNumMeshes();
        if (numMeshes == 0) {
            System.err.println("No meshes found in GLB file");
            return null;
        }
        
        // Process meshes
        PointerBuffer meshes = scene.mMeshes();
        
        List<Float> verticesList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>(); 
        List<Float> texCoordsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();
        List<GLBMesh> meshList = new ArrayList<>();
        
        int currentVertexOffset = 0;
        
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
                        indicesList.add(currentVertexOffset + indices.get(idx));
                    }
                }
            }
            
            // Create mesh info
            int indexCount = indicesList.size() - startIndex;
            GLBMesh glbMesh = new GLBMesh("Mesh_" + i, mesh.mMaterialIndex(), startIndex, indexCount);
            meshList.add(glbMesh);
            
            currentVertexOffset += vertexCount;
        }
        
        // Convert lists to arrays
        modelData.vertices = new float[verticesList.size()];
        for (int i = 0; i < verticesList.size(); i++) {
            modelData.vertices[i] = verticesList.get(i);
        }
        
        modelData.normals = new float[normalsList.size()];
        for (int i = 0; i < normalsList.size(); i++) {
            modelData.normals[i] = normalsList.get(i);
        }
        
        modelData.texCoords = new float[texCoordsList.size()];
        for (int i = 0; i < texCoordsList.size(); i++) {
            modelData.texCoords[i] = texCoordsList.get(i);
        }
        
        modelData.indices = indicesList.stream().mapToInt(Integer::intValue).toArray();
        modelData.vertexCount = verticesList.size() / 3;
        modelData.meshes = meshList.toArray(new GLBMesh[0]);
        
        // Extract materials from scene
        modelData.materials = extractMaterials(scene);
        
        return modelData;
    }
    
    private static GLBMaterial[] extractMaterials(AIScene scene) {
        int numMaterials = scene.mNumMaterials();
        if (numMaterials == 0) {
            System.out.println("No materials found in GLB file");
            return new GLBMaterial[0];
        }
        
        System.out.println("Extracting " + numMaterials + " materials from GLB file");
        
        PointerBuffer materialPtrs = scene.mMaterials();
        GLBMaterial[] materials = new GLBMaterial[numMaterials];
        
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(materialPtrs.get(i));
            
            // Get material name
            AIString name = AIString.calloc();
            String materialName = "Material_" + i;
            
            if (aiGetMaterialString(aiMaterial, AI_MATKEY_NAME, 0, 0, name) == aiReturn_SUCCESS) {
                materialName = name.dataString();
            }
            name.free();
            
            System.out.println("  Material " + i + ": " + materialName);
            
            // Create material and try to extract texture
            GLBMaterial material = new GLBMaterial(materialName);
            
            // Try to get diffuse texture
            AIString texturePath = AIString.calloc();
            if (aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, texturePath, 
                                     (IntBuffer)null, (IntBuffer)null, (FloatBuffer)null, 
                                     (IntBuffer)null, (IntBuffer)null, (IntBuffer)null) == aiReturn_SUCCESS) {
                
                String texPath = texturePath.dataString();
                System.out.println("    Texture path: " + texPath);
                
                // GLB files often have embedded textures with paths like "*0", "*1", etc.
                if (texPath.startsWith("*")) {
                    try {
                        int textureIndex = Integer.parseInt(texPath.substring(1));
                        byte[] textureData = extractEmbeddedTexture(scene, textureIndex);
                        if (textureData != null) {
                            material.diffuseTexture = textureData;
                            System.out.println("    Extracted embedded texture " + textureIndex + 
                                             " (" + textureData.length + " bytes)");
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("    Invalid embedded texture index: " + texPath);
                    }
                }
            }
            texturePath.free();
            
            materials[i] = material;
        }
        
        return materials;
    }
    
    private static byte[] extractEmbeddedTexture(AIScene scene, int textureIndex) {
        int numTextures = scene.mNumTextures();
        System.out.println("    Scene has " + numTextures + " embedded textures, requesting index " + textureIndex);
        
        PointerBuffer texturePtrs = scene.mTextures();
        if (texturePtrs == null || textureIndex >= numTextures || textureIndex < 0) {
            System.err.println("    Invalid texture index " + textureIndex + " (available: 0-" + (numTextures-1) + ")");
            return null;
        }
        
        AITexture aiTexture = AITexture.create(texturePtrs.get(textureIndex));
        
        int width = aiTexture.mWidth();
        int height = aiTexture.mHeight();
        
        System.out.println("    Texture dimensions: " + width + "x" + height);
        
        if (height == 0) {
            // Compressed texture (like PNG, JPEG) - width contains data size
            int dataSize = width;
            System.out.println("    Compressed texture, data size: " + dataSize + " bytes");
            
            if (dataSize > 0) {
                try {
                    // For compressed textures, access raw byte data directly
                    AITexel.Buffer texelBuffer = aiTexture.pcData();
                    if (texelBuffer == null) {
                        System.err.println("    Texture buffer is null");
                        return null;
                    }
                    
                    // Create a ByteBuffer from the texture buffer's memory
                    long bufferAddress = texelBuffer.address();
                    ByteBuffer rawBuffer = org.lwjgl.system.MemoryUtil.memByteBuffer(bufferAddress, dataSize);
                    
                    int bufferSize = rawBuffer.remaining();
                    System.out.println("    Raw buffer size: " + bufferSize + " bytes");
                    
                    // Read the raw bytes directly
                    byte[] data = new byte[Math.min(dataSize, bufferSize)];
                    rawBuffer.get(data);
                    return data;
                } catch (Exception e) {
                    System.err.println("    Error extracting compressed texture: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
        } else {
            // Uncompressed texture (RGBA)
            int pixelCount = width * height;
            System.out.println("    Uncompressed texture, " + pixelCount + " pixels");
            
            if (pixelCount > 0) {
                try {
                    AITexel.Buffer texelBuffer = aiTexture.pcData();
                    if (texelBuffer == null) {
                        System.err.println("    Texture buffer is null");
                        return null;
                    }
                    
                    int bufferSize = texelBuffer.remaining();
                    System.out.println("    Buffer size: " + bufferSize + " texels");
                    
                    byte[] data = new byte[pixelCount * 4]; // RGBA = 4 bytes per pixel
                    
                    for (int i = 0; i < Math.min(pixelCount, bufferSize); i++) {
                        AITexel texel = texelBuffer.get(i);
                        data[i * 4] = texel.r();     // Red
                        data[i * 4 + 1] = texel.g(); // Green  
                        data[i * 4 + 2] = texel.b(); // Blue
                        data[i * 4 + 3] = texel.a(); // Alpha
                    }
                    return data;
                } catch (Exception e) {
                    System.err.println("    Error extracting uncompressed texture: " + e.getMessage());
                    return null;
                }
            }
        }
        
        return null;
    }
    
    // TODO: These methods will be implemented once jgltf API is properly set up
    /*
    private static void extractVertexData(GltfModel gltfModel, GLBModelData modelData) {
        // Implementation pending
    }
    
    private static void extractMaterialData(GltfModel gltfModel, GLBModelData modelData) {
        // Implementation pending
    }
    
    private static void extractMeshData(GltfModel gltfModel, GLBModelData modelData) {
        // Implementation pending
    }
    */
} 