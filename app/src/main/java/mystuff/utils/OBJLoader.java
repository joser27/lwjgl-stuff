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
    
    public static ModelData loadOBJModel(String filePath) {
        List<Float> vertices = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        List<Float> tempVertices = new ArrayList<>();
        List<Float> tempTexCoords = new ArrayList<>();
        List<Float> tempNormals = new ArrayList<>();
        
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
                        processFace(parts, tempVertices, tempTexCoords, tempNormals, 
                                  vertices, texCoords, normals, indices);
                        break;
                }
            }
            
            reader.close();
            
        } catch (IOException e) {
            System.err.println("Error loading OBJ file: " + e.getMessage());
            return null;
        }
        
        ModelData modelData = new ModelData();
        modelData.vertices = listToArray(vertices);
        modelData.texCoords = listToArray(texCoords);
        modelData.normals = listToArray(normals);
        modelData.indices = listToIntArray(indices);
        modelData.vertexCount = modelData.indices.length;
        
        System.out.println("Loaded OBJ model: " + filePath);
        System.out.println("Vertices: " + modelData.vertices.length / 3);
        System.out.println("Faces: " + modelData.indices.length / 3);
        
        return modelData;
    }
    
    private static void processFace(String[] parts, List<Float> tempVertices, 
                                   List<Float> tempTexCoords, List<Float> tempNormals,
                                   List<Float> vertices, List<Float> texCoords, 
                                   List<Float> normals, List<Integer> indices) {
        
        // Handle different face formats: f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3
        // or f v1//vn1 v2//vn2 v3//vn3 or f v1 v2 v3
        
        for (int i = 1; i <= 3; i++) {
            String[] vertexData = parts[i].split("/");
            
            int vertexIndex = Integer.parseInt(vertexData[0]) - 1;
            int texCoordIndex = vertexData.length > 1 && !vertexData[1].isEmpty() ? 
                               Integer.parseInt(vertexData[1]) - 1 : -1;
            int normalIndex = vertexData.length > 2 ? 
                             Integer.parseInt(vertexData[2]) - 1 : -1;
            
            // Add vertex
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
            
            indices.add(indices.size());
        }
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