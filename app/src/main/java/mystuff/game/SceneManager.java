package mystuff.game;

import mystuff.engine.Window;
import mystuff.utils.DebugRenderer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Manages game scenes and transitions between them
 */
public class SceneManager {
    private static SceneManager instance;
    private Scene currentScene = Scene.MENU;
    private Scene previousScene = Scene.MENU;
    private Stack<Scene> sceneStack = new Stack<>();
    
    // Scene transition state
    private boolean isTransitioning = false;
    private float transitionProgress = 0.0f;
    private float transitionDuration = 0.5f; // seconds
    private Scene targetScene = null;
    
    // Scene-specific data
    private Map<Scene, Object> sceneData = new HashMap<>();
    
    // Menu state
    private int selectedMenuItem = 0;
    private String[] menuItems = {"Start Game", "Settings", "Exit"};
    
    // Settings state
    private int selectedSetting = 0;
    private String[] settingsItems = {"Graphics", "Audio", "Controls", "Back"};
    
    private SceneManager() {
        // Private constructor for singleton
    }
    
    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }
    
    /**
     * Change to a new scene
     */
    public void changeScene(Scene newScene) {
        if (currentScene == newScene) return;
        
        previousScene = currentScene;
        currentScene = newScene;
        
        DebugRenderer.getInstance().addMessage("Scene changed to: " + newScene.getDisplayName(), 2.0f);
        
        // Handle scene-specific initialization
        handleSceneChange(newScene);
    }
    
    /**
     * Push a scene onto the stack (for overlays like pause menu)
     */
    public void pushScene(Scene scene) {
        sceneStack.push(currentScene);
        changeScene(scene);
    }
    
    /**
     * Pop the top scene from the stack
     */
    public void popScene() {
        if (!sceneStack.isEmpty()) {
            Scene previousScene = sceneStack.pop();
            changeScene(previousScene);
        }
    }
    
    /**
     * Start a smooth transition to a new scene
     */
    public void transitionToScene(Scene newScene) {
        if (isTransitioning || currentScene == newScene) return;
        
        isTransitioning = true;
        transitionProgress = 0.0f;
        targetScene = newScene;
        previousScene = currentScene;
    }
    
    /**
     * Update scene manager (called each frame)
     */
    public void update(float deltaTime) {
        // Handle scene transitions
        if (isTransitioning) {
            transitionProgress += deltaTime / transitionDuration;
            
            if (transitionProgress >= 1.0f) {
                // Transition complete
                isTransitioning = false;
                transitionProgress = 0.0f;
                currentScene = targetScene;
                targetScene = null;
                handleSceneChange(currentScene);
                DebugRenderer.getInstance().addMessage("Transition complete to: " + currentScene.getDisplayName(), 2.0f);
            }
        }
    }
    
    /**
     * Handle input for the current scene
     */
    public void handleInput(Window window) {
        switch (currentScene) {
            case MENU:
                handleMenuInput(window);
                break;
            case PLAYING:
                handlePlayingInput(window);
                break;
            case PAUSED:
                handlePausedInput(window);
                break;
            case SETTINGS:
                handleSettingsInput(window);
                break;
            case GAME_OVER:
                handleGameOverInput(window);
                break;
            case LOADING:
                // Loading scene doesn't handle input
                break;
        }
    }
    
    /**
     * Render the current scene
     */
    public void render(Window window) {
        switch (currentScene) {
            case MENU:
                renderMenu(window);
                break;
            case PLAYING:
                // Playing scene is rendered by the main game render method
                break;
            case PAUSED:
                renderPausedOverlay(window);
                break;
            case SETTINGS:
                renderSettings(window);
                break;
            case GAME_OVER:
                renderGameOver(window);
                break;
            case LOADING:
                renderLoading(window);
                break;
        }
        
        // Render transition overlay if transitioning
        if (isTransitioning) {
            renderTransitionOverlay(window);
        }
    }
    
    private void handleSceneChange(Scene newScene) {
        switch (newScene) {
            case MENU:
                // Reset menu selection
                selectedMenuItem = 0;
                break;
            case PLAYING:
                // Resume game logic
                break;
            case PAUSED:
                // Pause game logic
                break;
            case SETTINGS:
                selectedSetting = 0;
                break;
            case GAME_OVER:
                // Handle game over logic
                break;
            case LOADING:
                // Start loading process
                break;
        }
    }
    
    private void handleMenuInput(Window window) {
        // Navigate menu with arrow keys (only on key press, not hold)
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_UP)) {
            selectedMenuItem = (selectedMenuItem - 1 + menuItems.length) % menuItems.length;
        }
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_DOWN)) {
            selectedMenuItem = (selectedMenuItem + 1) % menuItems.length;
        }
        
        // Select menu item with Enter
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_ENTER)) {
            switch (selectedMenuItem) {
                case 0: // Start Game
                    transitionToScene(Scene.PLAYING);
                    break;
                case 1: // Settings
                    pushScene(Scene.SETTINGS);
                    break;
                case 2: // Exit
                    GLFW.glfwSetWindowShouldClose(window.getWindowHandle(), true);
                    break;
            }
        }
        
        // Exit with Escape
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_ESCAPE)) {
            GLFW.glfwSetWindowShouldClose(window.getWindowHandle(), true);
        }
    }
    
    private void handlePlayingInput(Window window) {
        // Pause with Escape
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_ESCAPE)) {
            pushScene(Scene.PAUSED);
        }
    }
    
    private void handlePausedInput(Window window) {
        // Resume with Escape or Enter
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_ESCAPE) ||
            mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_ENTER)) {
            popScene();
        }
        
        // Quit to menu with Q
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_Q)) {
            // Pop the pause scene and transition to menu
            popScene();
            transitionToScene(Scene.MENU);
        }
    }
    
    private void handleSettingsInput(Window window) {
        // Navigate settings with arrow keys (only on key press, not hold)
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_UP)) {
            selectedSetting = (selectedSetting - 1 + settingsItems.length) % settingsItems.length;
        }
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_DOWN)) {
            selectedSetting = (selectedSetting + 1) % settingsItems.length;
        }
        
        // Select setting with Enter
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_ENTER)) {
            if (selectedSetting == settingsItems.length - 1) { // Back
                popScene();
            }
        }
        
        // Back with Escape
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_ESCAPE)) {
            popScene();
        }
    }
    
    private void handleGameOverInput(Window window) {
        // Restart with Enter
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_ENTER)) {
            transitionToScene(Scene.PLAYING);
        }
        
        // Return to menu with Escape
        if (mystuff.utils.KeyboardManager.isKeyJustPressed(GLFW.GLFW_KEY_ESCAPE)) {
            transitionToScene(Scene.MENU);
        }
    }
    
    private void renderMenu(Window window) {
        // Clear the screen first to ensure clean rendering
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        // Set up 2D rendering
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        // Render title
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        renderText("TACO STAND", window.getWidth() / 2 - 150, 200);
        
        // Render menu items
        int startY = 300;
        int lineHeight = 50;
        
        for (int i = 0; i < menuItems.length; i++) {
            if (i == selectedMenuItem) {
                GL11.glColor3f(1.0f, 1.0f, 0.0f); // Yellow for selected
            } else {
                GL11.glColor3f(0.8f, 0.8f, 0.8f); // Gray for unselected
            }
            renderText(menuItems[i], window.getWidth() / 2 - 100, startY + i * lineHeight);
        }
        
        // Render instructions
        GL11.glColor3f(0.6f, 0.6f, 0.6f);
        renderText("Use Arrow Keys to navigate, Enter to select", window.getWidth() / 2 - 200, window.getHeight() - 100);
        
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
    
    private void renderPausedOverlay(Window window) {
        // Set up 2D rendering for overlay
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        // Semi-transparent overlay
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(window.getWidth(), 0);
        GL11.glVertex2f(window.getWidth(), window.getHeight());
        GL11.glVertex2f(0, window.getHeight());
        GL11.glEnd();
        
        // Pause text
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        renderText("PAUSED", window.getWidth() / 2 - 100, window.getHeight() / 2 - 100);
        renderText("Press Escape or Enter to resume", window.getWidth() / 2 - 150, window.getHeight() / 2 - 50);
        renderText("Press Q to quit to menu", window.getWidth() / 2 - 120, window.getHeight() / 2);
        
        GL11.glDisable(GL11.GL_BLEND);
        
        // Restore matrices
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
    
    private void renderSettings(Window window) {
        // Clear the screen first to remove any background content
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        // Similar to menu rendering but for settings
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        // Render title
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        renderText("SETTINGS", window.getWidth() / 2 - 100, 200);
        
        // Render settings items
        int startY = 300;
        int lineHeight = 50;
        
        for (int i = 0; i < settingsItems.length; i++) {
            if (i == selectedSetting) {
                GL11.glColor3f(1.0f, 1.0f, 0.0f);
            } else {
                GL11.glColor3f(0.8f, 0.8f, 0.8f);
            }
            renderText(settingsItems[i], window.getWidth() / 2 - 100, startY + i * lineHeight);
        }
        
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
    
    private void renderGameOver(Window window) {
        // Set up 2D rendering for overlay
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        // Similar overlay to pause
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(window.getWidth(), 0);
        GL11.glVertex2f(window.getWidth(), window.getHeight());
        GL11.glVertex2f(0, window.getHeight());
        GL11.glEnd();
        
        GL11.glColor3f(1.0f, 0.0f, 0.0f);
        renderText("GAME OVER", window.getWidth() / 2 - 100, window.getHeight() / 2 - 100);
        
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        renderText("Press Enter to restart", window.getWidth() / 2 - 120, window.getHeight() / 2 - 50);
        renderText("Press Escape for main menu", window.getWidth() / 2 - 140, window.getHeight() / 2);
        
        GL11.glDisable(GL11.GL_BLEND);
        
        // Restore matrices
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
    
    private void renderLoading(Window window) {
        // Clear the screen first to ensure clean rendering
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        renderText("LOADING...", window.getWidth() / 2 - 100, window.getHeight() / 2);
        
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
    
    private void renderTransitionOverlay(Window window) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Fade to black transition
        float alpha = transitionProgress;
        GL11.glColor4f(0.0f, 0.0f, 0.0f, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(window.getWidth(), 0);
        GL11.glVertex2f(window.getWidth(), window.getHeight());
        GL11.glVertex2f(0, window.getHeight());
        GL11.glEnd();
        
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glDisable(GL11.GL_BLEND);
    }
    
    private void renderText(String text, int x, int y) {
        mystuff.utils.FontLoader.renderText(text, x, y);
    }
    
    // Getters and setters
    public Scene getCurrentScene() {
        return currentScene;
    }
    
    public Scene getPreviousScene() {
        return previousScene;
    }
    
    public boolean isTransitioning() {
        return isTransitioning;
    }
    
    public void setSceneData(Scene scene, Object data) {
        sceneData.put(scene, data);
    }
    
    public Object getSceneData(Scene scene) {
        return sceneData.get(scene);
    }
} 