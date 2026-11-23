#!/usr/bin/env python3
import subprocess
import time
import os
from PIL import Image

def run(cmd):
    print(f">> {cmd}")
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if res.returncode != 0 and res.stderr:
        print(f"ERR: {res.stderr.strip()}")
    return res

os.makedirs("screenshots/salah-launcher-icon", exist_ok=True)

# 1. Light Mode Drawer Capture
print("1. Capturing Light Mode Drawer...")
run("adb shell cmd uimode night no")
run("adb shell settings put secure themed_icon_enabled 0")
run("adb shell am force-stop com.google.android.apps.nexuslauncher")
time.sleep(1)
run("adb shell input keyevent KEYCODE_HOME")
time.sleep(1)
run("adb shell input swipe 540 1800 540 400 200")
time.sleep(2)

# Screencap drawer light
run("adb exec-out screencap -p > screenshots/salah-launcher-icon/01_app_drawer_light.png")

# Crop the icon: bounds are [540, 1612][787, 1927]
drawer_light = Image.open("screenshots/salah-launcher-icon/01_app_drawer_light.png")
icon_crop_light = drawer_light.crop((540, 1612, 787, 1927))
icon_crop_light.save("screenshots/salah-launcher-icon/02_icon_drawer_crop_light.png")

# Drag icon from drawer to Home Screen
print("2. Dragging Salah icon to Home Screen...")
# Long press at 663, 1750 and drag to top center (540, 600)
run("adb shell input swipe 663 1750 540 600 1000")
time.sleep(2)
run("adb exec-out screencap -p > screenshots/salah-launcher-icon/03_home_screen_light.png")

# 3. Dark Mode
print("3. Capturing Dark Mode...")
run("adb shell cmd uimode night yes")
time.sleep(2)
run("adb exec-out screencap -p > screenshots/salah-launcher-icon/04_home_screen_dark.png")

# Open drawer in dark mode
run("adb shell input swipe 540 1800 540 400 200")
time.sleep(2)
run("adb exec-out screencap -p > screenshots/salah-launcher-icon/05_app_drawer_dark.png")
drawer_dark = Image.open("screenshots/salah-launcher-icon/05_app_drawer_dark.png")
icon_crop_dark = drawer_dark.crop((540, 1612, 787, 1927))
icon_crop_dark.save("screenshots/salah-launcher-icon/06_icon_drawer_crop_dark.png")

# 4. Themed Icons
print("4. Enabling Themed Icons...")
run("adb shell settings put secure themed_icon_enabled 1")
run("adb shell am force-stop com.google.android.apps.nexuslauncher")
time.sleep(1)
run("adb shell input keyevent KEYCODE_HOME")
time.sleep(2)
run("adb exec-out screencap -p > screenshots/salah-launcher-icon/07_home_screen_themed_dark.png")

run("adb shell input swipe 540 1800 540 400 200")
time.sleep(2)
run("adb exec-out screencap -p > screenshots/salah-launcher-icon/08_app_drawer_themed_dark.png")
drawer_themed_dark = Image.open("screenshots/salah-launcher-icon/08_app_drawer_themed_dark.png")
icon_crop_themed_dark = drawer_themed_dark.crop((540, 1612, 787, 1927))
icon_crop_themed_dark.save("screenshots/salah-launcher-icon/09_icon_drawer_crop_themed_dark.png")

# Themed in Light Mode
run("adb shell cmd uimode night no")
time.sleep(1)
run("adb shell input keyevent KEYCODE_HOME")
time.sleep(2)
run("adb exec-out screencap -p > screenshots/salah-launcher-icon/10_home_screen_themed_light.png")

# 5. Build Comprehensive Verification Sheet
print("5. Generating Verification Matrix Sheet...")
matrix = Image.new("RGBA", (1920, 1080), (15, 16, 18, 255))

# Place 512 Playstore icon
playstore = Image.open("playstore/ic_launcher-playstore.png").resize((400, 400), Image.Resampling.LANCZOS)
matrix.paste(playstore, (80, 200), playstore)

# Place raw adaptive layers
bg = Image.open("app/src/main/res/mipmap-xxxhdpi/ic_launcher_background.png").resize((220, 220), Image.Resampling.LANCZOS)
fg = Image.open("app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png").resize((220, 220), Image.Resampling.LANCZOS)
mono = Image.open("app/src/main/res/mipmap-xxxhdpi/ic_launcher_monochrome.png").resize((220, 220), Image.Resampling.LANCZOS)

matrix.paste(bg, (540, 140))
matrix.paste(fg, (540, 140), fg)

mono_bg = Image.new("RGBA", (220, 220), (45, 52, 54, 255))
matrix.paste(mono_bg, (540, 420))
matrix.paste(mono, (540, 420), mono)

# Place drawer crops: Light & Dark
ic_l = icon_crop_light.resize((240, 306), Image.Resampling.LANCZOS)
matrix.paste(ic_l, (840, 180))

ic_d = icon_crop_dark.resize((240, 306), Image.Resampling.LANCZOS)
matrix.paste(ic_d, (1140, 180))

# Place device preview
device_preview = drawer_light.resize((324, 720), Image.Resampling.LANCZOS)
matrix.paste(device_preview, (1480, 180))

matrix.save("screenshots/salah-launcher-icon/launcher_icon_verification_matrix.png")
print("Saved screenshots/salah-launcher-icon/launcher_icon_verification_matrix.png")
