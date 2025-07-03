package mystuff.game;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;

public class WorldBorderShader {
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    
    public WorldBorderShader() {
        programId = GL20.glCreateProgram();
        if (programId == 0) {
            throw new RuntimeException("Could not create shader program");
        }
        
        createVertexShader();
        createFragmentShader();
        link();
    }
    
    private void createVertexShader() {
        String vertexShaderCode = 
            "#version 330 core\n" +
            "layout (location = 0) in vec3 position;\n" +
            "layout (location = 1) in vec3 color;\n" +
            "\n" +
            "uniform mat4 projectionMatrix;\n" +
            "uniform mat4 viewMatrix;\n" +
            "uniform mat4 modelMatrix;\n" +
            "\n" +
            "out vec3 exColor;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);\n" +
            "    exColor = color;\n" +
            "}\n";
        
        vertexShaderId = createShader(vertexShaderCode, GL20.GL_VERTEX_SHADER);
    }
    
    private void createFragmentShader() {
        String fragmentShaderCode = 
            "#version 330 core\n" +
            "in vec3 exColor;\n" +
            "out vec4 fragColor;\n" +
            "\n" +
            "void main() {\n" +
            "    fragColor = vec4(exColor, 0.3); // 30% opacity\n" +
            "}\n";
        
        fragmentShaderId = createShader(fragmentShaderCode, GL20.GL_FRAGMENT_SHADER);
    }
    
    private int createShader(String shaderCode, int shaderType) {
        int shaderId = GL20.glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new RuntimeException("Error creating shader. Type: " + shaderType);
        }
        
        GL20.glShaderSource(shaderId, shaderCode);
        GL20.glCompileShader(shaderId);
        
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Error compiling shader code: " + GL20.glGetShaderInfoLog(shaderId, 1024));
        }
        
        GL20.glAttachShader(programId, shaderId);
        
        return shaderId;
    }
    
    private void link() {
        GL20.glLinkProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Error linking shader code: " + GL20.glGetProgramInfoLog(programId, 1024));
        }
        
        if (vertexShaderId != 0) {
            GL20.glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            GL20.glDetachShader(programId, fragmentShaderId);
        }
        
        GL20.glValidateProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating shader code: " + GL20.glGetProgramInfoLog(programId, 1024));
        }
    }
    
    public void bind() {
        GL20.glUseProgram(programId);
    }
    
    public void unbind() {
        GL20.glUseProgram(0);
    }
    
    public void cleanup() {
        unbind();
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
        }
    }
    
    public void setUniform(String uniformName, float value) {
        int location = GL20.glGetUniformLocation(programId, uniformName);
        if (location != -1) {
            GL20.glUniform1f(location, value);
        }
    }
    
    public void setUniform(String uniformName, int value) {
        int location = GL20.glGetUniformLocation(programId, uniformName);
        if (location != -1) {
            GL20.glUniform1i(location, value);
        }
    }
    
    public void setUniform(String uniformName, FloatBuffer value) {
        int location = GL20.glGetUniformLocation(programId, uniformName);
        if (location != -1) {
            GL20.glUniformMatrix4fv(location, false, value);
        }
    }
} 