import type { EarthquakeItem } from '../types/earthquake';

export interface ParsedLocation {
  isOffshore: boolean;
  isCrossBorder: boolean;
  environment: 'offshore' | 'cross_border' | 'land';
  displayTitle: string;
  region: string;
  nearestSettlement: string | null;
  distanceKm: number | null;
}

export function parseItemLocation(item: EarthquakeItem): ParsedLocation {
  if (item.displayLocation && item.environment) {
    const isOffshore = item.isOffshore ?? item.environment === 'offshore';
    const isCrossBorder = item.environment === 'cross_border';
    return {
      isOffshore,
      isCrossBorder,
      environment: isOffshore ? 'offshore' : (isCrossBorder ? 'cross_border' : 'land'),
      displayTitle: item.displayLocation,
      region: item.place?.split('-')[0]?.trim() || item.province || 'Bölge',
      nearestSettlement: item.nearestSettlement ?? null,
      distanceKm: item.distanceToCoastKm ?? null
    };
  }

  const rawPlace = item.place?.trim() || '';
  const bracketMatch = rawPlace.match(/^(.*?)\s*-\s*\[([0-9.,]+)\s*km\]\s*(.*?)(?:\s*\((.*?)\))?$/i);

  if (bracketMatch) {
    const region = bracketMatch[1].trim();
    const distanceKm = parseFloat(bracketMatch[2].replace(',', '.'));
    const district = bracketMatch[3].trim();
    const province = bracketMatch[4]?.trim() || '';
    const nearestSettlement = district + (province ? ` (${province})` : '');

    const isSea = /(deniz|k[oö]rfez|akdeniz|karadeniz|ege|marmara)/i.test(region);
    if (isSea) {
      return {
        isOffshore: true,
        isCrossBorder: false,
        environment: 'offshore',
        displayTitle: `${region} (Türkiye kıyısına • ${district} açıkları • ${distanceKm} km)`,
        region,
        nearestSettlement,
        distanceKm
      };
    } else {
      return {
        isOffshore: false,
        isCrossBorder: true,
        environment: 'cross_border',
        displayTitle: `${region} (${district} sınırında • ${distanceKm} km)`,
        region,
        nearestSettlement,
        distanceKm
      };
    }
  }

  const isPureSea = /(deniz|k[oö]rfez|akdeniz|karadeniz|ege|marmara)/i.test(rawPlace) &&
    !rawPlace.toLowerCase().includes('denizli');
  if (isPureSea) {
    return {
      isOffshore: true,
      isCrossBorder: false,
      environment: 'offshore',
      displayTitle: `${rawPlace} (Açık Deniz)`,
      region: rawPlace,
      nearestSettlement: item.district ? `${item.district}${item.province ? ` (${item.province})` : ''}` : null,
      distanceKm: null
    };
  }

  return {
    isOffshore: false,
    isCrossBorder: false,
    environment: 'land',
    displayTitle: rawPlace || (item.district ? `${item.district} (${item.province ?? ''})` : item.province ?? 'Bilinmeyen Konum'),
    region: item.province || 'Kara',
    nearestSettlement: null,
    distanceKm: null
  };
}
