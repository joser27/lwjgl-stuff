package mystuff.game;

import static org.lwjgl.opengl.GL11.*;
import mystuff.utils.TextureLoader;
import mystuff.utils.Shapes;
import mystuff.game.PlayerTextureMap.BodyPart;
import mystuff.utils.Debug;

public class PlayerRenderer {
    private static int playerTexture = -1;
    
    // Model dimensions
    private static final float HEAD_SIZE = 0.5f;
    private static final float NECK_SIZE = 0.25f;
    private static final float NECK_HEIGHT = 0.125f;
    private static final float BODY_WIDTH = 0.4f;
    private static final float BODY_HEIGHT = 0.8f;
    private static final float BODY_DEPTH = 0.2f;
    private static final float ARM_WIDTH = 0.25f;
    private static final float ARM_HEIGHT = 0.75f;
    private static final float ARM_DEPTH = 0.25f;
    private static final float LEG_WIDTH = 0.25f;
    private static final float LEG_HEIGHT = 0.75f;
    private static final float LEG_DEPTH = 0.25f;
    private static final int SPHERE_SLICES = 16;
    private static final int SPHERE_STACKS = 16;
    
    public void init() {
        if (playerTexture == -1) {
            playerTexture = TextureLoader.loadTexture("resources/textures/player.png");
            if (playerTexture != -1) {
                glBindTexture(GL_TEXTURE_2D, playerTexture);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                System.out.println("Player texture loaded successfully!");
            } else {
                System.err.println("Failed to load player texture!");
            }
        }
    }

    public void render(Player player, float yaw, float pitch) {
        // Only render player model in no-clip mode
        if (playerTexture == -1 || !player.isNoClipMode()) return;

        glPushMatrix();
        
        // Move to player position and adjust height to make feet touch ground
        glTranslatef(player.getX(), player.getY() - LEG_HEIGHT/3, player.getZ());
        
        // Enable texturing
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, playerTexture);
        
        // Set color to white to render texture properly
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Draw head with rotation
        glPushMatrix();
        glTranslatef(0, BODY_HEIGHT + HEAD_SIZE/2, 0); // Position head above body
        glRotatef(-yaw, 0, 1, 0);  // Rotate around Y axis (left/right)
        glRotatef(-pitch, 1, 0, 0);  // Invert pitch rotation for natural up/down movement
        
        // Set up texture coordinates for the head
        float startU = 160.0f/1280.0f;
        float startV = 160.0f/640.0f;
        float uvWidth = 160.0f/1280.0f;
        float uvHeight = 160.0f/640.0f;

        // Draw the head using improved spherical UV mapping
        for (int i = 0; i < SPHERE_STACKS; i++) {
            float phi1 = (float) Math.PI * i / SPHERE_STACKS;
            float phi2 = (float) Math.PI * (i + 1) / SPHERE_STACKS;
            
            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= SPHERE_SLICES; j++) {
                float theta = 2.0f * (float) Math.PI * j / SPHERE_SLICES;
                
                // Calculate UV coordinates based on spherical position
                float u = startU + uvWidth * (j / (float) SPHERE_SLICES);
                float v1 = startV + uvHeight * (i / (float) SPHERE_STACKS);
                float v2 = startV + uvHeight * ((i + 1) / (float) SPHERE_STACKS);
                
                // First vertex
                float x1 = (float) (Math.sin(phi1) * Math.cos(theta));
                float y1 = (float) Math.cos(phi1);
                float z1 = (float) (Math.sin(phi1) * Math.sin(theta));
                glTexCoord2f(u, v1);
                glNormal3f(x1, y1, z1);
                glVertex3f(HEAD_SIZE * x1, HEAD_SIZE * y1, HEAD_SIZE * z1);
                
                // Second vertex
                float x2 = (float) (Math.sin(phi2) * Math.cos(theta));
                float y2 = (float) Math.cos(phi2);
                float z2 = (float) (Math.sin(phi2) * Math.sin(theta));
                glTexCoord2f(u, v2);
                glNormal3f(x2, y2, z2);
                glVertex3f(HEAD_SIZE * x2, HEAD_SIZE * y2, HEAD_SIZE * z2);
            }
            glEnd();
        }
        glPopMatrix();

        // Draw body (cuboid) without rotation
        glPushMatrix();
        glTranslatef(0, BODY_HEIGHT/2, 0);
        renderBody();
        glPopMatrix();

        // Draw arms and legs without rotation
        renderArms();
        renderLegs();
        
        // Cleanup
        glDisable(GL_TEXTURE_2D);
        
        // Render bounding box if debug mode is enabled
        if (Debug.showBoundingBoxes()) {
            BoundingBox bb = player.getBoundingBox();
            glPushMatrix();
            // Reset position since we're already at player's position
            glColor3f(1.0f, 0.0f, 0.0f);  // Red for player bounding box
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);  // Wireframe mode
            Shapes.cuboid(bb.getWidth(), bb.getHeight(), bb.getDepth());
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);  // Back to fill mode
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);  // Reset color
            glPopMatrix();
        }

        glPopMatrix();
    }
    
    private void renderHead() {
        float size = HEAD_SIZE;
        
        // Front face
        renderQuad(BodyPart.HEAD_FRONT, 
            new float[] {-size, -size, size},
            new float[] {size, -size, size},
            new float[] {size, size, size},
            new float[] {-size, size, size}
        );
        
        // Back face
        renderQuad(BodyPart.HEAD_BACK,
            new float[] {-size, -size, -size},
            new float[] {size, -size, -size},
            new float[] {size, size, -size},
            new float[] {-size, size, -size}
        );
        
        // Right face
        renderQuad(BodyPart.HEAD_RIGHT,
            new float[] {size, -size, -size},
            new float[] {size, -size, size},
            new float[] {size, size, size},
            new float[] {size, size, -size}
        );
        
        // Left face
        renderQuad(BodyPart.HEAD_LEFT,
            new float[] {-size, -size, -size},
            new float[] {-size, -size, size},
            new float[] {-size, size, size},
            new float[] {-size, size, -size}
        );
        
        // Top face
        renderQuad(BodyPart.HEAD_TOP,
            new float[] {-size, size, -size},
            new float[] {size, size, -size},
            new float[] {size, size, size},
            new float[] {-size, size, size}
        );
        
        // Bottom face
        renderQuad(BodyPart.HEAD_BOTTOM,
            new float[] {-size, -size, -size},
            new float[] {size, -size, -size},
            new float[] {size, -size, size},
            new float[] {-size, -size, size}
        );
    }

    private void renderNeck() {
        float size = NECK_SIZE;
        float height = NECK_HEIGHT;
        
        // Front face
        renderQuad(BodyPart.BODY_FRONT,
            new float[] {-size, -height/2, size},
            new float[] {size, -height/2, size},
            new float[] {size, height/2, size},
            new float[] {-size, height/2, size}
        );
        
        // Back face
        renderQuad(BodyPart.BODY_BACK,
            new float[] {-size, -height/2, -size},
            new float[] {size, -height/2, -size},
            new float[] {size, height/2, -size},
            new float[] {-size, height/2, -size}
        );
    }

    private void renderBody() {
        float width = BODY_WIDTH;
        float height = BODY_HEIGHT;
        float depth = BODY_DEPTH;
        
        // Front face
        renderQuad(BodyPart.BODY_FRONT,
            new float[] {-width, -height/2, depth},
            new float[] {width, -height/2, depth},
            new float[] {width, height/2, depth},
            new float[] {-width, height/2, depth}
        );
        
        // Back face
        renderQuad(BodyPart.BODY_BACK,
            new float[] {-width, -height/2, -depth},
            new float[] {width, -height/2, -depth},
            new float[] {width, height/2, -depth},
            new float[] {-width, height/2, -depth}
        );
        
        // Right face
        renderQuad(BodyPart.BODY_RIGHT,
            new float[] {width, -height/2, -depth},
            new float[] {width, -height/2, depth},
            new float[] {width, height/2, depth},
            new float[] {width, height/2, -depth}
        );
        
        // Left face
        renderQuad(BodyPart.BODY_LEFT,
            new float[] {-width, -height/2, -depth},
            new float[] {-width, -height/2, depth},
            new float[] {-width, height/2, depth},
            new float[] {-width, height/2, -depth}
        );
    }

    private void renderArm(boolean isRight) {
        float width = ARM_WIDTH;
        float height = ARM_HEIGHT;
        float depth = ARM_DEPTH;
        BodyPart front = isRight ? BodyPart.ARM_RIGHT_FRONT : BodyPart.ARM_LEFT_FRONT;
        BodyPart back = isRight ? BodyPart.ARM_RIGHT_BACK : BodyPart.ARM_LEFT_BACK;
        BodyPart right = isRight ? BodyPart.ARM_RIGHT_RIGHT : BodyPart.ARM_LEFT_RIGHT;
        BodyPart left = isRight ? BodyPart.ARM_RIGHT_LEFT : BodyPart.ARM_LEFT_LEFT;
        
        // Front face
        renderQuad(front,
            new float[] {-width/2, -height, depth/2},
            new float[] {width/2, -height, depth/2},
            new float[] {width/2, 0, depth/2},
            new float[] {-width/2, 0, depth/2}
        );
        
        // Back face
        renderQuad(back,
            new float[] {-width/2, -height, -depth/2},
            new float[] {width/2, -height, -depth/2},
            new float[] {width/2, 0, -depth/2},
            new float[] {-width/2, 0, -depth/2}
        );
        
        // Right face
        renderQuad(right,
            new float[] {width/2, -height, -depth/2},
            new float[] {width/2, -height, depth/2},
            new float[] {width/2, 0, depth/2},
            new float[] {width/2, 0, -depth/2}
        );
        
        // Left face
        renderQuad(left,
            new float[] {-width/2, -height, -depth/2},
            new float[] {-width/2, -height, depth/2},
            new float[] {-width/2, 0, depth/2},
            new float[] {-width/2, 0, -depth/2}
        );
    }

    private void renderLeg(boolean isRight) {
        float width = LEG_WIDTH;
        float height = LEG_HEIGHT;
        float depth = LEG_DEPTH;
        BodyPart front = isRight ? BodyPart.LEG_RIGHT_FRONT : BodyPart.LEG_LEFT_FRONT;
        BodyPart back = isRight ? BodyPart.LEG_RIGHT_BACK : BodyPart.LEG_LEFT_BACK;
        BodyPart right = isRight ? BodyPart.LEG_RIGHT_RIGHT : BodyPart.LEG_LEFT_RIGHT;
        BodyPart left = isRight ? BodyPart.LEG_RIGHT_LEFT : BodyPart.LEG_LEFT_LEFT;
        
        // Front face
        renderQuad(front,
            new float[] {-width/2, -height, depth/2},
            new float[] {width/2, -height, depth/2},
            new float[] {width/2, 0, depth/2},
            new float[] {-width/2, 0, depth/2}
        );
        
        // Back face
        renderQuad(back,
            new float[] {-width/2, -height, -depth/2},
            new float[] {width/2, -height, -depth/2},
            new float[] {width/2, 0, -depth/2},
            new float[] {-width/2, 0, -depth/2}
        );
        
        // Right face
        renderQuad(right,
            new float[] {width/2, -height, -depth/2},
            new float[] {width/2, -height, depth/2},
            new float[] {width/2, 0, depth/2},
            new float[] {width/2, 0, -depth/2}
        );
        
        // Left face
        renderQuad(left,
            new float[] {-width/2, -height, -depth/2},
            new float[] {-width/2, -height, depth/2},
            new float[] {-width/2, 0, depth/2},
            new float[] {-width/2, 0, -depth/2}
        );
    }
    
    private void renderQuad(BodyPart part, float[] v1, float[] v2, float[] v3, float[] v4) {
        glBegin(GL_QUADS);
        glTexCoord2f(part.u, part.getEndV()); glVertex3f(v1[0], v1[1], v1[2]);
        glTexCoord2f(part.getEndU(), part.getEndV()); glVertex3f(v2[0], v2[1], v2[2]);
        glTexCoord2f(part.getEndU(), part.v); glVertex3f(v3[0], v3[1], v3[2]);
        glTexCoord2f(part.u, part.v); glVertex3f(v4[0], v4[1], v4[2]);
        glEnd();
    }
    
    private void renderArms() {
        // Right Arm
        glPushMatrix();
        glTranslatef(BODY_WIDTH + ARM_WIDTH/2, BODY_HEIGHT, 0);
        renderArm(true);
        glPopMatrix();

        // Left Arm
        glPushMatrix();
        glTranslatef(-(BODY_WIDTH + ARM_WIDTH/2), BODY_HEIGHT, 0);
        renderArm(false);
        glPopMatrix();
    }

    private void renderLegs() {
        // Right Leg
        glPushMatrix();
        glTranslatef(LEG_WIDTH/2, 0, 0);
        renderLeg(true);
        glPopMatrix();

        // Left Leg
        glPushMatrix();
        glTranslatef(-LEG_WIDTH/2, 0, 0);
        renderLeg(false);
        glPopMatrix();
    }
    
    public void cleanup() {
        if (playerTexture != -1) {
            glDeleteTextures(playerTexture);
            playerTexture = -1;
        }
    }
} 