package mystuff.engine;

import mystuff.utils.Debug;
import org.lwjgl.glfw.GLFW;

public class GameEngine implements Runnable {
    private final Window window;
    private final IGameLogic gameLogic;
    private final Timer timer;
    
    public GameEngine(String windowTitle, int width, int height, IGameLogic gameLogic, int targetFPS) {
        window = new Window(windowTitle, width, height);
        this.gameLogic = gameLogic;
        this.timer = new Timer();
    }
    
    @Override
    public void run() {
        try {
            init();
            gameLoop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    private void init() {
        window.init();
        timer.init();
        gameLogic.init(window);
    }
    
    private void gameLoop() {
        float elapsedTime;
        float accumulator = 0f;
        float interval = 1.0f / 60.0f; // Fixed time step for physics/game logic
        
        // Game loop
        while (!window.shouldClose()) {
            elapsedTime = timer.getElapsedTime();
            accumulator += elapsedTime;
            
            input();
            
            // Fixed timestep updates for game logic
            while (accumulator >= interval) {
                update(interval);
                accumulator -= interval;
            }
            
            render();
        }
    }
    
    private void input() {
        gameLogic.input(window);
    }
    
    private void update(float interval) {
        gameLogic.update(interval);
    }
    
    private void render() {
        gameLogic.render(window);
        window.update();
    }
    
    private void cleanup() {
        gameLogic.cleanup();
        window.cleanup();
    }
} 