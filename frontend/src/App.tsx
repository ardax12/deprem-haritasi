import { useState, useEffect, useCallback, useRef } from 'react';
import { Header } from './components/Header';
import { FilterBar } from './components/FilterBar';
import { EarthquakeMap } from './components/EarthquakeMap';
import { EarthquakeList } from './components/EarthquakeList';
import { EarthquakeDetailModal } from './components/EarthquakeDetailModal';
import type { EarthquakeItem, FilterParams, Freshness, PageInfo } from './types/earthquake';
import { fetchEarthquakes } from './services/api';
import './App.css';

const defaultFilters: FilterParams = {
  timeRange: '24h',
  minMagnitude: null,
  place: '',
  sortBy: 'occurredAt:desc'
};

export function App() {
  const [filters, setFilters] = useState<FilterParams>(defaultFilters);
  const [items, setItems] = useState<EarthquakeItem[]>([]);
  const [pageInfo, setPageInfo] = useState<PageInfo | null>(null);
  const [freshness, setFreshness] = useState<Freshness | undefined>(undefined);
  const [selectedItem, setSelectedItem] = useState<EarthquakeItem | null>(null);
  const [modalItem, setModalItem] = useState<EarthquakeItem | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [loadingMore, setLoadingMore] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'map' | 'list'>('map');

  const activeRequestIdRef = useRef<number>(0);

  // Fetch first page whenever filters change, cancelling previous in-flight requests
  useEffect(() => {
    const controller = new AbortController();
    const requestId = ++activeRequestIdRef.current;
    setLoading(true);
    setError(null);

    fetchEarthquakes(filters, 0, controller.signal)
      .then(response => {
        if (requestId === activeRequestIdRef.current) {
          setItems(response.items);
          setPageInfo(response.page);
          setFreshness(response.freshness);
          setLoading(false);
        }
      })
      .catch(err => {
        if (err.name === 'AbortError') return;
        if (requestId === activeRequestIdRef.current) {
          setError(err.message || 'Veriler alınamadı');
          setLoading(false);
        }
      });

    return () => {
      controller.abort();
    };
  }, [filters]);

  const refreshData = useCallback(async () => {
    const controller = new AbortController();
    const requestId = ++activeRequestIdRef.current;
    setLoading(true);
    setError(null);

    try {
      const response = await fetchEarthquakes(filters, 0, controller.signal);
      if (requestId === activeRequestIdRef.current) {
        setItems(response.items);
        setPageInfo(response.page);
        setFreshness(response.freshness);
      }
    } catch (err: any) {
      if (err.name === 'AbortError') return;
      if (requestId === activeRequestIdRef.current) {
        setError(err.message || 'Veriler alınamadı');
      }
    } finally {
      if (requestId === activeRequestIdRef.current) {
        setLoading(false);
      }
    }
  }, [filters]);

  // Periodic 5-minute auto-refresh (only when tab is visible)
  useEffect(() => {
    const interval = setInterval(() => {
      if (document.visibilityState === 'visible') {
        refreshData();
      }
    }, 5 * 60 * 1000);

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        refreshData();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      clearInterval(interval);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [refreshData]);

  const handleLoadMore = async () => {
    if (!pageInfo || loadingMore) return;
    const nextPage = pageInfo.currentPage + 1;
    if (nextPage >= pageInfo.totalPages) return;

    setLoadingMore(true);
    try {
      const response = await fetchEarthquakes(filters, nextPage);
      setItems(prev => [...prev, ...response.items]);
      setPageInfo(response.page);
      setFreshness(response.freshness);
    } catch (err: any) {
      console.error('Daha fazla veri yüklenemedi:', err);
    } finally {
      setLoadingMore(false);
    }
  };

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

  const totalCount = pageInfo ? pageInfo.totalElements : items.length;
  const hasMore = pageInfo ? items.length < pageInfo.totalElements : false;

  return (
    <div className="app-container">
      <Header
        freshness={freshness}
        itemsCount={items.length}
        totalCount={totalCount}
        loading={loading}
        onRefresh={refreshData}
      />

      <FilterBar
        filters={filters}
        onChange={handleFilterChange}
        onReset={handleFilterReset}
        itemsCount={items.length}
        totalCount={totalCount}
      />

      {error && (
        <div className="global-error-banner" role="alert">
          <span className="error-icon">⚠️</span>
          <span><strong>Bağlantı Sorunu:</strong> {error}. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.</span>
          <button className="btn-error-retry" onClick={refreshData}>Tekrar Dene</button>
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
          📋 Olay Listesi ({totalCount > items.length ? `${items.length} / ${totalCount}` : items.length})
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
            totalCount={totalCount}
            hasMore={hasMore}
            loadingMore={loadingMore}
            onLoadMore={handleLoadMore}
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
