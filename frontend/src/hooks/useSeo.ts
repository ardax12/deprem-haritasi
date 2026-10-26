import { useEffect } from 'react';
import type { EarthquakeItem } from '../types/earthquake';
import { parseItemLocation } from '../utils/locationHelper';
import { findProvinceByName } from '../data/provinces';

interface UseSeoOptions {
  provinceName?: string | null;
  activeItem?: EarthquakeItem | null;
  totalCount?: number;
  latestItem?: EarthquakeItem | null;
}

export function useSeo({ provinceName, activeItem, totalCount, latestItem }: UseSeoOptions) {
  useEffect(() => {
    const origin = window.location.origin;
    const matchedProvince = provinceName ? findProvinceByName(provinceName) : null;
    const provinceSlug = matchedProvince?.slug;

    // Modal query parameters should NEVER alter canonical tags
    const canonicalUrl = provinceSlug
      ? `${origin}/son-depremler/${provinceSlug}`
      : `${origin}/`;

    let pageTitle = 'Türkiye Deprem İzleme - Canlı AFAD & Kandilli Haritası ve Son Depremler';
    let metaDescription = 'AFAD ve Kandilli anlık açık verileriyle Türkiye ve komşu ülkelerdeki son depremleri canlı interaktif harita, derinlik, büyüklük ve revizyon geçmişiyle izleyin.';

    if (activeItem) {
      const loc = parseItemLocation(activeItem);
      const magVal = activeItem.magnitude?.value !== null && activeItem.magnitude?.value !== undefined
        ? `M${activeItem.magnitude.value}`
        : 'Deprem';
      pageTitle = `${magVal} - ${loc.displayTitle} Depremi | Türkiye Deprem İzleme`;
      metaDescription = `${loc.displayTitle} bölgesinde gerçekleşen ${magVal} büyüklüğündeki depremin koordinatları, odak derinliği (${activeItem.depthKm ?? '-'} km) ve sismik verileri.`;
    } else if (provinceName) {
      pageTitle = `${provinceName} Son Depremler - Canlı Deprem Haritası | Türkiye Deprem İzleme`;
      metaDescription = `${provinceName} ve çevresinde meydana gelen güncel sarsıntılar, son deprem listesi ve canlı fay haritası ile anlık güncellenmektedir.`;
    }

    document.title = pageTitle;

    // Update Meta Description
    let descTag = document.querySelector('meta[name="description"]');
    if (!descTag) {
      descTag = document.createElement('meta');
      descTag.setAttribute('name', 'description');
      document.head.appendChild(descTag);
    }
    descTag.setAttribute('content', metaDescription);

    // Update Canonical URL
    let canonTag = document.querySelector('link[rel="canonical"]');
    if (!canonTag) {
      canonTag = document.createElement('link');
      canonTag.setAttribute('rel', 'canonical');
      document.head.appendChild(canonTag);
    }
    canonTag.setAttribute('href', canonicalUrl);

    // Update Open Graph tags
    const setOgTag = (property: string, content: string) => {
      let og = document.querySelector(`meta[property="${property}"]`);
      if (!og) {
        og = document.createElement('meta');
        og.setAttribute('property', property);
        document.head.appendChild(og);
      }
      og.setAttribute('content', content);
    };

    setOgTag('og:title', pageTitle);
    setOgTag('og:description', metaDescription);
    setOgTag('og:url', canonicalUrl);

    // Build Schema.org JSON-LD graph (GEO / WebApplication & BreadcrumbList standard)
    const jsonLdGraph: any[] = [
      {
        '@context': 'https://schema.org',
        '@type': 'WebApplication',
        '@id': `${origin}/#webapp`,
        name: 'Türkiye Deprem Haritası',
        alternateName: 'AFAD & Kandilli Canlı Deprem İzleme Haritası',
        description: 'AFAD ve Kandilli Rasathanesi kamuya açık verileriyle Türkiye son depremlerini canlı harita üzerinde gösteren bağımsız sismik izleme platformu.',
        url: `${origin}/`,
        applicationCategory: 'UtilitiesApplication',
        operatingSystem: 'All',
        inLanguage: 'tr',
        offers: {
          '@type': 'Offer',
          price: '0',
          priceCurrency: 'TRY',
          url: `${origin}/`
        }
      },
      {
        '@context': 'https://schema.org',
        '@type': 'WebSite',
        '@id': `${origin}/#website`,
        url: `${origin}/`,
        name: 'Türkiye Deprem Haritası',
        alternateName: [
          'Deprem Haritası',
          'Türkiye Canlı Deprem İzleme',
          'AFAD ve Kandilli Deprem Haritası'
        ],
        inLanguage: 'tr'
      },
      {
        '@context': 'https://schema.org',
        '@type': 'BreadcrumbList',
        itemListElement: [
          {
            '@type': 'ListItem',
            position: 1,
            name: 'Ana Sayfa',
            item: `${origin}/`
          },
          ...(provinceName && provinceSlug ? [{
            '@type': 'ListItem',
            position: 2,
            name: `${provinceName} Son Depremler`,
            item: `${origin}/son-depremler/${provinceSlug}`
          }] : [])
        ]
      }
    ];

    if (activeItem) {
      const loc = parseItemLocation(activeItem);
      const srcName = activeItem.source === 'KOERI' ? 'Kandilli Rasathanesi' : 'AFAD';
      jsonLdGraph.push({
        '@context': 'https://schema.org',
        '@type': 'Event',
        '@id': `${origin}/?event=${encodeURIComponent(activeItem.id)}`,
        name: `${loc.displayTitle} Depremi`,
        startDate: activeItem.occurredAt,
        location: {
          '@type': 'Place',
          name: loc.displayTitle,
          geo: {
            '@type': 'GeoCoordinates',
            latitude: activeItem.latitude,
            longitude: activeItem.longitude
          }
        },
        description: `${loc.displayTitle} bölgesinde M${activeItem.magnitude?.value ?? '-'} büyüklüğünde, ${activeItem.depthKm ?? '-'} km odak derinliğinde ${srcName} tarafından kaydedilen deprem.`
      });
    }

    // Insert or update script tag
    let scriptTag = document.getElementById('schema-org-seo') as HTMLScriptElement | null;
    if (!scriptTag) {
      scriptTag = document.createElement('script');
      scriptTag.id = 'schema-org-seo';
      scriptTag.type = 'application/ld+json';
      document.head.appendChild(scriptTag);
    }
    scriptTag.textContent = JSON.stringify({
      '@context': 'https://schema.org',
      '@graph': jsonLdGraph
    });
  }, [provinceName, activeItem, totalCount, latestItem]);
}
