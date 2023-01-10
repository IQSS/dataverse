function initDvJS() {
  
  function initGeo() {
    // Order of coordinates in GEOBOX datafield
    const X1 = 0;
    const Y1 = 3;
    const X2 = 1;
    const Y2 = 2;

    const RECT_COLOR = '#e3276f';

    const TILE_LAYER_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
    const TILE_LAYER_COPYRIGHT = '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>';

    // Checks whether geobox is properly defined
    function isValidGeobox(points) {
      return points && points.length == 4 && points[Y2] > points[Y1];
    }

    // Checks whether we should wrap geobox around antimeridian
    function shouldWrapGeobox(points) {
      return points[X1] > points[X2];
    }

    // MetadataView – methods for Dataset/Metadata tab
    let metadataMapsData = new Map();

    let metadataView = {
      // Prepare entry for geobox
      prepare: function (key) {
        metadataMapsData.set(key, {
          leafMap: L.map(key),
          leafMapInitialized: false,
          values: []
        });
      },

      // Push value from geobox metadata
      pushValue: function (key, value) {
        let entry = metadataMapsData.get(key);
        entry.values.push(value);
      },

      // Draw map with geobox rectangle (must be called only when the target div is visible!)
      initializeMap: function (leafMap, points) {
        leafMap.invalidateSize();
        if (isValidGeobox(points)) {
          L.tileLayer(TILE_LAYER_URL, {
            maxZoom: 19,
            attribution: TILE_LAYER_COPYRIGHT
          }).addTo(leafMap);
          let bounds = shouldWrapGeobox(points)
            ? [[points[Y2], points[X1]], [points[Y1], points[X2] + 360.0]]
            : [[points[Y1], points[X1]], [points[Y2], points[X2]]];
          L.rectangle(bounds, { color: RECT_COLOR, weight: 1 }).addTo(leafMap);
          leafMap.fitBounds(bounds);
        }
      },

      // Mass initialization of maps (for event handler when section is shown)
      initializeAll: function (keyPrefix) {
        for (const [key, value] of metadataMapsData.entries()) {
          if (!key.startsWith(keyPrefix) || value.leafMapInitialized) {
            continue;
          }
          this.initializeMap(value.leafMap, value.values);
          value.leafMapInitialized = true;
        }
      }
    }

    return {
      MetadataView: metadataView
    };
  }

  return {
    Geo: initGeo()
  };
}