import { useEffect } from 'react';
import type { EarthquakeItem } from '../types/earthquake';
import { parseItemLocation } from '../utils/locationHelper';

interface UseSeoOptions {
  provinceName?: string | null;
  activeItem?: EarthquakeItem | null;
  totalCount?: number;
  latestItem?: EarthquakeItem | null;
}

export function useSeo({ provinceName, activeItem, totalCount, latestItem }: UseSeoOptions) {
  useEffect(() => {
    const origin = window.location.origin;
    let pageTitle = 'Türkiye Deprem İzleme - Canlı AFAD Haritası ve Son Depremler';
    let metaDescription = 'AFAD anlık açık verileriyle Türkiye ve komşu ülkelerdeki son depremleri canlı interaktif harita, derinlik, büyüklük ve revizyon geçmişiyle izleyin.';
    let canonicalUrl = `${origin}/`;

    if (activeItem) {
      const loc = parseItemLocation(activeItem);
      const magVal = activeItem.magnitude?.value !== null && activeItem.magnitude?.value !== undefined
        ? `M${activeItem.magnitude.value}`
        : 'Deprem';
      pageTitle = `${magVal} - ${loc.displayTitle} Depremi | Türkiye Deprem İzleme`;
      metaDescription = `${loc.displayTitle} bölgesinde gerçekleşen ${magVal} büyüklüğündeki depremin koordinatları, odak derinliği (${activeItem.depthKm ?? '-'} km) ve AFAD revizyon verileri.`;
      canonicalUrl = `${origin}/?event=${encodeURIComponent(activeItem.id)}`;
    } else if (provinceName) {
      pageTitle = `${provinceName} Son Depremler - Anlık AFAD Haritası | Türkiye Deprem İzleme`;
      metaDescription = `${provinceName} ve çevresinde son 24 saatte meydana gelen tüm sarsıntılar, AFAD deprem listesi ve canlı fay haritası ile anlık güncellenmektedir.`;
      canonicalUrl = `${origin}/son-depremler/${encodeURIComponent(provinceName.toLowerCase())}`;
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

    // Build Schema.org JSON-LD graph (GEO / AI Crawler standard)
    const jsonLdGraph: any[] = [
      {
        '@context': 'https://schema.org',
        '@type': 'Dataset',
        '@id': `${origin}/#dataset`,
        name: 'Türkiye ve Çevresi Deprem Kataloğu (AFAD)',
        description: 'T.C. İçişleri Bakanlığı AFAD açık veri servislerinden 5 dakikada bir senkronize edilen gerçek zamanlı sismik veri tabanı.',
        url: origin,
        license: 'https://deprem.afad.gov.tr',
        creator: {
          '@type': 'Organization',
          name: 'AFAD Deprem Dairesi Başkanlığı',
          url: 'https://deprem.afad.gov.tr'
        },
        distribution: [
          {
            '@type': 'DataDownload',
            encodingFormat: 'application/json',
            contentUrl: `${origin}/api/v1/earthquakes`
          }
        ]
      },
      {
        '@context': 'https://schema.org',
        '@type': 'SpecialAnnouncement',
        '@id': `${origin}/#announcement`,
        name: 'Türkiye Canlı Deprem Bilgilendirme ve İzleme Platformu',
        text: 'Acil durumlarda AFAD verisiyle eşzamanlı çalışan, internet bağlantısı kopsa dahi PWA ile önbellekten çalışan sismik izleme aracı.',
        category: 'https://schema.org/GovernmentService',
        url: origin
      },
      {
        '@context': 'https://schema.org',
        '@type': 'BreadcrumbList',
        itemListElement: [
          {
            '@type': 'ListItem',
            position: 1,
            name: 'Ana Sayfa',
            item: origin
          },
          ...(provinceName ? [{
            '@type': 'ListItem',
            position: 2,
            name: `${provinceName} Son Depremler`,
            item: canonicalUrl
          }] : [])
        ]
      }
    ];

    if (activeItem) {
      const loc = parseItemLocation(activeItem);
      jsonLdGraph.push({
        '@context': 'https://schema.org',
        '@type': 'Event',
        '@id': `${origin}/?event=${activeItem.id}`,
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
        description: `${loc.displayTitle} bölgesinde M${activeItem.magnitude?.value ?? '-'} büyüklüğünde, ${activeItem.depthKm ?? '-'} km odak derinliğinde AFAD tarafından kaydedilen deprem.`
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
