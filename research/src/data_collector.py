"""
AFAD Tarihsel Deprem Kataloğu Toplu İndirici (Batch Downloader)
Belirlenen tarih aralığındaki AFAD deprem verilerini parça parça indirir ve
analiz için diskte 'data/raw/afad_catalog.parquet' olarak kaydeder.
"""

import os
import time
import requests
import pandas as pd
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Optional

AFAD_API_URL = "https://deprem.afad.gov.tr/apiv2/event/filter"
DEFAULT_DATA_DIR = Path(__file__).resolve().parent.parent / "data" / "raw"


def fetch_interval(start_dt: datetime, end_dt: datetime, retries: int = 3) -> pd.DataFrame:
    """Belirli bir zaman dilimi için AFAD API'den olayları çeker."""
    start_str = start_dt.strftime("%Y-%m-%d %H:%M:%S")
    end_str = end_dt.strftime("%Y-%m-%d %H:%M:%S")
    params = {"start": start_str, "end": end_str}

    for attempt in range(1, retries + 1):
        try:
            resp = requests.get(AFAD_API_URL, params=params, timeout=30)
            if resp.status_code == 200:
                data = resp.json()
                if not data or not isinstance(data, list):
                    return pd.DataFrame()
                return pd.DataFrame(data)
            time.sleep(attempt * 1.5)
        except Exception as ex:
            if attempt == retries:
                print(f"[UYARI] {start_str} - {end_str} aralığı indirilemedi: {ex}")
            time.sleep(attempt * 1.5)

    return pd.DataFrame()


def download_historical_catalog(
    start_date: datetime,
    end_date: datetime,
    chunk_days: int = 2,
    delay_sec: float = 0.5,
    output_filename: str = "afad_catalog.parquet"
) -> pd.DataFrame:
    """
    Belirtilen tarih aralığını parçalar halinde indirir ve birleştirir.
    """
    DEFAULT_DATA_DIR.mkdir(parents=True, exist_ok=True)
    out_path = DEFAULT_DATA_DIR / output_filename

    print(f"=== AFAD Tarihsel Katalog İndirme Başlatılıyor ===")
    print(f"Aralık: {start_date.date()} -> {end_date.date()} ({chunk_days} günlük dilimlerle)")

    all_dfs = []
    current_start = start_date

    while current_start < end_date:
        current_end = min(current_start + timedelta(days=chunk_days), end_date)
        print(f"İndiriliyor: {current_start.strftime('%Y-%m-%d')} - {current_end.strftime('%Y-%m-%d')} ...", end=" ")

        df_chunk = fetch_interval(current_start, current_end)
        if not df_chunk.empty:
            all_dfs.append(df_chunk)
            print(f"✓ {len(df_chunk)} olay")
        else:
            print("✓ 0 olay")

        current_start = current_end
        if delay_sec > 0 and current_start < end_date:
            time.sleep(delay_sec)

    if not all_dfs:
        print("Hiçbir veri indirilemedi.")
        return pd.DataFrame()

    full_df = pd.concat(all_dfs, ignore_index=True)

    # Veri tiplerini dönüştür ve temizle
    full_df.drop_duplicates(subset=["eventId"], keep="last", inplace=True)

    num_cols = ["latitude", "longitude", "depth", "magnitude", "rms"]
    for c in num_cols:
        if c in full_df.columns:
            full_df[c] = pd.to_numeric(full_df[c], errors="coerce")

    if "date" in full_df.columns:
        full_df["date_utc"] = pd.to_datetime(full_df["date"], errors="coerce", utc=True)
        full_df.sort_values(by="date_utc", inplace=True)
        full_df.reset_index(drop=True, inplace=True)

    full_df.to_parquet(out_path, index=False)
    print(f"\n[BAŞARILI] Toplam {len(full_df)} benzersiz deprem kaydedildi -> {out_path}")
    return full_df


if __name__ == "__main__":
    # Test çalıştırması: Son 60 günün verisini indir
    now = datetime.now(timezone.utc)
    sixty_days_ago = now - timedelta(days=60)
    download_historical_catalog(sixty_days_ago, now, chunk_days=2)
