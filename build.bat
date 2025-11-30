@echo off

REM Smithing Template Enchantments - Build Script
REM This script compiles and packages the mod into a JAR file

echo ==========================================
echo Smithing Template Enchantments
echo Build Script
echo ==========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 21 or higher and try again
    exit /b 1
)

REM Clean previous build
echo Cleaning previous build...
call gradlew.bat clean
echo.

REM Build the mod
echo Building the mod...
call gradlew.bat build
echo.

REM Check if build was successful
if exist "build\libs\smithingtemplateenchantments-1.0.0.jar" (
    echo ==========================================
    echo Build successful!
    echo.
    echo Output JAR: build\libs\smithingtemplateenchantments-1.0.0.jar
    echo.
    echo To install:
    echo   1. Copy the JAR to your Minecraft mods folder
    echo   2. Launch Minecraft with Fabric Loader
    echo ==========================================
) else (
    echo ==========================================
    echo Build may have issues. Check output above.
    echo ==========================================
)

pause
