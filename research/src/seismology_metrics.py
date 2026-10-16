"""
İstatistiksel Sismoloji Metrikleri & Anomali Hesaplayıcı
- Gutenberg-Richter b-değeri (Aki-Utsu Maximum Likelihood Estimator)
- Tamlık Büyüklüğü (Completeness Magnitude - Mc, MAXC metodu)
- Kümülatif Benioff Gerilme Salınımı (Benioff Strain Energy Release)
"""

import numpy as np
import pandas as pd
from typing import Tuple, Dict, Any


def estimate_mc_maxc(magnitudes: np.ndarray, bin_width: float = 0.1) -> float:
    """
    Maksimum Eğrilik (MAXC) yöntemiyle katalog tamlık büyüklüğünü (Mc) tahmin eder.
    Frekans-büyüklük dağılımının tepe noktası Mc olarak alınır.
    """
    valid = magnitudes[~np.isnan(magnitudes)]
    if len(valid) < 20:
        return 1.5

    bins = np.arange(np.min(valid), np.max(valid) + bin_width, bin_width)
    counts, bin_edges = np.histogram(valid, bins=bins)
    max_idx = np.argmax(counts)
    return float(bin_edges[max_idx])


def calculate_b_value_mle(
    magnitudes: np.ndarray,
    mc: float = None,
    bin_width: float = 0.1
) -> Tuple[float, float, int]:
    """
    Aki (1965) - Utsu (1966) Maksimum Olabilirlik (MLE) yöntemiyle Gutenberg-Richter b-değerini hesaplar.
    Formül: b = (log10(e)) / (mean(M) - (Mc - bin_width/2))
    
    Dönüş: (b_value, b_standard_error, sample_size)
    """
    valid = magnitudes[~np.isnan(magnitudes)]
    if mc is None:
        mc = estimate_mc_maxc(valid, bin_width)

    subset = valid[valid >= mc]
    n = len(subset)

    if n < 25:
        return np.nan, np.nan, n

    mean_m = np.mean(subset)
    denom = mean_m - (mc - (bin_width / 2.0))
    if denom <= 1e-6:
        return np.nan, np.nan, n

    b_val = np.log10(np.e) / denom
    std_err = b_val / np.sqrt(n)

    return float(b_val), float(std_err), n


def calculate_benioff_strain(magnitudes: np.ndarray) -> np.ndarray:
    """
    Kümülatif Benioff Gerilme Salınımı (Benioff Strain).
    Sismik enerji salınımının kareköküyle orantılıdır: S = 10^(0.75 * M)
    Fayda biriken ve serbest kalan elastik gerilimin kümülatif göstergesidir.
    """
    if magnitudes is None or len(magnitudes) == 0:
        return np.array([])
    valid = np.nan_to_num(magnitudes, nan=0.0)
    valid = np.clip(valid, 0.0, 10.0)
    strain = 10.0 ** (0.75 * valid)
    return np.cumsum(strain)


def rolling_b_value_series(
    df: pd.DataFrame,
    window_events: int = 200,
    step_events: int = 50,
    mag_col: str = "magnitude",
    date_col: str = "date_utc"
) -> pd.DataFrame:
    """
    Zaman içinde kayan olay penceresiyle (rolling event window) b-değerinin değişimini çıkarır.
    b-değerinin ani düşüşleri yaklaşan büyük deprem stres birikimini gösterir.
    """
    if len(df) < window_events:
        return pd.DataFrame()

    df_sorted = df.sort_values(by=date_col).reset_index(drop=True)
    results = []

    for i in range(0, len(df_sorted) - window_events + 1, step_events):
        window_df = df_sorted.iloc[i : i + window_events]
        mags = window_df[mag_col].values
        mc = estimate_mc_maxc(mags)
        b, std_err, n = calculate_b_value_mle(mags, mc=mc)

        center_date = window_df[date_col].iloc[-1]
        results.append({
            "window_end_date": center_date,
            "mc": round(mc, 2),
            "b_value": round(b, 3) if not np.isnan(b) else None,
            "std_error": round(std_err, 3) if not np.isnan(std_err) else None,
            "sample_size": n
        })

    return pd.DataFrame(results)
