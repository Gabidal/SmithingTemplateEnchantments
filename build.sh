#!/bin/bash

# Smithing Template Enchantments - Build Script
# This script compiles and packages the mod into a JAR file

set -e

echo "=========================================="
echo "Smithing Template Enchantments"
echo "Build Script"
echo "=========================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 21 or higher and try again"
    exit 1
fi

# Clean previous build
echo "Cleaning previous build..."
./gradlew clean
echo ""

# Build the mod
echo "Building the mod..."
./gradlew build
echo ""

# Check if build was successful
if [ -f "./build/libs/smithingtemplateenchantments-1.0.0.jar" ]; then
    echo "=========================================="
    echo "Build successful!"
    echo ""
    echo "Output JAR: build/libs/smithingtemplateenchantments-1.0.0.jar"
    echo ""
    echo "To install:"
    echo "  1. Copy the JAR to your Minecraft mods folder"
    echo "  2. Launch Minecraft with Fabric Loader"
    echo "=========================================="
else
    echo "=========================================="
    echo "Build may have issues. Check output above."
    echo "=========================================="
    exit 1
fi
