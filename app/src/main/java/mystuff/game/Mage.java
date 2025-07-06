package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.utils.OBJModelRenderer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class Mage extends GameObject {
    private static final int ANIMATION_FRAMES = 25;
    private static final float ANIMATION_SPEED = 15.0f; // Frames per second
    
    private List<OBJModelRenderer> animationFrames;
    private float animationTime;
    private boolean isPlaying;
    private boolean isLooping;
    
    public Mage(float x, float y, float z) {
        super(x, y, z);
        loadAnimationFrames();
    }
    
    private void loadAnimationFrames() {
        animationFrames = new ArrayList<>();
        
        System.out.println("Loading mage attack animation...");
        
        for (int i = 1; i <= ANIMATION_FRAMES; i++) {
            String framePath = String.format("animations/attack/attack%04d.obj", i);
            System.out.println("Attempting to load: " + framePath);
            OBJModelRenderer frame = new OBJModelRenderer(framePath, "textures/mage_texture.png");
            
            if (frame.isLoaded()) {
                animationFrames.add(frame);
                System.out.println("✓ Loaded frame " + i + "/" + ANIMATION_FRAMES);
            } else {
                System.err.println("✗ Failed to load frame " + i + ": " + framePath);
            }
        }
        
        System.out.println("Animation loading complete. Loaded " + animationFrames.size() + " frames.");
        if (animationFrames.isEmpty()) {
            System.err.println("WARNING: No animation frames were loaded successfully!");
        }
    }
    
    public void playAttackAnimation() {
        isPlaying = true;
        animationTime = 0.0f;
    }
    
    public void stopAnimation() {
        isPlaying = false;
    }
    
    public void setLooping(boolean looping) {
        this.isLooping = looping;
    }
    
    @Override
    public void render() {
        if (animationFrames.isEmpty()) {
            System.err.println("Mage: No animation frames loaded! Rendering fallback cube.");
            renderFallbackCube();
            return;
        }
        
        // Calculate current frame based on animation time
        int currentFrame = 0;
        if (isPlaying) {
            float frameTime = 1.0f / ANIMATION_SPEED;
            currentFrame = (int) (animationTime / frameTime);
            
            if (currentFrame >= animationFrames.size()) {
                if (isLooping) {
                    currentFrame = currentFrame % animationFrames.size();
                } else {
                    currentFrame = animationFrames.size() - 1;
                    isPlaying = false;
                }
            }
        }
        
        // Always render at least the first frame, even when not animating
        currentFrame = Math.max(0, Math.min(currentFrame, animationFrames.size() - 1));
        
        // Render the current frame
        OBJModelRenderer currentModel = animationFrames.get(currentFrame);
        if (currentModel != null && currentModel.isLoaded()) {
            GL11.glPushMatrix();
            
            // Position the mage
            GL11.glTranslatef(getX(), getY(), getZ());
            
            // Rotate to face the camera (adjust as needed)
            GL11.glRotatef(0.0f, 1.0f, 0.0f, 0.0f);
            
            // Scale the mage
            float scale = 5.0f;
            

            
            // Render the current animation frame
            currentModel.render(scale);
            
            GL11.glPopMatrix();
        } else {
            System.err.println("Mage: Failed to render frame " + currentFrame + 
                             " (model: " + (currentModel != null) + 
                             ", loaded: " + (currentModel != null ? currentModel.isLoaded() : false) + ")");
        }
    }
    
    private void renderFallbackCube() {
        GL11.glPushMatrix();
        GL11.glTranslatef(getX(), getY(), getZ());
        GL11.glColor3f(1.0f, 0.0f, 0.0f); // Red color for visibility
        
        // Draw a simple cube
        float size = 5.0f;
        GL11.glBegin(GL11.GL_QUADS);
        // Front face
        GL11.glVertex3f(-size, -size, size);
        GL11.glVertex3f(size, -size, size);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(-size, size, size);
        // Back face
        GL11.glVertex3f(-size, -size, -size);
        GL11.glVertex3f(-size, size, -size);
        GL11.glVertex3f(size, size, -size);
        GL11.glVertex3f(size, -size, -size);
        // Top face
        GL11.glVertex3f(-size, size, -size);
        GL11.glVertex3f(-size, size, size);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(size, size, -size);
        // Bottom face
        GL11.glVertex3f(-size, -size, -size);
        GL11.glVertex3f(size, -size, -size);
        GL11.glVertex3f(size, -size, size);
        GL11.glVertex3f(-size, -size, size);
        // Right face
        GL11.glVertex3f(size, -size, -size);
        GL11.glVertex3f(size, size, -size);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(size, -size, size);
        // Left face
        GL11.glVertex3f(-size, -size, -size);
        GL11.glVertex3f(-size, -size, size);
        GL11.glVertex3f(-size, size, size);
        GL11.glVertex3f(-size, size, -size);
        GL11.glEnd();
        
        GL11.glColor3f(1.0f, 1.0f, 1.0f); // Reset color
        GL11.glPopMatrix();
    }
    
    @Override
    public void update(mystuff.engine.Window window, float deltaTime) {
        if (isPlaying) {
            animationTime += deltaTime;
        }
        this.x += 0.01f;
    }
    
    public void cleanup() {
        if (animationFrames != null) {
            for (OBJModelRenderer frame : animationFrames) {
                if (frame != null) {
                    frame.cleanup();
                }
            }
            animationFrames.clear();
        }
    }
    
    // Getters for animation state
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public boolean isLooping() {
        return isLooping;
    }
    
    public int getCurrentFrame() {
        if (animationFrames.isEmpty()) return 0;
        
        float frameTime = 1.0f / ANIMATION_SPEED;
        int frame = (int) (animationTime / frameTime);
        return Math.min(frame, animationFrames.size() - 1);
    }
    
    public int getTotalFrames() {
        return animationFrames.size();
    }
} 