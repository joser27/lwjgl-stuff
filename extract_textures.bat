@echo off
echo GLB Texture Extractor
echo ====================

cd app

echo Compiling texture extractor...
javac -cp "build\libs\*;build\classes\java\main" src\main\java\mystuff\utils\GLBTextureExtractor.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo Extracting textures from Quequis_House.glb...
java -cp "build\libs\*;build\classes\java\main;src\main\java" mystuff.utils.GLBTextureExtractor models/Quequis_House.glb src/main/resources/textures/extracted

echo.
echo Done! Check src/main/resources/textures/extracted for extracted textures.
pause 