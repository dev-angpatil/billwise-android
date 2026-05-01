import os
import sys
from PIL import Image

def generate_icons(image_path):
    if not os.path.exists(image_path):
        print(f"Error: {image_path} not found.")
        return

    try:
        img = Image.open(image_path).convert("RGBA")
    except Exception as e:
        print(f"Error reading image: {e}")
        return

    # Sizes for Android launcher icons
    sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    base_dir = "app/src/main/res"

    for folder, size in sizes.items():
        dir_path = os.path.join(base_dir, folder)
        os.makedirs(dir_path, exist_ok=True)
        
        # Resize image
        resized = img.resize((size, size), Image.Resampling.LANCZOS)
        
        # Save ic_launcher.png
        icon_path = os.path.join(dir_path, "ic_launcher.png")
        resized.save(icon_path, "PNG")
        
        # Save ic_launcher_round.png (we can make it circular)
        mask = Image.new('L', (size, size), 0)
        from PIL import ImageDraw
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size, size), fill=255)
        
        round_img = resized.copy()
        round_img.putalpha(mask)
        round_img_path = os.path.join(dir_path, "ic_launcher_round.png")
        round_img.save(round_img_path, "PNG")
        
        print(f"Generated {size}x{size} icons in {folder}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python generate_icons.py <path_to_logo.png>")
        sys.exit(1)
    
    generate_icons(sys.argv[1])
