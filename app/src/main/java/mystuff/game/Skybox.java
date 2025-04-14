package mystuff.game;

import org.lwjgl.opengl.GL11;
import mystuff.utils.TextureLoader;

public class Skybox {
    private static final float SIZE = 5000f;  // Size of the skybox
    private int textureID = -1;

    public void init() {
        // Load the skybox texture
        textureID = TextureLoader.loadTexture("resources/textures/Skyboxes/BlueSkySkybox.png");
        if (textureID != -1) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            System.out.println("Skybox texture loaded successfully!");
        } else {
            System.err.println("Failed to load skybox texture!");
        }
    }

    public void render() {
        if (textureID == -1) return;

        // Save current OpenGL state
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glPushMatrix();
        
        // Disable depth writing so skybox is always in background
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        // Front face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(-SIZE, -SIZE, SIZE);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f(SIZE, -SIZE, SIZE);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f(SIZE, SIZE, SIZE);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(-SIZE, SIZE, SIZE);
        GL11.glEnd();

        // Back face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f(-SIZE, -SIZE, -SIZE);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f(-SIZE, SIZE, -SIZE);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(SIZE, SIZE, -SIZE);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(SIZE, -SIZE, -SIZE);
        GL11.glEnd();

        // Top face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(-SIZE, SIZE, -SIZE);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(-SIZE, SIZE, SIZE);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f(SIZE, SIZE, SIZE);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f(SIZE, SIZE, -SIZE);
        GL11.glEnd();

        // Bottom face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f(-SIZE, -SIZE, -SIZE);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(SIZE, -SIZE, -SIZE);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(SIZE, -SIZE, SIZE);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f(-SIZE, -SIZE, SIZE);
        GL11.glEnd();

        // Right face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f(SIZE, -SIZE, -SIZE);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f(SIZE, SIZE, -SIZE);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(SIZE, SIZE, SIZE);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(SIZE, -SIZE, SIZE);
        GL11.glEnd();

        // Left face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(-SIZE, -SIZE, -SIZE);
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f(-SIZE, -SIZE, SIZE);
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f(-SIZE, SIZE, SIZE);
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(-SIZE, SIZE, -SIZE);
        GL11.glEnd();

        // Restore OpenGL state
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    public void cleanup() {
        if (textureID != -1) {
            GL11.glDeleteTextures(textureID);
            textureID = -1;
        }
    }
} 