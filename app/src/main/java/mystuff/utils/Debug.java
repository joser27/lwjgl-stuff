package mystuff.utils;

public class Debug {
    private static boolean debugMode = false;
    private static boolean showBoundingBoxes = false;
    private static boolean showPlayerInfo = false;
    private static boolean showBlockInfo = false;
    private static boolean showFPS = true;

    // Toggle methods
    public static void toggleDebugMode() {
        debugMode = !debugMode;
        System.out.println("Debug mode: " + (debugMode ? "ON" : "OFF"));
    }

    public static void toggleBoundingBoxes() {
        showBoundingBoxes = !showBoundingBoxes;
        System.out.println("Bounding boxes: " + (showBoundingBoxes ? "ON" : "OFF"));
    }

    public static void togglePlayerInfo() {
        showPlayerInfo = !showPlayerInfo;
        System.out.println("Player info: " + (showPlayerInfo ? "ON" : "OFF"));
    }

    public static void toggleBlockInfo() {
        showBlockInfo = !showBlockInfo;
        System.out.println("Block info: " + (showBlockInfo ? "ON" : "OFF"));
    }

    public static void toggleFPS() {
        showFPS = !showFPS;
        System.out.println("FPS display: " + (showFPS ? "ON" : "OFF"));
    }

    // Getter methods
    public static boolean isDebugMode() {
        return debugMode;
    }

    public static boolean showBoundingBoxes() {
        return debugMode && showBoundingBoxes;
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