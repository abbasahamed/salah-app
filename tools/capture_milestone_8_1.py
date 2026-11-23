#!/usr/bin/env python3
import subprocess
import time
import os
import hashlib
import sys
from PIL import Image

DEVICE = "emulator-5554"
OUT_DIR = "screenshots/salah-milestone-8.1"
os.makedirs(OUT_DIR, exist_ok=True)

seen_hashes = {}

def adb(cmd):
    return subprocess.run(f"adb -s {DEVICE} {cmd}", shell=True, capture_output=True, text=True)

def reset_env():
    adb("shell cmd uimode night yes")
    adb("shell settings put system font_scale 1.0")
    adb("shell wm size reset")
    adb("shell wm density reset")
    adb("shell settings put system accelerometer_rotation 0")
    adb("shell settings put system user_rotation 0")
    adb("shell cmd connectivity airplane-mode disable")
    time.sleep(1.0)

def dump_ui():
    adb("shell rm -f /sdcard/m81_dump.xml")
    adb("shell uiautomator dump /sdcard/m81_dump.xml")
    res = adb("shell cat /sdcard/m81_dump.xml")
    return res.stdout

def wait_for_ui(required_tokens, forbidden_tokens=None, timeout=14.0):
    start = time.time()
    last_ui = ""
    # Standard forbidden tokens for every screen
    effective_forbidden = list(forbidden_tokens or [])
    if "Loading location" not in required_tokens and "Loading location" not in effective_forbidden:
        effective_forbidden.append("Loading location")

    while time.time() - start < timeout:
        last_ui = dump_ui()
        if last_ui:
            missing = [t for t in required_tokens if t not in last_ui]
            found_forbidden = [f for f in effective_forbidden if f in last_ui]
            if not missing and not found_forbidden:
                return True, last_ui
        time.sleep(0.5)

    missing = [t for t in required_tokens if t not in last_ui]
    found_forbidden = [f for f in effective_forbidden if f in last_ui]
    err = f"UI check failed! Missing tokens: {missing}; Found forbidden tokens: {found_forbidden}"
    return False, err

def capture_verified(filename, required_tokens, forbidden_tokens=None, min_size=25000, timeout=14.0, require_light_bg=False):
    dest = os.path.join(OUT_DIR, filename)
    print(f"\n--- Verifying & Capturing: {filename} ---")
    ok, details = wait_for_ui(required_tokens, forbidden_tokens, timeout=timeout)
    if not ok:
        raise RuntimeError(f"State verification failed for {filename}: {details}")

    # Capture screenshot
    adb(f"exec-out screencap -p > {dest}")
    if not (os.path.exists(dest) and os.path.getsize(dest) >= min_size):
        raise RuntimeError(f"Capture failed or file too small for {filename}: {os.path.getsize(dest) if os.path.exists(dest) else 'missing'}")

    # Inspect image background pixel for light theme
    if require_light_bg:
        img = Image.open(dest)
        px = img.getpixel((100, 100))
        print(f" [LIGHT THEME CHECK] Background pixel at (100, 100): {px}")
        if px[0] < 180 or px[1] < 180 or px[2] < 180:
            raise AssertionError(f"FATAL: Screenshot {filename} expected light background, but got dark/splash pixel: {px}")

    # Compute SHA-256
    with open(dest, "rb") as f:
        file_bytes = f.read()
    sha = hashlib.sha256(file_bytes).hexdigest()

    if sha in seen_hashes and seen_hashes[sha] != filename:
        prev_file = seen_hashes[sha]
        raise AssertionError(f"FATAL: Screenshot {filename} has IDENTICAL hash to {prev_file} ({sha})! False-positive capture detected.")

    seen_hashes[sha] = filename
    size_kb = len(file_bytes) / 1024.0
    print(f" [PASS] {filename} verified! Size: {size_kb:.1f} KB | SHA-256: {sha[:16]}...")
    return sha

def ensure_kattankulathur_persisted():
    print("Checking persisted location in Salah...")
    adb("shell am force-stop com.salah.app")
    time.sleep(0.5)
    adb("shell am start -n com.salah.app/.MainActivity")
    ok, ui = wait_for_ui(["Kattankulathur"], timeout=4.0)
    if ok and "Today’s prayer times" in ui:
        print("Kattankulathur is already persisted with prayer schedule.")
        return

    print("Kattankulathur not active. Selecting Kattankulathur via offline picker...")
    if "Choose a city" in ui:
        adb("shell input tap 727 714") # tap Choose a city on onboarding
    else:
        adb("shell am start -n com.salah.app/.MainActivity --es scenario location")
    time.sleep(1.5)
    adb("shell input tap 540 932") # tap search field
    time.sleep(0.5)
    adb("shell input text 'Kattankulathur'")
    time.sleep(1.5)
    adb("shell input tap 342 1100") # tap Kattankulathur result
    time.sleep(2.0)
    ok, ui = wait_for_ui(["Kattankulathur", "Today’s prayer times"], timeout=8.0)
    if not ok:
        raise RuntimeError(f"Failed to persist Kattankulathur: {ui}")
    print("Kattankulathur successfully persisted.")

def launch(scenario=None, theme="dark"):
    adb("shell am force-stop com.salah.app")
    time.sleep(0.5)
    cmd = f"shell am start -n com.salah.app/.MainActivity --es theme {theme}"
    if scenario:
        cmd += f" --es scenario {scenario}"
    adb(cmd)

def load_existing_hashes():
    """Load hashes of all existing screenshots in OUT_DIR except the ones we will overwrite."""
    for f in os.listdir(OUT_DIR):
        if f.endswith(".png"):
            fp = os.path.join(OUT_DIR, f)
            with open(fp, "rb") as bf:
                h = hashlib.sha256(bf.read()).hexdigest()
            seen_hashes[h] = f
    print(f"Loaded {len(seen_hashes)} existing screenshot hashes.")

def capture_scenario_10_settings_light():
    # 10. Settings Light:
    print("\n--- Verifying & Capturing: 10_settings_light_verified.png ---")
    adb("shell cmd uimode night no")
    time.sleep(1.0)
    launch(scenario="settings", theme="light")
    ok, ui = wait_for_ui(
        ["Settings", "Appearance", "Light"],
        forbidden_tokens=["Loading location", "Starting window"],
        timeout=10.0,
    )
    if not ok:
        raise RuntimeError(f"Could not open Settings: {ui}")
    time.sleep(0.5)

    # Tap "Light" option to ensure state is selected in data store
    print("Tapping 'Light' appearance option to ensure selection...")
    adb("shell input tap 540 429")
    time.sleep(1.5)

    # Verify Settings is visible, Light is checked, appearance & calculation controls exist
    capture_verified(
        "10_settings_light_verified.png",
        required_tokens=[
            "Settings",
            "Appearance",
            "Prayer calculation",
            "Calculation method",
            "Karachi",
            "Asr convention",
            "Timing provider and privacy",
        ],
        forbidden_tokens=["Loading location", "Find your prayer times"],
        require_light_bg=True,
    )
    adb("shell cmd uimode night yes")
    time.sleep(0.5)

def capture_scenario_13_today_offline():
    # 13. Today Offline Mode:
    print("\n--- Verifying & Capturing: 13_today_offline_verified.png ---")
    ensure_kattankulathur_persisted()
    # Establish offline state via airplane mode
    print("Enabling airplane mode on device...")
    adb("shell cmd connectivity airplane-mode enable")
    time.sleep(1.5)
    try:
        launch(scenario="today_offline", theme="dark")
        p_tokens = ["Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"]
        capture_verified(
            "13_today_offline_verified.png",
            required_tokens=[
                "Kattankulathur",
                "Today’s prayer times",
                "Next prayer",
            ] + p_tokens,
            forbidden_tokens=[
                "Loading location",
                "Find your prayer times",
                "Choose a city",
            ],
            timeout=12.0,
        )
    finally:
        print("Disabling airplane mode...")
        adb("shell cmd connectivity airplane-mode disable")
        time.sleep(1.0)

def main():
    only_corrections = "--only-corrections" in sys.argv
    print("================================================================")
    print("  SALAH 3.0 — MILESTONE 8.1 VERIFIED SCREENSHOT SUITE")
    print(f"  Mode: {'TARGETED CORRECTIONS (10 & 13)' if only_corrections else 'FULL SUITE'}")
    print("  Target: SprintPixel (emulator-5554, API 36)")
    print("================================================================")
    reset_env()
    ensure_kattankulathur_persisted()

    p_tokens = ["Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"]

    if only_corrections:
        load_existing_hashes()
        # Remove previous 10 and 13 from seen_hashes so new captures can register
        for h, f in list(seen_hashes.items()):
            if f in ("10_settings_light_verified.png", "13_today_offline_verified.png"):
                del seen_hashes[h]

        capture_scenario_10_settings_light()
        capture_scenario_13_today_offline()

        reset_env()
        print("\n================================================================")
        print("  SUCCESS! Corrected scenarios 10 and 13 verified & captured!")
        print(f"  Total verified unique captures: {len(seen_hashes)}")
        print("================================================================")
        return

    # Full suite execution
    # 1. Today Dark
    launch(theme="dark")
    capture_verified(
        "01_today_dark_verified.png",
        required_tokens=["Kattankulathur", "Today’s prayer times"] + p_tokens,
        forbidden_tokens=["Find your prayer times", "Choose a city"]
    )

    # 2. Today Light
    launch(theme="light")
    capture_verified(
        "02_today_light_verified.png",
        required_tokens=["Kattankulathur", "Today’s prayer times"] + p_tokens,
        forbidden_tokens=["Find your prayer times", "Choose a city"]
    )

    # 3. Calendar Dark
    launch(scenario="calendar", theme="dark")
    capture_verified(
        "03_calendar_dark_verified.png",
        required_tokens=["November 2026", "Jumada"] + p_tokens,
        forbidden_tokens=["No location selected"]
    )

    # 4. Calendar Light
    launch(scenario="calendar", theme="light")
    capture_verified(
        "04_calendar_light_verified.png",
        required_tokens=["November 2026", "Jumada"] + p_tokens,
        forbidden_tokens=["No location selected"]
    )

    # 5. Calendar Isha clearance above dock
    launch(scenario="calendar", theme="dark")
    wait_for_ui(["November 2026", "Jumada"], timeout=8.0)
    time.sleep(0.5)
    adb("shell input swipe 540 1800 540 400 500") # scroll down to bottom
    time.sleep(1.5)
    capture_verified(
        "05_calendar_isha_clearance_verified.png",
        required_tokens=["Isha", "Calculated Umm al-Qura civil dates", "Today", "Calendar", "Qibla", "Settings"]
    )

    # 6. Qibla Dark
    launch(scenario="qibla_aligned", theme="dark")
    capture_verified(
        "06_qibla_dark_verified.png",
        required_tokens=["Qibla", "Dubai", "258°", "north", "1,642 km"]
    )

    # 7. Qibla Light
    launch(scenario="qibla_aligned", theme="light")
    capture_verified(
        "07_qibla_light_verified.png",
        required_tokens=["Qibla", "Dubai", "258°", "north", "1,642 km"]
    )

    # 8. Qibla Low Accuracy
    launch(scenario="qibla_low_accuracy", theme="dark")
    capture_verified(
        "08_qibla_low_accuracy_verified.png",
        required_tokens=["Qibla", "Dubai", "figure-eight", "metal and magnets"]
    )

    # 9. Settings Dark
    launch(scenario="settings", theme="dark")
    capture_verified(
        "09_settings_dark_verified.png",
        required_tokens=["Settings", "Appearance", "Prayer calculation", "Calculation method", "Karachi"]
    )

    # 10. Settings Light
    capture_scenario_10_settings_light()

    # 11. Location Screen
    launch(scenario="location", theme="dark")
    capture_verified(
        "11_location_verified.png",
        required_tokens=["Location", "Kattankulathur", "Use current location", "Choose a city", "Search cities"]
    )

    # 12. Long City Name
    launch(scenario="today_long_city", theme="dark")
    capture_verified(
        "12_long_city_verified.png",
        required_tokens=["Kattankulathur, Chengalpattu, Tamil Nadu", "Today’s prayer times"] + p_tokens
    )

    # 13. Today Offline Mode
    capture_scenario_13_today_offline()

    # 14. Calendar Offline Mode
    launch(scenario="calendar_offline", theme="dark")
    capture_verified(
        "14_calendar_offline_verified.png",
        required_tokens=["November 2026", "Jumada"] + p_tokens
    )

    # 15. After-Isha Rollover to Next Fajr
    launch(scenario="today_after_isha", theme="dark")
    capture_verified(
        "15_after_isha_verified.png",
        required_tokens=["Kattankulathur", "Tomorrow", "Next prayer", "Fajr"]
    )

    # 16. 2x2 Widget
    print("\n--- Pinning & Capturing 2x2 Widget ---")
    adb("shell pm clear com.google.android.apps.nexuslauncher")
    time.sleep(2.0)
    adb("shell am force-stop com.salah.app")
    time.sleep(0.5)
    adb("shell am start -n com.salah.app/.MainActivity --es pin_widget 2x2")
    wait_for_ui(["Add to home screen", "2 × 2"], timeout=8.0)
    adb("shell input tap 844 2253") # tap Add to home screen
    time.sleep(1.5)
    adb("shell input keyevent KEYCODE_HOME")
    time.sleep(1.0)
    adb("shell input swipe 900 1000 100 1000 300") # swipe to page 1
    time.sleep(1.5)
    capture_verified(
        "16_widget_2x2_verified.png",
        required_tokens=["Next prayer", "Asr", "15:14"]
    )

    # 17. 4x2 Widget
    print("\n--- Pinning & Capturing 4x2 Widget ---")
    adb("shell pm clear com.google.android.apps.nexuslauncher")
    time.sleep(2.0)
    adb("shell am force-stop com.salah.app")
    time.sleep(0.5)
    adb("shell am start -n com.salah.app/.MainActivity --es pin_widget 4x2")
    wait_for_ui(["Add to home screen", "4 × 2"], timeout=8.0)
    adb("shell input tap 844 2253") # tap Add to home screen
    time.sleep(1.5)
    adb("shell input keyevent KEYCODE_HOME")
    time.sleep(1.0)
    adb("shell input swipe 900 1000 100 1000 300") # swipe to page 1
    time.sleep(1.5)
    capture_verified(
        "17_widget_4x2_verified.png",
        required_tokens=["Until Asr · 15:14", "Kattankulathur", "Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"]
    )

    # 18. 130% Font Scale
    print("\n--- Capturing 130% Font Scale ---")
    adb("shell settings put system font_scale 1.3")
    launch(theme="dark")
    capture_verified(
        "18_font_130_verified.png",
        required_tokens=["Kattankulathur", "Today’s prayer times"] + p_tokens
    )
    adb("shell settings put system font_scale 1.0")

    # 19. 200% Font Scale
    print("\n--- Capturing 200% Font Scale ---")
    adb("shell settings put system font_scale 2.0")
    launch(theme="dark")
    capture_verified(
        "19_font_200_verified.png",
        required_tokens=["Kattankulathur", "Today’s prayer times"] + p_tokens
    )
    adb("shell settings put system font_scale 1.0")

    # 20. 320dp Narrow Viewport
    print("\n--- Capturing 320dp Narrow Viewport ---")
    adb("shell wm density 540")
    time.sleep(1.0)
    launch(theme="dark")
    capture_verified(
        "20_narrow_320_verified.png",
        required_tokens=["Kattankulathur", "Today’s prayer times"] + p_tokens
    )
    adb("shell wm density reset")
    time.sleep(1.0)

    # 21. Optional Bonus: Both widgets rendered together
    print("\n--- Capturing Both Widgets on Home Screen ---")
    adb("shell pm clear com.google.android.apps.nexuslauncher")
    time.sleep(2.0)
    adb("shell am force-stop com.salah.app")
    time.sleep(0.5)
    adb("shell am start -n com.salah.app/.MainActivity --es pin_widget 2x2")
    wait_for_ui(["Add to home screen", "2 × 2"], timeout=8.0)
    adb("shell input tap 844 2253")
    time.sleep(1.5)
    adb("shell am force-stop com.salah.app")
    time.sleep(0.5)
    adb("shell am start -n com.salah.app/.MainActivity --es pin_widget 4x2")
    wait_for_ui(["Add to home screen", "4 × 2"], timeout=8.0)
    adb("shell input tap 844 2253")
    time.sleep(1.5)
    adb("shell input keyevent KEYCODE_HOME")
    time.sleep(1.0)
    adb("shell input swipe 900 1000 100 1000 300")
    time.sleep(1.5)
    capture_verified(
        "21_both_widgets_verified.png",
        required_tokens=["Until Asr · 15:14", "Kattankulathur", "Next prayer", "Asr", "15:14"]
    )

    reset_env()
    print("\n================================================================")
    print(f"  SUCCESS! All {len(seen_hashes)} screenshots captured with state verification!")
    print(f"  Zero duplicate SHA-256 hashes detected.")
    print("================================================================")

if __name__ == "__main__":
    main()
