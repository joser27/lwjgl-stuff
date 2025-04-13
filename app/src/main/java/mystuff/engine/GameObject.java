package mystuff.engine;

public abstract class GameObject {
    protected float x, y, z;
    protected float rotX, rotY, rotZ;
    protected float scale;

    public GameObject(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotX = 0;
        this.rotY = 0;
        this.rotZ = 0;
        this.scale = 1;
    }

    public abstract void update(Window window, float deltaTime);
    public abstract void render();

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setRotation(float x, float y, float z) {
        this.rotX = x;
        this.rotY = y;
        this.rotZ = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getRotX() { return rotX; }
    public float getRotY() { return rotY; }
    public float getRotZ() { return rotZ; }
    public float getScale() { return scale; }
} 