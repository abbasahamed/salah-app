#!/usr/bin/env python3
import subprocess
import time
import os

OUTPUT_DIR = "screenshots/salah-qibla-final-polish"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def run(cmd):
    print(f"Running: {cmd}")
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"Error ({res.returncode}): {res.stderr}")
    return res.stdout.strip()

def adb(cmd):
    return run(f"adb {cmd}")

def capture(filename):
    path = os.path.join(OUTPUT_DIR, filename)
    print(f"Capturing -> {path}")
    subprocess.run(f"adb exec-out screencap -p > {path}", shell=True)
    assert os.path.exists(path) and os.path.getsize(path) > 50000, f"Capture failed or too small for {filename}"
    print(f"Success: {filename} ({os.path.getsize(path)} bytes)")

def reset_env():
    adb("shell cmd uimode night yes")
    adb("shell settings put system font_scale 1.0")
    adb("shell wm size reset")
    adb("shell wm density reset")
    adb("shell settings put system accelerometer_rotation 0")
    adb("shell settings put system user_rotation 0")

def main():
    reset_env()
    print("=== Starting Milestone 6.3 Screenshot Capture ===")

    # Warm up app first
    print("\n--- Warming up app ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_aligned --es theme dark")
    time.sleep(4.0)

    # 1. qibla-polish-aligned-dark.png
    print("\n--- 1. qibla-polish-aligned-dark.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_aligned --es theme dark")
    time.sleep(4.5)
    capture("qibla-polish-aligned-dark.png")

    # 2. qibla-polish-aligned-light.png
    print("\n--- 2. qibla-polish-aligned-light.png ---")
    adb("shell cmd uimode night no")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_aligned --es theme light")
    time.sleep(4.5)
    capture("qibla-polish-aligned-light.png")
    adb("shell cmd uimode night yes")

    # 3. qibla-polish-turn-left.png
    print("\n--- 3. qibla-polish-turn-left.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_turn_left --es theme dark")
    time.sleep(4.5)
    capture("qibla-polish-turn-left.png")

    # 4. qibla-polish-turn-right.png
    print("\n--- 4. qibla-polish-turn-right.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_turn_right --es theme dark")
    time.sleep(4.5)
    capture("qibla-polish-turn-right.png")

    # 5. qibla-polish-low-accuracy.png
    print("\n--- 5. qibla-polish-low-accuracy.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_low_accuracy --es theme dark")
    time.sleep(4.5)
    capture("qibla-polish-low-accuracy.png")

    # 6. qibla-polish-no-sensor.png
    print("\n--- 6. qibla-polish-no-sensor.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_no_sensor --es theme dark")
    time.sleep(4.5)
    capture("qibla-polish-no-sensor.png")

    # 7. qibla-polish-200pct.png
    print("\n--- 7. qibla-polish-200pct.png ---")
    adb("shell settings put system font_scale 2.0")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_turn_right --es theme dark")
    time.sleep(4.5)
    capture("qibla-polish-200pct.png")
    adb("shell settings put system font_scale 1.0")

    # 8. qibla-polish-320dp.png
    print("\n--- 8. qibla-polish-320dp.png ---")
    adb("shell wm density 540")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_turn_right --es theme dark")
    time.sleep(4.5)
    capture("qibla-polish-320dp.png")
    adb("shell wm density reset")

    # 9. qibla-polish-landscape.png
    print("\n--- 9. qibla-polish-landscape.png ---")
    adb("shell settings put system user_rotation 1")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_aligned --es theme dark")
    time.sleep(4.5)
    capture("qibla-polish-landscape.png")
    adb("shell settings put system user_rotation 0")

    # 10. qibla-polish-navigation-clearance.png
    print("\n--- 10. qibla-polish-navigation-clearance.png ---")
    adb("shell settings put system font_scale 2.0")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_low_accuracy --es theme dark")
    time.sleep(4.5)
    adb("shell input swipe 500 1600 500 800 300")
    time.sleep(2.0)
    capture("qibla-polish-navigation-clearance.png")
    adb("shell settings put system font_scale 1.0")

    # Angle tests to verify upright Kaaba across all orientations
    angles = [
        ("qibla_angle_0", "qibla-polish-angle-0.png"),
        ("qibla_angle_90", "qibla-polish-angle-90.png"),
        ("qibla_angle_180", "qibla-polish-angle-180.png"),
        ("qibla_angle_270", "qibla-polish-angle-270.png"),
        ("qibla_angle_359", "qibla-polish-angle-359.png"),
    ]
    for sc, fname in angles:
        print(f"\n--- Angle test: {fname} ({sc}) ---")
        adb("shell am force-stop com.salah.app")
        adb(f"shell am start -n com.salah.app/.MainActivity --es scenario {sc} --es theme dark")
        time.sleep(4.5)
        capture(fname)

    reset_env()
    print("\n=== Milestone 6.3 Capture Completed Successfully! ===")

if __name__ == "__main__":
    main()

