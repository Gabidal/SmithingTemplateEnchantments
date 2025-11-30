#!/bin/bash

# Smithing Template Enchantments - Initialization Script
# This script sets up the development environment for the mod

set -e

echo "=========================================="
echo "Smithing Template Enchantments"
echo "Project Initialization"
echo "=========================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 21 or higher and try again"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "Error: Java 21 or higher is required"
    echo "Current Java version: $JAVA_VERSION"
    exit 1
fi

echo "Java version check passed"
echo ""

# Make gradlew executable
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    echo "Made gradlew executable"
else
    echo "Warning: gradlew not found"
fi
echo ""

# Download dependencies
echo "Downloading dependencies..."
./gradlew --no-daemon setupDecompWorkspace
echo ""

# Setup IDE workspace
echo "Setting up IDE workspace..."
./gradlew --no-daemon idea eclipse
echo ""

echo "=========================================="
echo "Initialization complete!"
echo ""
echo "Next steps:"
echo "  - Run './build.sh' to build the mod"
echo "  - Import the project into your IDE"
echo "  - Edit configuration in config/trim_data.json after first run"
echo "=========================================="
