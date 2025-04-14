package mystuff.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FontLoader {
    private static final int BITMAP_WIDTH = 512;
    private static final int BITMAP_HEIGHT = 512;
    private static final int FIRST_CHAR = 32;
    private static final int NUM_CHARS = 96;
    private static final float FONT_HEIGHT = 24.0f;
    
    private static ByteBuffer ttfData;
    private static STBTTFontinfo fontInfo;
    private static STBTTBakedChar.Buffer charData;
    private static int fontTexture = -1;
    private static float scale;
    
    public static void init(String fontPath) {
        try {
            // Load font file
            byte[] ttfBytes = Files.readAllBytes(Paths.get(fontPath));
            ttfData = BufferUtils.createByteBuffer(ttfBytes.length);
            ttfData.put(ttfBytes);
            ttfData.flip();

            // Initialize font
            fontInfo = STBTTFontinfo.create();
            if (!STBTruetype.stbtt_InitFont(fontInfo, ttfData)) {
                throw new RuntimeException("Failed to initialize font");
            }

            // Create bitmap for font
            ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_WIDTH * BITMAP_HEIGHT);
            charData = STBTTBakedChar.malloc(NUM_CHARS);
            
            STBTruetype.stbtt_BakeFontBitmap(ttfData, FONT_HEIGHT, bitmap, 
                BITMAP_WIDTH, BITMAP_HEIGHT, FIRST_CHAR, charData);

            // Convert bitmap to texture
            fontTexture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_ALPHA, BITMAP_WIDTH, BITMAP_HEIGHT, 
                0, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, bitmap);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            // Calculate scale
            try (MemoryStack stack = MemoryStack.stackPush()) {
                scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, FONT_HEIGHT);
            }

            System.out.println("Font loaded successfully: " + fontPath);
        } catch (IOException e) {
            System.err.println("Failed to load font: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void renderText(String text, float x, float y) {
        if (fontTexture == -1) return;

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xpos = stack.floats(x);
            FloatBuffer ypos = stack.floats(y);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);
            
            GL11.glBegin(GL11.GL_QUADS);
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;
                
                STBTruetype.stbtt_GetBakedQuad(charData, BITMAP_WIDTH, BITMAP_HEIGHT, 
                    c - FIRST_CHAR, xpos, ypos, q, true);
                
                GL11.glTexCoord2f(q.s0(), q.t0()); GL11.glVertex2f(q.x0(), q.y0());
                GL11.glTexCoord2f(q.s1(), q.t0()); GL11.glVertex2f(q.x1(), q.y0());
                GL11.glTexCoord2f(q.s1(), q.t1()); GL11.glVertex2f(q.x1(), q.y1());
                GL11.glTexCoord2f(q.s0(), q.t1()); GL11.glVertex2f(q.x0(), q.y1());
            }
            GL11.glEnd();
        }
        
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    public static void cleanup() {
        if (fontTexture != -1) {
            GL11.glDeleteTextures(fontTexture);
            fontTexture = -1;
        }
        if (charData != null) {
            charData.free();
            charData = null;
        }
    }
} 