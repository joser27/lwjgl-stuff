# GLB Texture Extraction Guide

## Overview

This system allows you to extract embedded textures from GLB files and use them in your game. It includes:

1. **GLBTextureExtractor** - Extracts textures from GLB files
2. **Updated TextureMatcher** - Prioritizes extracted textures over generic ones
3. **Build scripts** - Easy-to-use extraction tools

## Quick Start

### Method 1: Using Gradle (Recommended)
```bash
cd app
./gradlew extractTextures
```

### Method 2: Using PowerShell (Windows)
```powershell
.\extract_textures.ps1
```

### Method 3: Using Batch File (Windows)
```cmd
extract_textures.bat
```

## How It Works

1. **Extraction**: The tool scans your GLB file for embedded textures and extracts them to `app/src/main/resources/textures/extracted/`

2. **Naming**: Extracted textures are named based on the GLB file and original texture names:
   - `Quequis_House_texture_0.png`
   - `Quequis_House_wood.jpg`
   - etc.

3. **Loading**: The updated TextureMatcher automatically:
   - First checks for extracted textures
   - Falls back to manual textures in `textures/house/`
   - Uses `missing_texture.jpg` as final fallback

## Current Status

The texture extractor currently:
- ✅ Detects embedded textures in GLB files
- ✅ Reports texture count and material information
- ⚠️ **Texture data extraction is not yet fully implemented**

The actual pixel data extraction needs to be completed using proper Assimp API calls.

## Next Steps

To complete the implementation:

1. **Run the extractor** to see what textures your GLB contains:
   ```bash
   cd app
   ./gradlew extractTextures
   ```

2. **Check the output** - it will tell you:
   - How many embedded textures were found
   - Material names and texture references
   - What needs to be implemented

3. **Complete the extraction** - The placeholder methods in `GLBTextureExtractor.java` need to be filled in with proper Assimp texture data extraction.

## Example Output

When you run the extractor, you might see:
```
Extracting textures from: models/Quequis_House.glb
Found 5 embedded textures
  Extracting compressed texture: texture_0 (2048 bytes)
    Compressed texture data extraction not yet implemented
Checking 3 materials for texture references...
  Material 0: "Wood_Material"
    Diffuse textures: 1
```

## Benefits

- **Accurate textures**: Use the exact textures from your 3D model
- **No manual matching**: Eliminates guesswork in texture assignment
- **Better performance**: Properly sized textures for your models
- **Easier workflow**: Extract once, use forever

## Customization

You can extract from different GLB files:
```bash
./gradlew extractTextures -PglbFile=models/MyModel.glb -PoutputDir=src/main/resources/textures/mymodel
``` 