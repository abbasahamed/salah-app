#!/bin/bash
set -e

DEVICE="RZCW11FNZEE"
OUT_DIR="screenshots/salah-compact-ui"
mkdir -p "$OUT_DIR"

capture() {
    local name="$1"
    local theme="$2"
    local scenario="$3"
    
    adb -s "$DEVICE" shell am force-stop com.salah.app
    sleep 0.5
    
    local cmd="am start -n com.salah.app/.MainActivity --es theme $theme"
    if [ -n "$scenario" ]; then
        cmd="$cmd --es scenario $scenario"
    fi
    
    adb -s "$DEVICE" shell "$cmd"
    sleep 2.5
    
    adb -s "$DEVICE" shell screencap -p "/sdcard/${name}.png"
    adb -s "$DEVICE" pull "/sdcard/${name}.png" "${OUT_DIR}/${name}.png"
    echo "Captured ${OUT_DIR}/${name}.png"
}

# Ensure standard font scale first
adb -s "$DEVICE" shell settings put system font_scale 1.0

# 1. Today Dark
capture "01_today_dark" "dark" ""

# 2. Today Light
capture "02_today_light" "light" ""

# 3. Calendar Dark
capture "03_calendar_dark" "dark" "calendar"

# 4. Calendar Light
capture "04_calendar_light" "light" "calendar"

# 5. Qibla Dark
capture "05_qibla_dark" "dark" "qibla"

# 6. Qibla Light
capture "06_qibla_light" "light" "qibla"

# 7. Settings Dark
capture "07_settings_dark" "dark" "settings"

# 8. Settings Light
capture "08_settings_light" "light" "settings"

# 9. Location Screen
capture "09_location_screen" "dark" "location"

# 10. Large Font Scale 1.30 (130%)
adb -s "$DEVICE" shell settings put system font_scale 1.3
capture "10_today_font_scale_130" "dark" ""

# 11. Maximum Font Scale 2.0 (200%)
adb -s "$DEVICE" shell settings put system font_scale 2.0
capture "11_today_font_scale_200" "dark" ""

# Reset font scale back to 1.0 (default)
adb -s "$DEVICE" shell settings put system font_scale 1.0
adb -s "$DEVICE" shell am force-stop com.salah.app
echo "All screenshots captured successfully!"

