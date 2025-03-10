#!/bin/bash

# Create config directory if not exists
mkdir -p config

# Check if application.properties exists, if not create it
if [ ! -f config/application.properties ]; then
    echo "Creating default application.properties..."
    cat > config/application.properties <<EOL
# TCP Platform Simulator Configuration File

# TCP Server Configuration
tcp.server.port=8081
tcp.server.threadPoolSize=20

# Simulation Configuration
simulation.updateIntervalMs=8000
simulation.maxUpdates=-1        # -1 means unlimited updates
simulation.minRateChange=-0.005  # Minimum change rate (-0.5%)
simulation.maxRateChange=0.005   # Maximum change rate (0.5%)
EOL
fi

# Check if rates-config.json exists, if not create it
if [ ! -f config/rates-config.json ]; then
    echo "Creating default rates-config.json..."
    cat > config/rates-config.json <<EOL
[
  {
    "rateName": "PF1_USDTRY",
    "initialBid": 33.60,
    "initialAsk": 35.90
  },
  {
    "rateName": "PF1_EURUSD",
    "initialBid": 1.0220,
    "initialAsk": 1.0450
  },
  {
    "rateName": "PF1_GBPUSD",
    "initialBid": 1.2510,
    "initialAsk": 1.2645
  }
]
EOL
fi

# Create logs directory
mkdir -p logs

# Start the application
echo "Starting TCP Platform Simulator..."
java -jar target/platform-simulator-tcp-*.jar --config=config/application.properties --rates=config/rates-config.json