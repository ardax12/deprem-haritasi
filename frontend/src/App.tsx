import { useState, useEffect, useCallback } from 'react';
import { Header } from './components/Header';
import { FilterBar } from './components/FilterBar';
import { EarthquakeMap } from './components/EarthquakeMap';
import { EarthquakeList } from './components/EarthquakeList';
import { EarthquakeDetailModal } from './components/EarthquakeDetailModal';
import type { EarthquakeItem, FilterParams, Freshness } from './types/earthquake';
import { fetchEarthquakes } from './services/api';
import './App.css';

const defaultFilters: FilterParams = {
  timeRange: '24h',
  minMagnitude: null,
  magnitudeType: 'ALL',
  place: '',
  sortBy: 'occurredAt:desc'
};

export function App() {
  const [filters, setFilters] = useState<FilterParams>(defaultFilters);
  const [items, setItems] = useState<EarthquakeItem[]>([]);
  const [freshness, setFreshness] = useState<Freshness | undefined>(undefined);
  const [selectedItem, setSelectedItem] = useState<EarthquakeItem | null>(null);
  const [modalItem, setModalItem] = useState<EarthquakeItem | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'map' | 'list'>('map');

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchEarthquakes(filters);
      setItems(response.items);
      setFreshness(response.freshness);
    } catch (err: any) {
      setError(err.message || 'Veriler alınamadı');
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Periodic 5-minute auto-refresh (only when tab is visible)
  useEffect(() => {
    const interval = setInterval(() => {
      if (document.visibilityState === 'visible') {
        loadData();
      }
    }, 5 * 60 * 1000);

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        loadData();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      clearInterval(interval);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [loadData]);

  const handleFilterChange = (updated: Partial<FilterParams>) => {
    setFilters(prev => ({ ...prev, ...updated }));
  };

  const handleFilterReset = () => {
    setFilters(defaultFilters);
  };

  const handleSelectItem = (item: EarthquakeItem) => {
    setSelectedItem(item);
  };

  const handleOpenDetail = (item: EarthquakeItem) => {
    setModalItem(item);
  };

  return (
    <div className="app-container">
      <Header
        freshness={freshness}
        totalCount={items.length}
        loading={loading}
        onRefresh={loadData}
      />

      <FilterBar
        filters={filters}
        onChange={handleFilterChange}
        onReset={handleFilterReset}
        totalCount={items.length}
      />

      {error && (
        <div className="global-error-banner" role="alert">
          <span className="error-icon">⚠️</span>
          <span><strong>Bağlantı Sorunu:</strong> {error}. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.</span>
          <button className="btn-error-retry" onClick={loadData}>Tekrar Dene</button>
        </div>
      )}

      {/* Mobile Tab Switcher */}
      <div className="mobile-tabs" role="tablist" aria-label="Görünüm Seçenekleri">
        <button
          id="tab-map"
          role="tab"
          aria-selected={activeTab === 'map'}
          aria-controls="panel-map"
          className={`tab-btn ${activeTab === 'map' ? 'active' : ''}`}
          onClick={() => setActiveTab('map')}
        >
          🗺️ Harita
        </button>
        <button
          id="tab-list"
          role="tab"
          aria-selected={activeTab === 'list'}
          aria-controls="panel-list"
          className={`tab-btn ${activeTab === 'list' ? 'active' : ''}`}
          onClick={() => setActiveTab('list')}
        >
          📋 Olay Listesi ({items.length})
        </button>
      </div>

      <main className="main-content">
        <div
          id="panel-map"
          role="tabpanel"
          aria-labelledby="tab-map"
          className={`map-panel ${activeTab === 'map' ? 'active-mobile' : ''}`}
        >
          <EarthquakeMap
            items={items}
            selectedItem={selectedItem}
            onSelect={handleSelectItem}
            onOpenDetail={handleOpenDetail}
          />
        </div>

        <div
          id="panel-list"
          role="tabpanel"
          aria-labelledby="tab-list"
          className={`list-panel ${activeTab === 'list' ? 'active-mobile' : ''}`}
        >
          <EarthquakeList
            items={items}
            selectedItem={selectedItem}
            onSelect={handleSelectItem}
            onOpenDetail={handleOpenDetail}
            loading={loading}
          />
        </div>
      </main>

      <EarthquakeDetailModal
        item={modalItem}
        onClose={() => setModalItem(null)}
      />
    </div>
  );
}

export default App;
