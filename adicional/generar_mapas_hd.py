# generar_mapas_hd.py
# pip install pillow requests
import math, os, requests
from PIL import Image
from io import BytesIO
from time import sleep

ZOOM = 14
OUTPUT_DIR = "maps"
TILE_SIZE = 256

def lat_lon_to_tile(lat, lon, zoom):
    n = 2 ** zoom
    x = int((lon + 180) / 360 * n)
    lat_rad = math.radians(lat)
    y = int((1 - math.log(math.tan(lat_rad) + 1 / math.cos(lat_rad)) / math.pi) / 2 * n)
    return x, y

def tile_to_lat(y, zoom):
    n = 2 ** zoom
    lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * y / n)))
    return math.degrees(lat_rad)

def tile_to_lon(x, zoom):
    return x / (2 ** zoom) * 360 - 180

def ajustar_proporcion(lat_min, lat_max, lon_min, lon_max, proporcion=(3, 2)):
    """Ajusta el bounding box para que tenga la proporción indicada (ancho:alto)"""
    lat_center = (lat_min + lat_max) / 2
    lon_center = (lon_min + lon_max) / 2

    lat_span = lat_max - lat_min
    lon_span = lon_max - lon_min

    objetivo_lon = lat_span * (proporcion[0] / proporcion[1])
    objetivo_lat = lon_span * (proporcion[1] / proporcion[0])

    # Expandir el eje más pequeño para alcanzar la proporción
    if lon_span / lat_span < proporcion[0] / proporcion[1]:
        # Demasiado alto → ampliar longitud
        lon_min = lon_center - objetivo_lon / 2
        lon_max = lon_center + objetivo_lon / 2
    else:
        # Demasiado ancho → ampliar latitud
        lat_min = lat_center - objetivo_lat / 2
        lat_max = lat_center + objetivo_lat / 2

    return lat_min, lat_max, lon_min, lon_max

def descargar_tile(x, y, zoom, servidor="a"):
    url = f"https://{servidor}.tile.openstreetmap.org/{zoom}/{x}/{y}.png"
    headers = {"User-Agent": "MapGenerator/1.0 (academic project UPV)"}
    try:
        r = requests.get(url, headers=headers, timeout=10)
        if r.status_code == 200:
            return Image.open(BytesIO(r.content)).convert("RGB")
    except Exception as e:
        print(f"  ⚠️  Error tile {x},{y}: {e}")
    return Image.new("RGB", (TILE_SIZE, TILE_SIZE), (200, 200, 200))

def generar_mapa(zona, zoom, proporcion=(3, 2)):
    nombre = zona["nombre"]
    print(f"\n📍 Generando {nombre} (zoom={zoom}, proporción {proporcion[0]}:{proporcion[1]})...")

    # Ajustar proporción
    lat_min, lat_max, lon_min, lon_max = ajustar_proporcion(
        zona["lat_min"], zona["lat_max"],
        zona["lon_min"], zona["lon_max"],
        proporcion
    )

    x_min, y_max = lat_lon_to_tile(lat_min, lon_min, zoom)
    x_max, y_min = lat_lon_to_tile(lat_max, lon_max, zoom)

    x_min, x_max = min(x_min, x_max), max(x_min, x_max)
    y_min, y_max = min(y_min, y_max), max(y_min, y_max)

    cols = x_max - x_min + 1
    rows = y_max - y_min + 1
    total = cols * rows
    px_w = cols * TILE_SIZE
    px_h = rows * TILE_SIZE
    print(f"   Tiles: {cols}×{rows} = {total} → imagen {px_w}×{px_h}px  (ratio real {px_w/px_h:.2f}:1)")

    img = Image.new("RGB", (px_w, px_h))
    servidores = ["a", "b", "c"]
    count = 0
    for row, y in enumerate(range(y_min, y_max + 1)):
        for col, x in enumerate(range(x_min, x_max + 1)):
            tile = descargar_tile(x, y, zoom, servidores[count % 3])
            img.paste(tile, (col * TILE_SIZE, row * TILE_SIZE))
            count += 1
            if count % 10 == 0:
                print(f"   {count}/{total} tiles...", end="\r")
            sleep(0.05)

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    ruta = os.path.join(OUTPUT_DIR, f"{nombre}.jpg")
    img.save(ruta, "JPEG", quality=95)
    tam_mb = os.path.getsize(ruta) / 1024 / 1024
    print(f"   ✅ {ruta}  ({px_w}×{px_h}px, {tam_mb:.1f} MB)")

    # Coordenadas reales de la imagen generada
    real_lat_max = tile_to_lat(y_min, zoom)
    real_lat_min = tile_to_lat(y_max + 1, zoom)
    real_lon_min = tile_to_lon(x_min, zoom)
    real_lon_max = tile_to_lon(x_max + 1, zoom)

    print(f"\n   📋 Copia esto en ensureDefaultMaps():")
    print(f'   new MapRegion("{nombre}", "maps/{nombre}.jpg",')
    print(f'       {real_lat_min:.6f}, {real_lat_max:.6f},')
    print(f'       {real_lon_min:.6f}, {real_lon_max:.6f});')

    return real_lat_min, real_lat_max, real_lon_min, real_lon_max


ZONAS = [
    {
        "nombre":  "valencia",
        "lat_min": 39.35, "lat_max": 39.55,
        "lon_min": -0.50, "lon_max": -0.30,
    },
    {
        "nombre":  "calderona",
        "lat_min": 39.60, "lat_max": 39.75,
        "lon_min": -0.55, "lon_max": -0.30,
    },
    {
        "nombre":  "pirineos",
        "lat_min": 42.60, "lat_max": 42.80,
        "lon_min":  0.00, "lon_max":  0.30,
    },
]

if __name__ == "__main__":
    print(f"🗺️  Generador de mapas HD — Zoom {ZOOM} — Proporción 3:2")
    for zona in ZONAS:
        generar_mapa(zona, ZOOM, proporcion=(3, 2))
    print("\n🎉 ¡Listo!")