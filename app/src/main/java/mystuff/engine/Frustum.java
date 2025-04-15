package mystuff.engine;

public class Frustum {
    private float[][] planes = new float[6][4]; // Left, Right, Bottom, Top, Near, Far planes
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int BOTTOM = 2;
    private static final int TOP = 3;
    private static final int NEAR = 4;
    private static final int FAR = 5;

    public void update(float[] projectionMatrix, float[] modelViewMatrix) {
        // Combine the projection and modelview matrices
        float[] clip = new float[16];
        
        // Matrix multiplication
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                clip[i * 4 + j] = 
                    modelViewMatrix[i * 4 + 0] * projectionMatrix[0 * 4 + j] +
                    modelViewMatrix[i * 4 + 1] * projectionMatrix[1 * 4 + j] +
                    modelViewMatrix[i * 4 + 2] * projectionMatrix[2 * 4 + j] +
                    modelViewMatrix[i * 4 + 3] * projectionMatrix[3 * 4 + j];
            }
        }

        // Extract the frustum planes
        // Left plane
        planes[LEFT][0] = clip[3] + clip[0];
        planes[LEFT][1] = clip[7] + clip[4];
        planes[LEFT][2] = clip[11] + clip[8];
        planes[LEFT][3] = clip[15] + clip[12];
        normalizePlane(LEFT);

        // Right plane
        planes[RIGHT][0] = clip[3] - clip[0];
        planes[RIGHT][1] = clip[7] - clip[4];
        planes[RIGHT][2] = clip[11] - clip[8];
        planes[RIGHT][3] = clip[15] - clip[12];
        normalizePlane(RIGHT);

        // Bottom plane
        planes[BOTTOM][0] = clip[3] + clip[1];
        planes[BOTTOM][1] = clip[7] + clip[5];
        planes[BOTTOM][2] = clip[11] + clip[9];
        planes[BOTTOM][3] = clip[15] + clip[13];
        normalizePlane(BOTTOM);

        // Top plane
        planes[TOP][0] = clip[3] - clip[1];
        planes[TOP][1] = clip[7] - clip[5];
        planes[TOP][2] = clip[11] - clip[9];
        planes[TOP][3] = clip[15] - clip[13];
        normalizePlane(TOP);

        // Near plane
        planes[NEAR][0] = clip[3] + clip[2];
        planes[NEAR][1] = clip[7] + clip[6];
        planes[NEAR][2] = clip[11] + clip[10];
        planes[NEAR][3] = clip[15] + clip[14];
        normalizePlane(NEAR);

        // Far plane
        planes[FAR][0] = clip[3] - clip[2];
        planes[FAR][1] = clip[7] - clip[6];
        planes[FAR][2] = clip[11] - clip[10];
        planes[FAR][3] = clip[15] - clip[14];
        normalizePlane(FAR);
    }

    private void normalizePlane(int side) {
        float magnitude = (float) Math.sqrt(
            planes[side][0] * planes[side][0] +
            planes[side][1] * planes[side][1] +
            planes[side][2] * planes[side][2]
        );
        
        if (magnitude != 0) {
            planes[side][0] /= magnitude;
            planes[side][1] /= magnitude;
            planes[side][2] /= magnitude;
            planes[side][3] /= magnitude;
        }
    }

    public boolean isBoxInFrustum(float x, float y, float z, float width, float height, float depth) {
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;
        float halfDepth = depth * 0.5f;

        for (int i = 0; i < 6; i++) {
            float[] plane = planes[i];
            float distance = plane[0] * (x + halfWidth) + 
                           plane[1] * (y + halfHeight) + 
                           plane[2] * (z + halfDepth) + 
                           plane[3];

            if (distance < -Math.max(Math.max(width, height), depth)) {
                return false;
            }
        }
        return true;
    }
} 