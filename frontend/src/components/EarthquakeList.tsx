import React from 'react';
import type { EarthquakeItem } from '../types/earthquake';
import { parseItemLocation } from '../utils/locationHelper';

interface EarthquakeListProps {
  items: EarthquakeItem[];
  selectedItem: EarthquakeItem | null;
  onSelect: (item: EarthquakeItem) => void;
  onOpenDetail: (item: EarthquakeItem) => void;
  loading: boolean;
  totalCount?: number;
  hasMore?: boolean;
  loadingMore?: boolean;
  onLoadMore?: () => void;
  error?: string | null;
  onRetry?: () => void;
}

export const EarthquakeList: React.FC<EarthquakeListProps> = ({
  items,
  selectedItem,
  onSelect,
  onOpenDetail,
  loading,
  totalCount,
  hasMore,
  loadingMore,
  onLoadMore,
  error,
  onRetry
}) => {
  const getMagClass = (val?: number | null) => {
    if (val === null || val === undefined) return 'mag-gray';
    if (val < 3.0) return 'mag-green';
    if (val < 4.5) return 'mag-yellow';
    if (val < 6.0) return 'mag-orange';
    return 'mag-red';
  };

  const formatDate = (isoStr: string) => {
    try {
      const date = new Date(isoStr);
      const tsi = date.toLocaleString('tr-TR', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }) + ' TSİ';

      const utcTime = date.toISOString().substring(11, 16) + ' UTC';
      return `${tsi} (${utcTime})`;
    } catch {
      return isoStr;
    }
  };

  if (loading && items.length === 0) {
    return (
      <div className="list-wrapper">
        <div className="list-placeholder" aria-live="polite">
          <div className="loader-spinner" />
          <p>Son depremler yükleniyor...</p>
        </div>
      </div>
    );
  }

  if (error && items.length === 0) {
    return (
      <div className="list-wrapper">
        <div className="list-empty list-error-state" aria-live="assertive" role="alert">
          <span className="empty-icon">⚠️</span>
          <h3>Bağlantı / Servis Hatası</h3>
          <p>{error}</p>
          {onRetry && (
            <button
              type="button"
              className="btn-error-retry"
              onClick={onRetry}
              style={{ marginTop: '12px', padding: '8px 18px', borderRadius: '6px', background: '#2563eb', color: '#fff', border: 'none', cursor: 'pointer', fontWeight: 600 }}
            >
              Tekrar Dene
            </button>
          )}
        </div>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="list-wrapper">
        <div className="list-empty" aria-live="polite">
          <span className="empty-icon">🔍</span>
          <h3>Kayıt Bulunamadı</h3>
          <p>Belirttiğiniz filtre kriterlerine uygun bir deprem kaydı bulunamadı. Filtreleri genişleterek tekrar deneyin.</p>
        </div>
      </div>
    );
  }

  const countDisplay = totalCount !== undefined && totalCount > items.length
    ? `${items.length} / ${totalCount}`
    : `${items.length}`;

  return (
    <div className="list-wrapper">
      <div className="list-header">
        <h2 className="list-title">Deprem Olayları ({countDisplay})</h2>
        <span className="list-hint">Haritada görmek için tıklayın</span>
      </div>

      <div className="earthquake-scroll-area" aria-label="Deprem Listesi">
        {items.map((item) => {
          const isSelected = selectedItem?.id === item.id;
          const magVal = item.magnitude?.value;
          const magType = item.magnitude?.type ?? '';
          const loc = parseItemLocation(item);

          return (
            <article
              key={item.id}
              tabIndex={0}
              className={`earthquake-card ${isSelected ? 'selected' : ''}`}
              onClick={() => onSelect(item)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  onSelect(item);
                }
              }}
              aria-label={`${loc.displayTitle} - Büyüklük: ${magVal !== null && magVal !== undefined ? magVal.toFixed(1) : '-'} ${magType}`}
              aria-current={isSelected ? 'true' : undefined}
            >
              <div className="card-top">
                <div className={`mag-badge ${getMagClass(magVal)}`}>
                  <span className="mag-value">{magVal !== null && magVal !== undefined ? magVal.toFixed(1) : '-'}</span>
                  {magType ? <span className="mag-type">{magType}</span> : null}
                </div>

                <div className="card-title-block">
                  <h3 className="card-place-title">
                    {loc.displayTitle}
                  </h3>
                  <div className="card-time">
                    <span>🕒 {formatDate(item.occurredAt)}</span>
                    {loc.isOffshore && <span className="card-env-tag sea">🌊 Açık Deniz</span>}
                    {loc.isCrossBorder && <span className="card-env-tag border">🌐 Sınır Ötesi</span>}
                  </div>
                </div>
              </div>

              <div className="card-details">
                <span className="detail-item">
                  Derinlik: <strong>{item.depthKm !== null ? `${item.depthKm} km` : 'Bilinmiyor'}</strong>
                </span>
                <span className="detail-dot">•</span>
                <span className="detail-item">
                  Kaynak: <strong>{item.source}</strong>
                </span>
                {item.revisionNumber > 1 && (
                  <>
                    <span className="detail-dot">•</span>
                    <span className="detail-rev-badge">Revizyon #{item.revisionNumber}</span>
                  </>
                )}
              </div>

              <div className="card-bottom">
                <button
                  type="button"
                  className="btn-card-detail"
                  onClick={(e) => {
                    e.stopPropagation();
                    onOpenDetail(item);
                  }}
                >
                  Detayları Gör →
                </button>
              </div>
            </article>
          );
        })}

        {hasMore && (
          <div className="list-load-more">
            <button
              type="button"
              className="btn-load-more"
              onClick={onLoadMore}
              disabled={loadingMore}
            >
              {loadingMore ? 'Daha fazla yükleniyor...' : 'Daha Fazla Olay Yükle'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
