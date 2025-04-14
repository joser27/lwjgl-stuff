package mystuff.utils;

import org.lwjgl.glfw.GLFW;

public class KeyboardManager {
    private static boolean[] currentKeys = new boolean[GLFW.GLFW_KEY_LAST];
    private static boolean[] previousKeys = new boolean[GLFW.GLFW_KEY_LAST];

    public static void update(long window) {
        // Update previous key states
        System.arraycopy(currentKeys, 0, previousKeys, 0, currentKeys.length);
        
        // Update current key states
        for (int i = 0; i < currentKeys.length; i++) {
            currentKeys[i] = GLFW.glfwGetKey(window, i) == GLFW.GLFW_PRESS;
        }
    }

    public static boolean isKeyPressed(int key) {
        return currentKeys[key];
    }

    public static boolean isKeyJustPressed(int key) {
        return currentKeys[key] && !previousKeys[key];
    }

    public static boolean isKeyReleased(int key) {
        return !currentKeys[key];
    }

    public static boolean isKeyJustReleased(int key) {
        return !currentKeys[key] && previousKeys[key];
    }
} 