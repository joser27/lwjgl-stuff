# LWJGL 3D Game Engine

A 3D game engine built with Java and LWJGL (Lightweight Java Game Library). Features a fully explorable world with 3D models, physics, lighting systems, and various gameplay modes.

![OpenGL](https://img.shields.io/badge/OpenGL-4.6-blue)
![LWJGL](https://img.shields.io/badge/LWJGL-3.3.3-green)
![Java](https://img.shields.io/badge/Java-11+-orange)

## Features

- **3D World Rendering**: Support for OBJ and GLB model formats
- **First-Person Camera**: Smooth mouse-look controls with pitch/yaw rotation
- **Physics System**: Realistic gravity, collision detection, and jumping
- **Dynamic Lighting System**: 5 different lighting modes (Day, Night, Dusk, Dawn, Horror Night)
- **Atmospheric Fog System**: Multiple fog types for horror atmosphere (Silent Hill style)
- **Entity System**: Animated characters (Mage, Beggar, Cat) with walking and attack animations
- **3D Model Support**: GLB and OBJ model loading with texture mapping
- **Multiple Game Modes**: Normal mode and NoClip (spectator) mode
- **Sprint System**: Hold Shift while moving forward to sprint
- **Performance Monitoring**: Real-time FPS, frame time, and CPU utilization
- **Debug Tools**: Comprehensive debugging features for development
- **Dynamic Horror Effects**: Adjustable fog intensity and horror levels

## How to Run

### Prerequisites
- Java 11 or higher
- Windows OS (native libraries are configured for Windows)

### Running the Game
From the project root directory:

```powershell
.\gradlew.bat run
```

The game will launch in a 1920x1080 window targeting 144 FPS.

## Game Distribution & Bundling

### Creating Distributable Game

The game can be bundled into a native Windows executable that includes the Java Runtime Environment, so players don't need to install Java separately.

#### Quick Bundle (Full JRE)
```powershell
# Create a bundled game with full JRE (~170 MB)
.\gradlew.bat clean bundleGame
.\gradlew.bat zipGame
```

#### Optimized Bundle (Recommended)
```powershell
# Create a bundled game with custom minimal JRE (~130 MB, 25% smaller)
.\gradlew.bat clean bundleGameOptimized
.\gradlew.bat zipGameOptimized
```

#### Bundle Options

| Bundle Type | Size | Use Case |
|-------------|------|----------|
| **Full JRE** | ~170 MB | Maximum compatibility |
| **Optimized JRE** | ~130 MB | **Recommended for distribution** |

### Distribution Files

After bundling, you'll find:

- **Full Bundle**: `app/build/dist/LWJGLGame.zip`
- **Optimized Bundle**: `app/build/dist/LWJGLGame-Optimized.zip`

### What Players Get

The bundled game includes:
- `LWJGLGame.exe` - Native executable (no Java required!)
- `natives/` - LWJGL native libraries
- `jre/` - Bundled Java Runtime Environment
- All game resources and textures

### Uploading to Distribution Platforms

#### Itch.io
1. Upload the zip file (`LWJGLGame-Optimized.zip` recommended)
2. Set as Windows executable download
3. Players can download and run without Java installation

#### Other Platforms
- **Steam**: Use the bundled executable
- **Direct Distribution**: Share the zip file directly

### Bundle Optimization

The optimized bundle uses:
- **Custom JRE**: Only includes required Java modules (~45 MB vs ~150 MB)
- **jlink**: Creates minimal runtime with essential modules
- **Compression**: Optimized file compression for smaller downloads

#### Included Java Modules
- `java.base` - Core Java functionality
- `java.desktop` - GUI support (for LWJGL)
- `java.logging` - Logging system
- `java.naming` - JNDI support
- `java.sql` - Database connectivity
- `java.xml` - XML processing

## Controls

### Movement
| Key | Action |
|-----|--------|
| **W** | Move Forward |
| **S** | Move Backward |
| **A** | Strafe Left |
| **D** | Strafe Right |
| **Space** | Jump (when on ground) |
| **Left Shift** | Sprint (while moving forward) |
| **Mouse** | Look around (pitch/yaw) |

### Game Controls
| Key | Action |
|-----|--------|
| **Escape** | Exit Game |
| **P** | Pause/Unpause Game |
| **F** | Toggle Wireframe Mode |
| **N** | Toggle NoClip Mode (Spectator) |
| **F3** | Toggle Debug Mode |

### Lighting Controls
| Key | Action |
|-----|--------|
| **L** | Cycle Through Lighting Modes (Day → Night → Dusk → Dawn → Horror Night) |

### Fog Controls
| Key | Action |
|-----|--------|
| **T** | Cycle Through Fog Types (None, Light Mist, Dense Fog, Dark Mist, Storm Fog, Night Fog) |
| **Up Arrow** | Increase Horror Intensity |
| **Down Arrow** | Decrease Horror Intensity |

### Animation Controls
| Key | Action |
|-----|--------|
| **M** | Start Mage Attack Animation |
| **B** | Toggle Beggar Walking Animation |

### Time Controls
| Key | Action |
|-----|--------|
| **[** | Decrease Time Scale |
| **]** | Increase Time Scale |

### Debug Controls
| Key | Action |
|-----|--------|
| **F3** | Toggle Debug Mode (enables all debug features) |

## Lighting System

The game features a dynamic lighting system with 5 different modes:

### Lighting Modes
- **DAY**: Bright daylight with sky blue background
- **NIGHT**: Dark night with blue-tinted moonlight and dark blue sky
- **DUSK**: Warm orange sunset lighting with orange sky
- **DAWN**: Cool blue sunrise lighting with blue sky
- **HORROR_NIGHT**: Extremely dark horror atmosphere with almost black sky

### Features
- **Global Lighting**: All objects are affected consistently
- **Dynamic Sky Colors**: Each mode has its own atmospheric sky color
- **Fog Integration**: Fog colors automatically match the lighting mode
- **Realistic Night Lighting**: Low ambient light with blue-tinted moonlight

## Entity System

The game includes animated 3D entities:

### Available Entities
- **Mage**: Animated character with attack animations
- **Beggar**: Walking character with looped walking animations
- **Cat**: Static 3D model
- **House**: 3D house model with detailed geometry

### Animation Features
- **Frame-based Animation**: Smooth animation playback
- **Looping Support**: Animations can be set to loop continuously
- **Multiple Animation Types**: Walking, attack, and idle animations
- **OBJ Sequence Loading**: Support for animation frame sequences

## Debug Features

The game includes comprehensive debug tools accessible via **F3**:

### Performance Metrics
- **FPS Counter**: Real-time frames per second
- **Frame Time**: Current, average, maximum, and minimum frame times
- **CPU Utilization**: Percentage and visual graph
- **Memory Usage**: Current and total memory consumption
- **Frame Timing Breakdown**: Update, render, and sleep times

### Player Information
- **Position**: Real-time X, Y, Z coordinates
- **Rotation**: Camera pitch and yaw angles
- **Game Mode**: Current mode (Normal/NoClip)
- **Sprint Status**: Visual indicator when sprinting
- **Wireframe Status**: Shows when wireframe mode is active
- **Game Time**: Total elapsed game time
- **Lighting Mode**: Current lighting mode
- **Fog Information**: Current fog type and visibility range

### Entity Information
- **Mage Animation**: Current animation state and frame count
- **Beggar Animation**: Walking status and frame information
- **Collision Detection**: Performance metrics for collision checks

### Performance Warnings
- **Frame Time Spikes**: Alerts when frame times exceed 32ms
- **High Memory Usage**: Warning when memory usage exceeds 90%

## Game Modes

### Normal Mode (Default)
- Standard 3D physics with gravity
- Collision detection with world objects
- Sprint by holding Shift while moving forward
- Jump with Space when on ground
- Smooth step-up and step-down movement

### NoClip Mode (Press N)
- Free camera movement (spectator mode)
- No collision detection
- Fly through objects and terrain
- Independent camera movement from player position
- Useful for exploring and debugging

## World Features

- **3D Models**: GLB and OBJ model support with texture mapping
- **Dynamic Lighting**: 5 different lighting modes with atmospheric effects
- **Atmospheric Fog**: 6 different fog types for horror atmosphere
- **Collision System**: Geometry-based collision detection for 3D models
- **Entity Animations**: Walking and attack animations for characters
- **Texture Support**: PNG texture loading with material mapping
- **Model Rendering**: Support for complex 3D models with multiple materials

## Technical Details

### Graphics
- **OpenGL 4.6** with NVIDIA GPU support
- **3D Model Loading**: GLB and OBJ format support
- **Texture Loading**: PNG texture support with material mapping
- **Font Rendering**: TrueType font support
- **Wireframe Mode**: Toggle between solid and wireframe rendering
- **Dynamic Lighting**: OpenGL fixed-function lighting with multiple modes

### Performance
- **Target FPS**: 144 FPS (configurable)
- **High-Precision Threading**: Better timing accuracy
- **Busy Wait Sleep Mode**: Reduced input latency
- **Optimized Rendering**: Efficient 3D model rendering

### Architecture
- **Component-Based**: Separate systems for physics, rendering, and input
- **Entity System**: GameObject base class for all world objects
- **Modular Design**: Clean separation between engine and game logic
- **Lighting Manager**: Centralized lighting system with multiple modes
- **Fog Renderer**: Atmospheric fog system with horror effects

## Project Structure

```
app/
├── src/main/java/
│   ├── mystuff/engine/     # Core engine components
│   ├── mystuff/game/       # Game-specific logic and entities
│   └── mystuff/utils/      # Utility classes (lighting, fog, model loading)
├── resources/              # Game assets
│   ├── textures/          # Textures for models and entities
│   ├── models/            # 3D models (GLB, OBJ files)
│   ├── animations/        # Animation frame sequences
│   └── fonts/             # Font files
├── build/                  # Build artifacts (generated)
│   ├── libs/              # Compiled JARs
│   ├── bundled-game/      # Full JRE bundle
│   ├── bundled-game-optimized/  # Optimized JRE bundle
│   ├── custom-jre/        # Custom minimal JRE
│   ├── dist/              # Distribution zips
│   └── packr-all-4.0.0.jar # Packr bundling tool
└── build.gradle           # Build configuration with bundling tasks
```

## Development

The game is built with:
- **LWJGL 3.3.3**: OpenGL bindings and windowing
- **Gradle**: Build system and dependency management
- **OpenGL**: 3D graphics rendering
- **Java 11+**: Programming language
- **Packr**: Game bundling and distribution
- **jlink**: Custom JRE creation for optimization

### Available Gradle Tasks

#### Core Development
```powershell
.\gradlew.bat run              # Run the game
.\gradlew.bat clean            # Clean build artifacts
.\gradlew.bat shadowJar        # Create fat JAR with dependencies
```

#### Game Bundling
```powershell
.\gradlew.bat bundleGame       # Bundle with full JRE (~170 MB)
.\gradlew.bat bundleGameOptimized  # Bundle with custom JRE (~130 MB)
.\gradlew.bat zipGame          # Create distribution zip (full JRE)
.\gradlew.bat zipGameOptimized # Create distribution zip (optimized)
```

#### Asset Management
```powershell
.\gradlew.bat extractTextures  # Extract textures from GLB models
.\gradlew.bat createLauncher   # Create launcher script for bundled game
```

#### JRE Management
```powershell
.\gradlew.bat createCustomJRE  # Create minimal custom JRE (~45 MB)
.\gradlew.bat downloadPackr    # Download Packr bundling tool
.\gradlew.bat downloadJRE      # Download full JRE for bundling
```

### Development Workflow

1. **Development**: Use `.\gradlew.bat run` for testing
2. **Testing**: Use debug mode (F3) and NoClip mode (N)
3. **Bundling**: Use `.\gradlew.bat bundleGameOptimized` for distribution
4. **Distribution**: Upload `LWJGLGame-Optimized.zip` to platforms

### Debug Tips
1. Use **F3** to enable debug mode for comprehensive information
2. **NoClip mode (N)** is great for exploring the 3D world
3. **Wireframe mode (F)** helps visualize geometry
4. **Lighting mode (L)** to test different atmospheric conditions
5. **Fog mode (T)** to test horror atmosphere effects
6. Monitor performance metrics to optimize gameplay
7. Use optimized bundle for faster player downloads

## Troubleshooting

### Model Loading Issues
If 3D models fail to load, ensure the `app/resources/` directory contains:
- `models/` directory with GLB and OBJ files
- `textures/` directory with PNG texture files
- `animations/` directory with animation frame sequences

### Performance Issues
- Monitor FPS and frame time with debug mode
- Check CPU utilization graph for bottlenecks
- Use wireframe mode to identify rendering issues

### Lighting Issues
- Press **L** to cycle through lighting modes
- Ensure fog is properly configured for the lighting mode
- Check that all models have proper material assignments

### Bundling Issues

#### jlink Not Found
If you get "jlink not found" errors:
- Ensure you have JDK 11+ installed
- Check that `JAVA_HOME` is set correctly
- Use `.\gradlew.bat bundleGame` for full JRE bundle instead

#### Configuration Cache Errors
If you encounter Gradle configuration cache issues:
- The project has `org.gradle.configuration-cache=false` in `gradle.properties`
- This ensures bundling tasks work correctly

#### Bundle Size Issues
- Use `bundleGameOptimized` for smaller distribution files
- Custom JRE saves ~40-50 MB compared to full JRE
- Check bundle size output in console for verification

#### Distribution Issues
- Ensure `LWJGLGame-Optimized.zip` is uploaded as Windows executable
- Players don't need Java installed to run the bundled game
- Test the bundled executable before distribution

---

Enjoy exploring your 3D world! 🎮🌟