import React from 'react';
import type { EarthquakeItem } from '../types/earthquake';
import { parseItemLocation } from '../utils/locationHelper';
import { formatRelativeTime } from '../utils/timeHelper';

interface AiSummarySnippetProps {
  items: EarthquakeItem[];
  totalCount: number;
  provinceName?: string | null;
}

export const AiSummarySnippet: React.FC<AiSummarySnippetProps> = ({
  items,
  totalCount,
  provinceName
}) => {
  if (items.length === 0) return null;

  // Find max magnitude in current set
  let maxItem = items[0];
  for (const it of items) {
    const val = it.magnitude?.value ?? 0;
    const currentMax = maxItem.magnitude?.value ?? 0;
    if (val > currentMax) {
      maxItem = it;
    }
  }

  const latestItem = items[0];
  const maxLoc = parseItemLocation(maxItem);
  const maxMagVal = maxItem.magnitude?.value !== null && maxItem.magnitude?.value !== undefined
    ? `M${maxItem.magnitude.value.toFixed(1)}`
    : 'Bilinmiyor';

  const relTime = formatRelativeTime(latestItem.occurredAt);

  return (
    <aside className="ai-summary-snippet" aria-label="Günlük Sismik Faaliyet Özeti">
      <div className="ai-summary-badge">24s Özet</div>
      <p className="ai-summary-text">
        {provinceName ? (
          <>
            <strong>{provinceName}</strong> ve yakın çevresinde son 24 saatte toplam <strong>{totalCount}</strong> sarsıntı kaydedildi.
            {maxItem && (
              <> En belirgin hareket: <strong>{maxLoc.displayTitle}</strong> ({maxMagVal}).</>
            )}
            {relTime && <> Son kayıt: <em>{relTime}</em>.</>}
          </>
        ) : (
          <>
            Türkiye ve komşu bölgelerde son 24 saatte toplam <strong>{totalCount}</strong> sarsıntı kaydedildi.
            {maxItem && (
              <> En büyük hareket: <strong>{maxLoc.displayTitle}</strong> ({maxMagVal}).</>
            )}
            {relTime && <> Son olay: <em>{relTime}</em>.</>}
          </>
        )}
      </p>
    </aside>
  );
};
