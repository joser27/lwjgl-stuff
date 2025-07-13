package mystuff.game;

import mystuff.engine.Camera;
import mystuff.game.Player;

/**
 * Manages the current state of the game including player positions and game data
 */
public class GameState {
    private static GameState instance;
    
    // Player starting position
    private float playerStartX = 0.0f;
    private float playerStartY = 18.0f; // Ground level + 1
    private float playerStartZ = 0.0f;
    
    // Camera starting position (same as player)
    private float cameraStartX = 0.0f;
    private float cameraStartY = 18.0f;
    private float cameraStartZ = 0.0f;
    
    // Game time
    private float gameTime = 0.0f;
    
    // Game flags
    private boolean isNewGame = true;
    private boolean isGameOver = false;
    private boolean shouldResetPlayer = false;
    
    private GameState() {
        // Private constructor for singleton
    }
    
    public static GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }
    
    /**
     * Reset the game state for a new game
     */
    public void resetGame() {
        gameTime = 0.0f;
        isNewGame = true;
        isGameOver = false;
        shouldResetPlayer = true; // Mark that player should be reset
    }
    
    /**
     * Reset player and camera to starting positions
     */
    public void resetPlayerPositions(Player player, Camera camera) {
        if (player != null) {
            player.setPosition(playerStartX, playerStartY, playerStartZ);
            player.setRotation(0.0f, 0.0f, 0.0f); // Reset rotation (x, y, z)
        }
        
        if (camera != null) {
            camera.setPosition(cameraStartX, cameraStartY, cameraStartZ);
            camera.setPitch(0.0f); // Reset pitch
            camera.setYaw(0.0f);   // Reset yaw
        }
        
        // Clear the reset flag after resetting
        shouldResetPlayer = false;
    }
    
    /**
     * Update game time
     */
    public void updateGameTime(float deltaTime) {
        gameTime += deltaTime;
        if (isNewGame && gameTime > 0.1f) {
            isNewGame = false; // No longer a new game after first update
        }
    }
    
    /**
     * Set game over state
     */
    public void setGameOver(boolean gameOver) {
        this.isGameOver = gameOver;
    }
    
    // Getters
    public float getPlayerStartX() { return playerStartX; }
    public float getPlayerStartY() { return playerStartY; }
    public float getPlayerStartZ() { return playerStartZ; }
    public float getCameraStartX() { return cameraStartX; }
    public float getCameraStartY() { return cameraStartY; }
    public float getCameraStartZ() { return cameraStartZ; }
    public float getGameTime() { return gameTime; }
    public boolean isNewGame() { return isNewGame; }
    public boolean isGameOver() { return isGameOver; }
    public boolean shouldResetPlayer() { return shouldResetPlayer; }
    
    // Setters for starting positions (can be used for different spawn points)
    public void setPlayerStartPosition(float x, float y, float z) {
        playerStartX = x;
        playerStartY = y;
        playerStartZ = z;
    }
    
    public void setCameraStartPosition(float x, float y, float z) {
        cameraStartX = x;
        cameraStartY = y;
        cameraStartZ = z;
    }
} 