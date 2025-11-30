@echo off

REM Smithing Template Enchantments - Initialization Script
REM This script sets up the development environment for the mod

echo ==========================================
echo Smithing Template Enchantments
echo Project Initialization
echo ==========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 21 or higher and try again
    exit /b 1
)

echo Java version check passed
echo.

REM Download dependencies
echo Downloading dependencies...
call gradlew.bat --no-daemon setupDecompWorkspace
echo.

REM Setup IDE workspace
echo Setting up IDE workspace...
call gradlew.bat --no-daemon idea eclipse
echo.

echo ==========================================
echo Initialization complete!
echo.
echo Next steps:
echo   - Run 'build.bat' to build the mod
echo   - Import the project into your IDE
echo   - Edit configuration in config\trim_data.json after first run
echo ==========================================

pause
