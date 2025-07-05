package mystuff.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
        
        // Set texture parameters for low quality
        setLowQualityParameters();
        
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

        // Load from classpath
        InputStream inputStream = TextureLoader.class.getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            System.err.println("Texture file does not exist: " + path);
            return -1;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Read all bytes from the input stream
            byte[] imageBytes = inputStream.readAllBytes();
            ByteBuffer imageBuffer = BufferUtils.createByteBuffer(imageBytes.length);
            imageBuffer.put(imageBytes);
            imageBuffer.flip();

            // Load the image from the buffer
            STBImage.stbi_set_flip_vertically_on_load(false); // Disable vertical flip
            ByteBuffer imageData = STBImage.stbi_load_from_memory(imageBuffer, width, height, channels, 4);
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

                // Set low quality parameters
                setLowQualityParameters();

                // Upload the texture data with alpha channel preserved
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
     * Sets texture parameters for low quality/high performance rendering
     */
    private static void setLowQualityParameters() {
        // Use nearest neighbor filtering for that pixelated look
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        
        // Use repeat mode for texture coordinates
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        
        // Disable texture LOD bias for that classic PS1 look
        GL14.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, -2.0f);
        
        // Set texture priority to lowest for better memory management
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_PRIORITY, 0.1f);
        
        // Hint to OpenGL that we prefer speed over quality
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_FASTEST);
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