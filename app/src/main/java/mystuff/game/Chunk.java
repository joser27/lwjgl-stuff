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
    private World world; // Reference to world for neighbor chunk access
    
    public Chunk(World world, int chunkX, int chunkY, int chunkZ) {
        this.world = world;
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

    private void renderBlockFaces(int x, int y, int z, Block block) {
        float size = World.BLOCK_SIZE / 2;
        float worldX = block.getX();
        float worldY = block.getY();
        float worldZ = block.getZ();

        // Only render faces that are exposed to air or transparent blocks
        BlockType type = block.getType();
        
        // Front face (positive Z)
        if (isTransparent(getBlockType(x, y, z + 1))) {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldX, worldY, worldZ);
            block.renderFace(Block.Face.FRONT);
            GL11.glPopMatrix();
        }
        
        // Back face (negative Z)
        if (isTransparent(getBlockType(x, y, z - 1))) {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldX, worldY, worldZ);
            block.renderFace(Block.Face.BACK);
            GL11.glPopMatrix();
        }
        
        // Right face (positive X)
        if (isTransparent(getBlockType(x + 1, y, z))) {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldX, worldY, worldZ);
            block.renderFace(Block.Face.RIGHT);
            GL11.glPopMatrix();
        }
        
        // Left face (negative X)
        if (isTransparent(getBlockType(x - 1, y, z))) {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldX, worldY, worldZ);
            block.renderFace(Block.Face.LEFT);
            GL11.glPopMatrix();
        }
        
        // Top face (positive Y)
        if (isTransparent(getBlockType(x, y + 1, z))) {
            GL11.glPushMatrix();
            GL11.glTranslatef(worldX, worldY, worldZ);
            block.renderFace(Block.Face.TOP);
            GL11.glPopMatrix();
        }
        
        // Bottom face (negative Y)
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