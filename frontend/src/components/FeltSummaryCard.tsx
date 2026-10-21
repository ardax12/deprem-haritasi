import React, { useState, useEffect, lazy, Suspense } from 'react';
import type { EarthquakeItem, FeltReportSummary } from '../types/earthquake';
import { fetchFeltSummary } from '../services/api';

const FeltSurveyWizard = lazy(() =>
  import('./FeltSurveyWizard').then((m) => ({ default: m.FeltSurveyWizard }))
);

interface FeltSummaryCardProps {
  item: EarthquakeItem;
}

export const FeltSummaryCard: React.FC<FeltSummaryCardProps> = ({ item }) => {
  const [summary, setSummary] = useState<FeltReportSummary | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [wizardOpen, setWizardOpen] = useState<boolean>(false);
  const [showProvinces, setShowProvinces] = useState<boolean>(false);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    fetchFeltSummary(item.id, controller.signal)
      .then((data) => {
        if (active) setSummary(data);
      })
      .catch((err) => {
        if (err.name !== 'AbortError') {
          console.warn('Hissedilme özeti alınamadı:', err);
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [item.id]);

  const handleWizardSuccess = (newSummary: FeltReportSummary) => {
    setSummary(newSummary);
    setWizardOpen(false);
  };

  const getIntensityBadgeClass = (intensity: number) => {
    if (intensity < 2.5) return 'felt-badge-low';
    if (intensity < 4.5) return 'felt-badge-moderate';
    if (intensity < 6.0) return 'felt-badge-strong';
    return 'felt-badge-violent';
  };

  return (
    <div className="felt-summary-card">
      <div className="felt-card-header">
        <div className="felt-card-title-group">
          <svg className="felt-card-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
          </svg>
          <span className="felt-card-title">Topluluk Hissedilme Düzeyi (EMS-98)</span>
        </div>

        {summary?.alreadyReportedByClient ? (
          <span className="felt-reported-badge">✓ Bildiriminiz Alındı</span>
        ) : (
          <button
            type="button"
            className="felt-btn-report-cta"
            onClick={() => setWizardOpen(true)}
          >
            <svg className="btn-cta-svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
            </svg>
            <span>Hissettiniz mi?</span>
          </button>
        )}
      </div>

      {loading ? (
        <div className="felt-loading">Hissedilme verileri yükleniyor...</div>
      ) : summary && summary.totalReports > 0 ? (
        <div className="felt-stats-body">
          <div className="felt-metric-row">
            <div className={`felt-intensity-badge ${getIntensityBadgeClass(summary.averageIntensity)}`}>
              {summary.averageIntensity.toFixed(1)}
            </div>
            <div className="felt-metric-info">
              <strong className="felt-category-text">{summary.intensityCategory}</strong>
              <span className="felt-sub-text">
                Toplam <strong>{summary.totalReports}</strong> vatandaş bildirimi · <em>{summary.dominantReaction}</em>
              </span>
            </div>
          </div>

          {summary.reportsByProvince && summary.reportsByProvince.length > 0 && (
            <div className="felt-provinces-section">
              <button
                type="button"
                className="felt-btn-toggle-provinces"
                onClick={() => setShowProvinces(!showProvinces)}
              >
                {showProvinces ? '▲ İllere göre dağılımı gizle' : `▼ İllere göre dağılımı gör (${summary.reportsByProvince.length} il)`}
              </button>

              {showProvinces && (
                <div className="felt-province-grid">
                  {summary.reportsByProvince.map((p) => (
                    <div key={p.province} className="felt-province-item">
                      <span className="felt-prov-name">{p.province}</span>
                      <span className="felt-prov-count">{p.count} bildirim</span>
                      <span className="felt-prov-score">Ort. {p.avgIntensity}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      ) : (
        <div className="felt-empty-state">
          <p className="felt-empty-desc">
            Bu deprem için henüz vatandaş bildirimi girilmedi. Sarsıntıyı hissettiyseniz gözlemlerinizi paylaşın.
          </p>
          {!summary?.alreadyReportedByClient && (
            <button
              type="button"
              className="btn-primary felt-btn-submit-first"
              onClick={() => setWizardOpen(true)}
            >
              İlk Bildirimi Siz Yapın →
            </button>
          )}
        </div>
      )}

      {wizardOpen && (
        <Suspense fallback={null}>
          <FeltSurveyWizard
            item={item}
            onClose={() => setWizardOpen(false)}
            onSuccess={handleWizardSuccess}
          />
        </Suspense>
      )}
    </div>
  );
};
