package mystuff.engine;

import org.lwjgl.opengl.GL11;
import mystuff.utils.Debug;

public class Camera {
    private float x, y, z;
    private float pitch, yaw;
    private Frustum frustum;
    private float[] modelViewMatrix;
    private float[] projectionMatrix;
    private boolean matricesDirty;

    public Camera(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = 0;
        this.yaw = 0;
        this.frustum = new Frustum();
        this.modelViewMatrix = new float[16];
        this.projectionMatrix = new float[16];
        this.matricesDirty = true;
    }

    public void rotate(float dpitch, float dyaw) {
        this.pitch -= dpitch;
        this.yaw += dyaw;
        
        // Clamp pitch to prevent camera flipping
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;
        
        // Keep yaw between 0 and 360 degrees
        if (yaw > 360.0f) yaw -= 360.0f;
        if (yaw < 0.0f) yaw += 360.0f;

        matricesDirty = true;
    }

    public void update() {
        if (matricesDirty) {
            // Get the current projection matrix
            GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projectionMatrix);
            
            // Get the current modelview matrix
            GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix);
            
            // Update the frustum with the new matrices
            frustum.update(projectionMatrix, modelViewMatrix);
            
            matricesDirty = false;
        }
    }

    public boolean isBoxInView(float x, float y, float z, float width, float height, float depth) {
        return frustum.isBoxInFrustum(x, y, z, width, height, depth);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getPitch() { return pitch; }
    public float getYaw() { return yaw; }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        matricesDirty = true;
    }
} 