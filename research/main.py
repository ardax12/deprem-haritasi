"""
Türkiye Deprem Araştırma & Tahmin Çalışma Alanı — Giriş Noktası (Entrypoint)
PyCharm'da doğrudan çalıştırılabilir.
"""

import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

# Proje kökünü sys.path'e ekle
sys.path.append(str(Path(__file__).resolve().parent))

from src.data_collector import download_historical_catalog
from src.seismology_metrics import calculate_b_value_mle, estimate_mc_maxc, rolling_b_value_series


def main():
    print("=" * 60)
    print("  TÜRKİYE DEPREM VERİ ANALİZİ & SİSMOLOJİK MODELLEME")
    print("=" * 60)

    # 1. Örnek Veri İndirme (Son 30 gün)
    now = datetime.now(timezone.utc)
    start_date = now - timedelta(days=30)

    print(f"\n[1/3] Son 30 günün AFAD kataloğu sorgulanıyor...")
    df = download_historical_catalog(start_date, now, chunk_days=2)

    if df.empty:
        print("[HATA] Katalog verisi alınamadı.")
        return

    print(f"\n[2/3] Katalog İstatistikleri:")
    print(f"Toplam Olay Sayısı: {len(df)}")
    mags = df["magnitude"].dropna().values
    print(f"Magnitüd Aralığı   : {mags.min():.1f} - {mags.max():.1f}")
    print(f"Ortalama Derinlik  : {df['depth'].dropna().mean():.1f} km")

    # 2. Gutenberg-Richter b-değeri hesabı
    print(f"\n[3/3] Gutenberg-Richter b-değeri Analizi:")
    mc = estimate_mc_maxc(mags)
    b_val, std_err, n_events = calculate_b_value_mle(mags, mc=mc)
    print(f"Tahmin Edilen Tamlık Magnitüdü (Mc): {mc:.2f}")
    print(f"Hesaplanan b-değeri (Aki-Utsu MLE) : {b_val:.3f} ± {std_err:.3f} (Örneklem N={n_events})")

    if b_val < 0.8:
        print(">> [DİKKAT] b-değeri genel Türkiye ortalamasının (~1.0) belirgin altında! Yüksek stres birikimi işareti.")
    else:
        print(">> [NORMAL] b-değeri standart tektonik arka plan dengesinde (~1.0).")

    print("\n✓ PyCharm ortamı başarıyla doğrulandı! Notebooks ve ML modelleri çalıştırmaya hazırdır.")


if __name__ == "__main__":
    main()
