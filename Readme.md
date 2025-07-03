# LWJGL Minecraft Clone

A 3D voxel-based Minecraft-like game built with Java and LWJGL (Lightweight Java Game Library). Features a fully explorable world with block-based terrain, physics, and various gameplay modes.

![OpenGL](https://img.shields.io/badge/OpenGL-4.6-blue)
![LWJGL](https://img.shields.io/badge/LWJGL-3.3.3-green)
![Java](https://img.shields.io/badge/Java-11+-orange)

## Features

- **3D Voxel World**: Minecraft-style block-based terrain generation
- **First-Person Camera**: Smooth mouse-look controls with pitch/yaw rotation
- **Physics System**: Realistic gravity, collision detection, and jumping
- **Chunk-based Rendering**: Optimized world loading and culling
- **Multiple Game Modes**: Normal mode and NoClip (spectator) mode
- **Sprint System**: Hold Shift while moving forward to sprint
- **Performance Monitoring**: Real-time FPS, frame time, and CPU utilization
- **Debug Tools**: Comprehensive debugging features for development

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
| **R** | Reset Player Position to (5, 5, 5) |

### Time Controls
| Key | Action |
|-----|--------|
| **[** | Decrease Time Scale |
| **]** | Increase Time Scale |

### Debug Controls
| Key | Action |
|-----|--------|
| **F3** | Toggle Debug Mode (enables all debug features) |

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

### Performance Warnings
- **Frame Time Spikes**: Alerts when frame times exceed 32ms
- **High Memory Usage**: Warning when memory usage exceeds 90%

### Physics Debug
- **Collision Detection**: Performance metrics for block collision checks
- **Ground Detection**: Real-time ground contact status
- **Velocity Tracking**: Current movement velocity

## Game Modes

### Normal Mode (Default)
- Standard Minecraft-like physics
- Gravity affects the player
- Collision detection with blocks
- Sprint by holding Shift while moving forward
- Jump with Space when on ground

### NoClip Mode (Press N)
- Free camera movement (spectator mode)
- No collision detection
- Fly through blocks and terrain
- Independent camera movement from player position
- Useful for exploring and debugging

## World Features

- **Block Types**: Dirt, Stone, Grass, Wood, Leaves
- **Chunk Loading**: Dynamic world generation and loading
- **Skybox**: Beautiful sky rendering with multiple skybox options
- **Frustum Culling**: Optimized rendering only shows visible chunks
- **Collision System**: Precise block-based collision detection

## Technical Details

### Graphics
- **OpenGL 4.6** with NVIDIA GPU support
- **Texture Loading**: PNG texture support with fallback colors
- **Font Rendering**: TrueType font support
- **Wireframe Mode**: Toggle between solid and wireframe rendering

### Performance
- **Target FPS**: 144 FPS (configurable)
- **High-Precision Threading**: Better timing accuracy
- **Busy Wait Sleep Mode**: Reduced input latency
- **Chunk-based Culling**: Only renders visible world sections

### Architecture
- **Component-Based**: Separate systems for physics, rendering, and input
- **Entity System**: GameObject base class for all world objects
- **Modular Design**: Clean separation between engine and game logic

## Project Structure

```
app/
├── src/main/java/
│   ├── mystuff/engine/     # Core engine components
│   ├── mystuff/game/       # Game-specific logic
│   └── mystuff/utils/      # Utility classes
├── resources/              # Game assets
│   ├── textures/          # Block and entity textures
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
2. **NoClip mode (N)** is great for exploring world generation
3. **Wireframe mode (F)** helps visualize geometry
4. Monitor performance metrics to optimize gameplay
5. Use **R** to quickly reset position if stuck

## Troubleshooting

### Texture Loading Issues
If textures fail to load, ensure the `app/resources/` directory contains:
- `textures/dirt.png`
- `textures/stone.png` 
- `textures/grass.png`
- `textures/player.png`
- `textures/Skyboxes/BlueSkySkybox.png`
- `fonts/reflow-sans-demo/Reflow Sans DEMO.ttf`

### Performance Issues
- Monitor FPS and frame time with debug mode
- Reduce render distance if experiencing lag
- Check CPU utilization graph for bottlenecks

---

Enjoy exploring your voxel world! 🎮⛏️