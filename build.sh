#!/bin/bash

# Network Device Configuration Workflow Builder
# Build and Setup Script

set -e

echo "=== Network Device Configuration Workflow Builder Setup ==="

# Check Java version
echo "Checking Java version..."
java -version

# Check Maven
echo "Checking Maven..."
mvn -version

# Check Python
echo "Checking Python..."
python3 --version

# Install Python dependencies
echo "Installing Python dependencies..."
pip3 install -r python/requirements.txt

# Make Python script executable
chmod +x python/netmiko_executor.py

# Create necessary directories
mkdir -p templates
mkdir -p logs

# Build Java application
echo "Building Java application..."
mvn clean compile

echo "=== Build completed successfully! ==="
echo ""
echo "To run the application:"
echo "  mvn javafx:run"
echo ""
echo "Or:"
echo "  mvn exec:java -Dexec.mainClass=\"com.networkflow.WorkflowApplication\""
echo ""
echo "To create executable JAR:"
echo "  mvn package"