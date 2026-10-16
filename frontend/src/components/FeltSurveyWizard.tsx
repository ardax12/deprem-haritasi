import React, { useState, useEffect, useRef } from 'react';
import type { EarthquakeItem, FeltReportRequest, FeltReportSummary } from '../types/earthquake';
import { submitFeltReport } from '../services/api';
import { TURKISH_PROVINCES, type ProvinceData } from '../data/provinces';

interface FeltSurveyWizardProps {
  item: EarthquakeItem;
  onClose: () => void;
  onSuccess: (summary: FeltReportSummary) => void;
}

export const FeltSurveyWizard: React.FC<FeltSurveyWizardProps> = ({ item, onClose, onSuccess }) => {
  const [step, setStep] = useState<number>(1);
  const [feltLevel, setFeltLevel] = useState<'NOT_FELT' | 'WEAK' | 'MODERATE' | 'STRONG' | 'VIOLENT'>('MODERATE');
  const [province, setProvince] = useState<string>(item.province || '');
  const [district, setDistrict] = useState<string>(item.district || '');
  const [floor, setFloor] = useState<number>(1);
  const [motionType, setMotionType] = useState<string>('RAPID_SHAKING');
  const [reaction, setReaction] = useState<string>('SURPRISED');
  const [soundHeard, setSoundHeard] = useState<boolean>(false);
  const [damageObserved, setDamageObserved] = useState<string>('NONE');

  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const modalRef = useRef<HTMLDivElement>(null);
  const closeBtnRef = useRef<HTMLButtonElement>(null);

  // Focus trap & Escape key handler
  useEffect(() => {
    closeBtnRef.current?.focus();

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
        return;
      }
      if (e.key === 'Tab' && modalRef.current) {
        const focusables = modalRef.current.querySelectorAll<HTMLElement>(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        if (focusables.length === 0) return;
        const first = focusables[0];
        const last = focusables[focusables.length - 1];

        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const payload: FeltReportRequest = {
      feltLevel,
      motionType,
      reaction,
      soundHeard,
      damageObserved,
      buildingFloor: floor,
      province: province.trim() || undefined,
      district: district.trim() || undefined
    };

    try {
      const summary = await submitFeltReport(item.id, payload);
      onSuccess(summary);
    } catch (err: any) {
      setError(err.message || 'Bildirim kaydedilemedi.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose} role="presentation">
      <div
        className="modal-container felt-wizard-modal"
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="felt-wizard-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div className="felt-wizard-header-info">
            <span className="felt-step-badge">Adım {step} / 3</span>
            <h2 id="felt-wizard-title" className="modal-title">
              Bu Depremi Hissettiniz mi?
            </h2>
          </div>
          <button
            ref={closeBtnRef}
            className="btn-modal-close"
            type="button"
            onClick={onClose}
            aria-label="Kapat"
          >
            ✕
          </button>
        </div>

        {error && (
          <div className="felt-error-alert" role="alert">
            <span>⚠</span> {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="felt-wizard-form">
          {step === 1 && (
            <div className="felt-step-panel">
              <p className="felt-step-desc">
                Sarsıntıyı nasıl hissettiğinizi seçin. Verileriniz bilimsel EMS-98 şiddet indeksine dönüştürülür.
              </p>
              <div className="felt-options-grid" role="radiogroup" aria-label="Sarsıntı Şiddeti">
                {[
                  { id: 'NOT_FELT', title: 'Hissetmedim', desc: 'Sarsıntıyı hiç fark etmedim.' },
                  { id: 'WEAK', title: 'Çok Hafif', desc: 'Dinlenme halindeyken veya üst katlarda hafifçe fark ettim.' },
                  { id: 'MODERATE', title: 'Belirgin', desc: 'Avizeler/nesneler sallandı, motorlu araç sallantısı gibiydi.' },
                  { id: 'STRONG', title: 'Güçlü', desc: 'Bina belirgin sallandı, korktum ya da uykudan uyandım.' },
                  { id: 'VIOLENT', title: 'Çok Şiddetli', desc: 'Ayakta durmak zordu, eşyalar düştü, panik yaşandı.' }
                ].map((opt) => (
                  <button
                    key={opt.id}
                    type="button"
                    className={`felt-option-card ${feltLevel === opt.id ? 'active' : ''}`}
                    onClick={() => setFeltLevel(opt.id as any)}
                  >
                    <div className="felt-option-radio">
                      <span className={`felt-dot ${feltLevel === opt.id ? 'selected' : ''}`} />
                    </div>
                    <div className="felt-option-text">
                      <strong className="felt-option-title">{opt.title}</strong>
                      <span className="felt-option-desc">{opt.desc}</span>
                    </div>
                  </button>
                ))}
              </div>

              <div className="felt-step-actions">
                <button
                  type="button"
                  className="btn-primary felt-btn-next"
                  onClick={() => setStep(2)}
                >
                  Devam Et →
                </button>
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="felt-step-panel">
              <p className="felt-step-desc">
                Sarsıntı anında nerede bulunduğunuzu belirtin. Bu bilgi sarsıntının mesafeye göre sönümlenmesini hesaplar.
              </p>

              <div className="felt-field-group">
                <label htmlFor="felt-province-select" className="felt-label">Bulunduğunuz İl:</label>
                <select
                  id="felt-province-select"
                  className="felt-select"
                  value={province}
                  onChange={(e) => setProvince(e.target.value)}
                >
                  <option value="">Seçiniz</option>
                  {TURKISH_PROVINCES.map((p: ProvinceData) => (
                    <option key={p.slug} value={p.name}>{p.name}</option>
                  ))}
                </select>
              </div>

              <div className="felt-field-group">
                <label htmlFor="felt-district-input" className="felt-label">İlçe / Semt (İsteğe bağlı):</label>
                <input
                  id="felt-district-input"
                  type="text"
                  className="felt-input"
                  placeholder="Örn: Bornova, Kadıköy, Çankaya"
                  value={district}
                  onChange={(e) => setDistrict(e.target.value)}
                  maxLength={50}
                />
              </div>

              <div className="felt-field-group">
                <label htmlFor="felt-floor-select" className="felt-label">Bulunduğunuz Kat:</label>
                <select
                  id="felt-floor-select"
                  className="felt-select"
                  value={floor}
                  onChange={(e) => setFloor(Number(e.target.value))}
                >
                  <option value={0}>Zemin / Giriş Kat</option>
                  <option value={1}>1. Kat</option>
                  <option value={2}>2. Kat</option>
                  <option value={3}>3. Kat</option>
                  <option value={4}>4. Kat</option>
                  <option value={5}>5. Kat</option>
                  <option value={7}>6 - 10. Katlar</option>
                  <option value={12}>11. Kat veya Üstü</option>
                </select>
                <span className="felt-hint">Yüksek katlarda sarsıntı bina salınımı nedeniyle daha güçlü algılanır.</span>
              </div>

              <div className="felt-step-actions-between">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setStep(1)}
                >
                  ← Geri
                </button>
                <button
                  type="button"
                  className="btn-primary"
                  onClick={() => setStep(3)}
                >
                  Devam Et →
                </button>
              </div>
            </div>
          )}

          {step === 3 && (
            <div className="felt-step-panel">
              <p className="felt-step-desc">
                Sarsıntı sırasındaki detaylı gözlemlerinizi tamamlayın.
              </p>

              <div className="felt-field-group">
                <label className="felt-label">Sarsıntı Hareketi:</label>
                <div className="felt-chip-group">
                  {[
                    { id: 'RAPID_SHAKING', label: 'Hızlı Titreşim' },
                    { id: 'SLOW_SWAYING', label: 'Yavaş Salınım / Sallantı' },
                    { id: 'JOLTING', label: 'Ani Vuruş / Sarsıntı' }
                  ].map((m) => (
                    <button
                      key={m.id}
                      type="button"
                      className={`felt-chip ${motionType === m.id ? 'active' : ''}`}
                      onClick={() => setMotionType(m.id)}
                    >
                      {m.label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="felt-field-group">
                <label className="felt-label">Tepkiniz:</label>
                <div className="felt-chip-group">
                  {[
                    { id: 'SURPRISED', label: 'Şaşırdım' },
                    { id: 'FRIGHTENED', label: 'Korktum / Tedirgin oldum' },
                    { id: 'RAN_OUTSIDE', label: 'Dışarı koştum' },
                    { id: 'NO_REACTION', label: 'Sakin kaldım' }
                  ].map((r) => (
                    <button
                      key={r.id}
                      type="button"
                      className={`felt-chip ${reaction === r.id ? 'active' : ''}`}
                      onClick={() => setReaction(r.id)}
                    >
                      {r.label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="felt-field-group">
                <label className="felt-checkbox-label">
                  <input
                    type="checkbox"
                    className="felt-checkbox"
                    checked={soundHeard}
                    onChange={(e) => setSoundHeard(e.target.checked)}
                  />
                  <span>Sarsıntı anında yer altından uğultu / ses duydum</span>
                </label>
              </div>

              <div className="felt-field-group">
                <label className="felt-label">Hasar / Eşya Durumu:</label>
                <select
                  className="felt-select"
                  value={damageObserved}
                  onChange={(e) => setDamageObserved(e.target.value)}
                >
                  <option value="NONE">Herhangi bir hasar veya eşya düşmesi olmadı</option>
                  <option value="OBJECTS_TOPPLED">Küçük eşyalar devrildi / raflardan düştü</option>
                  <option value="HAIRLINE_CRACKS">Duvar sıvasında ince kılcal çatlaklar oluştu</option>
                  <option value="STRUCTURAL">Belirgin yapısal hasar oluştu</option>
                </select>
              </div>

              <div className="felt-step-actions-between">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setStep(2)}
                  disabled={loading}
                >
                  ← Geri
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={loading}
                >
                  {loading ? 'Kaydediliyor...' : '✓ Bildirimi Tamamla'}
                </button>
              </div>
            </div>
          )}
        </form>
      </div>
    </div>
  );
};
