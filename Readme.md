# LWJGL Testing & Sandbox Repository

A Java-based 3D graphics testing environment built with LWJGL (Lightweight Java Game Library). This repository serves as a sandbox for experimenting with OpenGL, 3D rendering, game engine concepts, and various graphics techniques.

![OpenGL](https://img.shields.io/badge/OpenGL-4.6-blue)
![LWJGL](https://img.shields.io/badge/LWJGL-3.3.3-green)
![Java](https://img.shields.io/badge/Java-11+-orange)

## Overview

This is a personal testing ground for exploring LWJGL and OpenGL concepts. It's not a complete game, but rather a collection of experiments and prototypes for learning 3D graphics programming.

## Features

### Core Engine
- **3D Graphics Engine**: Basic OpenGL rendering pipeline
- **Camera System**: First-person camera with mouse look controls
- **Input Handling**: Keyboard and mouse input management
- **Performance Monitoring**: Real-time FPS, frame timing, and CPU utilization
- **Debug System**: Comprehensive in-game debugging tools

### Graphics Experiments
- **GLB Model Loading**: Support for GLTF/GLB 3D models with texture mapping
- **OBJ Model Rendering**: Wavefront OBJ file format support
- **Texture Management**: Dynamic texture loading and material system
- **Animation System**: Basic keyframe animation support
- **Wireframe Rendering**: Toggle between solid and wireframe modes

### Physics & Collision
- **Geometry-Based Collision**: Triangle-mesh collision detection for 3D models
- **Bounding Box System**: AABB collision detection
- **Physics Simulation**: Basic gravity and movement physics
- **Collision Manager**: Centralized collision detection system

### Visual Effects
- **Fog System**: Multiple atmospheric fog types for mood setting
- **Lighting**: Basic OpenGL lighting with ambient, diffuse, and specular components
- **Material System**: Support for different material types (textured, glass, mirror, emissive)
- **Font Rendering**: TrueType font support for UI elements

## How to Run

### Prerequisites
- Java 11 or higher
- Windows OS (native libraries configured for Windows)

### Running the Application
From the project root directory:

```powershell
.\gradlew.bat run
```

The application launches in a 1920x1080 window targeting 144 FPS.

## Controls

### Movement & Camera
| Key | Action |
|-----|--------|
| **W/A/S/D** | Move around the 3D space |
| **Space** | Jump (when on ground) |
| **Left Shift** | Sprint |
| **Mouse** | Look around (pitch/yaw) |

### Mode Switching
| Key | Action |
|-----|--------|
| **N** | Toggle NoClip Mode (free camera movement) |
| **F** | Toggle Wireframe Mode |
| **R** | Reset camera position |

### Debug & Testing
| Key | Action |
|-----|--------|
| **F3** | Toggle Debug Mode (comprehensive debug overlay) |
| **C** | Toggle collision detection |
| **V** | Show detailed collision debug info |
| **T** | Cycle through fog types |
| **P** | Pause/Unpause |

### Performance Controls
| Key | Action |
|-----|--------|
| **[** | Decrease time scale |
| **]** | Increase time scale |
| **Escape** | Exit application |

## Debug Features (F3)

When debug mode is enabled, you'll see:

### Performance Metrics
- Real-time FPS counter
- Frame timing statistics (current, average, max, min)
- CPU utilization percentage and graph
- Memory usage tracking
- Frame timing breakdown (update, render, sleep)

### Scene Information
- Camera position and rotation
- Player physics state (velocity, ground contact)
- Collision detection statistics
- Model loading status
- Animation frame information

### Visual Debug
- Wireframe rendering for geometry inspection
- Collision box visualization
- Model bounds display
- Texture loading status

## Project Structure

```
app/
├── src/main/java/
│   ├── mystuff/engine/     # Core engine (Window, Camera, Timer, etc.)
│   ├── mystuff/game/       # Game objects and logic
│   └── mystuff/utils/      # Utilities (texture loading, model loading, etc.)
├── resources/
│   ├── models/            # 3D models (GLB, OBJ files)
│   ├── textures/          # Texture files
│   ├── animations/        # Animation frames
│   └── fonts/             # Font files
└── build.gradle           # Build configuration
```

## Technical Details

### Graphics Pipeline
- **OpenGL 4.6** with modern rendering techniques
- **GLB/GLTF Support**: Industry-standard 3D model format
- **Material System**: Support for various material types
- **Texture Management**: Dynamic loading with fallback system

### Performance Features
- **High-Precision Timing**: Accurate frame timing and performance monitoring
- **Optimized Rendering**: Frustum culling and efficient draw calls
- **Memory Management**: Proper resource cleanup and texture caching

### Architecture
- **Component-Based Design**: Modular systems for different functionality
- **Entity System**: GameObject base class for all 3D objects
- **Debug Integration**: Comprehensive debugging throughout the codebase

## Development Notes

This repository is used for:
- **Learning LWJGL**: Understanding OpenGL bindings and 3D graphics
- **Testing Concepts**: Experimenting with rendering techniques
- **Prototyping**: Quick testing of game engine ideas
- **Performance Analysis**: Monitoring and optimizing graphics performance

### Current Experiments
- GLB model loading and rendering
- Geometry-based collision detection
- Material and texture systems
- Animation playback
- Performance profiling

## Troubleshooting

### Common Issues
- **Model Loading**: Ensure GLB/OBJ files are in the correct resource directories
- **Texture Issues**: Check texture file paths and formats
- **Performance**: Use debug mode (F3) to monitor frame times and CPU usage

### Debug Tips
1. Use **F3** for comprehensive debug information
2. **NoClip mode (N)** for free camera movement
3. **Wireframe mode (F)** for geometry inspection
4. Monitor performance metrics for optimization opportunities

---

This is a personal sandbox for LWJGL experimentation. Feel free to explore the code and use it as a reference for your own projects! 🎮🔧