import React, { useEffect, useRef, useState } from 'react';

interface InfoModalProps {
  isOpen: boolean;
  onClose: () => void;
}

type TabKey = 'source' | 'magnitudes' | 'faq' | 'safety';

export const InfoModal: React.FC<InfoModalProps> = ({ isOpen, onClose }) => {
  const [activeTab, setActiveTab] = useState<TabKey>('source');
  const modalRef = useRef<HTMLDivElement>(null);
  const closeBtnRef = useRef<HTMLButtonElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!isOpen) return;

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
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      className="modal-backdrop"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="info-modal-title"
    >
      <div
        className="modal-card info-modal-card"
        ref={modalRef}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <h2 id="info-modal-title" className="modal-title">
              Türkiye Deprem İzleme Rehberi & SSS
            </h2>
          </div>
          <button
            ref={closeBtnRef}
            type="button"
            className="btn-close-modal"
            onClick={onClose}
            aria-label="Kapat"
            title="Kapat"
          >
            ✕
          </button>
        </div>

        {/* Tab navigation */}
        <div className="info-tab-nav" role="tablist" aria-label="Rehber Sekmeleri">
          <button
            role="tab"
            id="tab-source"
            aria-selected={activeTab === 'source'}
            aria-controls="panel-source"
            className={`info-tab-btn ${activeTab === 'source' ? 'active' : ''}`}
            onClick={() => setActiveTab('source')}
          >
            Veri & Sistem
          </button>
          <button
            role="tab"
            id="tab-magnitudes"
            aria-selected={activeTab === 'magnitudes'}
            aria-controls="panel-magnitudes"
            className={`info-tab-btn ${activeTab === 'magnitudes' ? 'active' : ''}`}
            onClick={() => setActiveTab('magnitudes')}
          >
            Büyüklük Ölçekleri
          </button>
          <button
            role="tab"
            id="tab-faq"
            aria-selected={activeTab === 'faq'}
            aria-controls="panel-faq"
            className={`info-tab-btn ${activeTab === 'faq' ? 'active' : ''}`}
            onClick={() => setActiveTab('faq')}
          >
            Sıkça Sorulanlar (SSS)
          </button>
          <button
            role="tab"
            id="tab-safety"
            aria-selected={activeTab === 'safety'}
            aria-controls="panel-safety"
            className={`info-tab-btn ${activeTab === 'safety' ? 'active' : ''}`}
            onClick={() => setActiveTab('safety')}
          >
            Güvenlik Kuralları
          </button>
        </div>

        <div className="modal-body info-modal-body">
          {activeTab === 'source' && (
            <div id="panel-source" role="tabpanel" aria-labelledby="tab-source" className="info-tab-panel">
              <div className="info-section">
                <h3 className="info-section-title">Çoklu Kaynak Açık Veri Entegrasyonu (AFAD & Kandilli)</h3>
                <p className="info-text">
                  Uygulamamızdaki sismik veriler, Türkiye'nin iki yetkili sismoloji merkezinin kamuya açık bültenlerinden otomatik olarak derlenmektedir:
                </p>
                <ul className="info-text" style={{ paddingLeft: '20px', marginTop: '6px' }}>
                  <li><strong>AFAD:</strong> T.C. İçişleri Bakanlığı Afet ve Acil Durum Yönetimi Başkanlığı Deprem Dairesi API servisi.</li>
                  <li><strong>Kandilli (KOERI-BDTİM):</strong> Boğaziçi Üniversitesi Kandilli Rasathanesi ve Deprem Araştırma Enstitüsü Bölgesel Deprem-Tsunami İzleme ve Değerlendirme Merkezi hızlı çözümler bülteni.</li>
                </ul>
                <p className="info-text">
                  Sunucumuz her iki kaynağı periyodik olarak sorgular; gelen yeni veya revize edilmiş depremleri veri tabanına işler. Farklı kaynakların aynı fiziksel sarsıntıyı bildirdiği durumlar gelişmiş jeodezik mesafe ve zaman korelasyonu algoritmasıyla eşleştirilerek kullanıcılara şeffafça sunulur.
                </p>
              </div>

              <div className="info-highlight-box">
                <h4 className="info-highlight-title">Kandilli Resmi Kaynak Atfı</h4>
                <p className="info-text-sm">
                  Kaynak: Boğaziçi Üniversitesi Kandilli Rasathanesi ve Deprem Araştırma Enstitüsü, Bölgesel Deprem-Tsunami İzleme ve Değerlendirme Merkezi (KOERI-BDTİM).
                  Veriler bilimsel ve kamu bilgilendirme amaçlı açık verilerden aktarılmaktadır.
                </p>
              </div>

              <div className="info-highlight-box" style={{ marginTop: '12px' }}>
                <h4 className="info-highlight-title">Bağımsız İzleme Güvencesi</h4>
                <p className="info-text-sm">
                  Bu uygulama resmi bir kamu kurumu uygulaması değildir; AFAD ve Kandilli açık verilerini toplumun hızlı, sade ve anlaşılır şekilde takip edebilmesi için geliştirilmiş bağımsız bir sismik izleme ve arşiv aracıdır.
                </p>
              </div>

              <div className="info-section">
                <h3 className="info-section-title">Çevrimdışı ve PWA Özelliği</h3>
                <p className="info-text">
                  Uygulama Progressive Web App (PWA) mimarisiyle çalışır. Cihazınıza yükleyebilir ve acil durumlarda internet bağlantınız kopsa dahi cihazınıza önbelleğe alınmış son depremleri inceleyebilirsiniz.
                </p>
              </div>
            </div>
          )}

          {activeTab === 'magnitudes' && (
            <div id="panel-magnitudes" role="tabpanel" aria-labelledby="tab-magnitudes" className="info-tab-panel">
              <div className="info-section">
                <h3 className="info-section-title">Deprem Büyüklük Türleri Neden Farklıdır?</h3>
                <p className="info-text">
                  Sismik bültenlerde deprem büyüklüklerinin yanında <em>Mw, ML, Md</em> gibi harfler görürsünüz. Bunlar sarsıntının hangi yöntemle hesaplandığını gösterir:
                </p>
              </div>

              <div className="info-mag-grid">
                <div className="info-mag-card">
                  <div className="info-mag-header">
                    <span className="info-mag-code">Mw</span>
                    <span className="info-mag-name">Moment Büyüklüğü</span>
                  </div>
                  <p className="info-mag-desc">
                    Fay yüzeyinin kırılma alanını ve açığa çıkan toplam fiziksel enerjiyi ölçer. Orta ve büyük ölçekli depremlerde (≥ 4.0) bilimsel olarak en güvenilir ve standart kabul edilen büyüklüktür.
                  </p>
                </div>

                <div className="info-mag-card">
                  <div className="info-mag-header">
                    <span className="info-mag-code">ML</span>
                    <span className="info-mag-name">Yerel Büyüklük (Richter)</span>
                  </div>
                  <p className="info-mag-desc">
                    Sismograf iğnesinin maksimum sapma genliğine dayanır. Küçük ve orta büyüklükteki depremlerde ilk dakikalarda hızlıca hesaplanır; ancak 6.5 ve üzerindeki büyük sarsıntılarda doygunluğa ulaşır.
                  </p>
                </div>

                <div className="info-mag-card">
                  <div className="info-mag-header">
                    <span className="info-mag-code">Md</span>
                    <span className="info-mag-name">Süreye Bağlı Büyüklük</span>
                  </div>
                  <p className="info-mag-desc">
                    Sarsıntının sismometrede oluşturduğu titreşim sinyalinin toplam süresine göre belirlenir. Genellikle küçük ölçekli (M &lt; 4.0) depremlerde tercih edilir.
                  </p>
                </div>
              </div>

              <div className="info-section" style={{ marginTop: '16px' }}>
                <h3 className="info-section-title">Derinlik Dereceleri</h3>
                <p className="info-text">
                  <strong>Sığ Odaklı (&lt; 60 km):</strong> Yeryüzüne yakın oldukları için enerjilerini doğrudan yüzeye iletir ve daha kuvvetli hissedilirler. Türkiye'deki depremlerin büyük çoğunluğu sığ odaklıdır.
                </p>
              </div>
            </div>
          )}

          {activeTab === 'faq' && (
            <div id="panel-faq" role="tabpanel" aria-labelledby="tab-faq" className="info-tab-panel">
              <div className="info-faq-item">
                <h4 className="info-faq-question">1. AFAD ile Kandilli verileri neden farklı olabilir?</h4>
                <p className="info-faq-answer">
                  İki kurum Türkiye genelinde farklı sismoloji istasyon ağlarına ve farklı hız modellerine sahiptir. Depremin ilk anlarında yazılımlar otomatik çözüm üretir; daha sonra sismologlar istasyon kayıtlarını inceleyip revize ettikçe veriler birbirine yaklaşır.
                </p>
              </div>

              <div className="info-faq-item">
                <h4 className="info-faq-question">2. Haritadaki renkler neyi temsil ediyor?</h4>
                <p className="info-faq-answer">
                  Renkler büyüklük şiddetini gösterir: <strong>M &lt; 3.0</strong> hafif (yeşil/mavi halkalar), <strong>M 3.0 - 4.9</strong> orta şiddetli hissedilebilir (sarı/turuncu halkalar), <strong>M ≥ 5.0</strong> kuvvetli sarsıntılar (kırmızı ve genişleyen dalgalar).
                </p>
              </div>

              <div className="info-faq-item">
                <h4 className="info-faq-question">3. Belirli bir depremi başkasıyla nasıl paylaşabilirim?</h4>
                <p className="info-faq-answer">
                  Olay listesinden veya haritadan bir depreme tıkladığınızda açılan detay penceresindeki <strong>"Paylaş"</strong> butonuna basabilirsiniz. Bu buton özel bağlantıyı panoya kopyalar veya mobil cihazınızın paylaşım menüsünü açar.
                </p>
              </div>

              <div className="info-faq-item">
                <h4 className="info-faq-question">4. Bu bir erken uyarı sistemi midir?</h4>
                <p className="info-faq-answer">
                  Hayır. Bu harita bir erken uyarı veya tahmin sistemi değildir. Meydana gelmiş ve AFAD istasyonları tarafından kaydedilmiş sarsıntıların doğrulanmış açık kataloğudur.
                </p>
              </div>
            </div>
          )}

          {activeTab === 'safety' && (
            <div id="panel-safety" role="tabpanel" aria-labelledby="tab-safety" className="info-tab-panel">
              <div className="info-section">
                <h3 className="info-section-title">Deprem Anında: ÇÖK - KAPAN - TUTUN</h3>
                <p className="info-text">
                  Sarsıntı başladığında paniğe kapılmadan sağlam bir nesnenin (masa, sıra vb.) yanına <strong>ÇÖKÜN</strong>, başınızı ve boynunuzu kollarınızla <strong>KAPATIN</strong>, nesneye sıkıca <strong>TUTUNUN</strong>.
                </p>
              </div>

              <div className="info-rules-list">
                <div className="info-rule-row">
                  <span className="info-rule-bullet">✕</span>
                  <span><strong>Asla asansör kullanmayın</strong> ve merdivenlere doğru koşmayın.</span>
                </div>
                <div className="info-rule-row">
                  <span className="info-rule-bullet">✕</span>
                  <span>Balkona çıkmayın, pencere kenarlarından ve camlardan uzak durun.</span>
                </div>
                <div className="info-rule-row">
                  <span className="info-rule-bullet">✓</span>
                  <span>Sarsıntı bittiğinde elektrik şalterini, su ve doğalgaz vanalarını derhal kapatın.</span>
                </div>
                <div className="info-rule-row">
                  <span className="info-rule-bullet">✓</span>
                  <span>Acil durum çantanızı alarak binayı sakince tahliye edin ve açık toplanma alanına geçin.</span>
                </div>
              </div>

              <div className="info-highlight-box" style={{ marginTop: '16px' }}>
                <h4 className="info-highlight-title">Acil Durum İletişim Hatları</h4>
                <p className="info-text-sm">
                  Tüm acil durumlar için: <strong>112 Acil Çağrı Merkezi</strong>. Telefon hatlarını gereksiz yere meşgul etmemek için internet tabanlı mesajlaşma uygulamalarını ve SMS tercih edin.
                </p>
              </div>
            </div>
          )}
        </div>

        <div className="modal-footer" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
          <div style={{ display: 'flex', gap: '14px', flexWrap: 'wrap' }}>
            <a
              href="https://deprem.afad.gov.tr"
              target="_blank"
              rel="noopener noreferrer"
              className="external-link"
              style={{ fontSize: '0.8125rem' }}
            >
              AFAD Deprem Dairesi ↗
            </a>
            <a
              href="http://www.koeri.boun.edu.tr/scripts/lst0.asp"
              target="_blank"
              rel="noopener noreferrer"
              className="external-link"
              style={{ fontSize: '0.8125rem' }}
            >
              Kandilli KOERI-BDTİM ↗
            </a>
          </div>
          <button type="button" className="btn-modal-primary" onClick={onClose}>
            Anladım, Kapat
          </button>
        </div>
      </div>
    </div>
  );
};
