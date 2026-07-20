const fs = require('fs');
const path = require('path');

function createPng(width, height) {
    const signature = Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]);
    
    function crc32(data) {
        let crc = 0xFFFFFFFF;
        const table = [];
        for (let i = 0; i < 256; i++) {
            let c = i;
            for (let j = 0; j < 8; j++) {
                c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
            }
            table[i] = c;
        }
        for (let i = 0; i < data.length; i++) {
            crc = table[(crc ^ data[i]) & 0xFF] ^ (crc >>> 8);
        }
        return (crc ^ 0xFFFFFFFF) >>> 0;
    }
    
    function createChunk(type, data) {
        const length = Buffer.alloc(4);
        length.writeUInt32BE(data.length);
        const typeBuffer = Buffer.from(type);
        const crcData = Buffer.concat([typeBuffer, data]);
        const crcValue = crc32(crcData);
        const crc = Buffer.alloc(4);
        crc.writeUInt32BE(crcValue);
        return Buffer.concat([length, typeBuffer, data, crc]);
    }
    
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(width, 0);
    ihdr.writeUInt32BE(height, 4);
    ihdr[8] = 8;
    ihdr[9] = 6;
    ihdr[10] = 0;
    ihdr[11] = 0;
    ihdr[12] = 0;
    
    const rawData = [];
    for (let y = 0; y < height; y++) {
        rawData.push(0);
        for (let x = 0; x < width; x++) {
            const cx = width / 2, cy = height / 2;
            const dist = Math.sqrt(Math.pow(x - cx, 2) + Math.pow(y - cy, 2));
            const maxDist = Math.min(width, height) / 2 - 4;
            
            if (dist < maxDist) {
                const ratio = dist / maxDist;
                const r = Math.floor(102 + ratio * 153);
                const g = Math.floor(118 + ratio * 137);
                const b = Math.floor(231 + ratio * 24);
                const a = 255;
                rawData.push(r, g, b, a);
            } else {
                rawData.push(255, 255, 255, 0);
            }
        }
    }
    
    const { deflateSync } = require('zlib');
    const compressed = deflateSync(Buffer.from(rawData));
    
    const ihdrChunk = createChunk('IHDR', ihdr);
    const idatChunk = createChunk('IDAT', compressed);
    const iendChunk = createChunk('IEND', Buffer.alloc(0));
    
    return Buffer.concat([signature, ihdrChunk, idatChunk, iendChunk]);
}

const icons = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
};

const resDir = path.join(__dirname, 'app', 'src', 'main', 'res');

for (const [density, size] of Object.entries(icons)) {
    const outputDir = path.join(resDir, `mipmap-${density}`);
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }
    const outputFile = path.join(outputDir, 'ic_launcher.png');
    const pngData = createPng(size, size);
    fs.writeFileSync(outputFile, pngData);
    console.log(`Created: ${outputFile}`);
}

console.log('Icon generation complete!');