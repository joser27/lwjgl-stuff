package mystuff.utils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.stream.Stream;

public class TextureMatcher {
    
    // Material type classification
    public enum MaterialType {
        TEXTURED,    // Normal textured material
        GLASS,       // Transparent/translucent material
        MIRROR,      // Reflective material
        COLOR,       // Solid color material
        EMISSIVE     // Light-emitting material
    }
    
    // Material information container
    public static class MaterialInfo {
        public final String texturePath;
        public final MaterialType type;
        public final float[] color;  // RGB color for non-textured materials
        public final float alpha;    // Alpha transparency (0.0 = transparent, 1.0 = opaque)
        
        public MaterialInfo(String texturePath, MaterialType type, float[] color, float alpha) {
            this.texturePath = texturePath;
            this.type = type;
            this.color = color != null ? color : new float[]{1.0f, 1.0f, 1.0f};
            this.alpha = alpha;
        }
        
        public MaterialInfo(String texturePath) {
            this(texturePath, MaterialType.TEXTURED, null, 1.0f);
        }
    }
    
    private static Map<String, String> textureCache = new HashMap<>();
    private static boolean cacheInitialized = false;
    
    // Common texture file extensions
    private static final String[] TEXTURE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".tga"};
    
    // Texture folder paths
    private static final String TEXTURE_FOLDER = "textures/taco_place/";
    
    /**
     * Initialize the texture cache by scanning the house folder
     */
    public static void initializeTextureCache() {
        if (cacheInitialized) return;
        
        System.out.println("Scanning texture folder...");
        
        // Scan house folder for textures
        System.out.println("Scanning for textures in " + TEXTURE_FOLDER + "...");
        scanTexturesInFolder(TEXTURE_FOLDER);
        
        System.out.println("Texture cache initialized with " + textureCache.size() + " textures");
        cacheInitialized = true;
    }
    
    /**
     * Dynamically scan a folder for texture files
     */
    private static void scanTexturesInFolder(String folderPath) {
        try {
            // Get the resource URL for the folder
            URL resourceUrl = TextureMatcher.class.getClassLoader().getResource(folderPath);
            if (resourceUrl == null) {
                System.out.println("  Folder not found: " + folderPath);
                return;
            }
            
            URI uri = resourceUrl.toURI();
            Path folderPath_nio;
            FileSystem fileSystem = null;
            
            // Handle both file system and jar file scenarios
            if (uri.getScheme().equals("jar")) {
                // We're running from a JAR file
                fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                folderPath_nio = fileSystem.getPath(folderPath);
            } else {
                // We're running from the file system (development)
                folderPath_nio = Paths.get(uri);
            }
            
            // Scan the directory for texture files
            try (Stream<Path> files = Files.walk(folderPath_nio, 1)) {
                files.filter(Files::isRegularFile)
                     .forEach(file -> {
                         String fileName = file.getFileName().toString();
                         if (isTextureFile(fileName)) {
                             String fullPath = folderPath + fileName;
                             String cleanName = cleanTextureName(fileName);
                             
                             // Add the texture to cache
                             textureCache.put(cleanName, fullPath);
                             System.out.println("  Found texture: \"" + fileName + "\" -> \"" + cleanName + "\"");
                         }
                     });
            }
            
            // Close the file system if we opened one
            if (fileSystem != null) {
                fileSystem.close();
            }
            
        } catch (Exception e) {
            System.err.println("Error scanning folder " + folderPath + ": " + e.getMessage());
            // Fallback to hardcoded scanning for this folder
            fallbackScanFolder(folderPath);
        }
    }
    
    /**
     * Fallback method using hardcoded patterns if dynamic scanning fails
     */
    private static void fallbackScanFolder(String folderPath) {
        System.out.println("  Using fallback scanning for " + folderPath);
        
        if (folderPath.equals(TEXTURE_FOLDER)) {
            // Fallback patterns for house folder
            String[] knownTextures = {
                "Wood.png", "Windows.png", "Wall_W.png", "Tile.jpg", "Table.png",
                "Roof_tiles.jpg", "Road.jpg", "Plaster.png", "Plaster_01.png", "Plaster_02.png",
                "Metal.jpg", "Metal_01.png", "Metal_02.jpg", "Metal_03.jpg", "MetalPlates.jpg",
                "Marble.jpg", "Houses.png", "Houses_01.png", "Grass.png", "Ground_dirt.jpg",
                "brick_wall.png", "brick_stone.jpg", "brick_modern.jpg", "Office_Ceiling.jpg",
                "Washbasin_M.png", "Toilet.png", "Shelf_B.png", "Shelf_FR.png", "Shelf_FR_I.png",
                "Refrigerator.png", "Refrigerator_01.png", "Refrijerator_M.png", "Oven.png",
                "Light.png", "Light_Emissor.png", "Grill.png", "Ventilation.png", "Showcase.png",
                "Soda_fountain.001.png", "Phone.png", "Paintings.png", "Napkinholder.png",
                "Knife.png", "cardboard_boxes.png", "Waffle_maker.png", "plants_flowers.png",
                "cactus.png", "Plants_01.png", "Plants_02.png", "extinguisher.png"
            };
            
            for (String texture : knownTextures) {
                if (textureExists(TEXTURE_FOLDER + texture)) {
                    String cleanName = cleanTextureName(texture);
                    textureCache.put(cleanName, TEXTURE_FOLDER + texture);
                    System.out.println("  Found (fallback): \"" + texture + "\" -> \"" + cleanName + "\"");
                }
            }
        }
    }
    
    /**
     * Check if a file is a texture file based on its extension
     */
    private static boolean isTextureFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        for (String extension : TEXTURE_EXTENSIONS) {
            if (lowerName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Calculate match score between material name and texture name
     */
    private static int calculateMatchScore(String materialName, String textureName) {
        if (materialName.equals(textureName)) return 100;
        
        int score = 0;
        
        // Exact substring match
        if (textureName.contains(materialName)) score += 50;
        if (materialName.contains(textureName)) score += 40;
        
        // Word matching
        String[] materialWords = materialName.split("[_\\s]+");
        String[] textureWords = textureName.split("[_\\s]+");
        
        for (String matWord : materialWords) {
            if (matWord.length() < 2) continue;
            for (String texWord : textureWords) {
                if (texWord.equals(matWord)) {
                    score += 15;
                } else if (texWord.contains(matWord) || matWord.contains(texWord)) {
                    score += 8;
                }
            }
        }
        
        return score;
    }
    
    /**
     * Clean texture filename for matching
     */
    private static String cleanTextureName(String filename) {
        if (filename == null) return "default";
        
        // Remove extension
        String name = filename.toLowerCase();
        for (String ext : TEXTURE_EXTENSIONS) {
            if (name.endsWith(ext)) {
                name = name.substring(0, name.length() - ext.length());
                break;
            }
        }
        
        // Clean up the name similar to material cleaning
        return name.replaceAll("[^a-z0-9_]", "_")
                  .replaceAll("_+", "_")
                  .replaceAll("^_|_$", "");
    }
    
    /**
     * Check if a texture file exists in resources
     */
    private static boolean textureExists(String path) {
        InputStream stream = TextureMatcher.class.getClassLoader().getResourceAsStream(path);
        if (stream != null) {
            try {
                stream.close();
                return true;
            } catch (Exception e) {
                // Ignore
            }
        }
        return false;
    }
    
    /**
     * Get fallback texture path
     */
    private static String getFallbackTexture() {
        return "textures/missing_texture.jpg";
    }
    
    /**
     * Get all available textures for debugging
     */
    public static Map<String, String> getAllTextures() {
        if (!cacheInitialized) {
            initializeTextureCache();
        }
        return new HashMap<>(textureCache);
    }

    /**
     * Get material information including type and rendering properties
     */
    public static MaterialInfo getMaterialInfo(String materialName) {
        if (!cacheInitialized) {
            initializeTextureCache();
        }
        
        if (materialName == null || materialName.trim().isEmpty()) {
            return new MaterialInfo(getFallbackTexture());
        }
        
        String cleanMaterialName = materialName.toLowerCase().trim();
        
        // Detect special material types
        MaterialType detectedType = detectMaterialType(cleanMaterialName);
        
        switch (detectedType) {
            case GLASS:
                return createGlassMaterial(cleanMaterialName);
            case MIRROR:
                return createMirrorMaterial(cleanMaterialName);
            case COLOR:
                return createColorMaterial(cleanMaterialName);
            case EMISSIVE:
                return createEmissiveMaterial(cleanMaterialName);
            default:
                // TEXTURED - find best texture match
                String texturePath = findBestTextureMatch(cleanMaterialName);
                return new MaterialInfo(texturePath);
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public static String findBestTexture(String materialName) {
        return getMaterialInfo(materialName).texturePath;
    }
    
    /**
     * Detect material type based on name patterns
     */
    private static MaterialType detectMaterialType(String materialName) {
        // Glass materials
        if (materialName.contains("glass") || materialName.contains("window") || 
            materialName.contains("transparent") || materialName.contains("crystal")) {
            return MaterialType.GLASS;
        }
        
        // Mirror materials
        if (materialName.contains("mirror") || materialName.contains("reflection") ||
            materialName.contains("chrome") || materialName.contains("shiny")) {
            return MaterialType.MIRROR;
        }
        
        // Light emissive materials
        if (materialName.contains("light") || materialName.contains("lamp") ||
            materialName.contains("emission") || materialName.contains("glow") ||
            materialName.contains("emissor")) {
            return MaterialType.EMISSIVE;
        }
        
        // Color materials (solid colors)
        if (materialName.equals("black") || materialName.equals("white") || 
            materialName.equals("red") || materialName.equals("blue") || 
            materialName.equals("green") || materialName.equals("yellow") ||
            materialName.equals("colorwhite") || materialName.equals("color_g") ||
            materialName.contains("color") && !materialName.contains("_")) {
            return MaterialType.COLOR;
        }
        
        // Default to textured
        return MaterialType.TEXTURED;
    }
    
    /**
     * Create glass material with transparency
     */
    private static MaterialInfo createGlassMaterial(String materialName) {
        // Try to find a glass texture first
        String texturePath = findBestTextureMatch(materialName);
        
        // If no texture found, use transparent properties
        if (texturePath.equals(getFallbackTexture())) {
            // Use subtle blue-tinted glass color
            float[] glassColor = {0.8f, 0.9f, 1.0f}; // Light blue tint
            return new MaterialInfo(null, MaterialType.GLASS, glassColor, 0.3f);
        }
        
        // Use texture with transparency
        return new MaterialInfo(texturePath, MaterialType.GLASS, null, 0.5f);
    }
    
    /**
     * Create mirror material with reflective properties
     */
    private static MaterialInfo createMirrorMaterial(String materialName) {
        // Try to find a metallic texture
        String texturePath = findBestTextureMatch(materialName);
        
        if (texturePath.equals(getFallbackTexture())) {
            // Use reflective silver color
            float[] mirrorColor = {0.9f, 0.9f, 0.9f}; // Silver
            return new MaterialInfo(null, MaterialType.MIRROR, mirrorColor, 1.0f);
        }
        
        return new MaterialInfo(texturePath, MaterialType.MIRROR, null, 1.0f);
    }
    
    /**
     * Create color material with solid color
     */
    private static MaterialInfo createColorMaterial(String materialName) {
        float[] color = parseColorFromName(materialName);
        return new MaterialInfo(null, MaterialType.COLOR, color, 1.0f);
    }
    
    /**
     * Create emissive (light-emitting) material
     */
    private static MaterialInfo createEmissiveMaterial(String materialName) {
        // Try to find light texture first
        String texturePath = findBestTextureMatch(materialName);
        
        if (texturePath.equals(getFallbackTexture())) {
            // Use bright white/yellow for lights
            float[] lightColor = {1.0f, 1.0f, 0.8f}; // Warm white
            return new MaterialInfo(null, MaterialType.EMISSIVE, lightColor, 1.0f);
        }
        
        return new MaterialInfo(texturePath, MaterialType.EMISSIVE, null, 1.0f);
    }
    
    /**
     * Parse color from material name
     */
    private static float[] parseColorFromName(String materialName) {
        switch (materialName) {
            case "black": return new float[]{0.0f, 0.0f, 0.0f};
            case "white": case "colorwhite": return new float[]{1.0f, 1.0f, 1.0f};
            case "red": return new float[]{1.0f, 0.0f, 0.0f};
            case "green": case "color_g": return new float[]{0.0f, 1.0f, 0.0f};
            case "blue": return new float[]{0.0f, 0.0f, 1.0f};
            case "yellow": return new float[]{1.0f, 1.0f, 0.0f};
            case "gray": case "grey": return new float[]{0.5f, 0.5f, 0.5f};
            default: return new float[]{0.8f, 0.8f, 0.8f}; // Default light gray
        }
    }
    
    /**
     * Find the best matching texture for a material name (original logic)
     */
    private static String findBestTextureMatch(String materialName) {
        // 1. Try exact match first
        if (textureCache.containsKey(materialName)) {
            String match = textureCache.get(materialName);
            System.out.println("  Exact match: \"" + materialName + "\" -> \"" + match + "\"");
            return match;
        }
        
        // 2. Use fuzzy matching to find the best approximate match
        String bestMatch = null;
        int bestScore = 0;
        
        for (Map.Entry<String, String> entry : textureCache.entrySet()) {
            String textureName = entry.getKey();
            String texturePath = entry.getValue();
            
            int score = calculateMatchScore(materialName, textureName);
            if (score > bestScore && score > 15) { // Increased threshold for better quality matches
                bestScore = score;
                bestMatch = texturePath;
            }
        }
        
        if (bestMatch != null) {
            System.out.println("  Fuzzy match: \"" + materialName + "\" -> \"" + bestMatch + "\" (score: " + bestScore + ")");
            return bestMatch;
        }
        
        System.out.println("  No match found for: \"" + materialName + "\", using fallback");
        return getFallbackTexture();
    }
} 