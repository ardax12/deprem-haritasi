import React, { useEffect, useRef, useCallback } from 'react';
import { Map, Popup, NavigationControl, ScaleControl, AttributionControl, setWorkerUrl } from 'maplibre-gl';
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

  const selectedItemRef = useRef<EarthquakeItem | null>(selectedItem);
  selectedItemRef.current = selectedItem;

  const itemsRef = useRef<EarthquakeItem[]>(items);
  itemsRef.current = items;

  const onSelectRef = useRef(onSelect);
  onSelectRef.current = onSelect;

  const onOpenDetailRef = useRef(onOpenDetail);
  onOpenDetailRef.current = onOpenDetail;

  const displayPopup = useCallback((item: EarthquakeItem, mapInstance: Map) => {
    mapInstance.flyTo({
      center: [item.longitude, item.latitude],
      zoom: Math.max(mapInstance.getZoom(), 7.5),
      essential: true
    });

    if (popupRef.current) {
      popupRef.current.remove();
    }

    const magText = item.magnitude?.value !== null && item.magnitude?.value !== undefined
      ? `${item.magnitude.value} ${item.magnitude?.type ?? ''}`.trim()
      : 'Bilinmiyor';

    const dateStr = new Date(item.occurredAt).toLocaleString('tr-TR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }) + ' TSİ';

    const loc = parseItemLocation(item);

    // Build safe DOM tree without innerHTML injection
    const popupContent = document.createElement('div');
    popupContent.className = 'map-popup-card';

    const badge = document.createElement('div');
    badge.className = 'popup-mag-badge';
    badge.textContent = magText;
    popupContent.appendChild(badge);

    const title = document.createElement('div');
    title.className = 'popup-title';
    title.textContent = loc.displayTitle;
    popupContent.appendChild(title);

    if (loc.isOffshore && loc.nearestSettlement) {
      const offshoreRow = document.createElement('div');
      offshoreRow.className = 'popup-row';
      const offshoreSpan = document.createElement('span');
      offshoreSpan.textContent = 'En Yakın TR Kıyısı: ';
      const offshoreStrong = document.createElement('strong');
      offshoreStrong.textContent = `${loc.nearestSettlement} (${loc.distanceKm ?? '-'} km)`;
      offshoreRow.appendChild(offshoreSpan);
      offshoreRow.appendChild(offshoreStrong);
      popupContent.appendChild(offshoreRow);
    }

    const dateRow = document.createElement('div');
    dateRow.className = 'popup-row';
    const dateSpan = document.createElement('span');
    dateSpan.textContent = 'Tarih: ';
    const dateStrong = document.createElement('strong');
    dateStrong.textContent = dateStr;
    dateRow.appendChild(dateSpan);
    dateRow.appendChild(dateStrong);
    popupContent.appendChild(dateRow);

    const depthRow = document.createElement('div');
    depthRow.className = 'popup-row';
    const depthSpan = document.createElement('span');
    depthSpan.textContent = 'Derinlik: ';
    const depthStrong = document.createElement('strong');
    depthStrong.textContent = item.depthKm !== null ? `${item.depthKm} km` : 'Bilinmiyor';
    depthRow.appendChild(depthSpan);
    depthRow.appendChild(depthStrong);
    popupContent.appendChild(depthRow);

    const srcRow = document.createElement('div');
    srcRow.className = 'popup-row';
    const srcSpan = document.createElement('span');
    srcSpan.textContent = 'Kaynak: ';
    const srcStrong = document.createElement('strong');
    srcStrong.textContent = item.source || 'AFAD';
    srcRow.appendChild(srcSpan);
    srcRow.appendChild(srcStrong);
    popupContent.appendChild(srcRow);

    const actionsDiv = document.createElement('div');
    actionsDiv.className = 'popup-actions';

    const btnShare = document.createElement('button');
    btnShare.className = 'popup-btn-share';
    btnShare.type = 'button';
    btnShare.title = 'Deprem bağlantısını kopyala veya paylaş';
    btnShare.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line></svg><span>Paylaş</span>';

    const btnDetail = document.createElement('button');
    btnDetail.className = 'popup-btn-detail';
    btnDetail.type = 'button';
    btnDetail.textContent = 'Detaylar →';

    actionsDiv.appendChild(btnShare);
    actionsDiv.appendChild(btnDetail);
    popupContent.appendChild(actionsDiv);

    btnDetail.addEventListener('click', () => {
      onOpenDetailRef.current(item);
    });

    btnShare.addEventListener('click', async () => {
      const shareUrl = `${window.location.origin}${window.location.pathname}?event=${encodeURIComponent(item.id)}`;
      const magVal = item.magnitude?.value !== null && item.magnitude?.value !== undefined ? `M${item.magnitude.value}` : 'Deprem';
      const shareTitle = `${magVal} - ${loc.displayTitle}`;
      const shareText = `${loc.displayTitle} - ${magVal} deprem (${dateStr}). AFAD verisiyle inceleyin:`;

      if (typeof navigator !== 'undefined' && navigator.share) {
        try {
          await navigator.share({ title: shareTitle, text: shareText, url: shareUrl });
          return;
        } catch (err: any) {
          if (err.name === 'AbortError') return;
        }
      }

      try {
        await navigator.clipboard.writeText(shareUrl);
        const originalHtml = btnShare.innerHTML;
        btnShare.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"></polyline></svg><span>Kopyalandı</span>';
        setTimeout(() => {
          btnShare.innerHTML = originalHtml;
        }, 2000);
      } catch (err) {
        console.warn('Bağlantı kopyalanamadı:', err);
      }
    });

    popupRef.current = new Popup({ offset: 15, closeButton: true })
      .setLngLat([item.longitude, item.latitude])
      .setDOMContent(popupContent)
      .addTo(mapInstance);
  }, []);

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
        type: item.magnitude?.type ?? '',
        place: item.place ?? 'Bilinmeyen Konum',
        depthKm: item.depthKm,
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
      attributionControl: false,
      maxBounds: [
        [20.0, 32.0], // Southwest boundary
        [50.0, 45.0]  // Northeast boundary
      ]
    });

    map.addControl(new NavigationControl({ showCompass: true }), 'top-right');
    map.addControl(new ScaleControl({ unit: 'metric' }), 'bottom-left');
    map.addControl(new AttributionControl({
      compact: false,
      customAttribution: '© <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> | <a href="https://openfreemap.org" target="_blank" rel="noopener">OpenFreeMap</a>'
    }), 'bottom-right');

    const handleLoaded = () => {
      applyTurkishLabels(map);
      ensureEarthquakeLayers(map);
      if (selectedItemRef.current) {
        displayPopup(selectedItemRef.current, map);
      }
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
  }, [applyTurkishLabels, ensureEarthquakeLayers, displayPopup]);

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
    displayPopup(selectedItem, map);
  }, [selectedItem, displayPopup]);

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
