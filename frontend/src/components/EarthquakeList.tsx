import React, { useState } from 'react';
import type { EarthquakeItem } from '../types/earthquake';
import { parseItemLocation } from '../utils/locationHelper';
import { formatRelativeTime } from '../utils/timeHelper';
import { IconClock, IconSea, IconLake, IconGlobe, IconAlertTriangle, IconSearch, IconInfo } from './Icons';

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

const formatQuality = (quality?: string | null) => {
  if (!quality) return '';
  const q = quality.toUpperCase();
  if (q === 'AUTOMATIC') return 'Otomatik';
  if (q === 'MANUAL' || q === 'MANUAL_REVIEWED') return 'İncelendi';
  if (q === 'PRELIMINARY') return 'Ön Çözüm';
  return quality;
};

interface EarthquakeCardItemProps {
  item: EarthquakeItem;
  isSelected: boolean;
  onSelect: (item: EarthquakeItem) => void;
  onOpenDetail: (item: EarthquakeItem) => void;
}

const EarthquakeCardItem = React.memo<EarthquakeCardItemProps>(({
  item,
  isSelected,
  onSelect,
  onOpenDetail
}) => {
  const magVal = item.magnitude?.value;
  const magType = item.magnitude?.type ?? '';
  const loc = parseItemLocation(item);
  const relTime = formatRelativeTime(item.occurredAt);

  return (
    <article
      className={`earthquake-card ${isSelected ? 'selected' : ''}`}
      tabIndex={0}
      role="button"
      aria-current={isSelected ? 'true' : undefined}
      aria-label={`${loc.displayTitle}, Büyüklük ${magVal !== null && magVal !== undefined ? magVal.toFixed(1) : 'Raporlanmadı'}, Derinlik ${item.depthKm !== null ? `${item.depthKm} km` : 'Raporlanmadı'}`}
      onClick={() => onSelect(item)}
      onKeyDown={(e) => {
        if (e.target !== e.currentTarget) return;
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onSelect(item);
        }
      }}
    >
      <div className="card-top">
        <div className={`mag-badge ${getMagClass(magVal)}`}>
          <span className="mag-value">{magVal !== null && magVal !== undefined ? magVal.toFixed(1) : '-'}</span>
          {magType ? <span className="mag-type">{magType}</span> : null}
        </div>

        <div className="card-title-block">
          <div className="card-title-header">
            <h3 className="card-place-title">
              {loc.displayTitle}
            </h3>
            {relTime && <span className="badge-rel-time">{relTime}</span>}
          </div>
          <div className="card-time">
            <span className="time-with-icon"><IconClock size={12} /> {formatDate(item.occurredAt)}</span>
            {loc.isLake ? (
              <span className="card-env-tag lake"><IconLake size={12} /> Göl İçi</span>
            ) : loc.isOffshore ? (
              <span className="card-env-tag sea"><IconSea size={12} /> Açık Deniz</span>
            ) : loc.isCrossBorder ? (
              <span className="card-env-tag border"><IconGlobe size={12} /> Sınır Ötesi</span>
            ) : null}
          </div>
        </div>
      </div>

      <div className="card-specs card-details">
        <span className={`source-tag ${item.source === 'KOERI' ? 'tag-koeri' : 'tag-afad'}`}>
          {item.source === 'KOERI' ? 'Kandilli' : 'AFAD'}
        </span>
        <span className="detail-dot">•</span>
        <span className="detail-item">
          Derinlik: <strong>{item.depthKm !== null ? `${item.depthKm} km` : 'Raporlanmadı'}</strong>
        </span>
        {item.solutionQuality && (
          <>
            <span className="detail-dot">•</span>
            <span className="detail-quality-badge">{formatQuality(item.solutionQuality)}</span>
          </>
        )}
        {item.matchStatus === 'AUTO_MATCHED' && (
          <>
            <span className="detail-dot">•</span>
            <span className="detail-matched-badge" title="AFAD ve Kandilli bültenleriyle karşılıklı doğrulanmış sismik olay">
              ✓ AFAD & Kandilli Doğrulandı
            </span>
          </>
        )}
        {item.revisionNumber > 1 && (
          <>
            <span className="detail-dot">•</span>
            <span className="detail-rev-badge">Güncellendi (Rev. {item.revisionNumber})</span>
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
          aria-label={`${loc.displayTitle} deprem detaylarını aç, büyüklük ${magVal !== null && magVal !== undefined ? magVal.toFixed(1) : ''} ${magType}`}
        >
          Detayları Gör →
        </button>
      </div>
    </article>
  );
});

EarthquakeCardItem.displayName = 'EarthquakeCardItem';

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
  const [advisoryVisible, setAdvisoryVisible] = useState<boolean>(true);

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
          <span className="empty-icon"><IconAlertTriangle size={36} /></span>
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
          <span className="empty-icon"><IconSearch size={36} /></span>
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

      {advisoryVisible && (
        <div className="list-advisory-banner" role="status">
          <div className="advisory-content">
            <IconInfo size={16} className="advisory-icon" />
            <p className="advisory-text">
              <strong>Sarsıntı mı hissettiniz?</strong> AFAD istasyonlarının ilk otomatik çözümleri genellikle 2–5 dakika içinde sisteme yansır. Sayfa otomatik güncellenmektedir.
            </p>
          </div>
          <button
            type="button"
            className="btn-dismiss-advisory"
            onClick={() => setAdvisoryVisible(false)}
            aria-label="Bilgilendirmeyi Kapat"
          >
            ✕
          </button>
        </div>
      )}

      <div className="earthquake-scroll-area" aria-label="Deprem Listesi">
        {items.map((item) => (
          <EarthquakeCardItem
            key={item.id}
            item={item}
            isSelected={selectedItem?.id === item.id}
            onSelect={onSelect}
            onOpenDetail={onOpenDetail}
          />
        ))}

        {hasMore && (
          <div className="list-load-more-container list-load-more">
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
