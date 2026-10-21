import React, { useEffect, useRef, useCallback, useState } from 'react';
import { Map, Popup, NavigationControl, ScaleControl, AttributionControl, setWorkerUrl } from 'maplibre-gl';
import type { StyleSpecification } from 'maplibre-gl';
import maplibreWorkerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?url';
import type { GeoJSONSource } from 'maplibre-gl';
import type { Feature, Point, FeatureCollection } from 'geojson';
import 'maplibre-gl/dist/maplibre-gl.css';
import type { EarthquakeItem } from '../types/earthquake';
import type { ProvinceData } from '../data/provinces';
import turkishDarkStyleJson from '../assets/turkish-dark-style.json';
import turkeyFaultsJson from '../assets/turkey-faults.json';
import { parseItemLocation } from '../utils/locationHelper';

setWorkerUrl(maplibreWorkerUrl);
const turkishDarkStyle = turkishDarkStyleJson as unknown as StyleSpecification;

interface EarthquakeMapProps {
  items: EarthquakeItem[];
  selectedItem: EarthquakeItem | null;
  activeProvince?: ProvinceData | null;
  isVisible?: boolean;
  onSelect: (item: EarthquakeItem) => void;
  onOpenDetail: (item: EarthquakeItem) => void;
}

export const EarthquakeMap: React.FC<EarthquakeMapProps> = ({
  items,
  selectedItem,
  activeProvince,
  isVisible,
  onSelect,
  onOpenDetail
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const popupRef = useRef<Popup | null>(null);
  const faultPopupRef = useRef<Popup | null>(null);

  const [showFaults, setShowFaults] = useState<boolean>(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('deprem_layer_faults') === 'true';
    }
    return false; // disabled by default per user request
  });
  const showFaultsRef = useRef<boolean>(showFaults);
  useEffect(() => {
    showFaultsRef.current = showFaults;
  }, [showFaults]);

  const toggleFaults = () => {
    const nextVal = !showFaults;
    setShowFaults(nextVal);
    if (typeof window !== 'undefined') {
      localStorage.setItem('deprem_layer_faults', String(nextVal));
    }
  };

  const selectedItemRef = useRef<EarthquakeItem | null>(selectedItem);
  const itemsRef = useRef<EarthquakeItem[]>(items);
  const onSelectRef = useRef(onSelect);
  const onOpenDetailRef = useRef(onOpenDetail);

  useEffect(() => {
    selectedItemRef.current = selectedItem;
    itemsRef.current = items;
    onSelectRef.current = onSelect;
    onOpenDetailRef.current = onOpenDetail;
  }, [selectedItem, items, onSelect, onOpenDetail]);

  const displayFaultPopup = useCallback((props: any, lngLat: [number, number], mapInstance: Map) => {
    if (faultPopupRef.current) {
      faultPopupRef.current.remove();
    }

    const typeDesc = props.type === 'STRIKE_SLIP'
      ? 'Doğrultu Atımlı Fay (Yanal Öteleme)'
      : props.type === 'NORMAL'
      ? 'Normal / Açılma Fayı (Graben Çöküntüsü)'
      : props.type === 'THRUST'
      ? 'Ters / Bindirme Fayı (Sıkışma Kuşağı)'
      : 'Aktif Fay';

    const typeColor = props.type === 'STRIKE_SLIP' ? '#ef4444' : props.type === 'NORMAL' ? '#f59e0b' : '#38bdf8';

    const popupContent = document.createElement('div');
    popupContent.className = 'fault-popup-card';

    const header = document.createElement('div');
    header.className = 'fault-popup-header';

    const pill = document.createElement('span');
    pill.className = 'fault-kinematics-pill';
    pill.style.backgroundColor = `${typeColor}22`;
    pill.style.color = typeColor;
    pill.style.border = `1px solid ${typeColor}55`;
    pill.textContent = props.type === 'STRIKE_SLIP' ? 'Doğrultu Atımlı' : props.type === 'NORMAL' ? 'Normal / Graben' : 'Bindirme / Ters';
    header.appendChild(pill);

    const badgeTitle = document.createElement('div');
    badgeTitle.className = 'fault-badge-title';
    badgeTitle.textContent = props.name || 'Diri Fay Segmenti';
    header.appendChild(badgeTitle);

    const badgeZone = document.createElement('div');
    badgeZone.className = 'fault-badge-zone';
    badgeZone.textContent = props.zone || 'Türkiye Diri Fay Kuşağı';
    header.appendChild(badgeZone);
    popupContent.appendChild(header);

    const row1 = document.createElement('div');
    row1.className = 'fault-popup-row';
    const label1 = document.createElement('span');
    label1.className = 'fault-row-label';
    label1.textContent = 'Kinematik Tip:';
    const strong1 = document.createElement('strong');
    strong1.textContent = typeDesc;
    row1.appendChild(label1);
    row1.appendChild(strong1);
    popupContent.appendChild(row1);

    const row2 = document.createElement('div');
    row2.className = 'fault-popup-row';
    const label2 = document.createElement('span');
    label2.className = 'fault-row-label';
    label2.textContent = 'Kaynak:';
    const span2 = document.createElement('span');
    span2.className = 'fault-source-mta';
    span2.textContent = 'MTA 1:1.250.000 Diri Fay Veri Seti';
    row2.appendChild(label2);
    row2.appendChild(span2);
    popupContent.appendChild(row2);

    faultPopupRef.current = new Popup({ offset: 10, closeButton: true })
      .setLngLat(lngLat)
      .setDOMContent(popupContent)
      .addTo(mapInstance);
  }, []);

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

    const dateTsi = new Date(item.occurredAt).toLocaleString('tr-TR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }) + ' TSİ';

    let dateUtc = '';
    try {
      dateUtc = new Date(item.occurredAt).toISOString().replace('T', ' ').substring(0, 16) + ' UTC';
    } catch {
      dateUtc = item.occurredAt;
    }

    const dateStr = `${dateTsi} (${dateUtc})`;

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

    const formatQuality = (quality?: string | null) => {
      if (!quality) return '';
      const q = quality.toUpperCase();
      if (q === 'AUTOMATIC') return 'Otomatik';
      if (q === 'MANUAL' || q === 'MANUAL_REVIEWED') return 'Uzman İncelemesi';
      if (q === 'PRELIMINARY') return 'Ön Çözüm';
      return quality;
    };

    const srcRow = document.createElement('div');
    srcRow.className = 'popup-row';
    const srcSpan = document.createElement('span');
    srcSpan.textContent = 'Kaynak: ';
    const srcStrong = document.createElement('strong');
    const sourceLabel = item.source === 'KOERI' ? 'Kandilli (KOERI)' : 'AFAD';
    const qualityLabel = formatQuality(item.solutionQuality);
    srcStrong.textContent = qualityLabel ? `${sourceLabel} (${qualityLabel})` : sourceLabel;
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
      const sourceName = item.source === 'KOERI' ? 'Kandilli' : 'AFAD';
      const shareText = `${loc.displayTitle} - ${magVal} deprem (${dateStr}). ${sourceName} verisiyle inceleyin:`;

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
    const features: Feature<Point>[] = itemsRef.current.map((item) => {
      const magVal = item.magnitude?.value ?? 0;
      const magFormatted = item.magnitude?.value !== null && item.magnitude?.value !== undefined
        ? item.magnitude.value.toFixed(1)
        : '';

      return {
        type: 'Feature',
        geometry: {
          type: 'Point',
          coordinates: [item.longitude, item.latitude]
        },
        properties: {
          id: item.id,
          magnitude: magVal,
          magFormatted,
          type: item.magnitude?.type ?? '',
          place: item.place ?? 'Bilinmeyen Konum',
          depthKm: item.depthKm,
          occurredAt: item.occurredAt
        }
      };
    });

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
            3, 18,
            5, 26,
            7, 38
          ],
          'circle-color': [
            'step', ['coalesce', ['get', 'magnitude'], 1],
            '#22c55e', 3.0,
            '#eab308', 4.5,
            '#f97316', 6.0,
            '#ef4444'
          ],
          'circle-opacity': 0.25,
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
            1, 6,
            3, 10,
            5, 16,
            7, 24
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

    if (!map.getLayer('earthquakes-label')) {
      map.addLayer({
        id: 'earthquakes-label',
        type: 'symbol',
        source: 'earthquakes',
        minzoom: 5.0,
        layout: {
          'text-field': ['get', 'magFormatted'],
          'text-font': ['Noto Sans Regular'],
          'text-size': [
            'interpolate', ['linear'], ['zoom'],
            5.0, 9,
            7.0, 10.5,
            9.0, 12
          ],
          'text-anchor': 'center',
          'symbol-sort-key': ['-', ['coalesce', ['get', 'magnitude'], 0]],
          'text-allow-overlap': false,
          'text-ignore-placement': false
        },
        paint: {
          'text-color': '#ffffff',
          'text-halo-color': '#090d16',
          'text-halo-width': 1.5
        }
      });
    }
  }, [getGeoJSONCollection]);

  // Ensure MTA active fault lines layers exist on the map
  const ensureFaultLayers = useCallback((map: Map, isVisible: boolean) => {
    if (!map.getSource('mta-faults')) {
      map.addSource('mta-faults', {
        type: 'geojson',
        data: turkeyFaultsJson as any
      });
    }

    const visibility = isVisible ? 'visible' : 'none';

    if (!map.getLayer('mta-faults-casing')) {
      map.addLayer({
        id: 'mta-faults-casing',
        type: 'line',
        source: 'mta-faults',
        layout: {
          'visibility': visibility,
          'line-cap': 'round',
          'line-join': 'round'
        },
        paint: {
          'line-color': '#090d16',
          'line-width': [
            'interpolate', ['linear'], ['zoom'],
            4, 2.5,
            7, 3.8,
            10, 5.0
          ],
          'line-opacity': 0.85
        }
      }, 'earthquakes-glow');
    } else {
      map.setLayoutProperty('mta-faults-casing', 'visibility', visibility);
    }

    if (!map.getLayer('mta-faults-line')) {
      map.addLayer({
        id: 'mta-faults-line',
        type: 'line',
        source: 'mta-faults',
        layout: {
          'visibility': visibility,
          'line-cap': 'round',
          'line-join': 'round'
        },
        paint: {
          'line-color': [
            'match', ['get', 'type'],
            'STRIKE_SLIP', '#ef4444',
            'NORMAL', '#f59e0b',
            'THRUST', '#38bdf8',
            '#ef4444'
          ],
          'line-width': [
            'interpolate', ['linear'], ['zoom'],
            4, 1.2,
            7, 2.2,
            10, 3.5
          ],
          'line-opacity': 0.95
        }
      }, 'earthquakes-glow');
    } else {
      map.setLayoutProperty('mta-faults-line', 'visibility', visibility);
    }

    if (!map.getLayer('mta-faults-label')) {
      map.addLayer({
        id: 'mta-faults-label',
        type: 'symbol',
        source: 'mta-faults',
        minzoom: 7.2,
        layout: {
          'visibility': visibility,
          'text-field': ['get', 'name'],
          'text-font': ['Noto Sans Regular'],
          'text-size': [
            'interpolate', ['linear'], ['zoom'],
            7.2, 9,
            10, 11
          ],
          'symbol-placement': 'line',
          'text-anchor': 'center',
          'text-max-angle': 30,
          'text-offset': [0, 0.6]
        },
        paint: {
          'text-color': '#cbd5e1',
          'text-halo-color': '#090d16',
          'text-halo-width': 2.0
        }
      }, 'earthquakes-glow');
    } else {
      map.setLayoutProperty('mta-faults-label', 'visibility', visibility);
    }

    if (!map.getLayer('mta-faults-hitbox')) {
      map.addLayer({
        id: 'mta-faults-hitbox',
        type: 'line',
        source: 'mta-faults',
        layout: {
          'visibility': visibility
        },
        paint: {
          'line-width': 18,
          'line-opacity': 0.0
        }
      });
    } else {
      map.setLayoutProperty('mta-faults-hitbox', 'visibility', visibility);
    }
  }, []);

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
      customAttribution: '© <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> | <a href="https://openfreemap.org" target="_blank" rel="noopener">OpenFreeMap</a> | MTA Diri Faylar'
    }), 'bottom-right');

    const handleLoaded = () => {
      applyTurkishLabels(map);
      ensureEarthquakeLayers(map);
      ensureFaultLayers(map, showFaultsRef.current);
      if (selectedItemRef.current) {
        displayPopup(selectedItemRef.current, map);
      }
    };

    map.on('load', handleLoaded);
    map.on('style.load', handleLoaded);

    const interactiveLayers = ['earthquakes-circle', 'earthquakes-label'];

    // Dynamic click handler for earthquake markers and fault lines
    map.on('click', (e) => {
      const activeLayers = interactiveLayers.filter(l => map.getLayer(l));
      if (activeLayers.length > 0) {
        const features = map.queryRenderedFeatures(e.point, { layers: activeLayers });
        if (features && features.length > 0) {
          const feature = features[0];
          const props = feature.properties as any;
          const item = itemsRef.current.find(it => it.id === props.id);
          if (item) {
            onSelectRef.current(item);
            return;
          }
        }
      }

      // Check fault lines if faults layer is active
      if (showFaultsRef.current && map.getLayer('mta-faults-hitbox')) {
        const faultFeatures = map.queryRenderedFeatures(e.point, { layers: ['mta-faults-hitbox'] });
        if (faultFeatures && faultFeatures.length > 0) {
          displayFaultPopup(faultFeatures[0].properties, [e.lngLat.lng, e.lngLat.lat], map);
        }
      }
    });

    // Dynamic pointer cursor on hover
    map.on('mousemove', (e) => {
      const activeLayers = interactiveLayers.filter(l => map.getLayer(l));
      const features = activeLayers.length > 0
        ? map.queryRenderedFeatures(e.point, { layers: activeLayers })
        : [];

      if (features.length > 0) {
        map.getCanvas().style.cursor = 'pointer';
        return;
      }

      if (showFaultsRef.current && map.getLayer('mta-faults-hitbox')) {
        const faultFeatures = map.queryRenderedFeatures(e.point, { layers: ['mta-faults-hitbox'] });
        if (faultFeatures.length > 0) {
          map.getCanvas().style.cursor = 'pointer';
          return;
        }
      }

      map.getCanvas().style.cursor = '';
    });

    mapRef.current = map;

    return () => {
      map.remove();
    };
  }, [applyTurkishLabels, ensureEarthquakeLayers, ensureFaultLayers, displayPopup, displayFaultPopup]);

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

  // Synchronize fault layer visibility when showFaults changes
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !map.isStyleLoaded()) return;
    ensureFaultLayers(map, showFaults);
  }, [showFaults, ensureFaultLayers]);

  // Center & popup on selected earthquake
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    if (!selectedItem) {
      popupRef.current?.remove();
      popupRef.current = null;
      return;
    }
    displayPopup(selectedItem, map);
  }, [selectedItem, displayPopup]);

  // Center on active province when selected
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !activeProvince) return;
    if (selectedItemRef.current) return;

    map.flyTo({
      center: [activeProvince.lng, activeProvince.lat],
      zoom: activeProvince.zoom,
      duration: 900,
      essential: true
    });
  }, [activeProvince]);

  // Resize map when visibility changes
  useEffect(() => {
    if (isVisible) {
      mapRef.current?.resize();
    }
  }, [isVisible]);

  return (
    <div className="map-wrapper" aria-label="Deprem Haritası">
      <div ref={mapContainerRef} className="map-container" />

      {/* MTA Faults Toggle & Legend */}
      <div className="map-fault-controls">
        <button
          type="button"
          className={`btn-toggle-faults ${showFaults ? 'active' : ''}`}
          onClick={toggleFaults}
          title="MTA Diri Fay Hatları Katmanını Aç/Kapat"
          aria-pressed={showFaults}
        >
          <svg className="fault-icon-svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
          </svg>
          <span>Diri Faylar</span>
          <span className={`fault-status-tag ${showFaults ? 'on' : 'off'}`}>
            {showFaults ? 'Açık' : 'Kapalı'}
          </span>
        </button>

        {showFaults && (
          <div className="faults-mini-legend">
            <span className="legend-item"><span className="fault-legend-line line-red" /> Doğrultu Atımlı (KAF/DAF)</span>
            <span className="legend-item"><span className="fault-legend-line line-amber" /> Normal / Graben</span>
            <span className="legend-item"><span className="fault-legend-line line-blue" /> Bindirme / Ters</span>
          </div>
        )}
      </div>

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
