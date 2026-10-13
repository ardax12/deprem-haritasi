import type { EarthquakesResponse, EarthquakeDetail, SystemStatus, FilterParams } from '../types/earthquake';

const API_BASE = '/api/v1';

export interface TimeWindow {
  start: string;
  end: string;
}

export function computeTimeWindow(timeRange: FilterParams['timeRange']): TimeWindow {
  const now = new Date();
  let start = new Date(now.getTime() - 24 * 60 * 60 * 1000); // default 24h

  if (timeRange === '3d') {
    start = new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000);
  } else if (timeRange === '7d') {
    start = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
  } else if (timeRange === '30d') {
    start = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
  }

  return {
    start: start.toISOString(),
    end: now.toISOString()
  };
}

export async function fetchEarthquakes(
  filters: FilterParams,
  page: number = 0,
  timeWindow?: TimeWindow,
  signal?: AbortSignal
): Promise<EarthquakesResponse> {
  const window = timeWindow ?? computeTimeWindow(filters.timeRange);

  const query = new URLSearchParams();
  query.set('start', window.start);
  query.set('end', window.end);
  query.set('page', page.toString());
  query.set('pageSize', '500');
  query.set('sort', filters.sortBy);

  if (filters.minMagnitude !== null && filters.minMagnitude !== undefined && filters.minMagnitude > 0) {
    query.set('minMagnitude', filters.minMagnitude.toString());
  }
  if (filters.magnitudeType && filters.magnitudeType !== 'ALL') {
    query.set('magnitudeType', filters.magnitudeType.trim());
  }
  if (filters.place && filters.place.trim().length > 0) {
    query.set('place', filters.place.trim());
  }

  const res = await fetch(`${API_BASE}/earthquakes?${query.toString()}`, {
    signal,
    headers: {
      'Accept': 'application/json'
    }
  });

  if (!res.ok) {
    const errorBody = await res.json().catch(() => null);
    throw new Error(errorBody?.detail || `Sunucu hatası: HTTP ${res.status}`);
  }

  return res.json();
}

export async function fetchEarthquakeDetail(id: string): Promise<EarthquakeDetail> {
  const res = await fetch(`${API_BASE}/earthquakes/${encodeURIComponent(id)}`, {
    headers: {
      'Accept': 'application/json'
    }
  });

  if (!res.ok) {
    throw new Error(`Detay yüklenemedi: HTTP ${res.status}`);
  }

  return res.json();
}

export async function fetchStatus(): Promise<SystemStatus> {
  const res = await fetch(`${API_BASE}/status`, {
    headers: {
      'Accept': 'application/json'
    }
  });

  if (!res.ok) {
    throw new Error(`Durum sorgulanamadı: HTTP ${res.status}`);
  }

  return res.json();
}
