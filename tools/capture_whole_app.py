#!/usr/bin/env python3
import subprocess
import time
import os

OUTPUT_DIR = "screenshots/Whole_App"
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
    print("=== Starting Capture of Whole App (10 Screenshots) ===")

    # Warmup
    print("\n--- Warmup ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario today_gps --es theme dark")
    time.sleep(4.5)

    # 1. Today Screen (Dark)
    print("\n--- 1. 01_Today_Screen_Dark.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario today_gps --es theme dark")
    time.sleep(4.5)
    capture("01_Today_Screen_Dark.png")

    # 2. Today Screen (Light)
    print("\n--- 2. 02_Today_Screen_Light.png ---")
    adb("shell cmd uimode night no")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario today_gps --es theme light")
    time.sleep(4.5)
    capture("02_Today_Screen_Light.png")
    adb("shell cmd uimode night yes")

    # 3. Calendar Screen (Dark)
    print("\n--- 3. 03_Calendar_Screen_Dark.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario calendar --es theme dark")
    time.sleep(4.5)
    capture("03_Calendar_Screen_Dark.png")

    # 4. Calendar Screen (Light)
    print("\n--- 4. 04_Calendar_Screen_Light.png ---")
    adb("shell cmd uimode night no")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario calendar --es theme light")
    time.sleep(4.5)
    capture("04_Calendar_Screen_Light.png")
    adb("shell cmd uimode night yes")

    # 5. Qibla Compass Aligned (Dark)
    print("\n--- 5. 05_Qibla_Compass_Aligned.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_aligned --es theme dark")
    time.sleep(4.5)
    capture("05_Qibla_Compass_Aligned.png")

    # 6. Qibla Compass Turn Guidance (Dark)
    print("\n--- 6. 06_Qibla_Compass_TurnGuidance.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_turn_right --es theme dark")
    time.sleep(4.5)
    capture("06_Qibla_Compass_TurnGuidance.png")

    # 7. Qibla Compass Aligned (Light)
    print("\n--- 7. 07_Qibla_Compass_Light.png ---")
    adb("shell cmd uimode night no")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario qibla_aligned --es theme light")
    time.sleep(4.5)
    capture("07_Qibla_Compass_Light.png")
    adb("shell cmd uimode night yes")

    # 8. Settings Screen (Dark)
    print("\n--- 8. 08_Settings_Screen_Dark.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario settings --es theme dark")
    time.sleep(4.5)
    capture("08_Settings_Screen_Dark.png")

    # 9. Settings Screen (Light)
    print("\n--- 9. 09_Settings_Screen_Light.png ---")
    adb("shell cmd uimode night no")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario settings --es theme light")
    time.sleep(4.5)
    capture("09_Settings_Screen_Light.png")
    adb("shell cmd uimode night yes")

    # 10. Location Search Screen
    print("\n--- 10. 10_Location_Search_Screen.png ---")
    adb("shell am force-stop com.salah.app")
    adb("shell am start -n com.salah.app/.MainActivity --es scenario location --es theme dark")
    time.sleep(4.5)
    capture("10_Location_Search_Screen.png")

    reset_env()
    print("\n=== All 10 Screenshots Captured Successfully into screenshots/Whole_App! ===")

if __name__ == "__main__":
    main()

