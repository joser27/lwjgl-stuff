package mystuff.game;

/**
 * Enum representing different game scenes/states
 */
public enum Scene {
    MENU("Main Menu"),
    PLAYING("Playing"),
    PAUSED("Paused"),
    SETTINGS("Settings"),
    GAME_OVER("Game Over"),
    LOADING("Loading");
    
    private final String displayName;
    
    Scene(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 