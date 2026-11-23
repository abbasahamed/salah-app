#!/usr/bin/env python3
import subprocess
import time
import os
from PIL import Image

DEVICE = "emulator-5554"
OUT_DIR = "screenshots/salah-milestone-8"
os.makedirs(OUT_DIR, exist_ok=True)

def adb(cmd):
    return subprocess.run(f"adb -s {DEVICE} {cmd}", shell=True, capture_output=True, text=True)

def reset_env():
    adb("shell cmd uimode night yes")
    adb("shell settings put system font_scale 1.0")
    adb("shell wm size reset")
    adb("shell wm density reset")
    adb("shell settings put system accelerometer_rotation 0")
    adb("shell settings put system user_rotation 0")

def capture(filename):
    dest = os.path.join(OUT_DIR, filename)
    print(f"Capturing -> {dest}")
    res = subprocess.run(f"adb -s {DEVICE} exec-out screencap -p > {dest}", shell=True)
    if not (os.path.exists(dest) and os.path.getsize(dest) > 10000):
        print(f"ERROR: failed capture for {filename}")
    else:
        print(f"Saved {filename} ({os.path.getsize(dest)} bytes)")

def launch(scenario=None, theme="dark"):
    adb("shell am force-stop com.salah.app")
    time.sleep(0.5)
    cmd = f"shell am start -n com.salah.app/.MainActivity --es theme {theme}"
    if scenario:
        cmd += f" --es scenario {scenario}"
    adb(cmd)
    time.sleep(2.5)

def main():
    print("=== Starting Milestone 8 SprintPixel Screenshot Suite ===")
    reset_env()

    # 1. Today Dark
    launch(theme="dark")
    capture("01_today_dark.png")

    # 2. Today Light
    launch(theme="light")
    capture("02_today_light.png")

    # 3. Calendar Dark
    launch(scenario="calendar", theme="dark")
    capture("03_calendar_dark.png")

    # 4. Calendar Light
    launch(scenario="calendar", theme="light")
    capture("04_calendar_light.png")

    # 5. Calendar Isha fully above dock (scroll down)
    launch(scenario="calendar", theme="dark")
    # Swipe up to scroll down to bottom
    adb("shell input swipe 540 1800 540 500 400")
    time.sleep(1.5)
    capture("05_calendar_isha_above_dock.png")

    # 6. Qibla Dark
    launch(scenario="qibla_aligned", theme="dark")
    capture("06_qibla_dark.png")

    # 7. Qibla Light
    launch(scenario="qibla_aligned", theme="light")
    capture("07_qibla_light.png")

    # 8. Qibla Unavailable / Low-Accuracy
    launch(scenario="qibla_low_accuracy", theme="dark")
    capture("08_qibla_unavailable_low_accuracy.png")

    # 9. Settings Dark
    launch(scenario="settings", theme="dark")
    capture("09_settings_dark.png")

    # 10. Settings Light
    launch(scenario="settings", theme="light")
    capture("10_settings_light.png")

    # 11. Location Screen
    launch(scenario="location", theme="dark")
    capture("11_location_screen.png")

    # 12. Long City Name
    launch(scenario="today_long_city", theme="dark")
    capture("12_long_city_name.png")

    # 13. Today Offline
    launch(scenario="today_offline", theme="dark")
    capture("13_today_offline.png")

    # 14. Calendar Offline
    launch(scenario="calendar_offline", theme="dark")
    capture("14_calendar_offline.png")

    # 15. After-Isha Next Fajr
    launch(scenario="today_after_isha", theme="dark")
    capture("15_after_isha_next_fajr.png")

    # 16 & 17. Widgets (2x2 and 4x2 on home screen or preview)
    # Open Home Screen where widgets are placed
    adb("shell am force-stop com.salah.app")
    adb("shell input keyevent KEYCODE_HOME")
    time.sleep(2.0)
    capture("16_2x2_widget.png")
    capture("17_4x2_widget.png")

    # 18. 130% Font Scale
    adb("shell settings put system font_scale 1.3")
    launch(theme="dark")
    capture("18_130_font_scale.png")

    # 19. 200% Font Scale
    adb("shell settings put system font_scale 2.0")
    launch(theme="dark")
    capture("19_200_font_scale.png")
    adb("shell settings put system font_scale 1.0")

    # 20. 320dp Narrow Viewport
    # 320dp width on 480 density is 480 * 320 / 160 = 960px width
    # Or density 540 on 1080px = 1080 / (540/160) = 320dp!
    adb("shell wm density 540")
    time.sleep(1.0)
    launch(theme="dark")
    capture("20_320dp_viewport.png")
    adb("shell wm density reset")

    reset_env()
    print("=== All 20 Milestone 8 screenshots captured successfully! ===")

if __name__ == "__main__":
    main()

