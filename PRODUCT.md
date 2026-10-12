# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users
- **Birincil Kullanıcılar:** Türkiye ve yakın çevresinde yaşayan, deprem sarsıntısı hissettiğinde veya son olayları merak ettiğinde güvenilir bilgiye hızlıca ulaşmak isteyen Türkçe konuşan vatandaşlar ve genel kamuoyu.
- **İkincil / Araştırmacı Kullanıcılar:** Deprem büyüklüğü, derinlik, lokasyon revizyonları ve katalog istatistiklerini tarafsız ve kaynak doğrulamalı olarak incelemek isteyen analistler/araştırmacılar.

## Product Purpose
Türkiye ve yakın çevresindeki deprem olaylarını güvenilir resmî kaynaktan (AFAD) alıp şeffaf, normalize edilmiş, gecikme/stale durumu açıkça belirtilmiş bir şekilde etkileşimli harita ve erişilebilir liste üzerinde sunmak. Başarı, kullanıcının hissettiği sarsıntının büyüklük/derinlik bilgisini saniyeler içinde doğru ve net bir dille doğrulamasıdır.

## Positioning
Proje, sansasyonel veya bilimsel dayanağı olmayan tahminler/erken uyarı iddiaları üretmez. Resmî verileri revizyon geçmişiyle saklar, veri gecikmesini dürüstçe gösterir ve boş haritayı asla "deprem olmadı" şeklinde yanıltıcı sunmaz.

## Operating Context
- Mobil ve masaüstü tarayıcılar (özellikle bir sarsıntı sonrası ani yoğunluk ve panik anları).
- Düşük bant genişliği veya mobil bağlantı koşulları.
- Ekran okuyucu veya yalnızca klavye kullanan erişilebilirlik gereksinimleri olan ortamlar.

## Capabilities and Constraints
- **AFAD Kaynak Doğrulaması:** Birincil kaynak AFAD'dır. İzin alınmadıkça Kandilli otomatik veri çekimi yapılmaz.
- **Resmî Uyarı Değildir:** Uygulama asla AFAD yerine resmî acil durum veya erken uyarı hizmeti olarak konumlandırılamaz; net sorumluluk reddi metni zorunludur.
- **Veri Dürüstlüğü:** Olay oluş zamanı ile uygulamanın veriyi aldığı zaman ayrı tutulur. 20 dakikadan eski veriler "gecikmeli", 60 dakikadan eski veriler "eski veri" olarak işaretlenir.
- **Bütçe Kısıtı:** Aylık nakit altyapı bütçesi 0 USD'dir. İstemci taraflı gereksiz yük ve sorgu flood'ları önlenmelidir.
- **Erişilebilirlik:** WCAG 2.2 AA standardı; haritaya bağımlı kalmadan tam fonksiyonel klavye ve liste erişimi.

## Brand Commitments
- **İsim:** Türkiye Deprem İzleme Haritası
- **Ton ve Dil:** Sakin, güven verici, nesnel, teknik ve şeffaf. Sansasyondan ve korku tetikleyici grafiklerden kesinlikle uzak.
- **Atıf:** AFAD ve OpenStreetMap kaynak atıfları arayüzde görünür tutulmalıdır.

## Evidence on Hand
- Canlı AFAD Event Web Service entegrasyonu ve fixture testleri (`src/test/resources/fixtures/afad/`).
- Flyway veritabanı şeması ve revizyon tabanlı `source_report` modeli.
- MapLibre GL JS tabanlı harita ve React + TypeScript bileşenleri (`frontend/src/`).

## Product Principles
1. **Şeffaf ve Dürüst Bilgi:** Kaynak geciktiğinde veya veri bulunamadığında durumu gizleme; net gecikme ve doğrulama durumunu göster.
2. **Sakin ve Net Görsel Hiyerarşi:** Sarsıntı anında bilgi arayan kullanıcının kafasını karıştırma; büyüklük, derinlik ve zamanı en okunabilir şekilde öne çıkar.
3. **Erişilebilirlik Önceliği:** Harita görsel bir araçtır ancak tek seçenek değildir; liste ve klavye akışı eşit derecede güçlü olmalıdır.
4. **Sıfır Maliyet & Hafiflik:** Kaynakları idareli kullan; arka plandaki sekmelerde sorguları durdur ve ETag önbelleklemesini koru.

## Accessibility & Inclusion
WCAG 2.2 AA uyumluluğu. Renk tek başına büyüklük ve derinliği kodlamak için kullanılmaz; metinsel etiketler ve yüksek kontrastlı odak halkaları (focus indicators) sağlanır.
