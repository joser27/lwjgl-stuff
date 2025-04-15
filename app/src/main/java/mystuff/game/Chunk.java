package mystuff.game;

import mystuff.engine.Window;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.BufferUtils;
import mystuff.utils.Debug;
import java.nio.FloatBuffer;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    private Block[][][] blocks;
    private int chunkX, chunkY, chunkZ;
    private World world;
    private boolean isDirty;
    private int displayList;
    
    // VBO data
    private int vboVertexHandle;
    private int vboTextureHandle;
    private boolean needsVBOUpdate;
    private int vertexCount;
    
    // Track dirty regions for partial updates
    private boolean[][][] dirtyBlocks;
    private int dirtyMinX, dirtyMinY, dirtyMinZ;
    private int dirtyMaxX, dirtyMaxY, dirtyMaxZ;
    private boolean hasPartialUpdate;
    
    public Chunk(World world, int chunkX, int chunkY, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        this.dirtyBlocks = new boolean[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        this.isDirty = true;
        this.displayList = -1;
        
        // Initialize VBOs
        vboVertexHandle = GL15.glGenBuffers();
        vboTextureHandle = GL15.glGenBuffers();
        needsVBOUpdate = true;
        
        resetDirtyRegion();
    }
    
    private void resetDirtyRegion() {
        dirtyMinX = dirtyMinY = dirtyMinZ = CHUNK_SIZE;
        dirtyMaxX = dirtyMaxY = dirtyMaxZ = -1;
        hasPartialUpdate = false;
        
        // Reset dirty blocks array
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    dirtyBlocks[x][y][z] = false;
                }
            }
        }
    }
    
    private void markBlockDirty(int x, int y, int z) {
        if (!isValidPosition(x, y, z)) return;
        
        // If already marked dirty, don't process again
        if (dirtyBlocks[x][y][z]) return;
        
        dirtyBlocks[x][y][z] = true;
        hasPartialUpdate = true;
        
        // Update dirty region bounds
        dirtyMinX = Math.min(dirtyMinX, x);
        dirtyMinY = Math.min(dirtyMinY, y);
        dirtyMinZ = Math.min(dirtyMinZ, z);
        dirtyMaxX = Math.max(dirtyMaxX, x);
        dirtyMaxY = Math.max(dirtyMaxY, y);
        dirtyMaxZ = Math.max(dirtyMaxZ, z);
        
        // Mark surrounding blocks as dirty since their faces may need updating
        markBlockDirty(x+1, y, z);
        markBlockDirty(x-1, y, z);
        markBlockDirty(x, y+1, z);
        markBlockDirty(x, y-1, z);
        markBlockDirty(x, y, z+1);
        markBlockDirty(x, y, z-1);
    }
    
    public void setBlock(int localX, int localY, int localZ, BlockType type) {
        if (!isValidPosition(localX, localY, localZ)) return;
        
        // Convert local chunk coordinates to world coordinates
        float worldX = (chunkX * CHUNK_SIZE + localX) * World.BLOCK_SIZE;
        float worldY = (chunkY * CHUNK_SIZE + localY) * World.BLOCK_SIZE;
        float worldZ = (chunkZ * CHUNK_SIZE + localZ) * World.BLOCK_SIZE;
        
        blocks[localX][localY][localZ] = new Block(worldX, worldY, worldZ, type);
        
        // Mark this block and surrounding blocks as dirty
        markBlockDirty(localX, localY, localZ);
        
        // Mark neighboring chunks as dirty if the block is on the edge
        if (localX == 0) markNeighborDirty(-1, 0, 0);
        if (localX == CHUNK_SIZE - 1) markNeighborDirty(1, 0, 0);
        if (localY == 0) markNeighborDirty(0, -1, 0);
        if (localY == CHUNK_SIZE - 1) markNeighborDirty(0, 1, 0);
        if (localZ == 0) markNeighborDirty(0, 0, -1);
        if (localZ == CHUNK_SIZE - 1) markNeighborDirty(0, 0, 1);
    }

    private void markNeighborDirty(int dx, int dy, int dz) {
        Chunk neighbor = world.getChunk(chunkX + dx, chunkY + dy, chunkZ + dz);
        if (neighbor != null) {
            neighbor.isDirty = true;
        }
    }
    
    public Block getBlock(int localX, int localY, int localZ) {
        if (!isValidPosition(localX, localY, localZ)) return null;
        return blocks[localX][localY][localZ];
    }

    // Get block type, handling chunk boundaries
    private BlockType getBlockType(int localX, int localY, int localZ) {
        // If within this chunk's bounds, get from this chunk
        if (isValidPosition(localX, localY, localZ)) {
            Block block = blocks[localX][localY][localZ];
            return block != null ? block.getType() : BlockType.AIR;
        }
        
        // Otherwise, need to check neighboring chunk
        int neighborChunkX = chunkX;
        int neighborChunkY = chunkY;
        int neighborChunkZ = chunkZ;
        
        // Adjust coordinates and chunk position
        if (localX < 0) {
            neighborChunkX--;
            localX += CHUNK_SIZE;
        } else if (localX >= CHUNK_SIZE) {
            neighborChunkX++;
            localX -= CHUNK_SIZE;
        }
        
        if (localY < 0) {
            neighborChunkY--;
            localY += CHUNK_SIZE;
        } else if (localY >= CHUNK_SIZE) {
            neighborChunkY++;
            localY -= CHUNK_SIZE;
        }
        
        if (localZ < 0) {
            neighborChunkZ--;
            localZ += CHUNK_SIZE;
        } else if (localZ >= CHUNK_SIZE) {
            neighborChunkZ++;
            localZ -= CHUNK_SIZE;
        }
        
        // Get block from neighboring chunk
        Chunk neighbor = world.getChunk(neighborChunkX, neighborChunkY, neighborChunkZ);
        if (neighbor != null) {
            Block block = neighbor.getBlock(localX, localY, localZ);
            return block != null ? block.getType() : BlockType.AIR;
        }
        
        return BlockType.AIR;
    }
    
    private boolean isValidPosition(int x, int y, int z) {
        return x >= 0 && x < CHUNK_SIZE && 
               y >= 0 && y < CHUNK_SIZE && 
               z >= 0 && z < CHUNK_SIZE;
    }
    
    public void render() {
        if (isDirty) {
            rebuildDisplayList();
        } else if (hasPartialUpdate) {
            updateVBOData();
        }
        
        if (displayList != -1) {
            GL11.glCallList(displayList);
        }
        
        if (Debug.showBoundingBoxes()) {
            renderDebugBoundingBox();
        }
    }
    
    private void rebuildDisplayList() {
        if (displayList == -1) {
            displayList = GL11.glGenLists(1);
        }
        
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        
        // Enable texturing
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        
        // Render all blocks in the chunk
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    Block block = blocks[x][y][z];
                    if (block != null && block.getType() != BlockType.AIR) {
                        renderBlockFaces(x, y, z, block);
                    }
                }
            }
        }
        
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        
        GL11.glEndList();
        isDirty = false;
    }
    
    private void updateVBOData() {
        if (!hasPartialUpdate) return;
        
        // Only update the dirty region
        for (int x = Math.max(0, dirtyMinX - 1); x <= Math.min(CHUNK_SIZE - 1, dirtyMaxX + 1); x++) {
            for (int y = Math.max(0, dirtyMinY - 1); y <= Math.min(CHUNK_SIZE - 1, dirtyMaxY + 1); y++) {
                for (int z = Math.max(0, dirtyMinZ - 1); z <= Math.min(CHUNK_SIZE - 1, dirtyMaxZ + 1); z++) {
                    if (dirtyBlocks[x][y][z]) {
                        Block block = blocks[x][y][z];
                        if (block != null && block.getType() != BlockType.AIR) {
                            renderBlockFaces(x, y, z, block);
                        }
                    }
                }
            }
        }
        
        resetDirtyRegion();
    }
    
    private void renderBlockFaces(int x, int y, int z, Block block) {
        float worldX = (chunkX * CHUNK_SIZE + x) * World.BLOCK_SIZE;
        float worldY = (chunkY * CHUNK_SIZE + y) * World.BLOCK_SIZE;
        float worldZ = (chunkZ * CHUNK_SIZE + z) * World.BLOCK_SIZE;
        
        // Only render faces that are exposed to air or transparent blocks
        if (isTransparent(getBlockType(x, y, z + 1))) {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldX, worldY, worldZ);
            block.renderFace(Block.Face.FRONT);
            GL11.glPopMatrix();
        }
        
        if (isTransparent(getBlockType(x, y, z - 1))) {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldX, worldY, worldZ);
            block.renderFace(Block.Face.BACK);
            GL11.glPopMatrix();
        }
        
        if (isTransparent(getBlockType(x + 1, y, z))) {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldX, worldY, worldZ);
            block.renderFace(Block.Face.RIGHT);
            GL11.glPopMatrix();
        }
        
        if (isTransparent(getBlockType(x - 1, y, z))) {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldX, worldY, worldZ);
            block.renderFace(Block.Face.LEFT);
            GL11.glPopMatrix();
        }
        
        if (isTransparent(getBlockType(x, y + 1, z))) {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldX, worldY, worldZ);
            block.renderFace(Block.Face.TOP);
            GL11.glPopMatrix();
        }
        
        if (isTransparent(getBlockType(x, y - 1, z))) {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldX, worldY, worldZ);
            block.renderFace(Block.Face.BOTTOM);
            GL11.glPopMatrix();
        }
    }
    
    private boolean isTransparent(BlockType type) {
        return type == BlockType.AIR || type == null;
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
        
        // Delete VBO buffers
        GL15.glDeleteBuffers(vboVertexHandle);
        GL15.glDeleteBuffers(vboTextureHandle);
        
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