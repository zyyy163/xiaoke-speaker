import os
import base64

icons = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

svg_template = '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
    <circle cx="50" cy="50" r="45" fill="#FF9A9E"/>
    <circle cx="35" cy="45" r="8" fill="#FFFFFF"/>
    <circle cx="65" cy="45" r="8" fill="#FFFFFF"/>
    <circle cx="37" cy="47" r="4" fill="#333333"/>
    <circle cx="67" cy="47" r="4" fill="#333333"/>
    <path d="M35 65 Q50 80 65 65" stroke="#FFFFFF" stroke-width="4" fill="none"/>
    <rect x="20" y="20" width="15" height="20" rx="3" fill="#FFFFFF" opacity="0.8"/>
    <rect x="65" y="20" width="15" height="20" rx="3" fill="#FFFFFF" opacity="0.8"/>
</svg>'''

res_dir = os.path.join(os.path.dirname(__file__), 'app', 'src', 'main', 'res')

for density, size in icons.items():
    output_file = os.path.join(res_dir, f'mipmap-{density}', 'ic_launcher.svg')
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(svg_template)
    print(f'Created: {output_file}')

print('Icon generation complete!')