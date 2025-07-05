package mystuff.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
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
        
        // Set texture parameters for high quality
        setTextureParameters();
        
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

                // Set high quality parameters
                setTextureParameters();

                // Upload the texture data with alpha channel preserved
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width.get(), height.get(), 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, imageData);
                
                // Generate mipmaps for better quality at different distances
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

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
     * Sets texture parameters for high quality rendering
     */
    private static void setTextureParameters() {
        // Use linear filtering for smooth, high-quality textures
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        // Use repeat mode for texture coordinates
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        
        // Enable anisotropic filtering for even better quality (if supported)
        if (GL11.glGetString(GL11.GL_EXTENSIONS).contains("GL_EXT_texture_filter_anisotropic")) {
            float maxAnisotropy = GL11.glGetFloat(0x84FF); // GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, 0x84FE, maxAnisotropy); // GL_TEXTURE_MAX_ANISOTROPY_EXT
        }
        
        // Hint to OpenGL that we prefer quality over speed
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
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