#!/usr/bin/env python3
import subprocess
import time
import os

OUTPUT_DIR = "screenshots/salah-today-location"
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
    assert os.path.exists(path) and os.path.getsize(path) > 10000, f"Capture failed or too small for {filename}"
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
    print("=== Starting Today Location Header Screenshot Capture ===")

    # Ensure app is installed
    run("./gradlew installDebug")

    # Warm up app first
    print("\n--- Warming up app ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario today_gps --es theme dark")
    time.sleep(5.0)

    # 1. Today dark with location
    print("\n--- 1. today-dark-location.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario today_gps --es theme dark")
    time.sleep(5.0)
    capture("today-dark-location.png")

    # 2. Today light with location
    print("\n--- 2. today-light-location.png ---")
    adb("shell cmd uimode night no")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario today_gps --es theme light")
    time.sleep(5.0)
    capture("today-light-location.png")
    adb("shell cmd uimode night yes")

    # 3. Long city name
    print("\n--- 3. today-long-city.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario today_long_city --es theme dark")
    time.sleep(5.0)
    capture("today-long-city.png")

    # 4. No location selected
    print("\n--- 4. today-no-location.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario today_no_location --es theme dark")
    time.sleep(5.0)
    capture("today-no-location.png")

    # 5. 130% font scaling
    print("\n--- 5. today-130pct.png ---")
    adb("shell settings put system font_scale 1.3")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario today_gps --es theme dark")
    time.sleep(5.0)
    capture("today-130pct.png")
    adb("shell settings put system font_scale 1.0")

    # 6. 200% font scaling
    print("\n--- 6. today-200pct.png ---")
    adb("shell settings put system font_scale 2.0")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario today_gps --es theme dark")
    time.sleep(5.0)
    capture("today-200pct.png")
    adb("shell settings put system font_scale 1.0")

    # 7. Reference screen showing all six prayer rows above navigation
    print("\n--- 7. today-reference-all-prayers.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario today_gps --es theme dark")
    time.sleep(5.0)
    capture("today-reference-all-prayers.png")

    reset_env()
    print("\n=== Today Location Header Capture Completed Successfully! ===")

if __name__ == "__main__":
    main()
