#!/usr/bin/env python3
import os
from PIL import Image, ImageFilter
import numpy as np

os.makedirs("app/src/main/res/mipmap-mdpi", exist_ok=True)
os.makedirs("app/src/main/res/mipmap-hdpi", exist_ok=True)
os.makedirs("app/src/main/res/mipmap-xhdpi", exist_ok=True)
os.makedirs("app/src/main/res/mipmap-xxhdpi", exist_ok=True)
os.makedirs("app/src/main/res/mipmap-xxxhdpi", exist_ok=True)
os.makedirs("app/src/main/res/drawable-nodpi", exist_ok=True)
os.makedirs("app/src/main/res/mipmap-anydpi-v26", exist_ok=True)
os.makedirs("playstore", exist_ok=True)

# Load master artwork
im = Image.open("logo.png").convert("RGBA")
w, h = im.size

# We want:
# 1. 512x512 Play Store icon (full composition, pristine details)
playstore_icon = im.resize((512, 512), Image.Resampling.LANCZOS)
playstore_icon.save("playstore/ic_launcher-playstore.png", "PNG")
print("Saved playstore/ic_launcher-playstore.png (512x512)")

# 2. Extract foreground emblem with transparency
# Let's extract the golden/light emblem cleanly from the dark background
arr = np.array(im, dtype=float)
# Color channels
r, g, b = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2]
# Background color estimation: near-black graphite
# In logo.png, dark background pixels have r < 36, g < 36, b < 40.
# The emblem has vibrant golden/champagne color (r > 60, g > 45, etc.) or highlights.
# Let's calculate luminance and golden saturation:
lum = 0.299 * r + 0.587 * g + 0.114 * b

# Background baseline luminance across the canvas is roughly between 0 and 32.
# Let's compute smooth alpha:
# For lum <= 18: alpha = 0
# For lum >= 45: alpha = 1
# Smooth interpolation between 18 and 45
alpha = np.clip((lum - 16.0) / (48.0 - 16.0), 0.0, 1.0)

# Un-multiply / restore foreground RGB so it renders crisp against any background
fg_arr = arr.copy()
fg_arr[:, :, 3] = alpha * 255.0

# In the foreground, ensure the color of semi-transparent edge pixels stays warm champagne/gold
# rather than dark gray:
mask_semi = (alpha > 0.01) & (alpha < 0.99)
# Boost semi-transparent edges toward warm champagne if darkened by background
fg_im = Image.fromarray(fg_arr.astype(np.uint8))

# Save high-res extracted foreground
fg_im.save("playstore/extracted_foreground.png")
print("Extracted foreground saved.")

