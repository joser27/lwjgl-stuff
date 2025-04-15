package mystuff.game;

import mystuff.engine.Window;
import org.lwjgl.opengl.GL11;
import mystuff.utils.Debug;

public class Chunk {
    public static final int CHUNK_SIZE = 16; // Size of chunk in blocks (16x16x16)
    private Block[][][] blocks;
    private int chunkX, chunkY, chunkZ; // Chunk coordinates (not world coordinates)
    private boolean isDirty; // Flag to indicate if chunk needs to be re-rendered
    private int displayList; // OpenGL display list for rendering
    
    public Chunk(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        this.isDirty = true;
        this.displayList = -1;
    }
    
    public void setBlock(int localX, int localY, int localZ, BlockType type) {
        if (!isValidPosition(localX, localY, localZ)) return;
        
        // Convert local chunk coordinates to world coordinates
        float worldX = (chunkX * CHUNK_SIZE + localX) * World.BLOCK_SIZE;
        float worldY = (chunkY * CHUNK_SIZE + localY) * World.BLOCK_SIZE;
        float worldZ = (chunkZ * CHUNK_SIZE + localZ) * World.BLOCK_SIZE;
        
        blocks[localX][localY][localZ] = new Block(worldX, worldY, worldZ, type);
        isDirty = true; // Mark chunk for re-rendering
    }
    
    public Block getBlock(int localX, int localY, int localZ) {
        if (!isValidPosition(localX, localY, localZ)) return null;
        return blocks[localX][localY][localZ];
    }
    
    private boolean isValidPosition(int x, int y, int z) {
        return x >= 0 && x < CHUNK_SIZE && 
               y >= 0 && y < CHUNK_SIZE && 
               z >= 0 && z < CHUNK_SIZE;
    }
    
    public void render() {
        if (isDirty) {
            rebuildDisplayList();
        }
        
        if (displayList != -1) {
            GL11.glCallList(displayList);
            
            // Render debug information if enabled
            if (Debug.showBoundingBoxes()) {
                renderDebugBoundingBox();
            }
        }
    }
    
    private void rebuildDisplayList() {
        if (displayList == -1) {
            displayList = GL11.glGenLists(1);
        }
        
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        
        // Render all blocks in the chunk
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    Block block = blocks[x][y][z];
                    if (block != null && block.getType() != BlockType.AIR) {
                        block.render();
                    }
                }
            }
        }
        
        GL11.glEndList();
        isDirty = false;
    }
    
    private void renderDebugBoundingBox() {
        float worldX = chunkX * CHUNK_SIZE * World.BLOCK_SIZE;
        float worldY = chunkY * CHUNK_SIZE * World.BLOCK_SIZE;
        float worldZ = chunkZ * CHUNK_SIZE * World.BLOCK_SIZE;
        float size = CHUNK_SIZE * World.BLOCK_SIZE;
        
        GL11.glPushMatrix();
        GL11.glTranslatef(worldX + size/2, worldY + size/2, worldZ + size/2);
        GL11.glColor3f(1.0f, 1.0f, 0.0f); // Yellow for chunk boundaries
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        mystuff.utils.Shapes.cuboid(size, size, size);
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        GL11.glPopMatrix();
    }
    
    public void cleanup() {
        if (displayList != -1) {
            GL11.glDeleteLists(displayList, 1);
            displayList = -1;
        }
        
        // Cleanup blocks
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    if (blocks[x][y][z] != null) {
                        blocks[x][y][z].cleanup();
                    }
                }
            }
        }
    }
    
    public int getChunkX() { return chunkX; }
    public int getChunkY() { return chunkY; }
    public int getChunkZ() { return chunkZ; }
    
    // Convert world coordinates to chunk coordinates
    public static int worldToChunkCoord(float worldCoord) {
        return (int) Math.floor(worldCoord / (CHUNK_SIZE * World.BLOCK_SIZE));
    }
    
    // Convert world coordinates to local chunk coordinates
    public static int worldToLocalCoord(float worldCoord) {
        int localCoord = (int) Math.floor(worldCoord / World.BLOCK_SIZE) % CHUNK_SIZE;
        return localCoord >= 0 ? localCoord : localCoord + CHUNK_SIZE;
    }
} 