package mystuff.utils;

import static org.lwjgl.opengl.GL11.*;

public class FogRenderer {
    // Fog types for different atmospheric effects
    public enum FogType {
        NONE,
        LIGHT_MIST,      // Subtle atmospheric fog
        DENSE_FOG,       // Silent Hill style dense fog
        DARK_MIST,       // Dark, oppressive fog
        STORM_FOG,       // Dynamic, changing fog
        NIGHT_FOG        // Very dark, limited visibility
    }
    
    // Fog state
    private FogType currentFogType = FogType.NONE;
    private float fogStart = 5.0f;
    private float fogEnd = 30.0f;
    private float[] fogColor = {0.3f, 0.3f, 0.3f, 1.0f};
    
    // Dynamic fog properties
    private float fogIntensity = 1.0f;
    private float fogPulse = 0.0f;
    private boolean fogEnabled = false;
    
    // Horror-specific properties
    private float horrorIntensity = 0.0f; // 0.0 = normal, 1.0 = maximum horror
    private float playerFearLevel = 0.0f; // Player's current fear level
    
    public FogRenderer() {
        // Initialize with no fog
        disableFog();
    }
    
    /**
     * Set the fog type and configure it appropriately
     */
    public void setFogType(FogType type) {
        this.currentFogType = type;
        configureFogForType(type);
    }
    
    /**
     * Configure fog parameters based on the selected type
     */
    private void configureFogForType(FogType type) {
        switch (type) {
            case NONE:
                disableFog();
                break;
                
            case LIGHT_MIST:
                fogStart = 15.0f;
                fogEnd = 60.0f;
                fogColor = new float[]{0.6f, 0.6f, 0.65f, 1.0f}; // More visible light mist
                enableFog();
                break;
                
            case DENSE_FOG:
                fogStart = 5.0f;
                fogEnd = 25.0f;
                fogColor = new float[]{0.6f, 0.6f, 0.65f, 1.0f}; // Same color as light mist
                enableFog();
                break;
                
            case DARK_MIST:
                fogStart = 8.0f;
                fogEnd = 35.0f;
                fogColor = new float[]{0.2f, 0.2f, 0.25f, 1.0f}; // Slightly brighter dark mist
                enableFog();
                break;
                
            case STORM_FOG:
                fogStart = 3.0f;
                fogEnd = 20.0f;
                fogColor = new float[]{0.4f, 0.4f, 0.45f, 1.0f}; // More visible storm fog
                enableFog();
                break;
                
            case NIGHT_FOG:
                fogStart = 2.0f;
                fogEnd = 15.0f;
                fogColor = new float[]{0.15f, 0.15f, 0.2f, 1.0f}; // More visible night fog
                enableFog();
                break;
        }
    }
    
    /**
     * Enable fog with current settings
     */
    private void enableFog() {
        fogEnabled = true;
        glEnable(GL_FOG);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, fogStart);
        glFogf(GL_FOG_END, fogEnd);
        glFogfv(GL_FOG_COLOR, fogColor);
    }
    
    /**
     * Disable fog
     */
    public void disableFog() {
        fogEnabled = false;
        glDisable(GL_FOG);
    }
    
    /**
     * Update fog for dynamic effects (call this every frame)
     */
    public void update(float deltaTime) {
        if (!fogEnabled) return;
        
        // Update fog pulse for dynamic effects
        fogPulse += deltaTime * 2.0f;
        if (fogPulse > 6.28f) fogPulse -= 6.28f; // Keep in 0-2π range
        
        // Apply horror intensity effects
        applyHorrorEffects();
        
        // Apply dynamic fog changes for storm fog
        if (currentFogType == FogType.STORM_FOG) {
            applyStormEffects(deltaTime);
        }
        
        // Update OpenGL fog parameters
        updateFogParameters();
    }
    
    /**
     * Apply fog settings to OpenGL (call this before rendering)
     */
    public void applyFog() {
        if (!fogEnabled) {
            glDisable(GL_FOG);
            System.out.println("Fog disabled");
            return;
        }
        
        // Enable fog and set parameters
        glEnable(GL_FOG);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, fogStart);
        glFogf(GL_FOG_END, fogEnd);
        glFogfv(GL_FOG_COLOR, fogColor);
        
        // Set fog hint for performance
        glHint(GL_FOG_HINT, GL_FASTEST);
        
        System.out.println("Fog applied: " + currentFogType + " (start: " + fogStart + ", end: " + fogEnd + 
                          ", color: [" + fogColor[0] + ", " + fogColor[1] + ", " + fogColor[2] + "])");
    }
    
    /**
     * Apply horror-specific effects to the fog
     */
    private void applyHorrorEffects() {
        if (horrorIntensity <= 0.0f) return;
        
        // Make fog darker and closer as horror increases
        float horrorFactor = horrorIntensity * 0.8f;
        
        // Darken fog color
        fogColor[0] = Math.max(0.05f, fogColor[0] - horrorFactor * 0.3f);
        fogColor[1] = Math.max(0.05f, fogColor[1] - horrorFactor * 0.3f);
        fogColor[2] = Math.max(0.08f, fogColor[2] - horrorFactor * 0.2f);
        
        // Bring fog closer
        fogStart = Math.max(1.0f, fogStart - horrorFactor * 3.0f);
        fogEnd = Math.max(5.0f, fogEnd - horrorFactor * 10.0f);
    }
    
    /**
     * Apply storm-like dynamic effects
     */
    private void applyStormEffects(float deltaTime) {
        // Pulsing fog density
        float pulseFactor = (float) Math.sin(fogPulse) * 0.3f + 0.7f;
        
        // Vary fog start and end distances
        float baseStart = 3.0f;
        float baseEnd = 20.0f;
        
        fogStart = baseStart + (float) Math.sin(fogPulse * 0.5f) * 2.0f;
        fogEnd = baseEnd + (float) Math.sin(fogPulse * 0.3f) * 5.0f;
        
        // Vary fog color slightly
        float colorVariation = (float) Math.sin(fogPulse * 0.7f) * 0.1f;
        fogColor[0] = 0.2f + colorVariation;
        fogColor[1] = 0.2f + colorVariation;
        fogColor[2] = 0.25f + colorVariation;
    }
    
    /**
     * Update OpenGL fog parameters
     */
    private void updateFogParameters() {
        if (!fogEnabled) return;
        
        glFogf(GL_FOG_START, fogStart);
        glFogf(GL_FOG_END, fogEnd);
        glFogfv(GL_FOG_COLOR, fogColor);
    }
    
    /**
     * Set horror intensity (0.0 = normal, 1.0 = maximum horror)
     */
    public void setHorrorIntensity(float intensity) {
        this.horrorIntensity = Math.max(0.0f, Math.min(1.0f, intensity));
    }
    
    /**
     * Set player fear level (affects fog behavior)
     */
    public void setPlayerFearLevel(float fearLevel) {
        this.playerFearLevel = Math.max(0.0f, Math.min(1.0f, fearLevel));
    }
    
    /**
     * Get current horror intensity
     */
    public float getHorrorIntensity() {
        return horrorIntensity;
    }
    
    /**
     * Create a sudden fog change for jump scares or events
     */
    public void triggerFogEvent(FogType eventType, float duration) {
        // Store current fog type
        FogType previousType = currentFogType;
        
        // Switch to event fog
        setFogType(eventType);
        
        // Schedule return to previous fog
        // (You'd need a timer system for this in a real implementation)
    }
    
    /**
     * Get current fog visibility range
     */
    public float getVisibilityRange() {
        return fogEnd;
    }
    
    /**
     * Check if fog is currently enabled
     */
    public boolean isFogEnabled() {
        return fogEnabled;
    }
    
    /**
     * Get current fog type
     */
    public FogType getCurrentFogType() {
        return currentFogType;
    }
    
    /**
     * Get current fog color
     */
    public float[] getFogColor() {
        return fogColor.clone();
    }
    
    /**
     * Cleanup fog resources
     */
    public void cleanup() {
        disableFog();
    }
} 