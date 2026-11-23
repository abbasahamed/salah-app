#!/usr/bin/env python3
import subprocess
import time
import os

OUTPUT_DIR = "screenshots/salah-qibla-refined"
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
    print("=== Starting Milestone 6.1 Screenshot Capture ===")

    # Warm up app first
    print("\n--- Warming up app ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_turn_right")
    time.sleep(4.0)

    # 1. qibla-refined-dark.png
    print("\n--- 1. qibla-refined-dark.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_turn_right --es theme dark")
    time.sleep(4.5)
    capture("qibla-refined-dark.png")

    # 2. qibla-refined-light.png
    print("\n--- 2. qibla-refined-light.png ---")
    adb("shell cmd uimode night no")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_turn_right --es theme light")
    time.sleep(4.5)
    capture("qibla-refined-light.png")
    adb("shell cmd uimode night yes")

    # 3. qibla-refined-aligned.png
    print("\n--- 3. qibla-refined-aligned.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_aligned --es theme dark")
    time.sleep(4.5)
    capture("qibla-refined-aligned.png")

    # 4. qibla-refined-turn-right.png
    print("\n--- 4. qibla-refined-turn-right.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_turn_right --es theme dark")
    time.sleep(4.5)
    capture("qibla-refined-turn-right.png")

    # 5. qibla-refined-turn-left.png
    print("\n--- 5. qibla-refined-turn-left.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_turn_left --es theme dark")
    time.sleep(4.5)
    capture("qibla-refined-turn-left.png")

    # 6. qibla-refined-low-accuracy.png
    print("\n--- 6. qibla-refined-low-accuracy.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_low_accuracy --es theme dark")
    time.sleep(4.5)
    capture("qibla-refined-low-accuracy.png")

    # 7. qibla-refined-no-sensor.png
    print("\n--- 7. qibla-refined-no-sensor.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_no_sensor --es theme dark")
    time.sleep(4.5)
    capture("qibla-refined-no-sensor.png")

    # 8. qibla-refined-manual-location.png
    print("\n--- 8. qibla-refined-manual-location.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_manual_location --es theme dark")
    time.sleep(4.5)
    capture("qibla-refined-manual-location.png")

    # 9. qibla-refined-130pct.png
    print("\n--- 9. qibla-refined-130pct.png ---")
    adb("shell settings put system font_scale 1.3")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_aligned --es theme dark")
    time.sleep(5.0)
    capture("qibla-refined-130pct.png")
    adb("shell settings put system font_scale 1.0")

    # 10. qibla-refined-200pct.png
    print("\n--- 10. qibla-refined-200pct.png ---")
    adb("shell settings put system font_scale 2.0")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_aligned --es theme dark")
    time.sleep(5.0)
    capture("qibla-refined-200pct.png")
    adb("shell settings put system font_scale 1.0")

    # 11. qibla-refined-narrow.png (320dp width)
    print("\n--- 11. qibla-refined-narrow.png ---")
    adb("shell wm size 720x1600")
    adb("shell wm density 360")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_aligned --es theme dark")
    time.sleep(5.0)
    capture("qibla-refined-narrow.png")
    adb("shell wm density reset")
    adb("shell wm size reset")

    # 12. qibla-refined-landscape.png
    print("\n--- 12. qibla-refined-landscape.png ---")
    adb("shell settings put system accelerometer_rotation 0")
    adb("shell settings put system user_rotation 1")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_aligned --es theme dark")
    time.sleep(5.0)
    capture("qibla-refined-landscape.png")
    adb("shell settings put system user_rotation 0")

    reset_env()
    print("\n=== All 12 screenshots captured successfully! ===")

if __name__ == "__main__":
    main()
