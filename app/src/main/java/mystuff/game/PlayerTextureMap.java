package mystuff.game;

public class PlayerTextureMap {
    public static final float TEXTURE_WIDTH = 1280.0f;
    public static final float TEXTURE_HEIGHT = 640.0f;

    public enum BodyPart {
        // Format: name(x, y, width, height)
        HEAD_FRONT(160, 160, 160, 160),
        HEAD_BACK(320, 160, 160, 160),
        HEAD_RIGHT(480, 160, 160, 160),
        HEAD_LEFT(0, 160, 160, 160),
        HEAD_TOP(160, 0, 160, 160),
        HEAD_BOTTOM(160, 320, 160, 160),
        
        BODY_FRONT(640, 160, 160, 320),
        BODY_BACK(800, 160, 160, 320),
        BODY_RIGHT(960, 160, 160, 320),
        BODY_LEFT(480, 160, 160, 320),
        
        ARM_RIGHT_FRONT(160, 320, 80, 320),
        ARM_RIGHT_BACK(240, 320, 80, 320),
        ARM_RIGHT_RIGHT(320, 320, 80, 320),
        ARM_RIGHT_LEFT(80, 320, 80, 320),
        
        ARM_LEFT_FRONT(160, 320, 80, 320),
        ARM_LEFT_BACK(240, 320, 80, 320),
        ARM_LEFT_RIGHT(320, 320, 80, 320),
        ARM_LEFT_LEFT(80, 320, 80, 320),
        
        LEG_RIGHT_FRONT(0, 320, 80, 320),
        LEG_RIGHT_BACK(80, 320, 80, 320),
        LEG_RIGHT_RIGHT(160, 320, 80, 320),
        LEG_RIGHT_LEFT(240, 320, 80, 320),
        
        LEG_LEFT_FRONT(0, 320, 80, 320),
        LEG_LEFT_BACK(80, 320, 80, 320),
        LEG_LEFT_RIGHT(160, 320, 80, 320),
        LEG_LEFT_LEFT(240, 320, 80, 320);

        public final float u, v, width, height;

        BodyPart(float x, float y, float width, float height) {
            this.u = x / TEXTURE_WIDTH;
            this.v = y / TEXTURE_HEIGHT;
            this.width = width / TEXTURE_WIDTH;
            this.height = height / TEXTURE_HEIGHT;
        }

        public float getEndU() {
            return u + width;
        }

        public float getEndV() {
            return v + height;
        }
    }
} 