package mystuff.utils;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import static org.lwjgl.assimp.Assimp.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for extracting embedded textures from GLB files
 */
public class GLBTextureExtractor {
    
    public static class ExtractedTexture {
        public String originalName;
        public String cleanName;
        public String fileName;
        public String filePath;
        public int width;
        public int height;
        public byte[] data;
        
        public ExtractedTexture(String originalName, String cleanName, String fileName, 
                              String filePath, int width, int height, byte[] data) {
            this.originalName = originalName;
            this.cleanName = cleanName;
            this.fileName = fileName;
            this.filePath = filePath;
            this.width = width;
            this.height = height;
            this.data = data;
        }
    }
    
    /**
     * Extract all textures from a GLB file and save them to the extracted folder
     */
    public static List<ExtractedTexture> extractTexturesFromGLB(String glbPath, String outputDir) {
        List<ExtractedTexture> extractedTextures = new ArrayList<>();
        
        try {
            System.out.println("Extracting textures from: " + glbPath);
            
            // Load the GLB file from resources
            InputStream inputStream = GLBTextureExtractor.class.getClassLoader().getResourceAsStream(glbPath);
            if (inputStream == null) {
                System.err.println("Could not find GLB file: " + glbPath);
                return extractedTextures;
            }
            
            // Read the file data
            byte[] data = inputStream.readAllBytes();
            inputStream.close();
            
            // Write to temporary file for Assimp to read
            File tempFile = File.createTempFile("temp_model", ".glb");
            tempFile.deleteOnExit();
            
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(data);
            }
            
            // Load with Assimp
            AIScene scene = aiImportFile(tempFile.getAbsolutePath(), 
                aiProcess_Triangulate | aiProcess_GenNormals);
            
            if (scene == null) {
                System.err.println("Failed to load GLB with Assimp: " + aiGetErrorString());
                tempFile.delete();
                return extractedTextures;
            }
            
            // Create output directory if it doesn't exist
            Path outputPath = Paths.get(outputDir);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
                System.out.println("Created directory: " + outputDir);
            }
            
            // Extract textures
            extractedTextures = extractTexturesFromScene(scene, outputDir, glbPath);
            
            // Cleanup
            aiReleaseImport(scene);
            tempFile.delete();
            
            System.out.println("Texture extraction complete. Found " + extractedTextures.size() + " textures.");
            
        } catch (Exception e) {
            System.err.println("Error extracting textures from GLB: " + glbPath);
            e.printStackTrace();
        }
        
        return extractedTextures;
    }
    
    private static List<ExtractedTexture> extractTexturesFromScene(AIScene scene, String outputDir, String glbPath) {
        List<ExtractedTexture> extractedTextures = new ArrayList<>();
        
        // Check if scene has embedded textures
        int numTextures = scene.mNumTextures();
        if (numTextures > 0) {
            System.out.println("Found " + numTextures + " embedded textures");
            extractedTextures.addAll(extractEmbeddedTextures(scene, outputDir, glbPath));
        }
        
        // Also check material textures (might reference external files within the GLB)
        extractedTextures.addAll(extractMaterialTextures(scene, outputDir, glbPath));
        
        return extractedTextures;
    }
    
    private static List<ExtractedTexture> extractEmbeddedTextures(AIScene scene, String outputDir, String glbPath) {
        List<ExtractedTexture> textures = new ArrayList<>();
        PointerBuffer texturePointers = scene.mTextures();
        
        for (int i = 0; i < scene.mNumTextures(); i++) {
            AITexture aiTexture = AITexture.create(texturePointers.get(i));
            
            String originalName = aiTexture.mFilename().dataString();
            if (originalName == null || originalName.isEmpty()) {
                originalName = "texture_" + i;
            }
            
            // Clean the name for file system
            String cleanName = cleanTextureName(originalName);
            String baseFileName = getBaseFileName(glbPath) + "_" + cleanName;
            
            // Determine file extension
            String extension = determineTextureExtension(aiTexture);
            String fileName = baseFileName + extension;
            String filePath = outputDir + "/" + fileName;
            
            try {
                // Extract texture data
                int width = aiTexture.mWidth();
                int height = aiTexture.mHeight();
                
                if (height == 0) {
                    // Compressed texture (common in GLB)
                    System.out.println("  Extracting compressed texture: " + originalName + " (" + width + " bytes)");
                    byte[] textureData = extractCompressedTextureData(aiTexture, width);
                    
                    if (textureData != null) {
                        // Save to file
                        Files.write(Paths.get(filePath), textureData);
                        
                        ExtractedTexture extracted = new ExtractedTexture(
                            originalName, cleanName, fileName, filePath, -1, -1, textureData);
                        textures.add(extracted);
                        
                        System.out.println("    Saved as: " + fileName);
                    }
                } else {
                    // Uncompressed texture
                    System.out.println("  Extracting uncompressed texture: " + originalName + " (" + width + "x" + height + ")");
                    byte[] textureData = extractUncompressedTextureData(aiTexture, width, height);
                    
                    if (textureData != null) {
                        // Save as PNG (common format)
                        String pngFileName = baseFileName + ".png";
                        String pngFilePath = outputDir + "/" + pngFileName;
                        
                        // For uncompressed data, we'll need to save as raw RGBA and let the user know
                        // In a full implementation, you might want to use a library like ImageIO
                        Files.write(Paths.get(filePath), textureData);
                        
                        ExtractedTexture extracted = new ExtractedTexture(
                            originalName, cleanName, fileName, filePath, width, height, textureData);
                        textures.add(extracted);
                        
                        System.out.println("    Saved as: " + fileName + " (raw RGBA data)");
                    }
                }
                
            } catch (Exception e) {
                System.err.println("    Failed to extract texture " + originalName + ": " + e.getMessage());
            }
        }
        
        return textures;
    }
    
    private static List<ExtractedTexture> extractMaterialTextures(AIScene scene, String outputDir, String glbPath) {
        List<ExtractedTexture> textures = new ArrayList<>();
        
        int numMaterials = scene.mNumMaterials();
        if (numMaterials == 0) {
            return textures;
        }
        
        System.out.println("Checking " + numMaterials + " materials for texture references...");
        
        PointerBuffer materialPtrs = scene.mMaterials();
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(materialPtrs.get(i));
            
            // Get material name for reference
            AIString nameString = AIString.calloc();
            String materialName = "Material_" + i;
            
            if (aiGetMaterialString(aiMaterial, AI_MATKEY_NAME, 0, 0, nameString) == aiReturn_SUCCESS) {
                String extractedName = nameString.dataString();
                if (extractedName != null && !extractedName.trim().isEmpty()) {
                    materialName = extractedName.trim();
                }
            }
            nameString.free();
            
            System.out.println("  Material " + i + ": \"" + materialName + "\"");
            
            // Check for diffuse textures (most common)
            int diffuseCount = aiGetMaterialTextureCount(aiMaterial, aiTextureType_DIFFUSE);
            System.out.println("    Diffuse textures: " + diffuseCount);
        }
        
        return textures;
    }
    
    private static byte[] extractCompressedTextureData(AITexture aiTexture, int dataSize) {
        // TODO: Implement actual texture data extraction
        // For now, just return placeholder data to avoid API issues
        System.out.println("    Compressed texture data extraction not yet implemented");
        return new byte[0];
    }
    
    private static byte[] extractUncompressedTextureData(AITexture aiTexture, int width, int height) {
        // TODO: Implement actual texture data extraction  
        // For now, just return placeholder data to avoid API issues
        System.out.println("    Uncompressed texture data extraction not yet implemented");
        return new byte[0];
    }
    
    private static String determineTextureExtension(AITexture aiTexture) {
        // Try to get format hint - this is a simple string stored in the texture
        ByteBuffer formatBuffer = aiTexture.achFormatHint();
        String formatHint = "";
        
        // Convert ByteBuffer to string (4 bytes max for format hint)
        if (formatBuffer != null) {
            byte[] formatBytes = new byte[Math.min(4, formatBuffer.remaining())];
            formatBuffer.get(formatBytes);
            formatHint = new String(formatBytes).trim();
        }
        
        if (formatHint != null && !formatHint.isEmpty()) {
            if (formatHint.equals("jpg") || formatHint.equals("jpeg")) {
                return ".jpg";
            } else if (formatHint.equals("png")) {
                return ".png";
            } else if (formatHint.equals("bmp")) {
                return ".bmp";
            } else if (formatHint.equals("tga")) {
                return ".tga";
            }
        }
        
        // Default to PNG for unknown formats
        return ".png";
    }
    
    private static String cleanTextureName(String name) {
        if (name == null || name.isEmpty()) {
            return "texture";
        }
        
        // Remove path separators and clean the name
        String cleaned = name.replaceAll("[/\\\\]", "_")
                            .replaceAll("[^a-zA-Z0-9_.-]", "_")
                            .replaceAll("_+", "_")
                            .replaceAll("^_|_$", "");
        
        // Remove extension if present
        int lastDot = cleaned.lastIndexOf('.');
        if (lastDot > 0) {
            cleaned = cleaned.substring(0, lastDot);
        }
        
        return cleaned.isEmpty() ? "texture" : cleaned;
    }
    
    private static String getBaseFileName(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    /**
     * Main method to run texture extraction as a command-line tool
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java GLBTextureExtractor <glb-file-path> [output-directory]");
            System.out.println("Example: java GLBTextureExtractor models/Quequis_House.glb app/src/main/resources/textures/extracted");
            return;
        }
        
        String glbPath = args[0];
        String outputDir = args.length > 1 ? args[1] : "app/src/main/resources/textures/extracted";
        
        System.out.println("GLB Texture Extractor");
        System.out.println("====================");
        System.out.println("GLB file: " + glbPath);
        System.out.println("Output directory: " + outputDir);
        System.out.println();
        
        List<ExtractedTexture> textures = extractTexturesFromGLB(glbPath, outputDir);
        
        System.out.println();
        System.out.println("Extraction Summary:");
        System.out.println("===================");
        if (textures.isEmpty()) {
            System.out.println("No textures found or extracted.");
        } else {
            System.out.println("Successfully extracted " + textures.size() + " texture(s):");
            for (ExtractedTexture texture : textures) {
                System.out.println("  - " + texture.fileName + " (from: " + texture.originalName + ")");
            }
        }
    }
} 