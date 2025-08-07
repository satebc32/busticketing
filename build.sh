#!/bin/bash

# Network Device Configuration Workflow Builder
# Spring Boot Web Application Build and Setup Script

set -e

echo "=== Network Device Configuration Workflow Builder (Web) Setup ==="

# Check Java version
echo "Checking Java version..."
java -version

# Check Maven
echo "Checking Maven..."
mvn -version

echo "Checking Python installation..."
python --version
if [ $? -ne 0 ]; then
    echo "Python is not installed or not in PATH"
    echo "Please install Python and ensure it's accessible"
    exit 1
fi

# Install Python dependencies
echo "Installing Python dependencies..."
pip3 install -r python/requirements.txt

# Make Python script executable
chmod +x python/netmiko_executor.py

# Create necessary directories
mkdir -p templates
mkdir -p logs
mkdir -p static/uploads

# Build Spring Boot application
echo "Building Spring Boot application..."
mvn clean compile

# Run tests (if any)
echo "Running tests..."
mvn test

echo "=== Build completed successfully! ==="
echo ""
echo "To run the web application:"
echo "  mvn spring-boot:run"
echo ""
echo "Or:"
echo "  java -jar target/device-config-workflow-1.0.0.jar"
echo ""
echo "The application will be available at:"
echo "  http://localhost:8080"
echo ""
echo "To create executable JAR:"
echo "  mvn package"
echo ""
echo "Features:"
echo "  - Web-based drag-and-drop workflow builder"
echo "  - REST API for workflow management"
echo "  - Real-time updates via WebSocket"
echo "  - Template system for device configurations"
echo "  - Dynamic task insertion in workflows"
echo "  - Netmiko integration for device CLI"