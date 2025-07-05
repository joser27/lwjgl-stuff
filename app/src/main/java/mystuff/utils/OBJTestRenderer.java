package mystuff.utils;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;


public class OBJTestRenderer {
    private static long window;
    private static OBJModelRenderer testModel;

    public static void main(String[] args) {
        // Set up error callback
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Create window
        window = GLFW.glfwCreateWindow(800, 600, "OBJ Test Renderer", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        GLFW.glfwSwapInterval(1);

        // Set up OpenGL
        GL11.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE); // Disable culling for debugging

        // Set up simple perspective using basic OpenGL
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        float aspect = 800f / 600f;
        float fov = 60.0f;
        float near = 0.1f;
        float far = 100.0f;
        float f = (float) (1.0 / Math.tan(Math.toRadians(fov / 2.0)));
        GL11.glFrustum(-near * f / aspect, near * f / aspect, -near * f, near * f, near, far);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        // Load the test model
        System.out.println("Attempting to load test model: models/cat.obj");
        testModel = new OBJModelRenderer("models/cat.obj");
        if (!testModel.isLoaded()) {
            System.err.println("Failed to load test model!");
        } else {
            System.out.println("Test model loaded! Vertex count: " + testModel.getVertexCount());
            float[] bounds = testModel.getModelBounds();
            if (bounds != null) {
                System.out.printf("Model bounds: X[%.3f, %.3f] Y[%.3f, %.3f] Z[%.3f, %.3f]%n",
                    bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
                System.out.printf("Model size: %.3f x %.3f x %.3f%n",
                    bounds[1] - bounds[0], bounds[3] - bounds[2], bounds[5] - bounds[4]);
            }
        }

        // Main loop
        while (!GLFW.glfwWindowShouldClose(window)) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            GL11.glLoadIdentity();
            // Move camera back to see the model
            GL11.glTranslatef(0, -1, -5);

            // Render the model at the origin, large scale
            if (testModel != null && testModel.isLoaded()) {
                GL11.glPushMatrix();
                GL11.glColor3f(1.0f, 0.2f, 0.2f); // Bright color
                // Center vertically if needed (optional)
                float[] bounds = testModel.getModelBounds();
                if (bounds != null) {
                    float yCenter = (bounds[2] + bounds[3]) / 2.0f;
                    GL11.glTranslatef(0, -yCenter, 0);
                }
                testModel.render(10.0f); // Large scale
                GL11.glPopMatrix();
            }

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }

        // Cleanup
        if (testModel != null) testModel.cleanup();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
} 