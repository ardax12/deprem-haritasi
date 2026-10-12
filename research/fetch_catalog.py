"""
AFAD Deprem Kataloğu Kalite ve Tamlık Analizi Veri İndirme Betiği
Bu betik canlı uygulamaya entegre değildir; araştırma ve EDA amacıyla kullanılır.
"""

import os
import requests
import pandas as pd
from datetime import datetime, timezone

AFAD_API_URL = "https://deprem.afad.gov.tr/apiv2/event/filter"

def fetch_events(start_str: str, end_str: str) -> pd.DataFrame:
    """Belirtilen UTC zaman aralığındaki AFAD olaylarını DataFrame olarak çeker."""
    params = {
        "start": start_str,
        "end": end_str
    }
    print(f"AFAD verisi çekiliyor: {start_str} - {end_str}")
    response = requests.get(AFAD_API_URL, params=params, timeout=30)
    response.raise_for_status()
    data = response.json()

    if not data or not isinstance(data, list):
        print("Kayıt bulunamadı.")
        return pd.DataFrame()

    df = pd.DataFrame(data)
    print(f"Toplam {len(df)} kayıt alındı.")

    # Numeric conversion
    numeric_cols = ["latitude", "longitude", "depth", "magnitude", "rms"]
    for col in numeric_cols:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce")

    # Date conversion (interpreted as UTC)
    if "date" in df.columns:
        df["date_utc"] = pd.to_datetime(df["date"], errors="coerce", utc=True)

    return df

def audit_catalog_quality(df: pd.DataFrame) -> dict:
    """Katalog veri kalitesi ve eksik alan metriklerini hesaplar."""
    if df.empty:
        return {"status": "empty"}

    total = len(df)
    metrics = {
        "total_records": total,
        "missing_magnitude_pct": (df["magnitude"].isna().sum() / total) * 100,
        "missing_depth_pct": (df["depth"].isna().sum() / total) * 100,
        "missing_location_pct": (df["location"].isna().sum() / total) * 100,
        "min_date": str(df["date_utc"].min()),
        "max_date": str(df["date_utc"].max()),
        "magnitude_types": df["type"].value_counts().to_dict() if "type" in df.columns else {},
        "magnitude_summary": df["magnitude"].describe().to_dict()
    }
    return metrics

if __name__ == "__main__":
    # Test çalıştırması: son 2 gün
    now = datetime.now(timezone.utc)
    from datetime import timedelta
    two_days_ago = now - timedelta(days=2)

    start = two_days_ago.strftime("%Y-%m-%d %H:%M:%S")
    end = now.strftime("%Y-%m-%d %H:%M:%S")

    df = fetch_events(start, end)
    if not df.empty:
        report = audit_catalog_quality(df)
        print("\n--- Katalog Kalite Özeti ---")
        for k, v in report.items():
            print(f"{k}: {v}")
