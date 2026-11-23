#!/usr/bin/env python3
import os
from PIL import Image, ImageOps, ImageDraw
import numpy as np

# Adaptive icon standards:
# Canvas is 108dp x 108dp
# Safe area where content is NEVER clipped by any launcher mask is the central 66dp - 72dp diameter circle (approx 61% - 66%).
# Standard densities:
# mdpi: 48x48 icon (legacy), adaptive 108x108
# hdpi: 72x72 icon (legacy), adaptive 162x162
# xhdpi: 96x96 icon (legacy), adaptive 216x216
# xxhdpi: 144x144 icon (legacy), adaptive 324x324
# xxxhdpi: 192x192 icon (legacy), adaptive 432x432

DENSITIES = {
    "mipmap-mdpi": (48, 108),
    "mipmap-hdpi": (72, 162),
    "mipmap-xhdpi": (96, 216),
    "mipmap-xxhdpi": (144, 324),
    "mipmap-xxxhdpi": (192, 432),
}

# 1. Load original logo (1254x1254)
logo = Image.open("logo.png").convert("RGBA")
w_orig, h_orig = logo.size

# Extract foreground:
arr = np.array(logo, dtype=float)
r, g, b = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2]
lum = 0.299 * r + 0.587 * g + 0.114 * b

# Clean thresholding with anti-aliasing
alpha = np.clip((lum - 16.0) / (44.0 - 16.0), 0.0, 1.0)
fg_arr = arr.copy()
fg_arr[:, :, 3] = alpha * 255.0

# Crop foreground to tight bounding box of the emblem
alpha_mask = fg_arr[:, :, 3] > 15
rows = np.where(alpha_mask.any(axis=1))[0]
cols = np.where(alpha_mask.any(axis=0))[0]
min_y, max_y = rows[0], rows[-1]
min_x, max_x = cols[0], cols[-1]

emblem_cropped = Image.fromarray(fg_arr.astype(np.uint8)).crop((min_x, min_y, max_x + 1, max_y + 1))
ew, eh = emblem_cropped.size
print(f"Emblem cropped size: {ew} x {eh}")

# Create monochrome silhouette for Android themed icon
# The monochrome layer should be solid white (#FFFFFF) with the same alpha transparency
mono_arr = np.zeros((eh, ew, 4), dtype=np.uint8)
mono_arr[:, :, 0:3] = 255
# Use normalized luminance from emblem as alpha for rich gradient feel, or alpha itself
emblem_lum = np.array(emblem_cropped.convert("L"))
# In themed icon, white intensity matches luminance:
mono_arr[:, :, 3] = np.clip(emblem_lum.astype(float) * 1.1, 0, 255).astype(np.uint8)
monochrome_emblem = Image.fromarray(mono_arr)

# Background color: rich deep graphite (#121316 to #0D0E12)
# Matching Salah dark theme background (#0F1012)
BG_COLOR = (15, 16, 18, 255) # #0F1012

# Generate icons for each density
for folder, (legacy_size, adaptive_size) in DENSITIES.items():
    folder_path = os.path.join("app/src/main/res", folder)
    os.makedirs(folder_path, exist_ok=True)
    
    # --- A. Adaptive Foreground (108dp equivalent) ---
    # The emblem should sit inside the safe circle (diameter ~ 66% - 70% of adaptive_size)
    target_emblem_w = int(adaptive_size * 0.62)
    scale = target_emblem_w / float(ew)
    target_emblem_h = int(eh * scale)
    
    resized_fg = emblem_cropped.resize((target_emblem_w, target_emblem_h), Image.Resampling.LANCZOS)
    
    fg_canvas = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
    paste_x = (adaptive_size - target_emblem_w) // 2
    paste_y = (adaptive_size - target_emblem_h) // 2
    fg_canvas.paste(resized_fg, (paste_x, paste_y), resized_fg)
    fg_canvas.save(os.path.join(folder_path, "ic_launcher_foreground.png"), "PNG")
    
    # --- B. Adaptive Monochrome (108dp equivalent) ---
    resized_mono = monochrome_emblem.resize((target_emblem_w, target_emblem_h), Image.Resampling.LANCZOS)
    mono_canvas = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
    mono_canvas.paste(resized_mono, (paste_x, paste_y), resized_mono)
    mono_canvas.save(os.path.join(folder_path, "ic_launcher_monochrome.png"), "PNG")
    
    # --- C. Adaptive Background (108dp equivalent) ---
    # Subtle graphite background with very soft radial warmth matching logo
    bg_canvas = Image.new("RGBA", (adaptive_size, adaptive_size), BG_COLOR)
    bg_canvas.save(os.path.join(folder_path, "ic_launcher_background.png"), "PNG")
    
    # --- D. Legacy / Standalone Launcher Icon (48dp / 72dp / etc.) ---
    # Composed icon with dark rounded background and emblem
    # Full composited launcher
    comp_canvas = Image.new("RGBA", (legacy_size, legacy_size), (0, 0, 0, 0))
    
    # Let's draw a nice squircle / rounded rect for legacy icon
    draw = ImageDraw.Draw(comp_canvas)
    radius = int(legacy_size * 0.22)
    draw.rounded_rectangle([0, 0, legacy_size - 1, legacy_size - 1], radius=radius, fill=BG_COLOR)
    
    leg_emblem_w = int(legacy_size * 0.72)
    leg_scale = leg_emblem_w / float(ew)
    leg_emblem_h = int(eh * leg_scale)
    leg_resized_fg = emblem_cropped.resize((leg_emblem_w, leg_emblem_h), Image.Resampling.LANCZOS)
    leg_paste_x = (legacy_size - leg_emblem_w) // 2
    leg_paste_y = (legacy_size - leg_emblem_h) // 2
    comp_canvas.paste(leg_resized_fg, (leg_paste_x, leg_paste_y), leg_resized_fg)
    comp_canvas.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")
    
    # Legacy round icon (for devices requesting android:roundIcon)
    round_canvas = Image.new("RGBA", (legacy_size, legacy_size), (0, 0, 0, 0))
    r_draw = ImageDraw.Draw(round_canvas)
    r_draw.ellipse([0, 0, legacy_size - 1, legacy_size - 1], fill=BG_COLOR)
    round_canvas.paste(leg_resized_fg, (leg_paste_x, leg_paste_y), leg_resized_fg)
    round_canvas.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")
    
    print(f"Generated assets for {folder}: legacy={legacy_size}x{legacy_size}, adaptive={adaptive_size}x{adaptive_size}")

# Also generate 512x512 Play Store icon
playstore_canvas = Image.new("RGBA", (512, 512), BG_COLOR)
p_emblem_w = int(512 * 0.72)
p_scale = p_emblem_w / float(ew)
p_emblem_h = int(eh * p_scale)
p_resized = emblem_cropped.resize((p_emblem_w, p_emblem_h), Image.Resampling.LANCZOS)
p_paste_x = (512 - p_emblem_w) // 2
p_paste_y = (512 - p_emblem_h) // 2
playstore_canvas.paste(p_resized, (p_paste_x, p_paste_y), p_resized)
playstore_canvas.save("playstore/ic_launcher-playstore.png", "PNG")
print("Saved 512x512 Play Store icon")
