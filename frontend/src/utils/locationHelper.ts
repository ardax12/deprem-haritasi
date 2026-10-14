import type { EarthquakeItem } from '../types/earthquake';

export interface ParsedLocation {
  isOffshore: boolean;
  isLake: boolean;
  isCrossBorder: boolean;
  environment: 'offshore' | 'lake' | 'cross_border' | 'land';
  displayTitle: string;
  region: string;
  nearestSettlement: string | null;
  distanceKm: number | null;
}

export function parseItemLocation(item: EarthquakeItem): ParsedLocation {
  const rawPlace = item.place?.trim() || '';
  const isLakeByName = /(^|\s)(van g[oö]l[uü]|hazar g[oö]l[uü]|iznik g[oö]l[uü]|beyşehir g[oö]l[uü]|eğridir g[oö]l[uü]|sapanca g[oö]l[uü]|burdur g[oö]l[uü]|tuz g[oö]l[uü]|akşehir g[oö]l[uü]|g[oö]l[uü]?|baraj[iı]?)(\s|$|\(|-)/i.test(rawPlace);

  if (item.displayLocation && item.environment && !isLakeByName) {
    const isLake = item.environment === 'lake';
    const isOffshore = !isLake && (item.environment === 'offshore');
    const isCrossBorder = !isLake && (item.environment === 'cross_border');
    return {
      isOffshore,
      isLake,
      isCrossBorder,
      environment: isLake ? 'lake' : (item.environment as any) || 'land',
      displayTitle: item.displayLocation,
      region: item.place?.split('-')[0]?.trim() || item.province || 'Bölge',
      nearestSettlement: item.nearestSettlement ?? null,
      distanceKm: item.distanceToCoastKm ?? null
    };
  }

  const bracketMatch = rawPlace.match(/^(.*?)\s*-\s*\[([0-9.,]+)\s*km\]\s*(.*?)(?:\s*\((.*?)\))?$/i);

  if (bracketMatch) {
    const region = bracketMatch[1].trim();
    const distanceKm = parseFloat(bracketMatch[2].replace(',', '.'));
    const district = bracketMatch[3].trim();
    const province = bracketMatch[4]?.trim() || '';
    const nearestSettlement = district + (province ? ` (${province})` : '');

    const isSea = /(deniz|k[oö]rfez|akdeniz|karadeniz|ege|marmara)/i.test(region);
    const isLake = isLakeByName || /(^|\s)(van g[oö]l[uü]|hazar g[oö]l[uü]|iznik g[oö]l[uü]|beyşehir g[oö]l[uü]|eğridir g[oö]l[uü]|sapanca g[oö]l[uü]|burdur g[oö]l[uü]|tuz g[oö]l[uü]|akşehir g[oö]l[uü]|g[oö]l[uü]?|baraj[iı]?)(\s|$|\(|-)/i.test(region);

    if (isLake) {
      return {
        isOffshore: false,
        isLake: true,
        isCrossBorder: false,
        environment: 'lake',
        displayTitle: `${region} (${district} kıyısına • ${distanceKm} km)`,
        region,
        nearestSettlement,
        distanceKm
      };
    } else if (isSea) {
      return {
        isOffshore: true,
        isLake: false,
        isCrossBorder: false,
        environment: 'offshore',
        displayTitle: `${region} (Türkiye kıyısına • ${district} açıklarında • ${distanceKm} km)`,
        region,
        nearestSettlement,
        distanceKm
      };
    } else {
      return {
        isOffshore: false,
        isLake: false,
        isCrossBorder: true,
        environment: 'cross_border',
        displayTitle: `${region} (${district} sınırında • ${distanceKm} km)`,
        region,
        nearestSettlement,
        distanceKm
      };
    }
  }

  const isPureLake = isLakeByName || /(^|\s)(van g[oö]l[uü]|hazar g[oö]l[uü]|iznik g[oö]l[uü]|beyşehir g[oö]l[uü]|eğridir g[oö]l[uü]|sapanca g[oö]l[uü]|burdur g[oö]l[uü]|tuz g[oö]l[uü]|akşehir g[oö]l[uü])(\s|$|\()/i.test(rawPlace);
  if (isPureLake) {
    return {
      isOffshore: false,
      isLake: true,
      isCrossBorder: false,
      environment: 'lake',
      displayTitle: `${rawPlace} (Göl İçi)`,
      region: rawPlace,
      nearestSettlement: item.district ? `${item.district}${item.province ? ` (${item.province})` : ''}` : null,
      distanceKm: null
    };
  }

  const isPureSea = /(deniz|k[oö]rfez|akdeniz|karadeniz|ege|marmara)/i.test(rawPlace) &&
    !rawPlace.toLowerCase().includes('denizli');
  if (isPureSea) {
    return {
      isOffshore: true,
      isLake: false,
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
    isLake: false,
    isCrossBorder: false,
    environment: 'land',
    displayTitle: rawPlace || (item.district ? `${item.district} (${item.province ?? ''})` : item.province ?? 'Bilinmeyen Konum'),
    region: item.province || 'Kara',
    nearestSettlement: null,
    distanceKm: null
  };
}
