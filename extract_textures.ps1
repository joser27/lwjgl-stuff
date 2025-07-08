Write-Host "GLB Texture Extractor" -ForegroundColor Cyan
Write-Host "====================" -ForegroundColor Cyan
Write-Host ""

# Change to app directory
Set-Location app

Write-Host "Building project with Gradle..." -ForegroundColor Yellow
& .\gradlew.bat build

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "Extracting textures from Quequis_House.glb..." -ForegroundColor Yellow

# Run the texture extractor
& .\gradlew.bat -q --console=plain exec -PmainClass=mystuff.utils.GLBTextureExtractor -Pargs="models/Quequis_House.glb,src/main/resources/textures/extracted"

Write-Host ""
Write-Host "Done! Check src/main/resources/textures/extracted for extracted textures." -ForegroundColor Green
Write-Host ""

# List extracted files if any
$extractedPath = "src/main/resources/textures/extracted"
if (Test-Path $extractedPath) {
    $files = Get-ChildItem $extractedPath -File
    if ($files.Count -gt 0) {
        Write-Host "Extracted files:" -ForegroundColor Green
        foreach ($file in $files) {
            Write-Host "  - $($file.Name)" -ForegroundColor White
        }
    } else {
        Write-Host "No files were extracted. The GLB might not contain embedded textures." -ForegroundColor Yellow
    }
} else {
    Write-Host "Extracted folder was not created. Check for errors above." -ForegroundColor Yellow
}

Read-Host "Press Enter to exit" 