# Salah Offline Asset Generation Tools

This directory contains utility scripts for preprocessing offline geographic datasets used by the Salah Android application.

## Location Asset Pipeline

`build_location_assets.py` processes raw geographic data from [GeoNames](https://www.geonames.org/) and timezone boundary polygons from [Timezone Boundary Builder](https://github.com/evansiroky/timezone-boundary-builder) to create compact, offline binary assets bundled in `core/location/src/main/assets/location/`:

1. **`cities.tsv.gzip`** (~2.7 MB): Compressed tab-separated table of global cities with populations ≥ 15,000, including normalized name aliases, regional subdivisions, country names, geographic coordinates, and IANA timezone IDs.
2. **`timezones.bin.gzip`** (~7.4 MB): Compact binary spatial index containing simplified boundary polygons for all global civil timezones, enabling offline GPS coordinate-to-timezone resolution without third-party network queries.

### Requirements

- Python 3.10+
- `shapely` 2.x (`pip install shapely`)

### Usage

```bash
python3 tools/build_location_assets.py <path-to-raw-downloads-directory>
```

For full details on the offline location architecture and data formats, see [docs/architecture/location-and-privacy.md](../docs/architecture/location-and-privacy.md).

