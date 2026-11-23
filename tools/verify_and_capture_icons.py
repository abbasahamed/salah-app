#!/usr/bin/env python3
import subprocess
import time
import os
from PIL import Image

def run_cmd(cmd):
    print(f"Running: {cmd}")
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"Error ({res.returncode}): {res.stderr.strip()}")
    else:
        print(f"Success: {res.stdout.strip()}")
    return res

def wait_for_boot():
    print("Waiting for device...")
    subprocess.run("adb wait-for-device", shell=True, check=True)
    for _ in range(60):
        res = subprocess.run("adb shell getprop sys.boot_completed", shell=True, capture_output=True, text=True)
        if res.stdout.strip() == "1":
            print("Device boot completed!")
            time.sleep(3)
            return True
        time.sleep(2)
    return False

def main():
    if not wait_for_boot():
        print("Timeout waiting for boot.")
        return

    # Install APK
    print("Installing app-debug.apk...")
    run_cmd("adb install -r app/build/outputs/apk/debug/app-debug.apk")

    os.makedirs("screenshots/salah-launcher-icon", exist_ok=True)

    # Wake screen, unlock and go Home
    run_cmd("adb shell input keyevent KEYCODE_WAKEUP")
    run_cmd("adb shell wm dismiss-keyguard")
    run_cmd("adb shell input keyevent KEYCODE_HOME")
    time.sleep(2)

    # 1. Capture Home screen with standard icon
    # Ensure launcher has refreshed
    run_cmd("adb exec-out screencap -p > screenshots/salah-launcher-icon/01_launcher_standard.png")
    print("Captured 01_launcher_standard.png")

    # Open app drawer to show Salah icon clearly among all apps
    # Swipe up from bottom center
    run_cmd("adb shell input swipe 540 1800 540 600 300")
    time.sleep(2)
    run_cmd("adb exec-out screencap -p > screenshots/salah-launcher-icon/02_app_drawer_icon.png")
    print("Captured 02_app_drawer_icon.png")

    # Enable Dark Mode
    run_cmd("adb shell cmd uimode night yes")
    time.sleep(2)
    run_cmd("adb exec-out screencap -p > screenshots/salah-launcher-icon/03_dark_mode_drawer.png")
    print("Captured 03_dark_mode_drawer.png")

    # Return Home in dark mode
    run_cmd("adb shell input keyevent KEYCODE_HOME")
    time.sleep(1)
    run_cmd("adb exec-out screencap -p > screenshots/salah-launcher-icon/04_dark_mode_home.png")
    print("Captured 04_dark_mode_home.png")

    # Try enabling Themed Icons (Launcher3 / Pixel Launcher flag or settings)
    run_cmd("adb shell settings put secure themed_icon_enabled 1")
    # For Nexus/Pixel launcher, sometimes themed_icon_enabled or restarting launcher applies it
    run_cmd("adb shell am force-stop com.google.android.apps.nexuslauncher")
    time.sleep(2)
    run_cmd("adb shell input keyevent KEYCODE_HOME")
    time.sleep(1)
    run_cmd("adb exec-out screencap -p > screenshots/salah-launcher-icon/05_themed_icon_home.png")
    print("Captured 05_themed_icon_home.png")

    # Open app drawer to show themed icon
    run_cmd("adb shell input swipe 540 1800 540 600 300")
    time.sleep(2)
    run_cmd("adb exec-out screencap -p > screenshots/salah-launcher-icon/06_themed_icon_drawer.png")
    print("Captured 06_themed_icon_drawer.png")

    # Return to light mode
    run_cmd("adb shell cmd uimode night no")
    time.sleep(1)
    run_cmd("adb shell input keyevent KEYCODE_HOME")

    # Launch Salah to ensure app opens cleanly with the new icon
    run_cmd("adb shell monkey -p com.salah.app -c android.intent.category.LAUNCHER 1")
    time.sleep(2)
    run_cmd("adb exec-out screencap -p > screenshots/salah-launcher-icon/07_app_launched.png")
    print("Captured 07_app_launched.png")

    # Create close-up crops & comparison collage of the icon
    create_icon_showcase()

def create_icon_showcase():
    print("Creating icon showcase...")
    # Load playstore icon and mipmap icons to create a crisp visual showcase artifact
    playstore_icon = Image.open("playstore/ic_launcher-playstore.png")
    
    # Also load the generated adaptive foreground & background
    fg = Image.open("app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png")
    bg = Image.open("app/src/main/res/mipmap-xxxhdpi/ic_launcher_background.png")
    mono = Image.open("app/src/main/res/mipmap-xxxhdpi/ic_launcher_monochrome.png")
    legacy_round = Image.open("app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png")
    legacy_sq = Image.open("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")

    # Save a verification summary image showing all icon layers and masks
    W, H = 1600, 900
    canvas = Image.new("RGBA", (W, H), (15, 16, 18, 255))
    
    # Resize components for display
    # 1. Play Store 512
    p_disp = playstore_icon.resize((360, 360), Image.Resampling.LANCZOS)
    canvas.paste(p_disp, (80, 270), p_disp)

    # 2. Round Mask
    r_disp = legacy_round.resize((300, 300), Image.Resampling.LANCZOS)
    canvas.paste(r_disp, (500, 300), r_disp)

    # 3. Squircle Mask
    s_disp = legacy_sq.resize((300, 300), Image.Resampling.LANCZOS)
    canvas.paste(s_disp, (860, 300), s_disp)

    # 4. Monochrome Layer
    m_disp = mono.resize((300, 300), Image.Resampling.LANCZOS)
    # Background for monochrome
    mono_bg = Image.new("RGBA", (300, 300), (45, 52, 54, 255))
    canvas.paste(mono_bg, (1220, 300))
    canvas.paste(m_disp, (1220, 300), m_disp)

    canvas.save("screenshots/salah-launcher-icon/00_icon_layers_showcase.png")
    print("Saved 00_icon_layers_showcase.png")

if __name__ == "__main__":
    main()

