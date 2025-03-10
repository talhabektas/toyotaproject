#!/bin/bash

# Create logs directory
mkdir -p logs

# Start the application
echo "Starting REST Platform Simulator..."
java -jar target/platform-simulator-rest-*.jar