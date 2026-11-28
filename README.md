# DualSense Trigger Lab

A small Android app for testing PlayStation DualSense L2/R2 controller input on Android.

## What it does
- Shows live L2 and R2 trigger values (0–100%).
- Detects controller input through Android's standard gamepad/joystick input events.
- Builds a debug APK with GitHub Actions.

## Important limitation
This is an input tester, **not an adaptive-trigger actuator**. Android's standard public gamepad APIs expose trigger input but do not provide a general public API to command the DualSense adaptive-trigger resistance motors. Actual adaptive feedback may require supported game/controller integration or lower-level device-specific protocol work; this project does not claim to enable it.

## Build
Push to `main`, or open **Actions → Build Android APK → Run workflow**. Download the `DualSenseTriggerLab-debug` artifact from the completed workflow run.
