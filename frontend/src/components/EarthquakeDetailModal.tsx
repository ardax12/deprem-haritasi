import React, { useEffect, useState, useRef } from 'react';
import type { EarthquakeItem, EarthquakeDetail } from '../types/earthquake';
import { fetchEarthquakeDetail } from '../services/api';
import { parseItemLocation } from '../utils/locationHelper';
import { formatRelativeTime } from '../utils/timeHelper';
import { IconSea, IconLake, IconGlobe, IconLand, IconShare, IconCheck, IconAlertTriangle } from './Icons';

interface EarthquakeDetailModalProps {
  item: EarthquakeItem | null;
  onClose: () => void;
}

export const EarthquakeDetailModal: React.FC<EarthquakeDetailModalProps> = ({ item, onClose }) => {
  const [detail, setDetail] = useState<EarthquakeDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState<boolean>(false);

  const modalRef = useRef<HTMLDivElement>(null);
  const closeBtnRef = useRef<HTMLButtonElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  const activeItem = detail ? detail.current : item;
  const parsedLoc = activeItem ? parseItemLocation(activeItem) : null;
  const relTime = formatRelativeTime(activeItem?.occurredAt);

  const formatDate = (isoStr?: string | null) => {
    if (!isoStr) return '-';
    try {
      const date = new Date(isoStr);
      return date.toLocaleString('tr-TR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      }) + ' TSİ';
    } catch {
      return isoStr;
    }
  };

  const handleShare = async () => {
    if (!item || !activeItem || !parsedLoc) return;
    const shareUrl = `${window.location.origin}${window.location.pathname}?event=${encodeURIComponent(item.id)}`;
    const magVal = activeItem?.magnitude?.value !== null && activeItem?.magnitude?.value !== undefined ? `M${activeItem.magnitude.value}` : 'Deprem';
    const shareTitle = `${magVal} - ${parsedLoc.displayTitle}`;
    const shareText = `${parsedLoc.displayTitle} bölgesinde ${magVal} büyüklüğünde deprem (${formatDate(activeItem?.occurredAt)}). AFAD verisiyle inceleyin:`;

    if (typeof navigator !== 'undefined' && navigator.share) {
      try {
        await navigator.share({
          title: shareTitle,
          text: shareText,
          url: shareUrl
        });
        return;
      } catch (err: any) {
        if (err.name === 'AbortError') return;
      }
    }

    try {
      await navigator.clipboard.writeText(shareUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch (err) {
      console.warn('Bağlantı panoya kopyalanamadı:', err);
    }
  };

  useEffect(() => {
    if (!item) return;
    setLoading(true);
    setError(null);

    fetchEarthquakeDetail(item.id)
      .then(res => {
        setDetail(res);
        setLoading(false);
      })
      .catch(err => {
        setError(err.message || 'Detaylar yüklenemedi');
        setLoading(false);
      });
  }, [item]);

  useEffect(() => {
    if (!item) return;

    // Capture currently focused element to restore upon close
    previousFocusRef.current = document.activeElement as HTMLElement | null;

    const timer = setTimeout(() => {
      closeBtnRef.current?.focus();
    }, 50);

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
        return;
      }

      if (e.key === 'Tab' && modalRef.current) {
        const focusables = modalRef.current.querySelectorAll<HTMLElement>(
          'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
        );
        if (focusables.length === 0) return;

        const first = focusables[0];
        const last = focusables[focusables.length - 1];

        if (e.shiftKey) {
          if (document.activeElement === first) {
            e.preventDefault();
            last.focus();
          }
        } else {
          if (document.activeElement === last) {
            e.preventDefault();
            first.focus();
          }
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);

    return () => {
      clearTimeout(timer);
      window.removeEventListener('keydown', handleKeyDown);
      if (previousFocusRef.current && typeof previousFocusRef.current.focus === 'function') {
        previousFocusRef.current.focus();
      }
    };
  }, [item, onClose]);

  if (!item || !parsedLoc) return null;

  return (
    <div className="modal-backdrop" onClick={onClose} role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div ref={modalRef} className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <div className="modal-header-top">
              {parsedLoc.isLake ? (
                <span className="environment-badge lake"><IconLake size={14} /> Göl İçi Deprem</span>
              ) : parsedLoc.isOffshore ? (
                <span className="environment-badge offshore"><IconSea size={14} /> Açık Deniz Depremi</span>
              ) : parsedLoc.isCrossBorder ? (
                <span className="environment-badge cross-border"><IconGlobe size={14} /> Sınır Ötesi</span>
              ) : (
                <span className="environment-badge land"><IconLand size={14} /> Kara Depremi</span>
              )}
              {relTime && (
                <span className="badge-rel-time">{relTime}</span>
              )}
            </div>
            <h2 id="modal-title" className="modal-title">
              {parsedLoc.displayTitle}
            </h2>
          </div>
          <div className="modal-header-actions">
            <button
              type="button"
              className={`btn-modal-share ${copied ? 'copied' : ''}`}
              onClick={handleShare}
              title="Deprem bağlantısını kopyala veya paylaş"
              aria-label="Deprem bağlantısını paylaş"
            >
              {copied ? (
                <>
                  <IconCheck size={14} />
                  <span>Kopyalandı</span>
                </>
              ) : (
                <>
                  <IconShare size={14} />
                  <span>Paylaş</span>
                </>
              )}
            </button>
            <button ref={closeBtnRef} className="btn-modal-close" onClick={onClose} aria-label="Pencereyi Kapat">✕</button>
          </div>
        </div>

        <div className="modal-body">
          {loading && (
            <div className="modal-loader">
              <div className="loader-spinner" />
              <p>Olay verileri ve revizyon geçmişi alınıyor...</p>
            </div>
          )}

          {error && (
            <div className="modal-error" role="alert">
              <IconAlertTriangle size={16} />
              <span>{error}</span>
            </div>
          )}

          {!loading && detail && (
            <div className="modal-content-grid">
              <div className="modal-metric-card">
                <span className="metric-label">Büyüklük</span>
                <span className="metric-value-huge">
                  {detail.current.magnitude?.value !== null ? detail.current.magnitude?.value : 'Yok'}
                  <small>{detail.current.magnitude?.type ?? ''}</small>
                </span>
                <span className="metric-hint">
                  Kaynak: <strong>{detail.current.source === 'KOERI' ? 'Kandilli' : 'AFAD'}</strong>
                  {detail.current.solutionQuality ? ` (${detail.current.solutionQuality})` : ''}
                </span>
              </div>

              <div className="modal-metric-card">
                <span className="metric-label">Odak Derinliği</span>
                <span className="metric-value-huge">
                  {detail.current.depthKm !== null ? `${detail.current.depthKm}` : '-'}
                  <small>km</small>
                </span>
                <span className="metric-hint">Yeraltı derinliği</span>
              </div>

              <div className="modal-info-section full-width">
                <h3 className="section-title">Konum ve Koordinat Bilgileri</h3>
                {parsedLoc.isLake ? (
                  <div className="info-grid">
                    <div className="info-item">
                      <span className="info-label">Deprem Ortamı:</span>
                      <span className="info-val highlight-lake"><IconLake size={14} /> Göl İçi Deprem</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Göl / Havza:</span>
                      <span className="info-val">{parsedLoc.region}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">En Yakın Kıyı / Şehir:</span>
                      <span className="info-val">{parsedLoc.nearestSettlement || detail.current.district || '-'}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Kıyıya Mesafe (AFAD):</span>
                      <span className="info-val">
                        {parsedLoc.distanceKm !== null ? `${parsedLoc.distanceKm} km açıkta` : 'Göl İçi'}
                      </span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Enlem (Latitude):</span>
                      <span className="info-val">{detail.current.latitude.toFixed(5)}° N</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Boylam (Longitude):</span>
                      <span className="info-val">{detail.current.longitude.toFixed(5)}° E</span>
                    </div>
                  </div>
                ) : parsedLoc.isOffshore ? (
                  <div className="info-grid">
                    <div className="info-item">
                      <span className="info-label">Deprem Ortamı:</span>
                      <span className="info-val highlight-sea"><IconSea size={14} /> Açık Deniz</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Deniz / Havza:</span>
                      <span className="info-val">{parsedLoc.region}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">En Yakın Türkiye Kıyısı / Şehri:</span>
                      <span className="info-val">{parsedLoc.nearestSettlement || detail.current.district || '-'}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Türkiye Kıyısına Mesafe (AFAD):</span>
                      <span className="info-val">
                        {parsedLoc.distanceKm !== null ? `${parsedLoc.distanceKm} km açıkta` : 'Açık Deniz'}
                      </span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Enlem (Latitude):</span>
                      <span className="info-val">{detail.current.latitude.toFixed(5)}° N</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Boylam (Longitude):</span>
                      <span className="info-val">{detail.current.longitude.toFixed(5)}° E</span>
                    </div>
                  </div>
                ) : parsedLoc.isCrossBorder ? (
                  <div className="info-grid">
                    <div className="info-item">
                      <span className="info-label">Deprem Ortamı:</span>
                      <span className="info-val highlight-border"><IconGlobe size={14} /> Sınır Ötesi Deprem</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Bölge / Ülke:</span>
                      <span className="info-val">{parsedLoc.region}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">En Yakın Türkiye Sınırı:</span>
                      <span className="info-val">{parsedLoc.nearestSettlement || '-'}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Sınıra Mesafe:</span>
                      <span className="info-val">
                        {parsedLoc.distanceKm !== null ? `${parsedLoc.distanceKm} km` : '-'}
                      </span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Enlem (Latitude):</span>
                      <span className="info-val">{detail.current.latitude.toFixed(5)}° N</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Boylam (Longitude):</span>
                      <span className="info-val">{detail.current.longitude.toFixed(5)}° E</span>
                    </div>
                  </div>
                ) : (
                  <div className="info-grid">
                    <div className="info-item">
                      <span className="info-label">Deprem Ortamı:</span>
                      <span className="info-val"><IconLand size={14} /> Kara Depremi</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">İl / Bölge:</span>
                      <span className="info-val">{detail.current.province || '-'}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">İlçe:</span>
                      <span className="info-val">{detail.current.district || '-'}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Mahalle:</span>
                      <span className="info-val">{detail.current.neighborhood || '-'}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Enlem (Latitude):</span>
                      <span className="info-val">{detail.current.latitude.toFixed(5)}° N</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">Boylam (Longitude):</span>
                      <span className="info-val">{detail.current.longitude.toFixed(5)}° E</span>
                    </div>
                  </div>
                )}
              </div>

              <div className="modal-info-section full-width">
                <h3 className="section-title">Zaman Çizelgesi</h3>
                <div className="info-grid">
                  <div className="info-item">
                    <span className="info-label">Deprem Oluş Zamanı (TSİ):</span>
                    <span className="info-val">{formatDate(detail.current.occurredAt)}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Deprem Oluş Zamanı (UTC):</span>
                    <span className="info-val">{detail.current.occurredAt}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Uygulamanın Veriyi Alış Zamanı:</span>
                    <span className="info-val">{formatDate(detail.current.retrievedAt)}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Kaynak Kayıt Kimliği:</span>
                    <span className="info-val">{detail.current.id}</span>
                  </div>
                </div>
              </div>

              {detail.history && detail.history.length > 1 && (
                <div className="modal-info-section full-width">
                  <h3 className="section-title">Revizyon Geçmişi ({detail.history.length} Sürüm)</h3>
                  <p className="revision-hint">
                    AFAD tarafından sonradan yeniden hesaplanan ve güncellenen değerlerin şeffaf karşılaştırması:
                  </p>
                  <table className="revision-table">
                    <thead>
                      <tr>
                        <th>Revizyon</th>
                        <th>Büyüklük</th>
                        <th>Derinlik</th>
                        <th>Yer</th>
                        <th>Gözlem Zamanı</th>
                      </tr>
                    </thead>
                    <tbody>
                      {detail.history.map((rev) => (
                        <tr key={rev.revisionNumber} className={rev.revisionNumber === detail.current.revisionNumber ? 'row-current' : ''}>
                          <td><strong>Rev. {rev.revisionNumber}</strong> {rev.revisionNumber === detail.current.revisionNumber ? '(Aktif)' : ''}</td>
                          <td>{rev.magnitude?.value ?? '-'} {rev.magnitude?.type ?? ''}</td>
                          <td>{rev.depthKm !== null ? `${rev.depthKm} km` : '-'}</td>
                          <td>{rev.place || '-'}</td>
                          <td>{formatDate(rev.retrievedAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              {detail.matchedPartner && (
                <div className="modal-info-section full-width matched-partner-box">
                  <h3 className="section-title">✓ Doğrulanmış Kaynak Eşleşmesi (AFAD & Kandilli)</h3>
                  <p className="revision-hint">
                    Bu fiziksel sarsıntı hem AFAD hem Kandilli Rasathanesi tarafından bağımsız olarak kaydedilmiş ve sistem tarafından eşleştirilmiştir:
                  </p>
                  <table className="revision-table">
                    <thead>
                      <tr>
                        <th>Parametre</th>
                        <th>{detail.current.source === 'KOERI' ? 'Kandilli Raporu' : 'AFAD Raporu'}</th>
                        <th>{detail.matchedPartner.source === 'KOERI' ? 'Kandilli Raporu' : 'AFAD Raporu'}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td><strong>Büyüklük</strong></td>
                        <td>{detail.current.magnitude?.value ?? '-'} {detail.current.magnitude?.type ?? ''}</td>
                        <td>{detail.matchedPartner.magnitude?.value ?? '-'} {detail.matchedPartner.magnitude?.type ?? ''}</td>
                      </tr>
                      <tr>
                        <td><strong>Derinlik</strong></td>
                        <td>{detail.current.depthKm !== null ? `${detail.current.depthKm} km` : '-'}</td>
                        <td>{detail.matchedPartner.depthKm !== null ? `${detail.matchedPartner.depthKm} km` : '-'}</td>
                      </tr>
                      <tr>
                        <td><strong>Konum</strong></td>
                        <td>{detail.current.place || '-'}</td>
                        <td>{detail.matchedPartner.place || '-'}</td>
                      </tr>
                      <tr>
                        <td><strong>Koordinatlar</strong></td>
                        <td>{detail.current.latitude.toFixed(4)}°N, {detail.current.longitude.toFixed(4)}°E</td>
                        <td>{detail.matchedPartner.latitude.toFixed(4)}°N, {detail.matchedPartner.longitude.toFixed(4)}°E</td>
                      </tr>
                      <tr>
                        <td><strong>Oluş Zamanı</strong></td>
                        <td>{formatDate(detail.current.occurredAt)}</td>
                        <td>{formatDate(detail.matchedPartner.occurredAt)}</td>
                      </tr>
                      {detail.matchReason && (
                        <tr>
                          <td><strong>Eşleşme Gerekçesi</strong></td>
                          <td colSpan={2}><em>{detail.matchReason}</em></td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              )}

              {(detail.current.source === 'KOERI' || detail.matchedPartner?.source === 'KOERI') && (
                <div className="modal-info-section full-width koeri-attribution-box">
                  <h4 className="section-title" style={{ fontSize: '0.875rem', marginBottom: '6px' }}>Resmi Kaynak Atfı (KOERI)</h4>
                  <p className="info-text-sm" style={{ margin: 0, color: 'var(--text-secondary, #94a3b8)', fontSize: '0.8125rem' }}>
                    Kaynak: Boğaziçi Üniversitesi Kandilli Rasathanesi ve Deprem Araştırma Enstitüsü, Bölgesel Deprem-Tsunami İzleme ve Değerlendirme Merkezi (KOERI-BDTİM).
                    Veriler kamuya açık bültenlerden derlenmiştir.{' '}
                    <a href="http://www.koeri.boun.edu.tr/scripts/lst0.asp" target="_blank" rel="noopener noreferrer" className="external-link" style={{ display: 'inline-block', marginTop: '4px' }}>
                      Kandilli Orijinal Son Depremler Listesi ↗
                    </a>
                  </p>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button
            type="button"
            className="btn-modal-share-secondary"
            onClick={handleShare}
            aria-label="Deprem bağlantısını kopyala veya paylaş"
          >
            {copied ? (
              <>
                <IconCheck size={16} />
                <span>Bağlantı Kopyalandı</span>
              </>
            ) : (
              <>
                <IconShare size={16} />
                <span>Bağlantıyı Paylaş</span>
              </>
            )}
          </button>
          <button type="button" className="btn-modal-primary" onClick={onClose}>Tamam</button>
        </div>
      </div>
    </div>
  );
};
