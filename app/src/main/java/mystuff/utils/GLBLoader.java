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
        
        public MaterialInfo(String name) {
            this.name = name;
            this.cleanName = cleanMaterialName(name);
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
            
            materials[i] = new MaterialInfo(materialName);
            System.out.println("  Material " + i + ": \"" + materialName + "\" -> \"" + materials[i].cleanName + "\"");
        }
        
        return materials;
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