# Android Bluetooth HID Remote

Turn your Android phone into a Bluetooth Keyboard, Mouse, and Remote (DPAD).

## Features
- **Mouse**: Control cursor with relative touch movement. Left/Right click supported.
- **Keyboard**: Type using your phone's soft keyboard.
- **Remote**: DPAD navigation (Up, Down, Left, Right, Center, Back, Home) for Android TV.

## Requirements
- **Source Device**: Android Phone running Android 9 (Pie) or higher.
- **Target Device**: Android TV, Google TV, Windows PC, macOS, Linux (must support Bluetooth HID).

## Setup & Pairing Instructions
1. **Install**: Compile and install the application on your Android phone using Android Studio.
2. **Permissions**: Open the app and grant the requested Bluetooth permissions (Connect, Advertise, Scan).
3. **Register**: The app automatically registers as an HID device upon opening (check Status at top: "Registered as HID").
4. **Pairing**:
    - **Step 1**: On your Phone (in app), tap "Open Bluetooth Settings to Pair" to make your device discoverable if needed, or just go to your phone's Bluetooth settings and ensure it's visible. *Note: The app advertises, but often you need to initiate pairing from the TV/PC.*
    - **Step 2**: On your Target Device (TV/PC), go to Bluetooth Settings and "Add Accessory" or "Scan".
    - **Step 3**: Look for your phone's name or "HID Remote". Select it to pair.
    - **Step 4**: Confirm any pairing codes displayed.
5. **Connect**: Once paired, the Status in the app should change to "Connected to: [Device Name]".

## Compatibility Notes
- **Android TV**: HID Mouse support varies by app. Some apps (Netflix, YouTube) utilize custom navigation and ignore mouse pointers. DPAD mode is best for these interfaces.
- **Windows/Mac**: Fully supported.
- **Latency**: Bluetooth usage is optimized, but interference can occur. Ensure line-of-sight for best performance.

## HID Report Descriptors
The app uses a Composite HID Device with 3 Report IDs:
1. **Keyboard (ID 1)**: Standard Boot Protocol Keyboard.
2. **Mouse (ID 2)**: Standard 3-button Mouse (X, Y relative).
3. **Consumer Control (ID 3)**: For Media/DPAD keys (Up, Down, Home, Back).

Project structure:
- `HidLink.kt`: Handles Bluetooth Service/Connection.
- `HidUtils.kt`: Contains the raw byte arrays for HID Descriptors.
- `TouchpadView.kt`: Custom view for mouse input.
