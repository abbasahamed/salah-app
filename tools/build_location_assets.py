"""Build offline assets from the documented GeoNames and TBB downloads.

Usage: python3 tools/build_location_assets.py SOURCE_DIRECTORY
Requires Shapely 2.x. See docs/milestone-3.md for inputs and attribution.
"""
import csv
import gzip
import json
import struct
import sys
import unicodedata
import zipfile
from pathlib import Path

from shapely import get_parts
from shapely.geometry import shape

source = Path(sys.argv[1])
output = Path(__file__).resolve().parents[1] / "core/location/src/main/assets/location"
output.mkdir(parents=True, exist_ok=True)


def rows(file):
    return csv.reader(file, delimiter="\t", quoting=csv.QUOTE_NONE)


def normalized(value):
    decomposed = unicodedata.normalize("NFD", value.strip().lower())
    return "".join(c for c in decomposed if not unicodedata.category(c).startswith("M"))


def city_record(identifier, name, names, region, country, latitude, longitude, zone, population):
    aliases = dict.fromkeys(filter(None, map(normalized, [name, *names])))
    return "\t".join([identifier, name, ",".join(aliases), region, country,
                      latitude, longitude, zone, population]) + "\n"


def cities():
    with (source / "country.txt").open() as file:
        countries = {r[0]: r[4] for r in rows(file) if r and not r[0].startswith("#")}
    with (source / "admin.txt").open() as file:
        regions = {r[0]: r[1] for r in rows(file)}
    with zipfile.ZipFile(source / "cities15000.zip") as archive:
        original = archive.read("cities15000.txt").decode().splitlines()
    with zipfile.ZipFile(source / "cities.zip") as archive:
        india = [
            r for r in rows(archive.read("cities500.txt").decode().splitlines())
            if r[8] == "IN" and "kattankulathur" in ",".join(r[1:4]).lower()
        ]
    supplemental = [
        [r[0], "Kattankulathur", "Kattankulathur", ",".join(r[1:4]), *r[4:]]
        for r in india if r[6] == "P"
    ]
    with (output / "cities.tsv.gzip").open("wb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as dest:
        for r in [*rows(original), *supplemental]:
            dest.write(city_record(
                "geonames:" + r[0], r[1], [r[2], *r[3].split(",")],
                regions.get(r[8] + "." + r[10], ""), countries.get(r[8], ""),
                r[4], r[5], r[17], r[14],
            ).encode())
        if not supplemental:
            # SRM IST campus reference, see the handoff for the original coordinates.
            dest.write(city_record(
                "srmist:campus", "Kattankulathur", ["Kattankolathur"], "Tamil Nadu", "India",
                "12.820254", "80.037819", "Asia/Kolkata", "0",
            ).encode())


def zones():
    with zipfile.ZipFile(source / "zones.zip") as archive:
        features = json.loads(archive.read("combined-1970.json"))["features"]
    feature_polygons = []
    for feature in features:
        zone = feature["properties"]["tzid"]
        geometry = shape(feature["geometry"]).simplify(0.0001, preserve_topology=True)
        for polygon in get_parts(geometry):
            if not polygon.is_empty:
                feature_polygons.append((zone, polygon))
    with (output / "timezones.bin.gzip").open("wb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as dest:
        dest.write(struct.pack(">ii", 2, len(feature_polygons)))
        for zone, polygon in feature_polygons:
            name = zone.encode("ascii")
            rings = [polygon.exterior, *polygon.interiors]
            dest.write(struct.pack(">H", len(name)) + name + struct.pack(">i", len(rings)))
            for ring in rings:
                points = list(ring.coords)[:-1]
                dest.write(struct.pack(">i", len(points)))
                encoded = bytearray()
                previous_lng = previous_lat = 0
                for lng, lat in points:
                    lng = round(lng * 100000)
                    lat = round(lat * 100000)
                    append_delta(encoded, lng - previous_lng)
                    append_delta(encoded, lat - previous_lat)
                    previous_lng, previous_lat = lng, lat
                dest.write(encoded)


def append_delta(dest, delta):
    value = (delta << 1) ^ (delta >> 31)
    while value >= 128:
        dest.append((value & 127) | 128)
        value >>= 7
    dest.append(value)


if __name__ == "__main__":
    if "--zones-only" not in sys.argv:
        cities()
    if "--cities-only" not in sys.argv:
        zones()
