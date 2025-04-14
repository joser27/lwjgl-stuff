package mystuff.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TextureLoader {
    private static Map<String, Integer> textureCache = new HashMap<>();
    
    public static int createTestTexture() {
        // Create a 2x2 texture with red and white checkerboard pattern
        int width = 2;
        int height = 2;
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        
        // Red pixel
        pixels.put((byte) 255).put((byte) 0).put((byte) 0).put((byte) 255);
        // White pixel
        pixels.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
        // White pixel
        pixels.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
        // Red pixel
        pixels.put((byte) 255).put((byte) 0).put((byte) 0).put((byte) 255);
        
        pixels.flip();

        // Generate texture ID
        int textureID = GL11.glGenTextures();
        
        // Bind the texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        
        // Set texture parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        
        // Upload the texture data
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        
        return textureID;
    }

    /**
     * Loads a texture from a file and returns the OpenGL texture ID
     */
    public static int loadTexture(String path) {
        System.out.println("Attempting to load texture: " + path);
        
        // Check if texture is already loaded
        if (textureCache.containsKey(path)) {
            return textureCache.get(path);
        }

        // First verify the file exists
        if (!Files.exists(Paths.get(path))) {
            System.err.println("Texture file does not exist: " + path);
            return -1;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load the image
            ByteBuffer imageData = STBImage.stbi_load(path, width, height, channels, 4);
            if (imageData == null) {
                System.err.println("Failed to load texture: " + STBImage.stbi_failure_reason());
                return -1;
            }

            try {
                // Generate texture ID
                int textureID = GL11.glGenTextures();
                System.out.println("Generated texture ID: " + textureID);

                // Bind the texture
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

                // Upload the texture data
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width.get(), height.get(), 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, imageData);

                // Cache and return the texture ID
                textureCache.put(path, textureID);
                System.out.println("Successfully loaded texture: " + path);
                return textureID;

            } finally {
                STBImage.stbi_image_free(imageData);
            }
        } catch (Exception e) {
            System.err.println("Error loading texture: " + path);
            e.printStackTrace();
            return -1;
        }
    }
    

    /**
     * Binds a texture for rendering
     */
    public static void bindTexture(int textureID) {
        if (textureID <= 0) return;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
    }

    /**
     * Unbinds any bound texture
     */
    public static void unbindTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * Cleans up all loaded textures
     */
    public static void cleanup() {
        for (int textureID : textureCache.values()) {
            GL11.glDeleteTextures(textureID);
        }
        textureCache.clear();
    }
} 