package mystuff.utils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class TextureMatcher {
    
    private static Map<String, String> textureCache = new HashMap<>();
    private static boolean cacheInitialized = false;
    
    // Common texture file extensions
    private static final String[] TEXTURE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".tga"};
    
    // Texture folder path
    private static final String TEXTURE_FOLDER = "textures/house/";
    
    /**
     * Initialize the texture cache by scanning the house folder
     */
    public static void initializeTextureCache() {
        if (cacheInitialized) return;
        
        System.out.println("Scanning textures in " + TEXTURE_FOLDER + "...");
        
        // List of known texture files from the house folder
        // In a full implementation, this could be dynamically scanned
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
        
        // Build texture cache with cleaned names for matching
        for (String texture : knownTextures) {
            if (textureExists(TEXTURE_FOLDER + texture)) {
                String cleanName = cleanTextureName(texture);
                textureCache.put(cleanName, TEXTURE_FOLDER + texture);
                System.out.println("  Found texture: \"" + texture + "\" -> \"" + cleanName + "\"");
            }
        }
        
        System.out.println("Texture cache initialized with " + textureCache.size() + " textures");
        cacheInitialized = true;
    }
    
    /**
     * Find the best matching texture for a material name
     */
    public static String findBestTexture(String materialName) {
        if (!cacheInitialized) {
            initializeTextureCache();
        }
        
        if (materialName == null || materialName.trim().isEmpty()) {
            return getFallbackTexture();
        }
        
        String cleanMaterialName = materialName.toLowerCase().trim();
        
        // 1. Try exact match first
        if (textureCache.containsKey(cleanMaterialName)) {
            String match = textureCache.get(cleanMaterialName);
            System.out.println("  Exact match: \"" + materialName + "\" -> \"" + match + "\"");
            return match;
        }
        
        // 2. Try partial matches
        String bestMatch = null;
        int bestScore = 0;
        
        for (Map.Entry<String, String> entry : textureCache.entrySet()) {
            String textureName = entry.getKey();
            String texturePath = entry.getValue();
            
            int score = calculateMatchScore(cleanMaterialName, textureName);
            if (score > bestScore && score > 2) { // Minimum score threshold
                bestScore = score;
                bestMatch = texturePath;
            }
        }
        
        if (bestMatch != null) {
            System.out.println("  Fuzzy match: \"" + materialName + "\" -> \"" + bestMatch + "\" (score: " + bestScore + ")");
            return bestMatch;
        }
        
        // 3. Try common material type mapping
        String mappedTexture = getMappedTexture(cleanMaterialName);
        if (mappedTexture != null) {
            System.out.println("  Mapped match: \"" + materialName + "\" -> \"" + mappedTexture + "\"");
            return mappedTexture;
        }
        
        System.out.println("  No match found for: \"" + materialName + "\", using fallback");
        return getFallbackTexture();
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
     * Get mapped texture for common material types
     */
    private static String getMappedTexture(String materialName) {
        // Common material type mappings
        if (materialName.contains("wood") || materialName.contains("timber")) {
            return textureCache.get("wood");
        }
        if (materialName.contains("metal") || materialName.contains("steel") || materialName.contains("iron")) {
            return textureCache.get("metal");
        }
        if (materialName.contains("wall") || materialName.contains("brick")) {
            return textureCache.get("brick_wall");
        }
        if (materialName.contains("window") || materialName.contains("glass")) {
            return textureCache.get("windows");
        }
        if (materialName.contains("roof") || materialName.contains("tile")) {
            return textureCache.get("roof_tiles");
        }
        if (materialName.contains("floor") || materialName.contains("ground")) {
            return textureCache.get("tile");
        }
        if (materialName.contains("door") || materialName.contains("plank")) {
            return textureCache.get("wood");
        }
        
        return null;
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
} 