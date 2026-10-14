import { useState, useEffect, useCallback, useRef } from 'react';
import { Header } from './components/Header';
import { FilterBar } from './components/FilterBar';
import { EarthquakeMap } from './components/EarthquakeMap';
import { EarthquakeList } from './components/EarthquakeList';
import { EarthquakeDetailModal } from './components/EarthquakeDetailModal';
import { InfoModal } from './components/InfoModal';
import { IconMap, IconList, IconWifiOff, IconAlertTriangle } from './components/Icons';
import { AiSummarySnippet } from './components/AiSummarySnippet';
import { findProvinceBySlug, type ProvinceData } from './data/provinces';
import { useSeo } from './hooks/useSeo';
import type { EarthquakeItem, FilterParams, Freshness, PageInfo, EarthquakeDetail } from './types/earthquake';
import { fetchEarthquakes, fetchEarthquakeDetail, computeTimeWindow, type TimeWindow } from './services/api';
import './App.css';

const defaultFilters: FilterParams = {
  timeRange: '24h',
  minMagnitude: null,
  place: '',
  sortBy: 'occurredAt:desc'
};

const getInitialProvince = (): ProvinceData | null => {
  if (typeof window === 'undefined') return null;
  const path = window.location.pathname;
  const match = path.match(/^\/son-depremler\/([a-z0-9-]+)/i);
  if (match) {
    return findProvinceBySlug(match[1]) || null;
  }
  return null;
};

export function App() {
  const [activeProvince, setActiveProvince] = useState<ProvinceData | null>(getInitialProvince);
  const [filters, setFilters] = useState<FilterParams>(() => {
    const initProv = getInitialProvince();
    return initProv ? { ...defaultFilters, place: initProv.name } : defaultFilters;
  });
  const [items, setItems] = useState<EarthquakeItem[]>([]);
  const [pageInfo, setPageInfo] = useState<PageInfo | null>(null);
  const [freshness, setFreshness] = useState<Freshness | undefined>(undefined);
  const [selectedItem, setSelectedItem] = useState<EarthquakeItem | null>(null);
  const [modalItem, setModalItem] = useState<EarthquakeItem | null>(null);
  const [infoModalOpen, setInfoModalOpen] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);
  const [loadingMore, setLoadingMore] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'map' | 'list'>('map');
  const [isOnline, setIsOnline] = useState<boolean>(typeof navigator !== 'undefined' ? navigator.onLine : true);
  const [installPrompt, setInstallPrompt] = useState<any>(null);

  const activeRequestIdRef = useRef<number>(0);
  const deepLinkedItemRef = useRef<EarthquakeItem | null>(null);
  const currentTimeWindowRef = useRef<TimeWindow>(computeTimeWindow(defaultFilters.timeRange));

  // Monitor network online/offline status
  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Monitor PWA install prompt
  useEffect(() => {
    const handleBeforeInstall = (e: Event) => {
      e.preventDefault();
      setInstallPrompt(e);
    };

    const handleAppInstalled = () => {
      setInstallPrompt(null);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstall);
    window.addEventListener('appinstalled', handleAppInstalled);

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstall);
      window.removeEventListener('appinstalled', handleAppInstalled);
    };
  }, []);

  const handleInstallApp = async () => {
    if (!installPrompt) return;
    installPrompt.prompt();
    try {
      const choice = await installPrompt.userChoice;
      if (choice && choice.outcome === 'accepted') {
        setInstallPrompt(null);
      }
    } catch (err) {
      console.warn('Uygulama yükleme isteği tamamlanamadı:', err);
    }
  };

  // Deep-link initial check: if URL contains ?event=<id>, fetch its details immediately
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const eventId = params.get('event');
    if (!eventId) return;

    fetchEarthquakeDetail(eventId)
      .then((res: EarthquakeDetail) => {
        deepLinkedItemRef.current = res.current;
        setItems(prev => (prev.some(it => it.id === res.current.id) ? prev : [res.current, ...prev]));
        setSelectedItem(res.current);
        setModalItem(res.current);
      })
      .catch((err: any) => {
        console.warn('Derin bağlantı deprem verisi alınamadı:', err);
      });
  }, []);

  // Listen to browser Back/Forward (popstate)
  useEffect(() => {
    const handlePopState = () => {
      const params = new URLSearchParams(window.location.search);
      const eventId = params.get('event');
      if (!eventId) {
        setModalItem(null);
      } else {
        const found = items.find(it => it.id === eventId);
        if (found) {
          setSelectedItem(found);
          setModalItem(found);
        } else {
          fetchEarthquakeDetail(eventId)
            .then((res: EarthquakeDetail) => {
              deepLinkedItemRef.current = res.current;
              setItems(prev => (prev.some(it => it.id === res.current.id) ? prev : [res.current, ...prev]));
              setSelectedItem(res.current);
              setModalItem(res.current);
            })
            .catch(() => {});
        }
      }
    };

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, [items]);

  // Fetch first page whenever filters change, cancelling previous in-flight requests
  useEffect(() => {
    const controller = new AbortController();
    const requestId = ++activeRequestIdRef.current;
    setLoading(true);
    setError(null);

    const timeWindow = computeTimeWindow(filters.timeRange);
    currentTimeWindowRef.current = timeWindow;

    fetchEarthquakes(filters, 0, timeWindow, controller.signal)
      .then(response => {
        if (requestId === activeRequestIdRef.current) {
          const loadedItems = [...response.items];
          if (deepLinkedItemRef.current && !loadedItems.some(it => it.id === deepLinkedItemRef.current!.id)) {
            loadedItems.unshift(deepLinkedItemRef.current);
          }
          setItems(loadedItems);
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
      const timeWindow = computeTimeWindow(filters.timeRange);
      currentTimeWindowRef.current = timeWindow;
      const response = await fetchEarthquakes(filters, 0, timeWindow, controller.signal);
      if (requestId === activeRequestIdRef.current) {
        const loadedItems = [...response.items];
        if (deepLinkedItemRef.current && !loadedItems.some(it => it.id === deepLinkedItemRef.current!.id)) {
          loadedItems.unshift(deepLinkedItemRef.current);
        }
        setItems(loadedItems);
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
      const response = await fetchEarthquakes(filters, nextPage, currentTimeWindowRef.current);
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
    if (updated.place !== undefined && updated.place.trim() === '') {
      setActiveProvince(null);
      if (typeof window !== 'undefined' && window.location.pathname.startsWith('/son-depremler')) {
        window.history.pushState({}, '', '/');
      }
    }
    setFilters(prev => ({ ...prev, ...updated }));
  };

  const handleFilterReset = () => {
    setFilters(defaultFilters);
    setActiveProvince(null);
    if (typeof window !== 'undefined' && window.location.pathname.startsWith('/son-depremler')) {
      window.history.pushState({}, '', '/');
    }
  };

  const handleSelectItem = (item: EarthquakeItem) => {
    setSelectedItem(item);
    if (typeof window !== 'undefined' && window.innerWidth <= 900) {
      setActiveTab('map');
    }
  };

  const handleOpenDetail = (item: EarthquakeItem) => {
    setSelectedItem(item);
    setModalItem(item);
    const url = new URL(window.location.href);
    if (url.searchParams.get('event') !== item.id) {
      url.searchParams.set('event', item.id);
      window.history.pushState({ eventId: item.id }, '', url.toString());
    }
  };

  const handleCloseModal = () => {
    setModalItem(null);
    const url = new URL(window.location.href);
    if (url.searchParams.has('event')) {
      url.searchParams.delete('event');
      window.history.replaceState({}, '', url.toString());
    }
  };

  const totalCount = pageInfo ? pageInfo.totalElements : items.length;
  const hasMore = pageInfo ? items.length < pageInfo.totalElements : false;

  useSeo({
    provinceName: activeProvince?.name,
    activeItem: selectedItem,
    totalCount,
    latestItem: items[0]
  });

  return (
    <div className="app-container">
      <Header
        freshness={freshness}
        itemsCount={items.length}
        totalCount={totalCount}
        loading={loading}
        onRefresh={refreshData}
        onOpenInfo={() => setInfoModalOpen(true)}
        isOnline={isOnline}
        isInstallable={!!installPrompt}
        onInstall={handleInstallApp}
      />

      {!isOnline && (
        <div className="global-offline-banner" role="status">
          <span className="offline-icon"><IconWifiOff size={16} /></span>
          <span><strong>Çevrimdışı Mod:</strong> İnternet bağlantısı kesildi. Cihazınızda önbelleğe alınmış son deprem verileri gösterilmektedir.</span>
        </div>
      )}

      <FilterBar
        filters={filters}
        onChange={handleFilterChange}
        onReset={handleFilterReset}
        itemsCount={items.length}
        totalCount={totalCount}
      />

      <AiSummarySnippet
        items={items}
        totalCount={totalCount}
        provinceName={activeProvince?.name}
      />

      {error && (
        <div className="global-error-banner" role="alert">
          <span className="error-icon"><IconAlertTriangle size={16} /></span>
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
          <IconMap size={16} /> Harita
        </button>
        <button
          id="tab-list"
          role="tab"
          aria-selected={activeTab === 'list'}
          aria-controls="panel-list"
          className={`tab-btn ${activeTab === 'list' ? 'active' : ''}`}
          onClick={() => setActiveTab('list')}
        >
          <IconList size={16} /> Olay Listesi ({totalCount > items.length ? `${items.length} / ${totalCount}` : items.length})
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
            error={error}
            onRetry={refreshData}
          />
        </div>
      </main>

      <EarthquakeDetailModal
        item={modalItem}
        onClose={handleCloseModal}
      />

      <InfoModal
        isOpen={infoModalOpen}
        onClose={() => setInfoModalOpen(false)}
      />
    </div>
  );
}

export default App;
