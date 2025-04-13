package mystuff.game;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import mystuff.engine.GameObject;
import mystuff.engine.Window;
import mystuff.engine.Camera;
import mystuff.utils.Shapes;

public class Player extends GameObject {
    private float speed = 0.1f;
    private float size = 1.0f;
    private Camera camera;  // Reference to the camera
    private float mouseSensitivity = 0.1f;
    private boolean firstMouse = true;
    private float lastX = 400, lastY = 300;

    public Player(float x, float y, float z, Camera camera) {
        super(x, y, z);
        this.camera = camera;
        // Set initial camera position to player position
        camera.setPosition(x, y + size, z); // Eye level is above player position
    }

    @Override
    public void update(Window window) {
        // Calculate movement direction based on camera yaw
        float yaw = (float) Math.toRadians(camera.getYaw());
        
        // Forward vector points where the camera is looking
        float forwardX = (float) Math.sin(yaw);  // Removed negative, will handle direction in movement
        float forwardZ = (float) Math.cos(yaw);  // Removed negative, will handle direction in movement
        
        // Right vector is perpendicular to forward (rotate forward vector 90 degrees clockwise)
        float rightX = (float) Math.cos(yaw);
        float rightZ = (float) Math.sin(yaw);

        float dx = 0, dy = 0, dz = 0;

        // Forward/Backward
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            dx += forwardX * speed;  // Move in the direction we're facing
            dz -= forwardZ * speed;  // Negative Z is forward in OpenGL
        }
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            dx -= forwardX * speed;  // Move opposite to the direction we're facing
            dz += forwardZ * speed;  // Positive Z is backward in OpenGL
        }

        // Strafe Left/Right
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            dx -= rightX * speed;  // Move left (negative right vector)
            dz -= rightZ * speed;
        }
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            dx += rightX * speed;  // Move right (positive right vector)
            dz += rightZ * speed;
        }

        // Up/Down
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            dy += speed;
        }
        if (GLFW.glfwGetKey(window.getWindowHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            dy -= speed;
        }

        // Update player position
        x += dx;
        y += dy;
        z += dz;

        // Update camera position to follow player
        camera.setPosition(x, y + size, z);
    }

    @Override
    public void render() {
        // third-person:
        /*
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);
        GL11.glRotatef(rotX, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(rotY, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
        GL11.glScalef(scale, scale, scale);
        
        GL11.glColor3f(0.0f, 1.0f, 0.0f);
        Shapes.cube(size);
        
        GL11.glPopMatrix();
        */
    }

    public void handleMouseInput(float xpos, float ypos) {
        if (firstMouse) {
            lastX = xpos;
            lastY = ypos;
            firstMouse = false;
            return;
        }

        float xoffset = xpos - lastX;
        float yoffset = lastY - ypos;
        lastX = xpos;
        lastY = ypos;

        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;

        camera.rotate(yoffset, xoffset);
    }
} 