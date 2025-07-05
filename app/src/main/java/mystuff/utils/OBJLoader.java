package mystuff.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OBJLoader {
    
    public static class ModelData {
        public float[] vertices;
        public float[] texCoords;
        public float[] normals;
        public int[] indices;
        public int vertexCount;
    }
    
    public static class Face {
        public int[] vertexIndices;
        public int[] texCoordIndices;
        public int[] normalIndices;
        
        public Face(int[] vertexIndices, int[] texCoordIndices, int[] normalIndices) {
            this.vertexIndices = vertexIndices;
            this.texCoordIndices = texCoordIndices;
            this.normalIndices = normalIndices;
        }
    }
    
    public static ModelData loadOBJModel(String filePath) {
        List<Float> tempVertices = new ArrayList<>();
        List<Float> tempTexCoords = new ArrayList<>();
        List<Float> tempNormals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        // Store face data as we parse it
        List<Face> faces = new ArrayList<>();
        
        try {
            InputStream inputStream = OBJLoader.class.getClassLoader().getResourceAsStream(filePath);
            if (inputStream == null) {
                System.err.println("Could not find OBJ file: " + filePath);
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                
                if (parts.length == 0) continue;
                
                switch (parts[0]) {
                    case "v": // Vertex
                        tempVertices.add(Float.parseFloat(parts[1]));
                        tempVertices.add(Float.parseFloat(parts[2]));
                        tempVertices.add(Float.parseFloat(parts[3]));
                        break;
                        
                    case "vt": // Texture coordinate
                        tempTexCoords.add(Float.parseFloat(parts[1]));
                        tempTexCoords.add(Float.parseFloat(parts[2]));
                        break;
                        
                    case "vn": // Normal
                        tempNormals.add(Float.parseFloat(parts[1]));
                        tempNormals.add(Float.parseFloat(parts[2]));
                        tempNormals.add(Float.parseFloat(parts[3]));
                        break;
                        
                    case "f": // Face
                        faces.add(parseFace(parts));
                        break;
                }
            }
            
            reader.close();
            
        } catch (IOException e) {
            System.err.println("Error loading OBJ file: " + e.getMessage());
            return null;
        }
        
        // Now process the faces to create the final data
        return processFaces(faces, tempVertices, tempTexCoords, tempNormals);
    }
    
    private static ModelData processFaces(List<Face> faces, List<Float> tempVertices, 
                                        List<Float> tempTexCoords, List<Float> tempNormals) {
        List<Float> vertices = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        // Use a map to track unique vertex combinations
        java.util.Map<String, Integer> vertexMap = new java.util.HashMap<>();
        
        for (Face face : faces) {
            // Triangulate the face if it has more than 3 vertices
            List<Face> triangles = triangulateFace(face);
            
            for (Face triangle : triangles) {
                // Process each vertex in the triangle
                for (int i = 0; i < triangle.vertexIndices.length; i++) {
                    int vertexIndex = triangle.vertexIndices[i];
                    int texCoordIndex = triangle.texCoordIndices[i];
                    int normalIndex = triangle.normalIndices[i];
                    
                    // Create a unique key for this vertex combination
                    String vertexKey = vertexIndex + "/" + texCoordIndex + "/" + normalIndex;
                    
                    Integer existingIndex = vertexMap.get(vertexKey);
                    if (existingIndex != null) {
                        // Reuse existing vertex
                        indices.add(existingIndex);
                    } else {
                        // Create new vertex
                        int newIndex = vertices.size() / 3;
                        vertexMap.put(vertexKey, newIndex);
                        indices.add(newIndex);
                        
                        // Add vertex position
                        vertices.add(tempVertices.get(vertexIndex * 3));
                        vertices.add(tempVertices.get(vertexIndex * 3 + 1));
                        vertices.add(tempVertices.get(vertexIndex * 3 + 2));
                        
                        // Add texture coordinate (if available)
                        if (texCoordIndex >= 0 && texCoordIndex < tempTexCoords.size() / 2) {
                            texCoords.add(tempTexCoords.get(texCoordIndex * 2));
                            texCoords.add(tempTexCoords.get(texCoordIndex * 2 + 1));
                        } else {
                            texCoords.add(0.0f);
                            texCoords.add(0.0f);
                        }
                        
                        // Add normal (if available)
                        if (normalIndex >= 0 && normalIndex < tempNormals.size() / 3) {
                            normals.add(tempNormals.get(normalIndex * 3));
                            normals.add(tempNormals.get(normalIndex * 3 + 1));
                            normals.add(tempNormals.get(normalIndex * 3 + 2));
                        } else {
                            normals.add(0.0f);
                            normals.add(1.0f);
                            normals.add(0.0f);
                        }
                    }
                }
            }
        }
        
        ModelData modelData = new ModelData();
        modelData.vertices = listToArray(vertices);
        modelData.texCoords = listToArray(texCoords);
        modelData.normals = listToArray(normals);
        modelData.indices = listToIntArray(indices);
        modelData.vertexCount = modelData.indices.length;
        
        System.out.println("Loaded OBJ model with proper indexing");
        System.out.println("Vertices: " + modelData.vertices.length / 3);
        System.out.println("Faces: " + modelData.indices.length / 3);
        
        return modelData;
    }
    
    private static Face parseFace(String[] parts) {
        // Handle different face formats: f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3
        // or f v1//vn1 v2//vn2 v3//vn3 or f v1 v2 v3
        
        int[] vertexIndices = new int[parts.length - 1];
        int[] texCoordIndices = new int[parts.length - 1];
        int[] normalIndices = new int[parts.length - 1];
        
        for (int i = 1; i < parts.length; i++) {
            String[] vertexData = parts[i].split("/");
            
            vertexIndices[i-1] = Integer.parseInt(vertexData[0]) - 1;
            texCoordIndices[i-1] = vertexData.length > 1 && !vertexData[1].isEmpty() ? 
                                  Integer.parseInt(vertexData[1]) - 1 : -1;
            normalIndices[i-1] = vertexData.length > 2 ? 
                                Integer.parseInt(vertexData[2]) - 1 : -1;
        }
        
        return new Face(vertexIndices, texCoordIndices, normalIndices);
    }
    
    private static List<Face> triangulateFace(Face face) {
        List<Face> triangles = new ArrayList<>();
        
        // If it's already a triangle, return it as is
        if (face.vertexIndices.length == 3) {
            triangles.add(face);
            return triangles;
        }
        
        // Triangulate using fan triangulation (simple but effective)
        for (int i = 1; i < face.vertexIndices.length - 1; i++) {
            int[] triVertexIndices = {face.vertexIndices[0], face.vertexIndices[i], face.vertexIndices[i + 1]};
            int[] triTexCoordIndices = {face.texCoordIndices[0], face.texCoordIndices[i], face.texCoordIndices[i + 1]};
            int[] triNormalIndices = {face.normalIndices[0], face.normalIndices[i], face.normalIndices[i + 1]};
            
            triangles.add(new Face(triVertexIndices, triTexCoordIndices, triNormalIndices));
        }
        
        return triangles;
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