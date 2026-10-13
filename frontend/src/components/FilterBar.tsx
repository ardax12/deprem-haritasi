import React, { useState, useEffect } from 'react';
import type { FilterParams } from '../types/earthquake';

interface FilterBarProps {
  filters: FilterParams;
  onChange: (updated: Partial<FilterParams>) => void;
  onReset: () => void;
  itemsCount: number;
  totalCount: number;
}

export const FilterBar: React.FC<FilterBarProps> = ({
  filters,
  onChange,
  onReset,
  itemsCount,
  totalCount
}) => {
  const [localPlace, setLocalPlace] = useState(filters.place);

  // Sync local place input when parent filters change (e.g. on reset)
  useEffect(() => {
    setLocalPlace(filters.place);
  }, [filters.place]);

  // Debounce search input by 350ms to prevent request flooding and race conditions
  useEffect(() => {
    const timer = setTimeout(() => {
      if (localPlace !== filters.place) {
        onChange({ place: localPlace });
      }
    }, 350);
    return () => clearTimeout(timer);
  }, [localPlace, filters.place, onChange]);

  const handlePlaceKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (localPlace !== filters.place) {
        onChange({ place: localPlace });
      }
    }
  };

  return (
    <div className="filter-bar" role="search" aria-label="Deprem Filtreleri">
      <div className="filter-group">
        <label htmlFor="time-range-select" className="filter-label">Zaman Aralığı</label>
        <select
          id="time-range-select"
          className="filter-select"
          value={filters.timeRange}
          onChange={(e) => onChange({ timeRange: e.target.value as any })}
        >
          <option value="24h">Son 24 Saat</option>
          <option value="3d">Son 3 Gün</option>
          <option value="7d">Son 7 Gün</option>
          <option value="30d">Son 30 Gün (1 Ay)</option>
        </select>
      </div>

      <div className="filter-group">
        <label htmlFor="min-magnitude-select" className="filter-label">Min Büyüklük</label>
        <select
          id="min-magnitude-select"
          className="filter-select"
          value={filters.minMagnitude !== null && filters.minMagnitude !== undefined ? filters.minMagnitude.toFixed(1) : ''}
          onChange={(e) => {
            const val = e.target.value !== '' ? parseFloat(e.target.value) : null;
            onChange({ minMagnitude: val });
          }}
        >
          <option value="">Tümü (Hepsi)</option>
          <option value="2.0">M ≥ 2.0</option>
          <option value="3.0">M ≥ 3.0</option>
          <option value="4.0">M ≥ 4.0</option>
          <option value="5.0">M ≥ 5.0</option>
        </select>
      </div>

      <div className="filter-group filter-group-grow">
        <label htmlFor="place-search-input" className="filter-label">Yer / Bölge Arama</label>
        <input
          id="place-search-input"
          type="text"
          className="filter-input"
          placeholder="İl, ilçe veya bölge (örn: Malatya, Maraş)..."
          value={localPlace}
          onChange={(e) => setLocalPlace(e.target.value)}
          onKeyDown={handlePlaceKeyDown}
        />
      </div>

      <div className="filter-group">
        <label htmlFor="sort-select" className="filter-label">Sıralama</label>
        <select
          id="sort-select"
          className="filter-select"
          value={filters.sortBy}
          onChange={(e) => onChange({ sortBy: e.target.value as any })}
        >
          <option value="occurredAt:desc">En Yeni Olaylar</option>
          <option value="magnitude:desc">En Büyük Olaylar</option>
        </select>
      </div>

      <div className="filter-meta">
        <span className="count-badge" aria-live="polite">
          {totalCount > itemsCount ? (
            <>
              <strong>{itemsCount} / {totalCount}</strong> deprem listelendi
            </>
          ) : (
            <>
              <strong>{totalCount}</strong> deprem listelendi
            </>
          )}
        </span>
        {(filters.minMagnitude !== null || filters.place !== '' || filters.timeRange !== '24h') && (
          <button type="button" className="btn-reset" onClick={onReset} title="Filtreleri Temizle" aria-label="Filtreleri Temizle">
            ✕ Temizle
          </button>
        )}
      </div>
    </div>
  );
};
