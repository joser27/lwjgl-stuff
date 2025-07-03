package mystuff.game;

import static org.lwjgl.opengl.GL11.*;
import mystuff.utils.TextureLoader;
import mystuff.utils.Shapes;
import mystuff.game.PlayerTextureMap.BodyPart;
import mystuff.utils.Debug;

public class PlayerRenderer {
    private static int playerTexture = -1;
    private static int headTexture = -1; // Separate texture for the head
    
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
        
        if (headTexture == -1) {
            headTexture = TextureLoader.loadTexture("resources/textures/lesterface.png");
            if (headTexture != -1) {
                glBindTexture(GL_TEXTURE_2D, headTexture);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
                System.out.println("Head texture (lesterface.png) loaded successfully!");
            } else {
                System.err.println("Failed to load head texture (lesterface.png)!");
            }
        }
    }

    public void render(Player player, float yaw, float pitch) {
        // Only render player model if texture is loaded AND we're in no-clip mode
        // In first-person mode, the body should be hidden
        if (playerTexture == -1 || !player.isNoClipMode()) return;

        glPushMatrix();
        
        // Move to player position and adjust height to make feet touch ground
        glTranslatef(player.getX(), player.getY() - LEG_HEIGHT/3, player.getZ());
        
        // Enable texturing
        glEnable(GL_TEXTURE_2D);
        
        // Draw head with rotation
        glPushMatrix();
        glTranslatef(0, BODY_HEIGHT + HEAD_SIZE/2, 0); // Position head above body
        glRotatef(-yaw, 0, 1, 0);  // Rotate around Y axis (left/right)
        glRotatef(-pitch, 1, 0, 0);  // Invert pitch rotation for natural up/down movement
        
        // Use head texture for the head
        if (headTexture != -1) {
            glBindTexture(GL_TEXTURE_2D, headTexture);
        } else {
            glBindTexture(GL_TEXTURE_2D, playerTexture);
        }
        
        // Set color to white to render texture properly
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        // Draw the head as a 10-sided polygon with two front faces for the face texture
        if (headTexture != -1) {
            // Use face texture
            glBindTexture(GL_TEXTURE_2D, headTexture);
            
            // Head dimensions
            float headRadius = HEAD_SIZE * 0.5f;
            float headHeight = HEAD_SIZE * 1.0f;
            float headDepth = HEAD_SIZE * 0.6f;
            
            // 10-sided polygon (decagon)
            int sides = 10;
            float angleStep = 2.0f * (float) Math.PI / sides;
            
            // Draw the head as a 10-sided prism
            for (int i = 0; i < sides; i++) {
                float angle1 = i * angleStep;
                float angle2 = (i + 1) * angleStep;
                
                // Calculate vertex positions for this face
                float x1 = (float) Math.cos(angle1) * headRadius;
                float z1 = (float) Math.sin(angle1) * headRadius;
                float x2 = (float) Math.cos(angle2) * headRadius;
                float z2 = (float) Math.sin(angle2) * headRadius;
                
                // Determine if this is a front face (facing forward)
                boolean isFrontFace = (angle1 >= -angleStep/2 && angle1 <= angleStep/2) || 
                                     (angle2 >= -angleStep/2 && angle2 <= angleStep/2) ||
                                     (angle1 <= -Math.PI + angleStep/2 && angle2 >= Math.PI - angleStep/2);
                
                if (isFrontFace) {
                    // Use face texture for front faces
                    glBindTexture(GL_TEXTURE_2D, headTexture);
                    
                    // Draw the front face with face texture
                    glBegin(GL_QUADS);
                    
                    // Map half the face texture to each front face
                    float u1 = (i == 0) ? 0.0f : 0.5f;  // Left half or right half
                    float u2 = (i == 0) ? 0.5f : 1.0f;
                    
                    glTexCoord2f(u1, 1.0f); glVertex3f(x1, -headHeight/2, headDepth/2);
                    glTexCoord2f(u2, 1.0f); glVertex3f(x2, -headHeight/2, headDepth/2);
                    glTexCoord2f(u2, 0.0f); glVertex3f(x2,  headHeight/2, headDepth/2);
                    glTexCoord2f(u1, 0.0f); glVertex3f(x1,  headHeight/2, headDepth/2);
                    
                    glEnd();
                } else {
                    // Use solid color for side faces
                    glDisable(GL_TEXTURE_2D);
                    glColor3f(0.75f, 0.55f, 0.35f); // Skin tone color
                    
                    glBegin(GL_QUADS);
                    glVertex3f(x1, -headHeight/2, headDepth/2);
                    glVertex3f(x2, -headHeight/2, headDepth/2);
                    glVertex3f(x2,  headHeight/2, headDepth/2);
                    glVertex3f(x1,  headHeight/2, headDepth/2);
                    glEnd();
                    
                    // Re-enable texturing
                    glEnable(GL_TEXTURE_2D);
                    glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                }
            }
            
            // Draw top and bottom faces
            glDisable(GL_TEXTURE_2D);
            glColor3f(0.75f, 0.55f, 0.35f); // Skin tone color
            
            // Top face
            glBegin(GL_POLYGON);
            for (int i = 0; i < sides; i++) {
                float angle = i * angleStep;
                float x = (float) Math.cos(angle) * headRadius;
                float z = (float) Math.sin(angle) * headRadius;
                glVertex3f(x, headHeight/2, z);
            }
            glEnd();
            
            // Bottom face
            glBegin(GL_POLYGON);
            for (int i = 0; i < sides; i++) {
                float angle = i * angleStep;
                float x = (float) Math.cos(angle) * headRadius;
                float z = (float) Math.sin(angle) * headRadius;
                glVertex3f(x, -headHeight/2, z);
            }
            glEnd();
            
            // Re-enable texturing and reset color
            glEnable(GL_TEXTURE_2D);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            
        } else {
            // Fallback: draw a simple colored sphere
            glDisable(GL_TEXTURE_2D);
            glColor3f(0.8f, 0.6f, 0.4f); // Skin tone color
            
            // Simple sphere using quad strips
            for (int i = 0; i < SPHERE_STACKS; i++) {
                float phi1 = (float) Math.PI * i / SPHERE_STACKS;
                float phi2 = (float) Math.PI * (i + 1) / SPHERE_STACKS;
                
                glBegin(GL_QUAD_STRIP);
                for (int j = 0; j <= SPHERE_SLICES; j++) {
                    float theta = 2.0f * (float) Math.PI * j / SPHERE_SLICES;
                    
                    float x1 = (float) (Math.sin(phi1) * Math.cos(theta));
                    float y1 = (float) Math.cos(phi1);
                    float z1 = (float) (Math.sin(phi1) * Math.sin(theta));
                    
                    float x2 = (float) (Math.sin(phi2) * Math.cos(theta));
                    float y2 = (float) Math.cos(phi2);
                    float z2 = (float) (Math.sin(phi2) * Math.sin(theta));
                    
                    glNormal3f(x1, y1, z1);
                    glVertex3f(HEAD_SIZE * x1, HEAD_SIZE * y1, HEAD_SIZE * z1);
                    
                    glNormal3f(x2, y2, z2);
                    glVertex3f(HEAD_SIZE * x2, HEAD_SIZE * y2, HEAD_SIZE * z2);
                }
                glEnd();
            }
            
            glEnable(GL_TEXTURE_2D);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        }
        glPopMatrix();

        // Switch back to player texture for body parts
        glBindTexture(GL_TEXTURE_2D, playerTexture);

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
            
            // Draw simple wireframe box
            float[] size = bb.getSize();
            float width = size[0];
            float height = size[1];
            float depth = size[2];
            
            // Draw the 12 edges of the bounding box
            glBegin(GL_LINES);
            // Front face
            glVertex3f(-width/2, -height/2, depth/2); glVertex3f(width/2, -height/2, depth/2);
            glVertex3f(width/2, -height/2, depth/2); glVertex3f(width/2, height/2, depth/2);
            glVertex3f(width/2, height/2, depth/2); glVertex3f(-width/2, height/2, depth/2);
            glVertex3f(-width/2, height/2, depth/2); glVertex3f(-width/2, -height/2, depth/2);
            // Back face
            glVertex3f(-width/2, -height/2, -depth/2); glVertex3f(width/2, -height/2, -depth/2);
            glVertex3f(width/2, -height/2, -depth/2); glVertex3f(width/2, height/2, -depth/2);
            glVertex3f(width/2, height/2, -depth/2); glVertex3f(-width/2, height/2, -depth/2);
            glVertex3f(-width/2, height/2, -depth/2); glVertex3f(-width/2, -height/2, -depth/2);
            // Connecting edges
            glVertex3f(-width/2, -height/2, depth/2); glVertex3f(-width/2, -height/2, -depth/2);
            glVertex3f(width/2, -height/2, depth/2); glVertex3f(width/2, -height/2, -depth/2);
            glVertex3f(width/2, height/2, depth/2); glVertex3f(width/2, height/2, -depth/2);
            glVertex3f(-width/2, height/2, depth/2); glVertex3f(-width/2, height/2, -depth/2);
            glEnd();
            
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