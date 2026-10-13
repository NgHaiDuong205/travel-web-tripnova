#!/bin/sh
# Chuẩn bị dữ liệu OSRM (Việt Nam, profile ô tô, thuật toán MLD) — chạy 1 lần, kết quả nằm trong volume osrm-data.
# Bản đồ do service osrm-fetch tải (~300 MB từ Geofabrik); extract cần ~4-6 GB RAM, mất 10-30 phút.
set -e
cd /data
REGION="${OSRM_REGION:-asia/vietnam}"
NAME="$(basename "$REGION")-latest"
if [ -f "$NAME.osrm.mldgr" ]; then
    echo "OSRM: dữ liệu $NAME đã sẵn sàng"
    exit 0
fi
if [ ! -f "$NAME.osm.pbf" ]; then
    echo "OSRM: thiếu /data/$NAME.osm.pbf (service osrm-fetch chưa tải xong?)" >&2
    exit 1
fi
osrm-extract -p /opt/car.lua "$NAME.osm.pbf"
osrm-partition "$NAME.osrm"
osrm-customize "$NAME.osrm"
echo "OSRM: xong"
