package mystuff.engine;

import mystuff.utils.Debug;
import org.lwjgl.glfw.GLFW;

/**
 * Enhanced GameEngine with improved performance, thread handling, and profiling
 */
public class GameEngine implements Runnable {
    // Core components
    private final Window window;
    private final IGameLogic gameLogic;
    private final Timer timer;
    
    // Engine settings
    private final long targetFrameTime; // Nanoseconds per frame based on target FPS
    private boolean running = false;
    private boolean paused = false;
    private final int targetUPS = 120; // Updates per second increased for smoother gameplay
    
    // Thread management
    private Thread gameThread;
    private boolean highPrecisionThread = true;
    
    // Performance counters
    private int frameCount = 0;
    private int updateCount = 0;
    private long lastPerformanceReport = 0;
    private static final long PERFORMANCE_REPORT_INTERVAL = 5_000_000_000L; // 5 seconds in nanoseconds
    
    // Sleep mode handling
    public enum SleepMode {
        YIELD,       // Just yield to other threads
        SLEEP,       // Use Thread.sleep (less precise but more CPU friendly)
        BUSY_WAIT    // Busy-wait (most precise but consumes a CPU core)
    }
    private SleepMode sleepMode = SleepMode.SLEEP;
    
    /**
     * Creates a new GameEngine instance
     */
    public GameEngine(String windowTitle, int width, int height, IGameLogic gameLogic, int targetFPS) {
        window = new Window(windowTitle, width, height);
        this.gameLogic = gameLogic;
        this.timer = new Timer();
        // Calculate target frame time in nanoseconds based on desired FPS
        this.targetFrameTime = targetFPS > 0 ? 1_000_000_000L / targetFPS : 0;
    }
    
    /**
     * Starts the game in a separate thread
     */
    public void start() {
        if (gameThread != null && gameThread.isAlive()) {
            return; // Already running
        }
        
        gameThread = new Thread(this, "GameThread");
        running = true;
        gameThread.start();
    }
    
    /**
     * Stops the game
     */
    public void stop() {
        running = false;
        try {
            if (gameThread != null) {
                gameThread.join(5000); // Wait up to 5 seconds for clean shutdown
            }
        } catch (InterruptedException e) {
            System.err.println("Error stopping game thread: " + e.getMessage());
        }
    }
    
    /**
     * Sets thread priority for better performance
     */
    private void configureThread() {
        if (highPrecisionThread) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            try {
                // On some platforms, this can make the thread more time-critical
                Thread.currentThread().setName("HighPriorityGameLoop");
            } catch (SecurityException e) {
                System.err.println("Could not set thread properties: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void run() {
        try {
            configureThread();
            init();
            gameLoop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    private void init() {
        // Initialize window first to create OpenGL context
        window.init();
        window.setParentEngine(this); // Set parent engine reference
        
        // Ensure the window's context is current on this thread
        GLFW.glfwMakeContextCurrent(window.getWindowHandle());
        
        // Initialize timer
        timer.init();
        
        // Initialize game logic with window
        gameLogic.init(window);
        
        // If game logic is an instance of Game, pass the timer reference
        if (gameLogic instanceof mystuff.game.Game) {
            ((mystuff.game.Game) gameLogic).setTimer(timer);
        }
        
        // Start performance tracking
        lastPerformanceReport = Timer.getCurrentTime();
        
        System.out.println("Game engine initialized successfully");
    }
    
    private void gameLoop() {
        float deltaTime; // Time since last frame in milliseconds
        float accumulator = 0f;
        float interval = 1.0f / targetUPS; // Fixed update interval
        int maxUpdatesPerFrame = 10; // Prevent spiral of death
        
        // Game loop
        while (running && !window.shouldClose()) {
            timer.startFrame();
            
            // Get elapsed time since last frame
            deltaTime = timer.getElapsedTime();
            if (!paused) {
                accumulator += deltaTime / 1000.0f; // Convert to seconds
            }
            
            // Process input regardless of pause state
            input();
            
            // Fixed timestep updates with maximum number of updates per frame
            int updates = 0;
            timer.startFrame(); // Reset to time update phase specifically
            while (accumulator >= interval && updates < maxUpdatesPerFrame && !paused) {
                update(interval);
                accumulator -= interval;
                updates++;
                updateCount++;
            }
            timer.recordUpdateTime();
            
            // If we're falling behind, drop accumulator time
            if (accumulator > interval * 3) {
                if (Debug.showPlayerInfo()) {
                    System.out.println("WARNING: Game is running slow, dropping " + 
                                    accumulator + " seconds of simulation time");
                }
                accumulator = interval * 2;
            }
            
            // Render at the interpolated state
            render(Math.min(1.0f, accumulator / interval));
            timer.recordRenderTime();
            
            frameCount++;
            
            // Frame limiter
            if (targetFrameTime > 0 && !window.isVSync()) {
                limitFrameRate();
            }
            
            // Report performance statistics periodically
            reportPerformance();
        }
    }
    
    private void limitFrameRate() {
        long currentTime = Timer.getCurrentTime();
        long frameTime = currentTime - timer.getLastLoopTimeNanos();
        long sleepTime = targetFrameTime - frameTime;
        
        if (sleepTime > 0) {
            switch (sleepMode) {
                case YIELD:
                    Thread.yield();
                    break;
                    
                case BUSY_WAIT:
                    // Optimized busy-wait with hybrid approach
                    // Sleep for most of the time, then busy wait for precision
                    long busyWaitStart = Timer.getCurrentTime();
                    if (sleepTime > 2_000_000) { // If we have more than 2ms to wait
                        try {
                            // Sleep for most of the time minus 1ms for precision
                            Thread.sleep((sleepTime - 1_000_000) / 1_000_000L);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }
                    // Busy wait for the remaining time for maximum precision
                    while (Timer.getCurrentTime() - busyWaitStart < sleepTime) {
                        // Use a minimal yield every 0.5ms to reduce CPU load
                        if ((Timer.getCurrentTime() - busyWaitStart) % 500_000 == 0) {
                            Thread.yield();
                        }
                    }
                    break;
                    
                case SLEEP:
                default:
                    try {
                        timer.recordSleepTime(sleepTime);
                        // For sleep mode, reserve 0.5ms for wakeup time to reduce oversleeping
                        if (sleepTime > 1_000_000) {
                            Thread.sleep((sleepTime - 500_000) / 1_000_000L, (int)((sleepTime - 500_000) % 1_000_000L));
                        } else {
                            Thread.yield();
                        }
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                    break;
            }
        } else {
            // No time to sleep, just yield to give other threads a chance
            timer.recordSleepTime(0);
            Thread.yield();
        }
    }
    
    private void reportPerformance() {
        long currentTime = Timer.getCurrentTime();
        if (currentTime - lastPerformanceReport >= PERFORMANCE_REPORT_INTERVAL && Debug.showPlayerInfo()) {
            double elapsedSeconds = (currentTime - lastPerformanceReport) / 1_000_000_000.0;
            double avgFPS = frameCount / elapsedSeconds;
            double avgUPS = updateCount / elapsedSeconds;
            
            System.out.printf("Performance: %.1f FPS, %.1f UPS, Update: %.2fms, Render: %.2fms, CPU: %.1f%%\n",
                avgFPS, avgUPS, timer.getUpdateTimeMs(), timer.getRenderTimeMs(), timer.getFrameUtilization());
            
            // Reset counters
            frameCount = 0;
            updateCount = 0;
            lastPerformanceReport = currentTime;
        }
    }
    
    private void input() {
        gameLogic.input(window);
    }
    
    private void update(float interval) {
        gameLogic.update(interval);
    }
    
    private void render(float interpolation) {
        try {
            // Make sure context is current on this thread
            long handle = window.getWindowHandle();
            if (GLFW.glfwGetCurrentContext() != handle) {
                GLFW.glfwMakeContextCurrent(handle);
                System.out.println("Re-bound OpenGL context before rendering");
            }
            
            // Render game logic
            gameLogic.render(window);
            
            // Update window (swap buffers)
            window.update();
        } catch (Exception e) {
            System.err.println("Error during rendering: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cleanup() {
        gameLogic.cleanup();
        window.cleanup();
    }
    
    // Accessor methods
    public void setSleepMode(SleepMode mode) {
        this.sleepMode = mode;
    }
    
    public void setHighPrecisionThread(boolean highPrecision) {
        this.highPrecisionThread = highPrecision;
    }
    
    public void setPaused(boolean paused) {
        this.paused = paused;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public void togglePause() {
        this.paused = !this.paused;
    }
    
    public Timer getTimer() {
        return timer;
    }
} 