---
name: "Türkiye Deprem İzleme Haritası"
description: "Güvenilir AFAD Kaynaklı Açık Deprem Kataloğu & İzleme Arayüzü"
colors:
  primary: "#3b82f6"
  primary-hover: "#60a5fa"
  surface: "#0f172a"
  background: "#090d16"
  border: "#1e293b"
  text-main: "#f8fafc"
  text-muted: "#94a3b8"
  accent-green: "#22c55e"
  accent-green-bg: "rgba(34, 197, 94, 0.18)"
  accent-yellow: "#eab308"
  accent-yellow-bg: "rgba(234, 179, 8, 0.18)"
  accent-orange: "#f97316"
  accent-orange-bg: "rgba(249, 115, 22, 0.2)"
  accent-red: "#ef4444"
  accent-red-bg: "rgba(239, 68, 68, 0.22)"
  focus-ring: "#3b82f6"
typography:
  display:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif"
    fontSize: "1.25rem"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "normal"
  headline:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: "normal"
  title:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: "normal"
  body:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "normal"
  label:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: "0.05em"
rounded:
  sm: "4px"
  md: "6px"
  lg: "8px"
  xl: "12px"
  full: "9999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "20px"
  xxl: "24px"
components:
  button-primary:
    backgroundColor: "{colors.focus-ring}"
    textColor: "{colors.surface}"
    rounded: "{rounded.md}"
    padding: "8px 16px"
  button-refresh:
    backgroundColor: "{colors.background}"
    textColor: "{colors.text-main}"
    rounded: "{rounded.md}"
    padding: "6px 12px"
  card-earthquake:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.lg}"
    padding: "12px"
---

# Design System: Türkiye Deprem İzleme Haritası

## Overview

**Creative North Star: "Sismik Radar / Güvenli Gözlemevi"**

Türkiye Deprem İzleme Haritası, acil durum ve sarsıntı sonrasındaki stresli anlarda kullanıcıya sakin, güven veren ve doğrudan doğrulanabilir veri sunan bir gözlemevi arayüzüdür. Tasarım, sansasyonel panik öğelerinden, aşırı parlayan alarmlardan ve göz yoran süslemelerden bilinçli olarak kaçınır. Bunun yerine derin gök mavisi ve teknik nötr zeminler üzerinde, sarsıntı büyüklüğünü (M) ve derinliğini saniyeler içinde netleştiren yüksek kontrastlı görsel göstergeler kullanır.

Sistem, harita ve liste görünümünü eşit derecede birinci sınıf vatandaş olarak ele alır. Harita coğrafi bağlamı aktarırken, liste görünümü klavye erişilebilirliği ve ekran okuyucu uyumu ile her koşulda tam fonksiyonellik sağlar.

**Key Characteristics:**
- **Sakin Ciddiyet:** Derin lacivert (`#1e3a8a`) ve dengeli gri tonları (`#f8fafc`, `#64748b`) panik duygusunu yatıştırır.
- **Anlaşılır Büyüklük Hiyerarşisi:** Deprem büyüklükleri dört aşamalı renk skalası (yeşil, sarı, turuncu, kırmızı) ve açık sayısal tipografiyle ayırt edilir.
- **Şeffaf Veri Durumu:** Resmî erken uyarı sorumluluk reddi ve veri gecikme durumu ekranın en üstünde kesintisiz görünür.
- **Hafif ve Duyarlı:** Aşırı gölge ve ağır animasyonlardan uzak; hızlı yüklenen, mobil cihazlarda kolay parmakla dokunulan düzen.

## Colors

Renk paleti, bilimsel güvenilirlik veren lacivert temel üzerine inşa edilmiş olup, deprem şiddetini temsil eden kontrollü uyarı aksanlarına sahiptir.

### Primary
- **Işıltılı Radar Mavisi** (`#3b82f6`): Başlıklar, logo vurgusu ve marka kimliğini temsil eden birincil otorite rengi.
- **Canlı Mavi Vurgusu** (`#60a5fa`): Buton hover ve etkileşim geçişleri.

### Secondary
- **Radar Mavisi / Odak Rengi** (`#3b82f6`): Seçili kart çerçeveleri, bağlantılar, butonlar ve erişilebilirlik odak halkaları.

### Neutral (Dark Mode)
- **Ana Zemin / Gece Gökyüzü** (`#090d16`): Sayfa kök arka planı ve harita zemin tonu.
- **Panel Yüzeyi** (`#0f172a`): Üst bilgi çubuğu, filtre çubuğu ve liste paneli yüzeyleri (Slate-900).
- **Kart Yüzeyi** (`#131c2e`): Olay kartları ve metrik kutularının yüzeyi.
- **Sınır ve Ayrım Çizgisi** (`#1e293b` / `#27354f`): Paneller, kartlar ve tablolar arası ayrım çizgileri.
- **Ana Metin Beyaz-Arduvaz** (`#f8fafc`): Yüksek kontrastlı ana gövde ve başlık metni.
- **İkincil Metin Grisi** (`#94a3b8`): Zaman damgaları, etiketler ve teknik parametre açıklamaları (WCAG AAA uyumlu).

### Sismik Şiddet Aksanları (Büyüklük Skalası - Koyu Tema)
- **Hafif Sarsıntı (M < 3.0)** (`#22c55e` / Arka plan: `rgba(34, 197, 94, 0.18)`): Rutin ve hissedilmesi zor sarsıntılar için canlı yeşil.
- **Belirgin Sarsıntı (3.0 ≤ M < 4.5)** (`#eab308` / Arka plan: `rgba(234, 179, 8, 0.18)`): Hissedilebilir sarsıntılar için uyarıcı sarı.
- **Kuvvetli Sarsıntı (4.5 ≤ M < 6.0)** (`#f97316` / Arka plan: `rgba(249, 115, 22, 0.20)`): Dikkat çekici turuncu.
- **Yıkıcı / Şiddetli Sarsıntı (M ≥ 6.0)** (`#ef4444` / Arka plan: `rgba(239, 68, 68, 0.22)`): Aciliyet belirten parlak kırmızı.

### Named Rules
**The Double-Coding Rule.** Büyüklük ve durum bilgisi hiçbir zaman yalnızca renkle kodlanamaz; daima sayısal değer (`2.1`) ve metinsel ölçek tipi (`ML`, `Mw`) ile birlikte sunulmalıdır.  
**The Banner Honesty Rule.** AFAD veri akışı 20 dakikadan eski olduğunda sarı, 60 dakikadan eski olduğunda kırmızı durum rozeti gizlenemez veya yumuşatılamaz.

## Typography

Sistem, işletim sistemi yerel font ailesini kullanarak sıfır ek ağ maliyeti ve anında metin render performansı sağlar.

**Display & Body Font:** `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif`  
**Character:** Temiz, modern, son derece net ve küçük boyutlarda dahi rakamları kusursuz ayrıştıran sistem tipi.

### Hierarchy
- **Display** (Bold 700, 1.25rem, 1.2): Ana sayfa başlığı (`Türkiye Deprem İzleme Haritası`).
- **Headline** (Bold 700, 1.125rem, 1.25): Modal pencere başlığı ve kart büyüklük rakamları.
- **Title** (Bold 700, 0.9375rem, 1.3): Deprem yer/ilçe adları (`Soma (Manisa)`).
- **Body** (Regular 400, 0.875rem, 1.5): Bilgilendirme açıklamaları, filtre etiketleri ve genel metin.
- **Label** (SemiBold 600, 0.75rem, 1.2): Durum rozetleri, derinlik göstergeleri ve zaman bilgisi (`TSİ / UTC`).

### Named Rules
**The Tabular Numbers Rule.** Olay zaman damgaları, koordinatlar ve büyüklük değerleri kaymaları engellemek için `font-variant-numeric: tabular-nums` ile dizilir.

## Layout

Arayüz, masaüstünde tam ekran bölünmüş çalışma alanı (Split Panel), mobilde ise sekme kontrollü akıcı bir ızgara kullanır.

- **Masaüstü Yapısı:** Üstte sabit bilgilendirme bandı + filtre çubuğu; solda esnek WebGL haritası (%100 genişlik), sağda sabit 440px genişliğinde kaydırılabilir olay listesi.
- **Mobil Yapısı (≤ 900px):** Üst çubukta "Harita" ve "Olay Listesi" geçiş sekmeleri; ekranı dikeyde bölerek her iki görünümün de ferah kullanılmasını sağlar.
- **Ritim ve Boşluk:** 4px tabanlı ızgara (`4px`, `8px`, `12px`, `16px`, `20px`).

## Elevation & Depth

Sistem, tonal katmanlaşma (tonal layering) ve ince 1px kenarlıklar (`#e2e8f0`) üzerine kuruludur. Derinlik abartılı gölgeler yerine yüzey rengi farkları ve odak çerçeveleriyle aktarılır.

### Shadow Vocabulary
- **Hafif Yüzey Gölgesi** (`0 2px 6px rgba(0, 0, 0, 0.05)`): Olay kartı hover anında hafif yükselme.
- **Harita Lejant Gölgesi** (`0 2px 8px rgba(0, 0, 0, 0.1)`): Yarı saydam harita lejantı ayrımı.
- **Modal Odak Gölgesi** (`0 20px 25px -5px rgba(0, 0, 0, 0.2)`): Deprem detay modalının arka plandan ayrılması.

### Named Rules
**The Border-Over-Shadow Rule.** Kartlar ve bölümler gölgeyle değil, 1px sınır çizgisi (`#e2e8f0`) ve hafif arka plan ayrımıyla sınırlandırılır.

## Shapes

- **Kartlar ve Paneller:** 8px yuvarlatılmış köşeler (`border-radius: 8px`).
- **Butonlar ve Girdiler:** 6px kompakt yuvarlatılmış köşeler (`border-radius: 6px`).
- **Durum Rozetleri:** Tam yuvarlatılmış kapsül form (`border-radius: 9999px`).
- **Modal Pencereleri:** 12px yumuşak köşe bitişleri (`border-radius: 12px`).

## Components

### Buttons
- **Yenile Butonu (Refresh):** Açık gri zemin (`#f1f5f9`), 1px kenarlık, hover durumunda `#e2e8f0`, yükleme esnasında dönen döngü animasyonu.
- **Detay Butonu:** Düz metin bağlantı tarzı (`#2563eb`), tıklandığında modalı açar.
- **Modal Onay Butonu:** Mavi dolgulu birincil aksiyon (`#2563eb`), beyaz metin, 6px köşe yuvarlatma.

### Cards (Earthquake Card)
- **Köşe & Zemin:** Beyaz zemin (`#ffffff`), 8px köşe yuvarlatma, 12px iç boşluk.
- **Seçili Durum:** Açık mavi zemin (`#eff6ff`), 2px mavi çerçeve (`#2563eb`).
- **Odak Durumu (Keyboard Focus):** `box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2)`.

### Inputs & Filters
- **Girdiler:** Açık gri zemin (`#f8fafc`), 1px kenarlık (`#e2e8f0`), tıklandığında beyaz zemin ve 2px mavi odak çizgisi.

### Büyüklük Rozetleri (Mag Badge)
- **Kompakt Kutu:** 48px minimum genişlik, dikeyde değer (`1.125rem` Bold) ve ölçek tipi (`0.625rem` Bold) hiyerarşisi.

## Do's and Don'ts

### Do:
- **Do** Her zaman sarsıntı zamanını TSİ ve UTC olarak açıkça belirtin.
- **Do** Harita üzerinde büyüklüğe göre ölçeklenen çember katmanı ve 0.25 opaklıkta dış ışıma halkası kullanın.
- **Do** Liste kartlarında klavye ile Enter ve Boşluk tuşlarıyla gezinmeyi kusursuz tutun.
- **Do** Eksik büyüklük veya derinlik değerlerinde boşluk bırakmak yerine "Raporlanmadı" ibaresini kullanın.

### Don't:
- **Don't** Kırmızı rengi tek başına hafif sarsıntılar veya genel butonlar için kullanmayın; kırmızı yalnızca kritik büyüklük (≥ 6.0) veya ciddi veri kesintisi içindir.
- **Don't** Haritayı tek etkileşim yolu haline getirmeyin; liste görünümü tam bağımsız çalışabilmelidir.
- **Don't** Resmî kurum izlenimi yaratacak asılsız erken uyarı grafikleri veya siren efektleri eklemeyin.
