package mystuff.utils;

import org.lwjgl.opengl.GL11;

public class FogRenderer {
    // Fog modes
    public enum FogMode {
        LINEAR,     // Linear fog (most efficient)
        EXP,        // Exponential fog
        EXP2       // Exponential squared fog (more realistic but more expensive)
    }
    
    // Fog parameters
    private static FogMode currentMode = FogMode.LINEAR;
    private static float[] fogColor = {0.6f, 0.7f, 0.8f, 1.0f}; // Sky blue-gray color
    private static float fogDensity = 0.03f;
    private static float fogStart = 0.0f;
    private static float fogEnd = 64.0f;
    private static boolean fogEnabled = true;
    
    /**
     * Enables fog rendering with current settings
     */
    public static void enable() {
        if (!fogEnabled) return;
        
        // Enable fog
        GL11.glEnable(GL11.GL_FOG);
        
        // Set fog color
        GL11.glFogfv(GL11.GL_FOG_COLOR, fogColor);
        
        // Set fog mode and parameters
        switch (currentMode) {
            case LINEAR:
                GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
                GL11.glFogf(GL11.GL_FOG_START, fogStart);
                GL11.glFogf(GL11.GL_FOG_END, fogEnd);
                break;
            case EXP:
                GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
                GL11.glFogf(GL11.GL_FOG_DENSITY, fogDensity);
                break;
            case EXP2:
                GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP2);
                GL11.glFogf(GL11.GL_FOG_DENSITY, fogDensity);
                break;
        }
        
        // Set fog hint for quality/performance trade-off
        GL11.glHint(GL11.GL_FOG_HINT, GL11.GL_FASTEST);
    }
    
    /**
     * Disables fog rendering
     */
    public static void disable() {
        GL11.glDisable(GL11.GL_FOG);
    }
    
    /**
     * Updates fog parameters
     */
    public static void updateFog(FogMode mode, float[] color, float density, float start, float end) {
        currentMode = mode;
        System.arraycopy(color, 0, fogColor, 0, 4);
        fogDensity = density;
        fogStart = start;
        fogEnd = end;
        
        // Re-enable fog to apply new settings
        if (fogEnabled) {
            enable();
        }
    }
    
    /**
     * Sets the fog color
     */
    public static void setFogColor(float r, float g, float b, float a) {
        fogColor[0] = r;
        fogColor[1] = g;
        fogColor[2] = b;
        fogColor[3] = a;
        if (fogEnabled) {
            GL11.glFogfv(GL11.GL_FOG_COLOR, fogColor);
        }
    }
    
    /**
     * Sets the fog distance parameters for linear fog
     */
    public static void setFogDistance(float start, float end) {
        fogStart = start;
        fogEnd = end;
        if (fogEnabled && currentMode == FogMode.LINEAR) {
            GL11.glFogf(GL11.GL_FOG_START, start);
            GL11.glFogf(GL11.GL_FOG_END, end);
        }
    }
    
    /**
     * Sets the fog density for exponential fog modes
     */
    public static void setFogDensity(float density) {
        fogDensity = density;
        if (fogEnabled && (currentMode == FogMode.EXP || currentMode == FogMode.EXP2)) {
            GL11.glFogf(GL11.GL_FOG_DENSITY, density);
        }
    }
    
    /**
     * Sets the fog mode
     */
    public static void setFogMode(FogMode mode) {
        if (currentMode != mode) {
            currentMode = mode;
            if (fogEnabled) {
                enable(); // Re-enable fog with new mode
            }
        }
    }
    
    /**
     * Toggles fog on/off
     */
    public static void toggleFog() {
        fogEnabled = !fogEnabled;
        if (fogEnabled) {
            enable();
        } else {
            disable();
        }
    }
    
    /**
     * Returns whether fog is currently enabled
     */
    public static boolean isFogEnabled() {
        return fogEnabled;
    }
    
    /**
     * Returns the current fog mode
     */
    public static FogMode getCurrentMode() {
        return currentMode;
    }
    
    /**
     * Returns the current fog color
     */
    public static float[] getFogColor() {
        return fogColor.clone();
    }
    
    /**
     * Returns the current fog density
     */
    public static float getFogDensity() {
        return fogDensity;
    }
    
    /**
     * Returns the current fog start distance
     */
    public static float getFogStart() {
        return fogStart;
    }
    
    /**
     * Returns the current fog end distance
     */
    public static float getFogEnd() {
        return fogEnd;
    }
} 