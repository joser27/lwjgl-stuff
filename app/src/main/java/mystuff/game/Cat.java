package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.utils.OBJModelRenderer;
import org.lwjgl.opengl.GL11;

public class Cat extends GameObject {
    private static OBJModelRenderer catModel = null;
    private static boolean triedLoad = false;

    public Cat(float x, float y, float z) {
        super(x, y, z);
        if (!triedLoad) {
            catModel = new OBJModelRenderer("models/cat.obj");
            triedLoad = true;
            if (catModel.isLoaded()) {
                System.out.println("Cat OBJ loaded! Vertices: " + catModel.getVertexCount());
                float[] bounds = catModel.getModelBounds();
                if (bounds != null) {
                    System.out.printf("Cat bounds: X[%.3f, %.3f] Y[%.3f, %.3f] Z[%.3f, %.3f]%n",
                        bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
                    System.out.printf("Cat size: %.3f x %.3f x %.3f%n",
                        bounds[1] - bounds[0], bounds[3] - bounds[2], bounds[5] - bounds[4]);
                }
            } else {
                System.err.println("Failed to load cat.obj!");
            }
        }
    }

    @Override
    public void render() {
        if (catModel != null && catModel.isLoaded()) {
            GL11.glPushMatrix();
            GL11.glTranslatef(getX(), getY(), getZ());
            // Center vertically if needed
            float[] bounds = catModel.getModelBounds();
            if (bounds != null) {
                float yCenter = (bounds[2] + bounds[3]) / 2.0f;
                GL11.glTranslatef(0, -yCenter, 0);
            }
            GL11.glColor3f(1.0f, 0.7f, 0.2f); // Orange-ish for visibility
            catModel.render(10.0f); // Large scale for visibility
            GL11.glPopMatrix();
        }
    }

    public void cleanup() {
        if (catModel != null) catModel.cleanup();
    }

    @Override
    public void update(mystuff.engine.Window window, float deltaTime) {
        // No update logic needed for static cat
    }
} 