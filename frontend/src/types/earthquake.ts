export interface Magnitude {
  value: number | null;
  type: string | null;
}

export interface EarthquakeItem {
  id: string;
  occurredAt: string;
  retrievedAt: string;
  latitude: number;
  longitude: number;
  depthKm: number | null;
  magnitude: Magnitude | null;
  place: string | null;
  country: string | null;
  province: string | null;
  district: string | null;
  neighborhood: string | null;
  source: string;
  revisionStatus: string;
  revisionNumber: number;
  dataQuality: string[];
  isOffshore?: boolean | null;
  distanceToCoastKm?: number | null;
  nearestSettlement?: string | null;
  environment?: 'offshore' | 'land' | 'cross_border' | string | null;
  displayLocation?: string | null;
}

export interface PageInfo {
  size: number;
  totalElements: number;
  totalPages: number;
  currentPage: number;
  nextCursor: string | null;
}

export interface Freshness {
  source: string;
  lastSuccessfulFetchAt: string | null;
  status: 'current' | 'delayed' | 'stale' | string;
}

export interface EarthquakesResponse {
  items: EarthquakeItem[];
  page: PageInfo;
  freshness: Freshness;
}

export interface EarthquakeDetail {
  current: EarthquakeItem;
  history: EarthquakeItem[];
}

export interface SourceCoverage {
  from: string | null;
  to: string | null;
  complete: boolean;
}

export interface SourceStatus {
  name: string;
  status: string;
  lastSuccessfulFetchAt: string | null;
  lastAttemptAt: string | null;
  coverage: SourceCoverage;
}

export interface SystemStatus {
  application: string;
  mode: string;
  sources: SourceStatus[];
}

export interface FilterParams {
  timeRange: '24h' | '3d' | '7d';
  minMagnitude: number | null;
  magnitudeType: string;
  place: string;
  sortBy: 'occurredAt:desc' | 'magnitude:desc';
}
