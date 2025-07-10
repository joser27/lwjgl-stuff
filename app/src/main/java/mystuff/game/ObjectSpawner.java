package mystuff.game;

import mystuff.utils.DebugRenderer;

/**
 * Helper class for easily spawning objects in the game.
 * Provides convenient methods for adding common objects with predefined settings.
 */
public class ObjectSpawner {
    
    /**
     * Spawn wooden stairs at a specific position
     */
    public static GLBObject spawnWoodenStairs(float x, float y, float z) {
        return spawnWoodenStairs(x, y, z, 1.0f, 0, 0, 0);
    }
    
    /**
     * Spawn wooden stairs with custom scale and rotation
     */
    public static GLBObject spawnWoodenStairs(float x, float y, float z, float scale, float rotX, float rotY, float rotZ) {
        GLBObject stairs = new GLBObject(x, y, z, "models/wooden_stairs_21.glb", "textures/wooden_stairs_21/", scale, rotX, rotY, rotZ);
        DebugRenderer.getInstance().addMessage("Spawned wooden stairs at (" + x + ", " + y + ", " + z + ")", 2.0f);
        return stairs;
    }
    
    /**
     * Spawn a house at a specific position
     */
    public static HouseMap spawnHouse(float x, float y, float z) {
        HouseMap house = new HouseMap(x, y, z);
        DebugRenderer.getInstance().addMessage("Spawned house at (" + x + ", " + y + ", " + z + ")", 2.0f);
        return house;
    }
    
    /**
     * Spawn any GLB model with automatic texture matching
     */
    public static GLBObject spawnGLBModel(float x, float y, float z, String modelPath, String textureFolder) {
        return spawnGLBModel(x, y, z, modelPath, textureFolder, 1.0f, 0, 0, 0);
    }
    
    /**
     * Spawn any GLB model with custom scale and rotation
     */
    public static GLBObject spawnGLBModel(float x, float y, float z, String modelPath, String textureFolder, 
                                        float scale, float rotX, float rotY, float rotZ) {
        GLBObject object = new GLBObject(x, y, z, modelPath, textureFolder, scale, rotX, rotY, rotZ);
        DebugRenderer.getInstance().addMessage("Spawned GLB model: " + modelPath + " at (" + x + ", " + y + ", " + z + ")", 2.0f);
        return object;
    }
    
    /**
     * Spawn a cat at a specific position
     */
    public static Cat spawnCat(float x, float y, float z) {
        Cat cat = new Cat(x, y, z);
        DebugRenderer.getInstance().addMessage("Spawned cat at (" + x + ", " + y + ", " + z + ")", 2.0f);
        return cat;
    }
    
    /**
     * Spawn a mage at a specific position
     */
    public static Mage spawnMage(float x, float y, float z) {
        Mage mage = new Mage(x, y, z);
        DebugRenderer.getInstance().addMessage("Spawned mage at (" + x + ", " + y + ", " + z + ")", 2.0f);
        return mage;
    }
    
    /**
     * Spawn a beggar at a specific position
     */
    public static Beggar spawnBeggar(float x, float y, float z) {
        Beggar beggar = new Beggar(x, y, z, null);
        DebugRenderer.getInstance().addMessage("Spawned beggar at (" + x + ", " + y + ", " + z + ")", 2.0f);
        return beggar;
    }
} 