import React, { useState, useEffect } from 'react';
import type { FilterParams } from '../types/earthquake';
import { TURKISH_PROVINCES, findProvinceByName } from '../data/provinces';

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
      const matched = findProvinceByName(localPlace);
      const targetPlace = matched ? matched.name : localPlace;
      if (matched && matched.name !== localPlace) {
        setLocalPlace(matched.name);
      }
      if (targetPlace !== filters.place) {
        onChange({ place: targetPlace });
      }
    }
  };

  const handleClearPlace = () => {
    setLocalPlace('');
    onChange({ place: '' });
  };

  const isChipActive = (type: 'all' | 'felt' | 'major' | '3d') => {
    if (type === 'all') {
      return filters.minMagnitude === null && filters.timeRange === '24h' && !filters.place && (!filters.source || filters.source === 'ALL');
    }
    if (type === 'felt') {
      return filters.minMagnitude === 3.5 && filters.timeRange === '24h';
    }
    if (type === 'major') {
      return filters.minMagnitude === 4.5;
    }
    if (type === '3d') {
      return filters.timeRange === '3d' && filters.minMagnitude === null;
    }
    return false;
  };

  const handleChipClick = (type: 'all' | 'felt' | 'major' | '3d') => {
    if (type === 'all') {
      onReset();
    } else if (type === 'felt') {
      onChange({ minMagnitude: 3.5, timeRange: '24h' });
    } else if (type === 'major') {
      onChange({ minMagnitude: 4.5, timeRange: '7d' });
    } else if (type === '3d') {
      onChange({ timeRange: '3d', minMagnitude: null });
    }
  };

  return (
    <div className="filter-wrapper" role="search" aria-label="Deprem Filtreleri">
      <div className="filter-chips-container" role="group" aria-label="Hızlı Filtre Çipleri">
        <span className="chips-label">Hızlı Seçim:</span>
        <button
          type="button"
          className={`chip-btn ${isChipActive('all') ? 'active' : ''}`}
          onClick={() => handleChipClick('all')}
        >
          Tümü (Son 24s)
        </button>
        <button
          type="button"
          className={`chip-btn ${isChipActive('felt') ? 'active' : ''}`}
          onClick={() => handleChipClick('felt')}
        >
          Hissedilenler (M ≥ 3.5)
        </button>
        <button
          type="button"
          className={`chip-btn ${isChipActive('major') ? 'active' : ''}`}
          onClick={() => handleChipClick('major')}
        >
          Belirgin (M ≥ 4.5)
        </button>
        <button
          type="button"
          className={`chip-btn ${isChipActive('3d') ? 'active' : ''}`}
          onClick={() => handleChipClick('3d')}
        >
          Son 3 Gün
        </button>
      </div>

      <div className="filter-bar">
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
            <option value="3.5">M ≥ 3.5</option>
            <option value="4.0">M ≥ 4.0</option>
            <option value="4.5">M ≥ 4.5</option>
            <option value="5.0">M ≥ 5.0</option>
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="source-select" className="filter-label">Veri Kaynağı</label>
          <select
            id="source-select"
            className="filter-select"
            value={filters.source || 'ALL'}
            onChange={(e) => onChange({ source: e.target.value as any })}
          >
            <option value="ALL">Tümü (AFAD & Kandilli)</option>
            <option value="AFAD">Yalnızca AFAD</option>
            <option value="KOERI">Yalnızca Kandilli</option>
          </select>
        </div>

        <div className="filter-group filter-group-grow">
          <label htmlFor="place-search-input" className="filter-label">Yer / Bölge Arama</label>
          <div className="search-input-wrapper">
            <input
              id="place-search-input"
              type="text"
              className="filter-input"
              placeholder="İl, ilçe veya bölge (örn: Malatya, Ege)..."
              list="provinces-datalist"
              autoComplete="off"
              value={localPlace}
              onChange={(e) => setLocalPlace(e.target.value)}
              onKeyDown={handlePlaceKeyDown}
            />
            <datalist id="provinces-datalist">
              {TURKISH_PROVINCES.map((prov) => (
                <option key={prov.plate} value={prov.name} label={`${prov.name} (${prov.region})`} />
              ))}
            </datalist>
            {localPlace && (
              <button
                type="button"
                className="btn-clear-input"
                onClick={handleClearPlace}
                title="Aramayı Temizle"
                aria-label="Aramayı Temizle"
              >
                ✕
              </button>
            )}
          </div>
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
        {(filters.minMagnitude !== null || filters.place !== '' || filters.timeRange !== '24h' || (filters.source && filters.source !== 'ALL') || filters.sortBy !== 'occurredAt:desc') && (
          <button type="button" className="btn-reset" onClick={onReset} title="Filtreleri Temizle" aria-label="Filtreleri Temizle">
            ✕ Temizle
          </button>
        )}
      </div>
    </div>
  </div>
);
};
