package mystuff.utils;

import static org.lwjgl.opengl.GL11.*;

public class LightingManager {
    
    public enum LightingMode {
        DAY,           // Bright daylight
        NIGHT,         // Dark night with moonlight
        DUSK,          // Sunset/dusk lighting
        DAWN,          // Sunrise/dawn lighting
        HORROR_NIGHT   // Very dark horror night
    }
    
    private LightingMode currentMode = LightingMode.DAY;
    private boolean lightingEnabled = true;
    
    // Light properties for different modes
    private float[] lightPosition = {100.0f, 100.0f, 100.0f, 1.0f};
    private float[] lightAmbient = {0.3f, 0.3f, 0.3f, 1.0f};
    private float[] lightDiffuse = {0.8f, 0.8f, 0.8f, 1.0f};
    private float[] lightSpecular = {0.5f, 0.5f, 0.5f, 1.0f};
    
    // Background colors for different modes
    private float[] skyColor = {0.5f, 0.8f, 1.0f, 1.0f}; // Sky blue
    private float[] fogColor = {0.3f, 0.3f, 0.3f, 1.0f}; // Default fog color
    
    public LightingManager() {
        setLightingMode(LightingMode.DAY);
    }
    
    /**
     * Set the lighting mode and configure it appropriately
     */
    public void setLightingMode(LightingMode mode) {
        this.currentMode = mode;
        configureLightingForMode(mode);
    }
    
    /**
     * Configure lighting parameters based on the selected mode
     */
    private void configureLightingForMode(LightingMode mode) {
        switch (mode) {
            case DAY:
                // Bright daylight
                lightPosition = new float[]{100.0f, 100.0f, 100.0f, 1.0f};
                lightAmbient = new float[]{0.3f, 0.3f, 0.3f, 1.0f};
                lightDiffuse = new float[]{0.8f, 0.8f, 0.8f, 1.0f};
                lightSpecular = new float[]{0.5f, 0.5f, 0.5f, 1.0f};
                skyColor = new float[]{0.5f, 0.8f, 1.0f, 1.0f}; // Sky blue
                fogColor = new float[]{0.6f, 0.6f, 0.65f, 1.0f}; // Light atmospheric fog
                break;
                
            case NIGHT:
                // Dark night with moonlight
                lightPosition = new float[]{50.0f, 80.0f, 50.0f, 1.0f}; // Moon position
                lightAmbient = new float[]{0.1f, 0.1f, 0.15f, 1.0f}; // Very dark ambient
                lightDiffuse = new float[]{0.2f, 0.2f, 0.3f, 1.0f}; // Blue-tinted moonlight
                lightSpecular = new float[]{0.1f, 0.1f, 0.15f, 1.0f}; // Dim specular
                skyColor = new float[]{0.05f, 0.05f, 0.1f, 1.0f}; // Dark blue night sky
                fogColor = new float[]{0.15f, 0.15f, 0.2f, 1.0f}; // Dark night fog
                break;
                
            case DUSK:
                // Sunset/dusk lighting
                lightPosition = new float[]{80.0f, 60.0f, 80.0f, 1.0f};
                lightAmbient = new float[]{0.2f, 0.15f, 0.1f, 1.0f}; // Warm ambient
                lightDiffuse = new float[]{0.6f, 0.4f, 0.2f, 1.0f}; // Orange sunset light
                lightSpecular = new float[]{0.3f, 0.2f, 0.1f, 1.0f};
                skyColor = new float[]{0.8f, 0.4f, 0.2f, 1.0f}; // Orange sunset sky
                fogColor = new float[]{0.4f, 0.25f, 0.15f, 1.0f}; // Warm dusk fog
                break;
                
            case DAWN:
                // Sunrise/dawn lighting
                lightPosition = new float[]{80.0f, 60.0f, 80.0f, 1.0f};
                lightAmbient = new float[]{0.15f, 0.2f, 0.25f, 1.0f}; // Cool ambient
                lightDiffuse = new float[]{0.4f, 0.6f, 0.8f, 1.0f}; // Blue dawn light
                lightSpecular = new float[]{0.2f, 0.3f, 0.4f, 1.0f};
                skyColor = new float[]{0.4f, 0.6f, 0.8f, 1.0f}; // Blue dawn sky
                fogColor = new float[]{0.25f, 0.35f, 0.45f, 1.0f}; // Cool dawn fog
                break;
                
            case HORROR_NIGHT:
                // Very dark horror night
                lightPosition = new float[]{30.0f, 60.0f, 30.0f, 1.0f};
                lightAmbient = new float[]{0.05f, 0.05f, 0.08f, 1.0f}; // Extremely dark ambient
                lightDiffuse = new float[]{0.1f, 0.1f, 0.15f, 1.0f}; // Very dim light
                lightSpecular = new float[]{0.05f, 0.05f, 0.08f, 1.0f}; // Minimal specular
                skyColor = new float[]{0.02f, 0.02f, 0.05f, 1.0f}; // Almost black sky
                fogColor = new float[]{0.05f, 0.05f, 0.08f, 1.0f}; // Very dark horror fog
                break;
        }
    }
    
    /**
     * Enable lighting with current settings
     */
    public void enableLighting() {
        lightingEnabled = true;
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);
        glEnable(GL_COLOR_MATERIAL);
        
        // Apply current light settings
        glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);
        glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
        glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
        glLightfv(GL_LIGHT0, GL_SPECULAR, lightSpecular);
    }
    
    /**
     * Disable lighting
     */
    public void disableLighting() {
        lightingEnabled = false;
        glDisable(GL_LIGHTING);
        glDisable(GL_LIGHT0);
        glDisable(GL_COLOR_MATERIAL);
    }
    
    /**
     * Apply lighting settings (call this before rendering)
     */
    public void applyLighting() {
        if (lightingEnabled) {
            enableLighting();
        } else {
            disableLighting();
        }
    }
    
    /**
     * Get the current sky color for background clearing
     */
    public float[] getSkyColor() {
        return skyColor;
    }
    
    /**
     * Get the current fog color for the lighting mode
     */
    public float[] getFogColor() {
        return fogColor;
    }
    
    /**
     * Get current lighting mode
     */
    public LightingMode getCurrentMode() {
        return currentMode;
    }
    
    /**
     * Check if lighting is enabled
     */
    public boolean isLightingEnabled() {
        return lightingEnabled;
    }
    
    /**
     * Toggle lighting on/off
     */
    public void toggleLighting() {
        lightingEnabled = !lightingEnabled;
    }
    
    /**
     * Cycle through lighting modes
     */
    public void cycleLightingMode() {
        LightingMode[] modes = LightingMode.values();
        int currentIndex = currentMode.ordinal();
        int nextIndex = (currentIndex + 1) % modes.length;
        setLightingMode(modes[nextIndex]);
    }
    
    /**
     * Set custom light position
     */
    public void setLightPosition(float x, float y, float z) {
        lightPosition[0] = x;
        lightPosition[1] = y;
        lightPosition[2] = z;
        lightPosition[3] = 1.0f;
        
        if (lightingEnabled) {
            glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);
        }
    }
    
    /**
     * Set custom ambient light
     */
    public void setAmbientLight(float r, float g, float b) {
        lightAmbient[0] = r;
        lightAmbient[1] = g;
        lightAmbient[2] = b;
        lightAmbient[3] = 1.0f;
        
        if (lightingEnabled) {
            glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
        }
    }
    
    /**
     * Set custom diffuse light
     */
    public void setDiffuseLight(float r, float g, float b) {
        lightDiffuse[0] = r;
        lightDiffuse[1] = g;
        lightDiffuse[2] = b;
        lightDiffuse[3] = 1.0f;
        
        if (lightingEnabled) {
            glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
        }
    }
} 