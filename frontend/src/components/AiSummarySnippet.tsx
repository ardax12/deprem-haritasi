import React from 'react';
import type { EarthquakeItem } from '../types/earthquake';
import { parseItemLocation } from '../utils/locationHelper';
import { formatRelativeTime } from '../utils/timeHelper';

interface AiSummarySnippetProps {
  items: EarthquakeItem[];
  totalCount: number;
  provinceName?: string | null;
  timeRange?: '24h' | '3d' | '7d' | '30d';
  minMagnitude?: number | null;
}

export const AiSummarySnippet: React.FC<AiSummarySnippetProps> = ({
  items,
  totalCount,
  provinceName,
  timeRange = '24h',
  minMagnitude = null
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

  // Time range labels and copy
  let badgeLabel = '24s Özet';
  let timeRangePhrase = 'son 24 saatte';

  if (timeRange === '3d') {
    badgeLabel = '3 Günlük Özet';
    timeRangePhrase = 'son 3 günde';
  } else if (timeRange === '7d') {
    badgeLabel = '7 Günlük Özet';
    timeRangePhrase = 'son 7 günde';
  } else if (timeRange === '30d') {
    badgeLabel = '30 Günlük Özet';
    timeRangePhrase = 'son 30 günde';
  }

  const magConstraint = minMagnitude !== null && minMagnitude > 0
    ? ` (M ≥ ${minMagnitude.toFixed(1)})`
    : '';

  return (
    <aside className="ai-summary-snippet" aria-label={`${badgeLabel} Sismik Faaliyet Özeti`}>
      <div className="ai-summary-badge">{badgeLabel}</div>
      <p className="ai-summary-text">
        {provinceName ? (
          <>
            <strong>{provinceName}</strong> ve yakın çevresinde {timeRangePhrase} toplam <strong>{totalCount}</strong> sarsıntı{magConstraint} kaydedildi.
            {maxItem && (
              <> En belirgin hareket: <strong>{maxLoc.displayTitle}</strong> ({maxMagVal}).</>
            )}
            {relTime && <> Son kayıt: <em>{relTime}</em>.</>}
          </>
        ) : (
          <>
            Türkiye ve komşu bölgelerde {timeRangePhrase} toplam <strong>{totalCount}</strong> sarsıntı{magConstraint} kaydedildi.
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
