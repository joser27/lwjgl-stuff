package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.utils.OBJModelRenderer;
import mystuff.utils.DebugRenderer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class Beggar extends GameObject {
    private static final int ANIMATION_FRAMES = 24;
    private static final float ANIMATION_SPEED = 12.0f; // Frames per second (slightly slower for walking)
    private static final float WALK_SPEED = 2.0f; // Units per second for movement
    private static final float BEGGAR_SCALE = 1.1f; // Size of the beggar model
    
    private List<OBJModelRenderer> walkFrames;
    private float animationTime;
    private boolean isWalking;
    private boolean isLooping;
    private World world; // Reference to world for terrain height (can be null)
    private static final float GROUND_LEVEL = 17.0f; // Fixed ground level when no terrain
    private float walkDirection = 1.0f; // 1.0f for positive x, -1.0f for negative x
    private float walkRange = 50.0f; // How far to walk before turning around
    private float startX; // Starting position to calculate walk range
    
    public Beggar(float x, float y, float z, World world) {
        super(x, y, z);
        this.world = world;
        this.startX = x;
        this.isLooping = true; // Walking animation should loop by default
        this.isWalking = true; // Start walking immediately
        // Set initial rotation to face walking direction
        this.rotY = walkDirection > 0 ? 90.0f : 270.0f;
        loadWalkAnimation();
    }
    
    private void loadWalkAnimation() {
        walkFrames = new ArrayList<>();
        
        DebugRenderer.getInstance().addMessage("Loading beggar walk animation...", 3.0f);
        
        for (int i = 1; i <= ANIMATION_FRAMES; i++) {
            String framePath = String.format("animations/beggar/walk/walk%04d.obj", i);
            OBJModelRenderer frame = new OBJModelRenderer(framePath, "textures/beggar1st_albedo.png");
            
            if (frame.isLoaded()) {
                walkFrames.add(frame);
                DebugRenderer.getInstance().addMessage("✓ Loaded walk frame " + i + "/" + ANIMATION_FRAMES, 2.0f);
            } else {
                DebugRenderer.getInstance().addError("✗ Failed to load walk frame " + i + ": " + framePath, 3.0f);
            }
        }
        
        DebugRenderer.getInstance().addMessage("Walk animation loading complete. Loaded " + walkFrames.size() + " frames.", 3.0f);
        if (walkFrames.isEmpty()) {
            DebugRenderer.getInstance().addError("WARNING: No walk animation frames were loaded successfully!", 5.0f);
        }
    }
    
    public void startWalking() {
        isWalking = true;
        animationTime = 0.0f;
    }
    
    public void stopWalking() {
        isWalking = false;
    }
    
    public void setLooping(boolean looping) {
        this.isLooping = looping;
    }
    
    @Override
    public void render() {
        if (walkFrames.isEmpty()) {
            DebugRenderer.getInstance().addError("Beggar: No walk frames loaded! Rendering fallback cube.", 2.0f);
            renderFallbackCube();
            return;
        }
        
        // Calculate current frame based on animation time
        int currentFrame = 0;
        if (isWalking) {
            float frameTime = 1.0f / ANIMATION_SPEED;
            currentFrame = (int) (animationTime / frameTime);
            
            if (currentFrame >= walkFrames.size()) {
                if (isLooping) {
                    currentFrame = currentFrame % walkFrames.size();
                } else {
                    currentFrame = walkFrames.size() - 1;
                    isWalking = false;
                }
            }
        }
        
        // Always render at least the first frame, even when not walking
        currentFrame = Math.max(0, Math.min(currentFrame, walkFrames.size() - 1));
        
        // Render the current frame
        OBJModelRenderer currentModel = walkFrames.get(currentFrame);
        if (currentModel != null && currentModel.isLoaded()) {
            GL11.glPushMatrix();
            
            // Position the beggar
            GL11.glTranslatef(getX(), getY(), getZ());
            
            // Rotate to face walking direction
            GL11.glRotatef(getRotY(), 0.0f, 1.0f, 0.0f);
            
            // Render the current animation frame
            currentModel.render(BEGGAR_SCALE);
            
            GL11.glPopMatrix();
        } else {
            DebugRenderer.getInstance().addError("Beggar: Failed to render frame " + currentFrame + 
                             " (model: " + (currentModel != null) + 
                             ", loaded: " + (currentModel != null ? currentModel.isLoaded() : false) + ")", 2.0f);
        }
    }
    
    private void renderFallbackCube() {
        GL11.glPushMatrix();
        GL11.glTranslatef(getX(), getY(), getZ());
        GL11.glColor3f(0.5f, 0.3f, 0.1f); // Brown color for beggar
        
        // Draw a simple cube
        float size = BEGGAR_SCALE;
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
        if (isWalking) {
            animationTime += deltaTime;
            
            // Move along x-axis
            float deltaX = WALK_SPEED * walkDirection * deltaTime;
            x += deltaX;
            
            // Check if we've walked too far and need to turn around
            float distanceFromStart = Math.abs(x - startX);
            if (distanceFromStart >= walkRange) {
                walkDirection *= -1.0f; // Reverse direction
                // Update rotation to face the new walking direction
                rotY = walkDirection > 0 ? 90.0f : 270.0f;
            }
            
            // Update Y position to follow terrain height or use fixed ground level
            if (world != null) {
                float terrainHeight = world.getHeightAt(x, z);
                y = terrainHeight;
            } else {
                // Use fixed ground level when no terrain system
                y = GROUND_LEVEL;
            }
        }
    }
    
    public void cleanup() {
        if (walkFrames != null) {
            for (OBJModelRenderer frame : walkFrames) {
                if (frame != null) {
                    frame.cleanup();
                }
            }
            walkFrames.clear();
        }
    }
    
    // Getters for animation state
    public boolean isWalking() {
        return isWalking;
    }
    
    public boolean isLooping() {
        return isLooping;
    }
    
    public int getCurrentFrame() {
        if (walkFrames.isEmpty()) return 0;
        
        float frameTime = 1.0f / ANIMATION_SPEED;
        int frame = (int) (animationTime / frameTime);
        return Math.min(frame, walkFrames.size() - 1);
    }
    
    public int getTotalFrames() {
        return walkFrames.size();
    }
}
