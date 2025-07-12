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
└── build.gradle           # Build configuration
```

## Development

The game is built with:
- **LWJGL 3.3.3**: OpenGL bindings and windowing
- **Gradle**: Build system and dependency management
- **OpenGL**: 3D graphics rendering
- **Java 11+**: Programming language

### Debug Tips
1. Use **F3** to enable debug mode for comprehensive information
2. **NoClip mode (N)** is great for exploring the 3D world
3. **Wireframe mode (F)** helps visualize geometry
4. **Lighting mode (L)** to test different atmospheric conditions
5. **Fog mode (T)** to test horror atmosphere effects
6. Monitor performance metrics to optimize gameplay

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

---

Enjoy exploring your 3D world! 🎮🌟