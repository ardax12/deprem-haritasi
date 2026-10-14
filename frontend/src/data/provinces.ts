export interface ProvinceData {
  plate: number;
  name: string;
  slug: string;
  lng: number;
  lat: number;
  zoom: number;
  region: string;
}

export const TURKISH_PROVINCES: ProvinceData[] = [
  { plate: 1, name: 'Adana', slug: 'adana', lng: 35.3308, lat: 37.0000, zoom: 9, region: 'Akdeniz' },
  { plate: 2, name: 'Adıyaman', slug: 'adiyaman', lng: 38.2773, lat: 37.7636, zoom: 9, region: 'Güneydoğu Anadolu' },
  { plate: 3, name: 'Afyonkarahisar', slug: 'afyonkarahisar', lng: 30.5433, lat: 38.7569, zoom: 9, region: 'Ege' },
  { plate: 4, name: 'Ağrı', slug: 'agri', lng: 43.0519, lat: 39.7191, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 5, name: 'Amasya', slug: 'amasya', lng: 35.8331, lat: 40.6524, zoom: 9, region: 'Karadeniz' },
  { plate: 6, name: 'Ankara', slug: 'ankara', lng: 32.8597, lat: 39.9334, zoom: 9, region: 'İç Anadolu' },
  { plate: 7, name: 'Antalya', slug: 'antalya', lng: 30.7133, lat: 36.8969, zoom: 9, region: 'Akdeniz' },
  { plate: 8, name: 'Artvin', slug: 'artvin', lng: 41.8183, lat: 41.1828, zoom: 9, region: 'Karadeniz' },
  { plate: 9, name: 'Aydın', slug: 'aydin', lng: 27.8456, lat: 37.8444, zoom: 9, region: 'Ege' },
  { plate: 10, name: 'Balıkesir', slug: 'balikesir', lng: 27.8903, lat: 39.6484, zoom: 9, region: 'Marmara' },
  { plate: 11, name: 'Bilecik', slug: 'bilecik', lng: 29.9792, lat: 40.1419, zoom: 9, region: 'Marmara' },
  { plate: 12, name: 'Bingöl', slug: 'bingol', lng: 40.4939, lat: 38.8855, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 13, name: 'Bitlis', slug: 'bitlis', lng: 42.1095, lat: 38.4006, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 14, name: 'Bolu', slug: 'bolu', lng: 31.6061, lat: 40.7358, zoom: 9, region: 'Karadeniz' },
  { plate: 15, name: 'Burdur', slug: 'burdur', lng: 30.2889, lat: 37.7203, zoom: 9, region: 'Akdeniz' },
  { plate: 16, name: 'Bursa', slug: 'bursa', lng: 29.0610, lat: 40.1885, zoom: 9, region: 'Marmara' },
  { plate: 17, name: 'Çanakkale', slug: 'canakkale', lng: 26.4086, lat: 40.1553, zoom: 9, region: 'Marmara' },
  { plate: 18, name: 'Çankırı', slug: 'cankiri', lng: 33.6139, lat: 40.6013, zoom: 9, region: 'İç Anadolu' },
  { plate: 19, name: 'Çorum', slug: 'corum', lng: 34.9537, lat: 40.5506, zoom: 9, region: 'Karadeniz' },
  { plate: 20, name: 'Denizli', slug: 'denizli', lng: 29.0864, lat: 37.7765, zoom: 9, region: 'Ege' },
  { plate: 21, name: 'Diyarbakır', slug: 'diyarbakir', lng: 40.2189, lat: 37.9144, zoom: 9, region: 'Güneydoğu Anadolu' },
  { plate: 22, name: 'Edirne', slug: 'edirne', lng: 26.5557, lat: 41.6771, zoom: 9, region: 'Marmara' },
  { plate: 23, name: 'Elazığ', slug: 'elazig', lng: 39.2232, lat: 38.6810, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 24, name: 'Erzincan', slug: 'erzincan', lng: 39.4926, lat: 39.7500, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 25, name: 'Erzurum', slug: 'erzurum', lng: 41.2769, lat: 39.9043, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 26, name: 'Eskişehir', slug: 'eskisehir', lng: 30.5256, lat: 39.7767, zoom: 9, region: 'İç Anadolu' },
  { plate: 27, name: 'Gaziantep', slug: 'gaziantep', lng: 37.3822, lat: 37.0662, zoom: 9, region: 'Güneydoğu Anadolu' },
  { plate: 28, name: 'Giresun', slug: 'giresun', lng: 38.3895, lat: 40.9128, zoom: 9, region: 'Karadeniz' },
  { plate: 29, name: 'Gümüşhane', slug: 'gumushane', lng: 39.4814, lat: 40.4600, zoom: 9, region: 'Karadeniz' },
  { plate: 30, name: 'Hakkari', slug: 'hakkari', lng: 43.7408, lat: 37.5833, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 31, name: 'Hatay', slug: 'hatay', lng: 36.1667, lat: 36.2000, zoom: 9, region: 'Akdeniz' },
  { plate: 32, name: 'Isparta', slug: 'isparta', lng: 30.5537, lat: 37.7648, zoom: 9, region: 'Akdeniz' },
  { plate: 33, name: 'Mersin', slug: 'mersin', lng: 34.6415, lat: 36.8121, zoom: 9, region: 'Akdeniz' },
  { plate: 34, name: 'İstanbul', slug: 'istanbul', lng: 28.9784, lat: 41.0082, zoom: 9.5, region: 'Marmara' },
  { plate: 35, name: 'İzmir', slug: 'izmir', lng: 27.1428, lat: 38.4237, zoom: 9.5, region: 'Ege' },
  { plate: 36, name: 'Kars', slug: 'kars', lng: 43.0975, lat: 40.6013, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 37, name: 'Kastamonu', slug: 'kastamonu', lng: 33.7765, lat: 41.3887, zoom: 9, region: 'Karadeniz' },
  { plate: 38, name: 'Kayseri', slug: 'kayseri', lng: 35.4853, lat: 38.7312, zoom: 9, region: 'İç Anadolu' },
  { plate: 39, name: 'Kırklareli', slug: 'kirklareli', lng: 27.2244, lat: 41.7333, zoom: 9, region: 'Marmara' },
  { plate: 40, name: 'Kırşehir', slug: 'kirsehir', lng: 34.1709, lat: 39.1425, zoom: 9, region: 'İç Anadolu' },
  { plate: 41, name: 'Kocaeli', slug: 'kocaeli', lng: 29.9169, lat: 40.8533, zoom: 9.5, region: 'Marmara' },
  { plate: 42, name: 'Konya', slug: 'konya', lng: 32.4846, lat: 37.8667, zoom: 8.5, region: 'İç Anadolu' },
  { plate: 43, name: 'Kütahya', slug: 'kutahya', lng: 29.9833, lat: 39.4167, zoom: 9, region: 'Ege' },
  { plate: 44, name: 'Malatya', slug: 'malatya', lng: 38.3552, lat: 38.3552, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 45, name: 'Manisa', slug: 'manisa', lng: 27.4289, lat: 38.6191, zoom: 9, region: 'Ege' },
  { plate: 46, name: 'Kahramanmaraş', slug: 'kahramanmaras', lng: 36.9372, lat: 37.5858, zoom: 9, region: 'Akdeniz' },
  { plate: 47, name: 'Mardin', slug: 'mardin', lng: 40.7339, lat: 37.3212, zoom: 9, region: 'Güneydoğu Anadolu' },
  { plate: 48, name: 'Muğla', slug: 'mugla', lng: 28.3636, lat: 37.2153, zoom: 9, region: 'Ege' },
  { plate: 49, name: 'Muş', slug: 'mus', lng: 41.4911, lat: 38.7432, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 50, name: 'Nevşehir', slug: 'nevsehir', lng: 34.7144, lat: 38.6244, zoom: 9, region: 'İç Anadolu' },
  { plate: 51, name: 'Niğde', slug: 'nigde', lng: 34.6857, lat: 37.9667, zoom: 9, region: 'İç Anadolu' },
  { plate: 52, name: 'Ordu', slug: 'ordu', lng: 37.8797, lat: 40.9839, zoom: 9, region: 'Karadeniz' },
  { plate: 53, name: 'Rize', slug: 'rize', lng: 40.5208, lat: 41.0201, zoom: 9, region: 'Karadeniz' },
  { plate: 54, name: 'Sakarya', slug: 'sakarya', lng: 30.4000, lat: 40.7500, zoom: 9, region: 'Marmara' },
  { plate: 55, name: 'Samsun', slug: 'samsun', lng: 36.3300, lat: 41.2928, zoom: 9, region: 'Karadeniz' },
  { plate: 56, name: 'Siirt', slug: 'siirt', lng: 41.9403, lat: 37.9333, zoom: 9, region: 'Güneydoğu Anadolu' },
  { plate: 57, name: 'Sinop', slug: 'sinop', lng: 35.1531, lat: 42.0231, zoom: 9, region: 'Karadeniz' },
  { plate: 58, name: 'Sivas', slug: 'sivas', lng: 37.0179, lat: 39.7477, zoom: 8.5, region: 'İç Anadolu' },
  { plate: 59, name: 'Tekirdağ', slug: 'tekirdag', lng: 27.5110, lat: 40.9833, zoom: 9, region: 'Marmara' },
  { plate: 60, name: 'Tokat', slug: 'tokat', lng: 36.5544, lat: 40.3167, zoom: 9, region: 'Karadeniz' },
  { plate: 61, name: 'Trabzon', slug: 'trabzon', lng: 39.7167, lat: 41.0015, zoom: 9, region: 'Karadeniz' },
  { plate: 62, name: 'Tunceli', slug: 'tunceli', lng: 39.5401, lat: 39.1079, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 63, name: 'Şanlıurfa', slug: 'sanliurfa', lng: 38.7955, lat: 37.1591, zoom: 8.5, region: 'Güneydoğu Anadolu' },
  { plate: 64, name: 'Uşak', slug: 'usak', lng: 29.4058, lat: 38.6823, zoom: 9, region: 'Ege' },
  { plate: 65, name: 'Van', slug: 'van', lng: 43.3725, lat: 38.4891, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 66, name: 'Yozgat', slug: 'yozgat', lng: 34.8044, lat: 39.8181, zoom: 9, region: 'İç Anadolu' },
  { plate: 67, name: 'Zonguldak', slug: 'zonguldak', lng: 31.7987, lat: 41.4564, zoom: 9, region: 'Karadeniz' },
  { plate: 68, name: 'Aksaray', slug: 'aksaray', lng: 34.0254, lat: 38.3687, zoom: 9, region: 'İç Anadolu' },
  { plate: 69, name: 'Bayburt', slug: 'bayburt', lng: 40.2249, lat: 40.2552, zoom: 9, region: 'Karadeniz' },
  { plate: 70, name: 'Karaman', slug: 'karaman', lng: 33.2144, lat: 37.1759, zoom: 9, region: 'İç Anadolu' },
  { plate: 71, name: 'Kırıkkale', slug: 'kirikkale', lng: 33.5066, lat: 39.8468, zoom: 9, region: 'İç Anadolu' },
  { plate: 72, name: 'Batman', slug: 'batman', lng: 41.1322, lat: 37.8812, zoom: 9, region: 'Güneydoğu Anadolu' },
  { plate: 73, name: 'Şırnak', slug: 'sirnak', lng: 42.4594, lat: 37.5164, zoom: 9, region: 'Güneydoğu Anadolu' },
  { plate: 74, name: 'Bartın', slug: 'bartin', lng: 32.3375, lat: 41.6344, zoom: 9, region: 'Karadeniz' },
  { plate: 75, name: 'Ardahan', slug: 'ardahan', lng: 42.7022, lat: 41.1105, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 76, name: 'Iğdır', slug: 'igdir', lng: 44.0450, lat: 39.9196, zoom: 9, region: 'Doğu Anadolu' },
  { plate: 77, name: 'Yalova', slug: 'yalova', lng: 29.2769, lat: 40.6500, zoom: 9.5, region: 'Marmara' },
  { plate: 78, name: 'Karabük', slug: 'karabuk', lng: 32.6277, lat: 41.2061, zoom: 9, region: 'Karadeniz' },
  { plate: 79, name: 'Kilis', slug: 'kilis', lng: 37.1150, lat: 36.7184, zoom: 9, region: 'Güneydoğu Anadolu' },
  { plate: 80, name: 'Osmaniye', slug: 'osmaniye', lng: 36.2464, lat: 37.0742, zoom: 9, region: 'Akdeniz' },
  { plate: 81, name: 'Düzce', slug: 'duzce', lng: 31.1625, lat: 40.8438, zoom: 9.5, region: 'Karadeniz' }
];

export function findProvinceBySlug(slug: string): ProvinceData | undefined {
  const normalized = slug.trim().toLowerCase();
  return TURKISH_PROVINCES.find((p) => p.slug === normalized);
}

export function findProvinceByName(name: string): ProvinceData | undefined {
  const normalized = name.trim().toLowerCase()
    .replace(/i̇/g, 'i')
    .replace(/ı/g, 'i')
    .replace(/ş/g, 's')
    .replace(/ğ/g, 'g')
    .replace(/ü/g, 'u')
    .replace(/ö/g, 'o')
    .replace(/ç/g, 'c');

  return TURKISH_PROVINCES.find((p) => {
    const pNorm = p.name.toLowerCase()
      .replace(/i̇/g, 'i')
      .replace(/ı/g, 'i')
      .replace(/ş/g, 's')
      .replace(/ğ/g, 'g')
      .replace(/ü/g, 'u')
      .replace(/ö/g, 'o')
      .replace(/ç/g, 'c');
    return pNorm === normalized || p.slug === normalized;
  });
}
