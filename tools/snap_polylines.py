"""
Snap NasBus route lines to real roads using the public OSRM demo server.

Run AFTER you have the 5 JSON files in a folder. Needs internet + Python 3 (no extra packages).
Usage:  python snap_polylines.py <folder_with_json_files>
        e.g.  python tools/snap_polylines.py tools

OPTIONAL: choose the road yourself with "via points".
Create a file named via_points.json in the same folder as the JSON files:

{
  "segments": [
    {
      "from": "Tuy Public Transport Terminal",
      "to":   "Nasugbu Stop",
      "via":  [[14.0600, 120.7500], [14.0580, 120.8000]]
    }
  ]
}

- "from" and "to" are stop names exactly as in stops.json and must be consecutive stops.
- "via" is a list of [latitude, longitude] points (copy them from Google Maps, in the
  same order the bus travels) placed ON the road you want the line to follow.
- A segment is applied to EVERY forward route that has those two stops one after the other.
- Via points are only used to shape the line. They do NOT become stops on the map.

The result is written into pathPolyline of each forward route AND its reversed twin.
A backup is saved as routes.backup.json. Stops, distances, fares and schedules do not change.
"""
import json
import shutil
import sys
import time
import urllib.request
from pathlib import Path

folder = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent
routes_path = folder / "routes.json"
stops_path = folder / "stops.json"
via_path = folder / "via_points.json"
OSRM = ("https://router.project-osrm.org/route/v1/driving/{coords}"
        "?overview=full&geometries=geojson")


def fetch(points):
    coords = ";".join(f"{lng},{lat}" for lng, lat in points)
    req = urllib.request.Request(
        OSRM.format(coords=coords),
        headers={"User-Agent": "NasBus-student-project"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    if data.get("code") != "Ok":
        raise RuntimeError(f'{data.get("code")}: {data.get("message")}')
    route = data["routes"][0]
    return route["geometry"]["coordinates"], route["distance"] / 1000.0


def load_segments():
    if not via_path.exists():
        return []
    data = json.loads(via_path.read_text(encoding="utf-8"))
    return data.get("segments", [])


def build_points(route_stops, segments):
    """Stops in order, with via points inserted between the stops named in via_points.json."""
    points, used = [], 0
    for i, s in enumerate(route_stops):
        points.append((s["longitude"], s["latitude"]))
        if i + 1 < len(route_stops):
            nxt = route_stops[i + 1]
            for seg in segments:
                if seg["from"] == s["stopName"] and seg["to"] == nxt["stopName"]:
                    for lat, lng in seg["via"]:
                        points.append((lng, lat))
                        used += 1
    return points, used


def main():
    routes = json.loads(routes_path.read_text(encoding="utf-8"))
    stops = json.loads(stops_path.read_text(encoding="utf-8"))
    segments = load_segments()
    by_id = {r["routeId"]: r for r in routes}
    shutil.copy(routes_path, folder / "routes.backup.json")
    if segments:
        print(f"Using {len(segments)} via segment(s) from via_points.json")

    done = 0
    for r in routes:
        if r["direction"] != "fwd":
            continue
        route_stops = sorted(
            (s for s in stops if s["routeId"] == r["routeId"]),
            key=lambda s: s["sequenceOrder"],
        )
        points, used = build_points(route_stops, segments)
        try:
            line, road_km = fetch(points)
        except Exception as e:  # keep the old line if anything fails
            print(f'FAILED  route {r["routeId"]} {r["routeName"]} ({r["companyId"]}): {e}')
            continue
        line = [[round(x, 5), round(y, 5)] for x, y in line]
        r["pathPolyline"] = json.dumps(line, separators=(",", ":"))
        twin = by_id.get(r["pairedRouteId"])
        if twin:
            twin["pathPolyline"] = json.dumps(line[::-1], separators=(",", ":"))
        done += 1
        print(f'OK      route {r["routeId"]} {r["routeName"]} (company {r["companyId"]}): '
              f'{len(line)} points, {used} via point(s), OSRM road distance {road_km:.1f} km, '
              f'your distanceKm {r["distanceKm"]}')
        time.sleep(1.5)  # be polite to the free demo server

    routes_path.write_text(json.dumps(routes, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"Done: {done} of 6 routes snapped. Copy routes.json into app/src/main/assets/.")


if __name__ == "__main__":
    main()
