package mystuff.engine;

import org.lwjgl.glfw.GLFW;

public class Timer {
    private double lastLoopTime;
    
    public void init() {
        lastLoopTime = GLFW.glfwGetTime();
    }
    
    public float getElapsedTime() {
        double time = GLFW.glfwGetTime();
        float elapsedTime = (float) (time - lastLoopTime);
        lastLoopTime = time;
        return elapsedTime;
    }
    
    public double getLastLoopTime() {
        return lastLoopTime;
    }
} 