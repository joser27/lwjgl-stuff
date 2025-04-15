package mystuff.engine;

import java.util.Arrays;

/**
 * Enhanced Timer class for game engine with optimized performance metrics.
 */
public class Timer {
    // Core timing variables
    private long lastLoopTime;
    private double timeScale = 1.0;
    
    // FPS tracking
    private float timeCount;
    private int fps;
    private int fpsCount;
    
    // Constants
    private static final float NANO_TO_MS = 1.0f / 1_000_000.0f;
    private static final float NANO_TO_SEC = 1.0f / 1_000_000_000.0f;
    
    // Frame time statistics with circular buffer
    private float[] frameTimeHistory;
    private int frameTimeIndex;
    private int frameTimeCount;
    private static final int FRAME_TIME_SAMPLES = 240;
    private float averageFrameTime;
    private float maxFrameTime;
    private float minFrameTime = Float.MAX_VALUE;
    
    // Performance tracking
    private long frameStartTime;
    private long updateTimeNano;
    private long renderTimeNano;
    private long sleepTimeNano;

    /**
     * Initializes the timer
     */
    public void init() {
        lastLoopTime = System.nanoTime();
        frameStartTime = lastLoopTime;
        timeCount = 0;
        fps = 0;
        fpsCount = 0;
        
        // Initialize frame time tracking
        frameTimeHistory = new float[FRAME_TIME_SAMPLES];
        frameTimeIndex = 0;
        frameTimeCount = 0;
        averageFrameTime = 0;
        maxFrameTime = 0;
        minFrameTime = Float.MAX_VALUE;
        
        updateTimeNano = 0;
        renderTimeNano = 0;
        sleepTimeNano = 0;
    }

    /**
     * Starts timing a new frame
     */
    public void startFrame() {
        frameStartTime = System.nanoTime();
    }
    
    /**
     * Records the time spent in update logic
     */
    public void recordUpdateTime() {
        updateTimeNano = System.nanoTime() - frameStartTime;
    }
    
    /**
     * Records the time spent in render logic
     */
    public void recordRenderTime() {
        renderTimeNano = System.nanoTime() - frameStartTime - updateTimeNano;
    }
    
    /**
     * Records the time spent sleeping
     */
    public void recordSleepTime(long sleepTimeNs) {
        sleepTimeNano = sleepTimeNs;
    }
    
    /**
     * Gets the elapsed time since the last frame
     * @return Time elapsed since last frame in milliseconds, scaled by timeScale
     */
    public float getElapsedTime() {
        long currentTime = System.nanoTime();
        float elapsedTime = (currentTime - lastLoopTime) * NANO_TO_MS;
        
        // Store raw frame time in history and update stats
        updateFrameTimeStats(elapsedTime);
        
        // Update loop time for next frame
        lastLoopTime = currentTime;
        
        // Update FPS counter
        timeCount += elapsedTime;
        fpsCount++;
        if (timeCount >= 1000.0f) { // Update every second
            fps = fpsCount;
            fpsCount = 0;
            timeCount = 0;
        }
        
        // Apply time scaling
        return (float)(elapsedTime * timeScale);
    }
    
    /**
     * Updates frame time statistics efficiently
     */
    private void updateFrameTimeStats(float frameTime) {
        // Update frame time history
        frameTimeHistory[frameTimeIndex] = frameTime;
        frameTimeIndex = (frameTimeIndex + 1) % FRAME_TIME_SAMPLES;
        
        if (frameTimeCount < FRAME_TIME_SAMPLES) {
            frameTimeCount++;
        }
        
        // Recalculate stats only when we have enough samples
        if (frameTimeCount >= 10) {
            // Calculate average more efficiently
            float sum = 0;
            maxFrameTime = 0;
            minFrameTime = Float.MAX_VALUE;
            
            for (int i = 0; i < frameTimeCount; i++) {
                float time = frameTimeHistory[i];
                sum += time;
                if (time > maxFrameTime) maxFrameTime = time;
                if (time < minFrameTime) minFrameTime = time;
            }
            
            averageFrameTime = sum / frameTimeCount;
        }
    }
    
    // Getters
    public long getLastLoopTimeNanos() { return lastLoopTime; }
    public float getLastLoopTimeMillis() { return lastLoopTime * NANO_TO_MS; }
    public int getFPS() { return fps; }
    public float getAverageFrameTime() { return averageFrameTime; }
    public float getMaxFrameTime() { return maxFrameTime; }
    public float getMinFrameTime() { return minFrameTime; }
    public float[] getFrameTimeHistory() { return frameTimeHistory; }
    public int getCurrentFrameTimeIndex() { return frameTimeIndex > 0 ? frameTimeIndex - 1 : FRAME_TIME_SAMPLES - 1; }
    
    // Performance metrics
    public float getUpdateTimeMs() { return updateTimeNano * NANO_TO_MS; }
    public float getRenderTimeMs() { return renderTimeNano * NANO_TO_MS; }
    public float getSleepTimeMs() { return sleepTimeNano * NANO_TO_MS; }
    public float getFrameUtilization() { 
        long totalFrameTime = updateTimeNano + renderTimeNano;
        float totalTimeMs = totalFrameTime * NANO_TO_MS;
        return (totalTimeMs / (totalTimeMs + getSleepTimeMs())) * 100.0f;
    }

    // Time scale methods
    public void setTimeScale(double scale) {
        this.timeScale = Math.max(0.1, Math.min(scale, 10.0));
    }
    
    public double getTimeScale() {
        return timeScale;
    }
    
    // Time measurement convenience methods
    public static long getCurrentTime() { return System.nanoTime(); }
    public static float getTimeMillis() { return System.nanoTime() * NANO_TO_MS; }
    public static float getTimeSec() { return System.nanoTime() * NANO_TO_SEC; }
} 