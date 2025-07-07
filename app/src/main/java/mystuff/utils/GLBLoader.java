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
    
    // Use the same ModelData class as OBJLoader for consistency
    public static class ModelData {
        public float[] vertices;
        public float[] texCoords;
        public float[] normals;
        public int[] indices;
        public int vertexCount;
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
            
            // Convert Assimp scene to simple ModelData (like OBJLoader)
            ModelData modelData = convertAssimpScene(scene);
            
            // Cleanup
            aiReleaseImport(scene);
            tempFile.delete();
            
            if (modelData != null) {
                System.out.println("GLB model loaded successfully: " + filePath);
                System.out.println("  Vertices: " + modelData.vertices.length / 3);
                System.out.println("  Faces: " + modelData.indices.length / 3);
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
        
        // Process all meshes and combine them into one ModelData (like OBJ does)
        PointerBuffer meshes = scene.mMeshes();
        
        List<Float> verticesList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>(); 
        List<Float> texCoordsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();
        
        int vertexOffset = 0;
        
        for (int i = 0; i < numMeshes; i++) {
            AIMesh mesh = AIMesh.create(meshes.get(i));
            
            int vertexCount = mesh.mNumVertices();
            
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
            
            vertexOffset += vertexCount;
        }
        
        // Convert lists to arrays (same as OBJLoader)
        ModelData modelData = new ModelData();
        modelData.vertices = listToArray(verticesList);
        modelData.normals = listToArray(normalsList);
        modelData.texCoords = listToArray(texCoordsList);
        modelData.indices = listToIntArray(indicesList);
        modelData.vertexCount = modelData.indices.length;
        
        return modelData;
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