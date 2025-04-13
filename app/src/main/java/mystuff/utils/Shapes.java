package mystuff.utils;

import org.lwjgl.opengl.GL11;

public class Shapes {

        
    /**
     * Draws a cube centered at the current position
     * @param size Half the length of each side
     */
    public static void cube(float size) {
        // Front face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-size, -size, size);
        GL11.glVertex3f(size, -size, size);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(-size, size, size);
        GL11.glEnd();

        // Back face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-size, -size, -size);
        GL11.glVertex3f(-size, size, -size);
        GL11.glVertex3f(size, size, -size);
        GL11.glVertex3f(size, -size, -size);
        GL11.glEnd();

        // Top face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-size, size, -size);
        GL11.glVertex3f(-size, size, size);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(size, size, -size);
        GL11.glEnd();

        // Bottom face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-size, -size, -size);
        GL11.glVertex3f(size, -size, -size);
        GL11.glVertex3f(size, -size, size);
        GL11.glVertex3f(-size, -size, size);
        GL11.glEnd();

        // Right face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(size, -size, -size);
        GL11.glVertex3f(size, size, -size);
        GL11.glVertex3f(size, size, size);
        GL11.glVertex3f(size, -size, size);
        GL11.glEnd();

        // Left face
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-size, -size, -size);
        GL11.glVertex3f(-size, -size, size);
        GL11.glVertex3f(-size, size, size);
        GL11.glVertex3f(-size, size, -size);
        GL11.glEnd();
    }

    /**
     * Draws a sphere using triangle strips
     * @param radius Radius of the sphere
     * @param slices Number of vertical slices
     * @param stacks Number of horizontal stacks
     */
    public static void sphere(float radius, int slices, int stacks) {
        float phi, theta;
        float dphi = (float) Math.PI / stacks;
        float dtheta = 2.0f * (float) Math.PI / slices;

        for (int i = 0; i < stacks; i++) {
            phi = i * dphi;
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int j = 0; j <= slices; j++) {
                theta = (j == slices) ? 0.0f : j * dtheta;
                float x = (float) (Math.sin(phi) * Math.cos(theta));
                float y = (float) Math.cos(phi);
                float z = (float) (Math.sin(phi) * Math.sin(theta));
                GL11.glNormal3f(x, y, z);
                GL11.glVertex3f(radius * x, radius * y, radius * z);
                
                x = (float) (Math.sin(phi + dphi) * Math.cos(theta));
                y = (float) Math.cos(phi + dphi);
                z = (float) (Math.sin(phi + dphi) * Math.sin(theta));
                GL11.glNormal3f(x, y, z);
                GL11.glVertex3f(radius * x, radius * y, radius * z);
            }
            GL11.glEnd();
        }
    }

    /**
     * Draws a cylinder
     * @param baseRadius Radius of the base
     * @param topRadius Radius of the top
     * @param height Height of the cylinder
     * @param slices Number of vertical slices
     */
    public static void cylinder(float baseRadius, float topRadius, float height, int slices) {
        float theta, dtheta;
        float z0 = -height / 2;
        float z1 = height / 2;

        dtheta = 2.0f * (float) Math.PI / slices;

        // Draw the sides
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= slices; i++) {
            theta = (i == slices) ? 0.0f : i * dtheta;
            float x = (float) Math.cos(theta);
            float y = (float) Math.sin(theta);

            GL11.glNormal3f(x, y, 0);
            GL11.glVertex3f(topRadius * x, topRadius * y, z1);
            GL11.glVertex3f(baseRadius * x, baseRadius * y, z0);
        }
        GL11.glEnd();

        // Draw the top circle
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glNormal3f(0, 0, 1);
        GL11.glVertex3f(0, 0, z1);
        for (int i = 0; i <= slices; i++) {
            theta = (i == slices) ? 0.0f : i * dtheta;
            float x = (float) Math.cos(theta);
            float y = (float) Math.sin(theta);
            GL11.glVertex3f(topRadius * x, topRadius * y, z1);
        }
        GL11.glEnd();

        // Draw the bottom circle
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glNormal3f(0, 0, -1);
        GL11.glVertex3f(0, 0, z0);
        for (int i = slices; i >= 0; i--) {
            theta = (i == slices) ? 0.0f : i * dtheta;
            float x = (float) Math.cos(theta);
            float y = (float) Math.sin(theta);
            GL11.glVertex3f(baseRadius * x, baseRadius * y, z0);
        }
        GL11.glEnd();
    }

    /**
     * Draws a pyramid
     * @param baseSize Half the length of the base
     * @param height Height of the pyramid
     */
    public static void pyramid(float baseSize, float height) {
        float h2 = height / 2;
        float b2 = baseSize;

        // Base
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(-b2, -h2, b2);
        GL11.glVertex3f(b2, -h2, b2);
        GL11.glVertex3f(b2, -h2, -b2);
        GL11.glVertex3f(-b2, -h2, -b2);
        GL11.glEnd();

        // Front face
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3f(0, h2, 0);
        GL11.glVertex3f(-b2, -h2, b2);
        GL11.glVertex3f(b2, -h2, b2);
        GL11.glEnd();

        // Right face
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3f(0, h2, 0);
        GL11.glVertex3f(b2, -h2, b2);
        GL11.glVertex3f(b2, -h2, -b2);
        GL11.glEnd();

        // Back face
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3f(0, h2, 0);
        GL11.glVertex3f(b2, -h2, -b2);
        GL11.glVertex3f(-b2, -h2, -b2);
        GL11.glEnd();

        // Left face
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3f(0, h2, 0);
        GL11.glVertex3f(-b2, -h2, -b2);
        GL11.glVertex3f(-b2, -h2, b2);
        GL11.glEnd();
    }
}
