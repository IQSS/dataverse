function initDvJS() {
  
  function initGeo() {
    // Coordinates mapping in GEOBOX datafield
    const X1 = 'W';
    const Y1 = 'S';
    const X2 = 'E';
    const Y2 = 'N';

    const RECT_COLOR = '#e3276f';
    const MAX_ZOOM = 19;
    const INIT_MAP_OPTS = { center: [52.1145028, 19.4235611], zoom: 4 };

    const TILE_LAYER_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
    const TILE_LAYER_COPYRIGHT = '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>';

    // Checks whether geobox is properly defined
    function isValidGeobox(values) {
      return values && Object.keys(values).length === 4 && values[Y2] >= values[Y1];
    }

    // Checks whether we should wrap geobox around antimeridian
    function shouldWrapGeobox(values) {
      return values[X1] > values[X2];
    }

    // Store value from input field
    function putValue(dataMap, key, field, value) {
      if (!isNaN(value) && value !== '') {
        let data = dataMap.get(key);
        data.values[field] = Number(value);
      }
    }

    // Mass initialization of maps (when section is shown/visible)
    function initializeAll(dataMap, keyPrefix, initializer) {
      for (const [key, data] of dataMap) {
        if (!key.startsWith(keyPrefix) || data.leafMapInitialized) {
          continue;
        }
        initializer(key, data);
        data.leafMapInitialized = true;
      }
    }

    // MetadataView – methods & data for Dataset/Metadata tab

    // Draw map with geobox rectangle (must be called only when the target div is visible!)
    function initializeMapInMetadataView(key, data) {
      data.leafMap = L.map(key);
      let leafMap = data.leafMap;
      leafMap.invalidateSize();
      let values = data.values;
      if (!isValidGeobox(values)) {
        return;
      }
      L.tileLayer(TILE_LAYER_URL, { maxZoom: MAX_ZOOM, attribution: TILE_LAYER_COPYRIGHT }).addTo(leafMap);
      let bounds = L.latLngBounds([
        [values[Y1], values[X1]],
        [values[Y2], values[X2] + (shouldWrapGeobox(values) ? 1 : 0) * 360.0]
      ]);
      ((values[X1] === values[X2] && values[Y1] === values[Y2])
        ? L.marker([values[Y1], values[X1]]).addTo(leafMap)
        : L.rectangle(bounds, { color: RECT_COLOR, weight: 1 }))
        .addTo(leafMap);
      leafMap.fitBounds(bounds.pad(0.1));
    }

    let metadataMapsData = new Map();

    let metadataView = {
      prepare: function (key) {
        metadataMapsData.set(key, {
          leafMap: undefined,
          leafMapInitialized: false,
          values: {}
        });
      },

      putValue: function (key, field, value) {
        putValue(metadataMapsData, key, field, value);
      },

      initializeAll: function (keyPrefix) {
        initializeAll(metadataMapsData, keyPrefix, initializeMapInMetadataView);
      }
    }

    // EditView – methods & data for edit forms (also in search)

    let editMapsData = new Map();

    function onMarkerDragged(data) {
      if (!data || !data.markerA || !data.markerB) {
        return;
      }
      let bounds = L.latLngBounds([data.markerA.getLatLng(), data.markerB.getLatLng()]);
      data.selection.setBounds(bounds);
      centerMap(data, bounds);
      updateInputs(data);
    }
    
    function centerMap(data, bounds) {
      let extendedBounds = bounds.pad(0.1);
      data.leafMap.fitBounds(extendedBounds);
    }

    function updateInputs(data) {
      let bounds = data.selection.getBounds();
      let sw = bounds.getSouthWest().wrap();
      updateInputAndValue(data, X1, sw.lng);
      updateInputAndValue(data, Y1, sw.lat);
      let ne = bounds.getNorthEast().wrap();
      updateInputAndValue(data, X2, ne.lng);
      updateInputAndValue(data, Y2, ne.lat);
    }

    function updateInputAndValue(data, coord, value) {
      PF(data.widgetVars[coord]).jq.val(value.toFixed(5));
      data.values[coord] = Number(value);
    }

    function onMapClicked(evt, data) {
      if (!data.markerA) {
        data.markerA = addMarker(evt.latlng, data);
      } else if (!data.markerB) {
        data.markerB = addMarker(evt.latlng, data);
        data.selection = addSelection(data);
        updateInputs(data);
        centerMap(data, data.selection.getBounds());
        data.leafMap.off('click'); // Unregister this handler after adding second marker
      } else {
        data.leafMap.off('click'); // Unregister this handler if all markers are already present
      }
    }

    function addMarker(latLng, data) {
      return L.marker(latLng, { draggable: true, autoPan: true, autoPanPadding: L.point(10, 10), autoPanSpeed: 1 })
              .addTo(data.leafMap)
              .on('dragend', function() { onMarkerDragged(data) });
    }

    function addSelection(data) {
      return L.rectangle([data.markerA.getLatLng(), data.markerB.getLatLng()], 
                          { color: RECT_COLOR, weight: 1 })
              .addTo(data.leafMap);
    }

    function initializeMapInView(key, data) {
      data.leafMap = L.map(key, INIT_MAP_OPTS);
      let map = data.leafMap;
      map.invalidateSize();
      map.on('click', function (evt) { onMapClicked(evt, data) });
      L.tileLayer(TILE_LAYER_URL, { maxZoom: MAX_ZOOM, attribution: TILE_LAYER_COPYRIGHT }).addTo(map);
      this.updateMap(key);
    }

    function updateEditableMap(dataMap, key) {
      let data = dataMap.get(key);
      if (!data || !isValidGeobox(data.values)) {
        return;
      }
      let wrap = shouldWrapGeobox(data.values);
      data.markerA = !data.markerA
        ? addMarker([data.values[Y1], data.values[X1]], data)
        : data.markerA.setLatLng([data.values[Y1], data.values[X1]]);
      data.markerB = !data.markerB
        ? addMarker([data.values[Y2], data.values[X2] + wrap * 360.0], data)
        : data.markerB.setLatLng([data.values[Y2], data.values[X2] + wrap * 360.0]);
      data.selection = !data.selection
        ? addSelection(data)
        : data.selection.setBounds([data.markerA.getLatLng(), data.markerB.getLatLng()]);
      centerMap(data, data.selection.getBounds());
    }

    function createEmptyEntry() {
      return {
        leafMap: undefined,
        leafMapInitialized: false,
        markerA: undefined,
        markerB: undefined,
        selection: undefined,
        values: {},
        widgetVars: {},
      };
    }

    let editView = {
      // Update position of markers and selection rectangle using the stored values
      updateMap: function(key) {
        updateEditableMap(editMapsData, key);
      },

      prepare: function(key) {
        if (editMapsData.has(key)) {
          // remove existing map on partial page update
          editMapsData.get(key).leafMap.remove();
        }
        editMapsData.set(key, createEmptyEntry());
      },

      remove: function(key) {
        editMapsData.get(key).leafMap.remove();
        editMapsData.delete(key);
      },

      putValue: function(key, field, value) {
        putValue(editMapsData, key, field, value);
      },

      putWidgetVar: function(key, field, widgetVar) {
        let data = editMapsData.get(key);
        data.widgetVars[field] = widgetVar;
      },

      initializeAll: function(keyPrefix) {
        initializeAll(editMapsData, keyPrefix, initializeMapInView.bind(this));
      }
    }

    // SearchView – methods & data for advanced search form

    let searchMapsData = new Map();

    let searchView = {
      // As the parent fields on advanced search form are not used to render 
      // their subfields, we have to count coordinate fields with the same key
      canCreateMap: function(key) {
        let vars = searchMapsData.get(key).widgetVars;
        return key && Object.keys(vars).length === 4;
      },

      updateMap: function (key) {
        updateEditableMap(searchMapsData, key);
      },

      prepare: function(key) {
        if (key && searchMapsData.has(key)) {
          return;
        }
        searchMapsData.set(key, createEmptyEntry());
      },

      putValue: function (key, field, value) {
        putValue(searchMapsData, key, field, value);
      },

      putWidgetVar: function (key, field, widgetVar) {
        let data = searchMapsData.get(key);
        data.widgetVars[field] = widgetVar;
      },

      initializeAll: function(keyPrefix) {
        initializeAll(searchMapsData, keyPrefix, initializeMapInView.bind(this));
      },

      removeAll: function() {
        for (const data of searchMapsData.values()) {
          data.leafMap.remove();
        }
        searchMapsData.clear();
      }
    }

    return {
      MetadataView: metadataView,
      EditView: editView,
      SearchView: searchView,
    };
  }

  return {
    Geo: initGeo()
  };
}