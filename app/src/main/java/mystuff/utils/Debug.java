package mystuff.utils;

public class Debug {
    private static boolean debugMode = false;
    private static boolean showPlayerInfo = false;
    private static boolean showBlockInfo = false;
    private static boolean showFPS = true;

    // Toggle methods
    public static void toggleDebugMode() {
        debugMode = !debugMode;
        DebugRenderer.getInstance().addMessage("Debug mode: " + (debugMode ? "ON" : "OFF"), 3.0f);
    }

    public static void togglePlayerInfo() {
        showPlayerInfo = !showPlayerInfo;
        DebugRenderer.getInstance().addMessage("Player info: " + (showPlayerInfo ? "ON" : "OFF"), 3.0f);
    }

    public static void toggleBlockInfo() {
        showBlockInfo = !showBlockInfo;
        DebugRenderer.getInstance().addMessage("Block info: " + (showBlockInfo ? "ON" : "OFF"), 3.0f);
    }

    public static void toggleFPS() {
        showFPS = !showFPS;
        DebugRenderer.getInstance().addMessage("FPS display: " + (showFPS ? "ON" : "OFF"), 3.0f);
    }

    // Getter methods
    public static boolean isDebugMode() {
        return debugMode;
    }

    public static boolean showPlayerInfo() {
        return debugMode && showPlayerInfo;
    }

    public static boolean showBlockInfo() {
        return debugMode && showBlockInfo;
    }

    public static boolean showFPS() {
        return showFPS;
    }
} 