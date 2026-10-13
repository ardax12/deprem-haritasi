import React from 'react';
import type { Freshness } from '../types/earthquake';

interface HeaderProps {
  freshness?: Freshness;
  itemsCount?: number;
  totalCount: number;
  loading: boolean;
  onRefresh: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  freshness,
  itemsCount,
  totalCount,
  loading,
  onRefresh
}) => {
  const getStatusBadge = () => {
    if (!freshness) return <span className="badge badge-gray">Bağlanıyor...</span>;
    if (freshness.status === 'current') {
      return <span className="badge badge-green" title="Kaynak güncel (< 20 dk)">● Canlı / Güncel</span>;
    } else if (freshness.status === 'delayed') {
      return <span className="badge badge-yellow" title="Veri alımı 20 dakikadan uzun süredir gecikmeli">▲ Gecikmeli Veri</span>;
    } else {
      return <span className="badge badge-red" title="Veri alımı 60 dakikadan eski">✕ Eski Veri (&gt; 60 dk)</span>;
    }
  };

  const formatLastFetch = (isoStr?: string | null) => {
    if (!isoStr) return 'Bilinmiyor';
    try {
      const date = new Date(isoStr);
      return date.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) + ' TSİ';
    } catch {
      return isoStr;
    }
  };

  const countDisplay = itemsCount !== undefined && totalCount > itemsCount
    ? `${itemsCount} / ${totalCount}`
    : `${totalCount}`;

  return (
    <header className="app-header">
      <div className="header-disclaimer-banner" role="alert">
        <span className="disclaimer-icon">ℹ</span>
        <span>
          <strong>Resmi Bilgilendirme Notu:</strong> Bu uygulama kamuya açık bir izleme aracıdır; resmi erken uyarı veya acil durum hizmeti değildir.
          Veriler kaynaklardan gecikmeli veya revizyonlu iletilebilir. Resmi ve anlık bilgi için lütfen{' '}
          <a href="https://deprem.afad.gov.tr" target="_blank" rel="noopener noreferrer" className="external-link">
            AFAD Resmi Portalı
          </a>'nı takip edin.
        </span>
      </div>

      <div className="header-main">
        <div className="header-title-group">
          <div className="header-logo-icon">🌐</div>
          <div>
            <h1 className="header-title">Türkiye Deprem İzleme Haritası</h1>
            <p className="header-subtitle">Güvenilir AFAD Kaynaklı Açık Deprem Kataloğu • {countDisplay} Olay Listelendi</p>
          </div>
        </div>

        <div className="header-actions">
          <div className="status-indicators">
            <span className="source-label">Kaynak: <strong>AFAD</strong></span>
            {getStatusBadge()}
            <span className="last-sync-time">
              Son Kontrol: {formatLastFetch(freshness?.lastSuccessfulFetchAt)}
            </span>
          </div>

          <button
            className={`btn-refresh ${loading ? 'loading' : ''}`}
            onClick={onRefresh}
            disabled={loading}
            aria-label="Verileri Şimdi Yenile"
            title="Verileri Yenile"
          >
            <span className={`refresh-icon ${loading ? 'spinning' : ''}`} aria-hidden="true">↺</span>
            <span>{loading ? 'Yenileniyor...' : 'Yenile'}</span>
          </button>
        </div>
      </div>
    </header>
  );
};
