#!/bin/bash
set -e

# Start Appium server in background
echo "Starting Appium server..."
mkdir -p "Test Results/Logs"
appium --log "Test Results/Logs/appium_server.log" &

# Wait longer for Appium to fully spin up
echo "Waiting for Appium to start..."
sleep 15

# Wait for emulator to be fully ready (boot_completed property)
echo "Waiting for emulator to be fully booted..."
adb wait-for-device || true

# Loop with timeout (max 90 seconds)
COUNTER=0
while :; do
  BOOT_STATUS=$(adb shell getprop sys.boot_completed 2>/dev/null || echo "0")
  if [ "$BOOT_STATUS" = "1" ]; then
    break
  fi
  echo "Emulator still booting..."
  sleep 5
  COUNTER=$((COUNTER+5))
  if [ $COUNTER -gt 90 ]; then
    echo "Timeout waiting for emulator boot. Proceeding anyway..."
    break
  fi
done
echo "Emulator ready!"
sleep 5

# Execute E2E Tests
echo "Running E2E tests..."
python -m unittest tests/appium/test_app_flow.py

# Kill Appium server
echo "Cleaning up..."
pkill -f appium || true
