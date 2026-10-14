import React from 'react';
import type { Freshness } from '../types/earthquake';
import { IconInfo, IconDownload, IconSeismicLogo } from './Icons';

interface HeaderProps {
  freshness?: Freshness;
  itemsCount?: number;
  totalCount: number;
  loading: boolean;
  onRefresh: () => void;
  onOpenInfo?: () => void;
  isInstallable?: boolean;
  onInstall?: () => void;
  isOnline?: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  freshness,
  itemsCount,
  totalCount,
  loading,
  onRefresh,
  onOpenInfo,
  isInstallable,
  onInstall,
  isOnline = true
}) => {
  const getStatusBadge = () => {
    if (!isOnline) {
      return <span className="badge badge-gray" title="İnternet bağlantısı yok">○ Çevrimdışı</span>;
    }
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
      <div className="header-disclaimer-banner" role="region" aria-label="Resmi Bilgilendirme Notu">
        <span className="disclaimer-icon"><IconInfo size={15} /></span>
        <span>
          <strong>Bilgilendirme Notu:</strong> Bu uygulama bağımsız bir açık veri izleme aracıdır; resmi acil durum hizmeti değildir.
          Veriler{' '}
          <a href="https://deprem.afad.gov.tr" target="_blank" rel="noopener noreferrer" className="external-link">
            AFAD
          </a>{' '}ve{' '}
          <a href="http://www.koeri.boun.edu.tr/scripts/lst0.asp" target="_blank" rel="noopener noreferrer" className="external-link">
            Kandilli Rasathanesi (KOERI-BDTİM)
          </a>{' '}kamuya açık bültenlerinden derlenmektedir.
        </span>
      </div>

      <div className="header-main">
        <div className="header-title-group">
          <div className="header-logo-icon">
            <IconSeismicLogo size={22} />
          </div>
          <div>
            <h1 className="header-title">Türkiye Deprem İzleme Haritası</h1>
            <p className="header-subtitle">AFAD & Kandilli Açık Deprem Kataloğu • {countDisplay} Olay Listelendi</p>
          </div>
        </div>

        <div className="header-actions">
          <div className="status-indicators">
            <span className="source-label">Kaynaklar: <strong>AFAD & Kandilli</strong></span>
            {getStatusBadge()}
            <span className="last-sync-time">
              Son Kontrol: {formatLastFetch(freshness?.lastSuccessfulFetchAt)}
            </span>
          </div>

          {isInstallable && (
            <button
              type="button"
              className="btn-install-app"
              onClick={onInstall}
              title="Deprem Haritasını Cihazınıza Yükleyin (PWA)"
              aria-label="Uygulamayı Yükle"
            >
              <IconDownload size={15} className="install-icon" />
              <span>Uygulamayı Yükle</span>
            </button>
          )}

          {onOpenInfo && (
            <button
              type="button"
              className="btn-info"
              onClick={onOpenInfo}
              title="Deprem Bilgi Rehberi ve SSS"
              aria-label="Deprem Bilgi Rehberi ve Sıkça Sorulan Sorular"
            >
              <IconInfo size={15} />
              <span>Rehber & SSS</span>
            </button>
          )}

          <button
            className={`btn-refresh ${loading ? 'loading' : ''}`}
            onClick={onRefresh}
            disabled={loading}
            aria-label="Verileri Şimdi Yenile"
            title="Verileri Yenile"
          >
            <svg
              width="15"
              height="15"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className={`refresh-icon ${loading ? 'spinning' : ''}`}
              aria-hidden="true"
            >
              <path d="M23 4v6h-6" />
              <path d="M1 20v-6h6" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            <span>{loading ? 'Yenileniyor...' : 'Yenile'}</span>
          </button>
        </div>
      </div>
    </header>
  );
};
