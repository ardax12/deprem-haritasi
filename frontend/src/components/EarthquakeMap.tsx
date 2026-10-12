import React, { useEffect, useRef, useCallback } from 'react';
import { Map, Popup, NavigationControl, ScaleControl, setWorkerUrl } from 'maplibre-gl';
import type { StyleSpecification } from 'maplibre-gl';
import maplibreWorkerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?url';
import type { GeoJSONSource } from 'maplibre-gl';
import type { Feature, Point, FeatureCollection } from 'geojson';
import 'maplibre-gl/dist/maplibre-gl.css';
import type { EarthquakeItem } from '../types/earthquake';
import turkishDarkStyleJson from '../assets/turkish-dark-style.json';
import { parseItemLocation } from '../utils/locationHelper';

setWorkerUrl(maplibreWorkerUrl);
const turkishDarkStyle = turkishDarkStyleJson as unknown as StyleSpecification;

interface EarthquakeMapProps {
  items: EarthquakeItem[];
  selectedItem: EarthquakeItem | null;
  onSelect: (item: EarthquakeItem) => void;
  onOpenDetail: (item: EarthquakeItem) => void;
}

export const EarthquakeMap: React.FC<EarthquakeMapProps> = ({
  items,
  selectedItem,
  onSelect,
  onOpenDetail
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const popupRef = useRef<Popup | null>(null);

  const itemsRef = useRef<EarthquakeItem[]>(items);
  itemsRef.current = items;

  const onSelectRef = useRef(onSelect);
  onSelectRef.current = onSelect;

  const onOpenDetailRef = useRef(onOpenDetail);
  onOpenDetailRef.current = onOpenDetail;

  // Build GeoJSON collection
  const getGeoJSONCollection = useCallback((): FeatureCollection<Point> => {
    const features: Feature<Point>[] = itemsRef.current.map((item) => ({
      type: 'Feature',
      geometry: {
        type: 'Point',
        coordinates: [item.longitude, item.latitude]
      },
      properties: {
        id: item.id,
        magnitude: item.magnitude?.value ?? 0,
        type: item.magnitude?.type ?? 'ML',
        place: item.place ?? 'Bilinmeyen Konum',
        depthKm: item.depthKm ?? 'Bilinmiyor',
        occurredAt: item.occurredAt
      }
    }));

    return {
      type: 'FeatureCollection',
      features
    };
  }, []);

  // Enforce 100% Turkish language labels on map vector symbol layers
  const applyTurkishLabels = useCallback((map: Map) => {
    try {
      const style = map.getStyle();
      if (!style || !style.layers) return;

      style.layers.forEach((layer) => {
        if (
          layer.type === 'symbol' &&
          layer.layout &&
          layer.layout['text-field'] &&
          (layer.id.startsWith('place_') || layer.id.startsWith('water_'))
        ) {
          map.setLayoutProperty(layer.id, 'text-field', [
            'coalesce',
            ['get', 'name:tr'],
            ['get', 'name_tr'],
            ['get', 'name'],
            ['get', 'name:latin'],
            ['get', 'name_en']
          ]);
        }
      });
    } catch (err) {
      console.warn('Turkish labels could not be applied:', err);
    }
  }, []);

  // Ensure earthquake layers exist on the map
  const ensureEarthquakeLayers = useCallback((map: Map) => {
    const geojsonData = getGeoJSONCollection();

    if (!map.getSource('earthquakes')) {
      map.addSource('earthquakes', {
        type: 'geojson',
        data: geojsonData
      });
    } else {
      (map.getSource('earthquakes') as GeoJSONSource).setData(geojsonData);
    }

    if (!map.getLayer('earthquakes-glow')) {
      map.addLayer({
        id: 'earthquakes-glow',
        type: 'circle',
        source: 'earthquakes',
        paint: {
          'circle-radius': [
            'interpolate', ['linear'], ['coalesce', ['get', 'magnitude'], 1],
            1, 10,
            3, 16,
            5, 24,
            7, 36
          ],
          'circle-color': [
            'step', ['coalesce', ['get', 'magnitude'], 1],
            '#22c55e', 3.0,
            '#eab308', 4.5,
            '#f97316', 6.0,
            '#ef4444'
          ],
          'circle-opacity': 0.35,
          'circle-blur': 0.4
        }
      });
    }

    if (!map.getLayer('earthquakes-circle')) {
      map.addLayer({
        id: 'earthquakes-circle',
        type: 'circle',
        source: 'earthquakes',
        paint: {
          'circle-radius': [
            'interpolate', ['linear'], ['coalesce', ['get', 'magnitude'], 1],
            1, 5,
            3, 8,
            5, 14,
            7, 22
          ],
          'circle-color': [
            'step', ['coalesce', ['get', 'magnitude'], 1],
            '#22c55e', 3.0,
            '#eab308', 4.5,
            '#f97316', 6.0,
            '#ef4444'
          ],
          'circle-stroke-width': 1.5,
          'circle-stroke-color': '#090d16'
        }
      });
    }
  }, [getGeoJSONCollection]);

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    const map = new Map({
      container: mapContainerRef.current,
      style: turkishDarkStyle,
      center: [35.2433, 38.9637], // Turkey center
      zoom: 5.5,
      maxBounds: [
        [20.0, 32.0], // Southwest boundary
        [50.0, 45.0]  // Northeast boundary
      ]
    });

    map.addControl(new NavigationControl({ showCompass: true }), 'top-right');
    map.addControl(new ScaleControl({ unit: 'metric' }), 'bottom-left');

    const handleLoaded = () => {
      applyTurkishLabels(map);
      ensureEarthquakeLayers(map);
    };

    map.on('load', handleLoaded);
    map.on('style.load', handleLoaded);

    // Dynamic click handler for earthquake circles
    map.on('click', (e) => {
      if (!map.getLayer('earthquakes-circle')) return;
      const features = map.queryRenderedFeatures(e.point, { layers: ['earthquakes-circle'] });
      if (features && features.length > 0) {
        const feature = features[0];
        const props = feature.properties as any;
        const item = itemsRef.current.find(it => it.id === props.id);
        if (item) {
          onSelectRef.current(item);
        }
      }
    });

    // Dynamic pointer cursor on hover
    map.on('mousemove', (e) => {
      if (!map.getLayer('earthquakes-circle')) return;
      const features = map.queryRenderedFeatures(e.point, { layers: ['earthquakes-circle'] });
      map.getCanvas().style.cursor = features.length > 0 ? 'pointer' : '';
    });

    mapRef.current = map;

    return () => {
      map.remove();
    };
  }, [applyTurkishLabels, ensureEarthquakeLayers]);

  // Update GeoJSON data when items change
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !map.isStyleLoaded()) return;

    const source = map.getSource('earthquakes') as GeoJSONSource | undefined;
    if (source) {
      source.setData(getGeoJSONCollection());
    } else {
      ensureEarthquakeLayers(map);
    }
  }, [items, ensureEarthquakeLayers, getGeoJSONCollection]);

  // Center & popup on selected earthquake
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !selectedItem) return;

    map.flyTo({
      center: [selectedItem.longitude, selectedItem.latitude],
      zoom: Math.max(map.getZoom(), 7.5),
      essential: true
    });

    if (popupRef.current) {
      popupRef.current.remove();
    }

    const magText = selectedItem.magnitude?.value
      ? `${selectedItem.magnitude.value} ${selectedItem.magnitude.type ?? ''}`
      : 'Raporlanmadı';

    const dateStr = new Date(selectedItem.occurredAt).toLocaleString('tr-TR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }) + ' TSİ';

    const loc = parseItemLocation(selectedItem);

    const popupContent = document.createElement('div');
    popupContent.className = 'map-popup-card';
    popupContent.innerHTML = `
      <div class="popup-mag-badge">${magText}</div>
      <div class="popup-title">${loc.displayTitle}</div>
      ${loc.isOffshore && loc.nearestSettlement ? `<div class="popup-row"><span>En Yakın TR Kıyısı:</span> <strong>${loc.nearestSettlement} (${loc.distanceKm ?? '-'} km)</strong></div>` : ''}
      <div class="popup-row"><span>Tarih:</span> <strong>${dateStr}</strong></div>
      <div class="popup-row"><span>Derinlik:</span> <strong>${selectedItem.depthKm !== null ? selectedItem.depthKm + ' km' : 'Bilinmiyor'}</strong></div>
      <div class="popup-row"><span>Kaynak:</span> <strong>${selectedItem.source}</strong></div>
      <button class="popup-btn-detail" id="popup-btn-more">Detayları İncele →</button>
    `;

    popupContent.querySelector('#popup-btn-more')?.addEventListener('click', () => {
      onOpenDetailRef.current(selectedItem);
    });

    popupRef.current = new Popup({ offset: 15, closeButton: true })
      .setLngLat([selectedItem.longitude, selectedItem.latitude])
      .setDOMContent(popupContent)
      .addTo(map);
  }, [selectedItem]);

  return (
    <div className="map-wrapper" aria-label="Deprem Haritası">
      <div ref={mapContainerRef} className="map-container" />

      {/* Map Legend */}
      <div className="map-legend">
        <span className="legend-title">Büyüklük:</span>
        <span className="legend-item"><span className="legend-dot dot-green" /> &lt; 3.0</span>
        <span className="legend-item"><span className="legend-dot dot-yellow" /> 3.0 - 4.5</span>
        <span className="legend-item"><span className="legend-dot dot-orange" /> 4.5 - 6.0</span>
        <span className="legend-item"><span className="legend-dot dot-red" /> &ge; 6.0</span>
      </div>
    </div>
  );
};
