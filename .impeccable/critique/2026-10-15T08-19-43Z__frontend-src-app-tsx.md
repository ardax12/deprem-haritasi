---
target: frontend/src/App.tsx
total_score: 33
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 2
target_identity: "file:C:\\Users\\LENOVO\\IdeaProjects\\deprem\\frontend\\src\\App.tsx"
target_fingerprint: "sha256:f21420e4055d156ea9858902b27fd444b2610f9015a96f15eb87e2485a7e876d"
target_path: "C:\\Users\\LENOVO\\IdeaProjects\\deprem\\frontend\\src\\App.tsx"
timestamp: 2026-10-15T08-19-43Z
slug: frontend-src-app-tsx
closed: true
---
Method: dual-agent (A: 25dc996a6f4990b1fc6d90668bb542b0 · B: e1acde5e93a5d0cf50ea09220c3631bf)

# Design Health Score
Score: 33/40 (Good / Production-Grade)
Target: frontend/src/App.tsx

| # | Heuristic | Score | Key Issue |
|---|-----------|:-----:|-----------|
| 1 | Visibility of System Status | 4 | Kusursuz canlı telemetri; veri tazelik hapları, senkronizasyon zamanı, PWA çevrimdışı bildirimleri eksiksiz. |
| 2 | Match System / Real World | 4 | Türkiye yerel terminolojisine tam uyum (İl/İlçe/Mesafe, Odak Derinliği, sismik büyüklük parametreleri). |
| 3 | User Control and Freedom | 3 | URL üzerinden derin bağlantı (`?event=`) ve geri tuşu desteği mevcut; ancak mobilde kart tıklandığında harita sekmesine zorunlu geçişte geri dönüş butonu eksik. |
| 4 | Consistency and Standards | 3 | Sismik renk kodları tutarlı; ancak `AiSummarySnippet`, 7 veya 30 günlük filtrelerde dahi "24s Özet" başlığını koruyarak tutarsızlık yaratıyor. |
| 5 | Error Prevention | 3 | 350ms debounce ve iptal denetleyicileri yarış durumlarını önlüyor; konum aramasında il/ilçe için otomatik tamamlama eksik, yazım hatalarında boş sonuç dönüyor. |
| 6 | Recognition Rather Than Recall | 3 | Hızlı filtre çipleri ve lejant tanınabilirliği yüksek; haritadaki daire işaretçilerinin içinde büyüklük rakamı yazmadığı için renk haricinde değer ayırt edilemiyor. |
| 7 | Flexibility and Efficiency | 3 | Hem acil kullanıcılar hem de analistler için veri katmanları mevcut; arama odağı veya kart geçişleri için klavye kısayolları (`/`, `Esc`) bulunmuyor. |
| 8 | Aesthetic and Minimalist Design | 3 | Koyu tema panik hissini önlüyor; ancak ekranın üst kısmında art arda yığılan 5 yatay bant (uyarı, başlık, çipler, filtreler, AI özet) harita ve listeyi aşağı itiyor. |
| 9 | Error Recovery | 3 | Hata ve boş durumlarda yönlendirici kurtarma butonları ("Filtreleri genişlet", "Tekrar dene") mevcut. |
| 10 | Help and Documentation | 4 | `InfoModal` içinde 4 sekmeli kapsamlı rehber: Veri kaynakları, ölçek türleri, SSS ve Hayat Kurtaran Güvenlik Bilgileri. |
| **Toplam** | | **33/40** | **Good (İyi - Üretim Seviyesi Temel)** |

---

## Design Specificity Verdict

- **LLM Değerlendirmesi**: Arayüz genel geçer bir harita şablonu değil; tamamen Türkiye sismoloji gerçekliğine ve Türk vatandaşlarının deprem anındaki reflekslerine göre tasarlanmış. `parseItemLocation` ile deniz aşırı ve göl içi sarsıntılarının en yakın anakaraya göre mesafelenmesi (`Bodrum 14 km açıkta`, `Van Gölü İçi`), AFAD ve Kandilli arasındaki kurumsal çift kaynak doğrulaması ve TSİ/UTC zaman damgası derin bir alan aidiyeti sunuyor.
- **Deterministik Tarama**: `impeccable detect` motoru `frontend/src/App.tsx` ve `frontend` ağacı üzerinde `0` kural ihlali buldu. Tipografi tokenları, renk kontrast oranları ve CSS taşma kuralları temiz.
- **Görsel Katmanlar (Visual Overlays)**: Test ortamında etkileşimli tarayıcı otomasyonu bulunmadığından canlı tarayıcı içi DOM enjeksiyonu atlandı; deterministik CLI bulguları ve kaynak kodu analizi esas alındı.

---

## Overall Impression
Sakin, güven veren koyu tema paleti ve iki kurumun (AFAD & Kandilli) verilerini uzlaştıran altyapısı ile teknik açıdan kusursuz bir kriz izleme aracı. En kritik darboğaz; ekranın üst kısmında art arda yığılan yatay bilgilendirme ve filtre bantlarının masaüstü ve mobilde asıl içerik olan harita ve son deprem listesini ekranın altına itmesidir.

---

## What's Working
1. **Psikolojik Güven Veren Renk ve Bilgilendirme Dili**: Kırmızı sirensi/panik yaratan efektlerden kaçınılmış, koyu arduvaz zemin ile resmi sismik gözlemevi ciddiyeti yakalanmış. "Sarsıntı mı hissettiniz? İlk çözümler 2-5 dakika içinde yansır" tavsiye bandı acil durumlarda kullanıcının kaygısını düşüren olağanüstü bir empati detayı.
2. **Çift Kurum ve Revizyon Şeffaflığı**: AFAD ile Kandilli arasındaki ilk çözüm farklılıklarını gizlemek yerine yan yana sunması, detay modalında revizyon loglarını vermesi bilgi kirliliğini önlüyor.
3. **Erişilebilirlik ve Tabular Rakamlar**: Rakamların kaymasını önleyen `tabular-nums` dizilimi, net kontrast oranları ve PWA çevrimdışı desteği altyapıyı sağlamlaştırıyor.

---

## Priority Issues

### [P1] Üst Bilgi Bantlarının Aşırı Yığılması (Vertical Banner Stack)
- **Sorun**: `App.tsx` üzerinde: (1) Yasal Sorumluluk Reddi $\rightarrow$ (2) Ana Başlık $\rightarrow$ (3) Çevrimdışı/Hata Bandı $\rightarrow$ (4) Hızlı Filtre Çipleri $\rightarrow$ (5) Filtre Dropdown Çubuğu $\rightarrow$ (6) AI Özet Bandı art arda dizilerek dikeyde ~260-320px alan tüketiyor.
- **Neden Önemli**: Özellikle dizüstü bilgisayarlarda ve mobil ekranlarda harita ve en son gerçekleşen deprem kartı ekranın altına itiliyor; acil durumda kullanıcının ilk saniyede sarsıntıyı görmesini zorlaştırıyor.
- **Çözüm**: Yasal uyarıyı başlık yanına kompakt bir bilgi ikonuna veya modalına taşıyın; filtre çipleri ile filtre çubuğunu tek bir kompakt araç çubuğunda birleştirin; AI özetini liste başlığına entegre edin.
- **Önerilen Komut**: `/impeccable layout` veya `/impeccable distill`

### [P1] AiSummarySnippet Filtre Senkronizasyon Uyumsuzluğu
- **Sorun**: `AiSummarySnippet.tsx`, kullanıcı filtre çubuğundan "Son 7 Gün" veya "Son 30 Gün" seçse bile sabit olarak "24s Özet" ve "son 24 saatte..." metnini gösteriyor.
- **Neden Önemli**: Kullanıcı 30 günlük veriyi incelerken rozetin "24s" yazması veriye olan güveni sarsıyor ve kafa karışıklığı yaratıyor.
- **Çözüm**: Metni ve başlığı seçili `filters.timeRange` durumuna göre dinamik hale getirin ("Son 7 Günlük Özet", "Son 30 Günlük Özet").
- **Önerilen Komut**: `/impeccable clarify`

### [P2] Harita İşaretçilerinde Sayısal Büyüklük Değerinin Olmaması
- **Sorun**: MapLibre GL haritasındaki daireler renklendirilmiş ancak üzerlerinde `4.2`, `5.8` gibi sayısal büyüklük etiketleri yer almıyor.
- **Neden Önemli**: Kullanıcı bir sarı halkanın $M 3.1$ mi yoksa $M 4.4$ mü olduğunu, veya kırmızı bir noktanın $M 6.0$ mı $M 7.7$ mi olduğunu tıklamadan ayırt edemiyor.
- **Çözüm**: Belli bir zoom seviyesinden itibaren ($\ge 6$) dairelerin merkezine sayısal büyüklük etiketini gösteren bir MapLibre Symbol katmanı ekleyin.
- **Önerilen Komut**: `/impeccable bolder`

### [P2] Konum Filtresinde Şehir Otomatik Tamamlamasının Olmaması
- **Sorun**: Konum arama alanı serbest metin kutusudur; Türkiye'nin 81 ili için açılır öneri listesi (autocomplete/typeahead) veya toleranslı arama sunmuyor.
- **Neden Önemli**: Panik anındaki bir vatandaş "Antep" yazdığında veya Türkçe klavye kullanmayıp "Izmir" yazdığında eşleşme bulunamayarak sıfır sonuç çıkabiliyor.
- **Çözüm**: `provinces.ts` listesini bir `<datalist>` veya typeahead bileşeni ile bağlayarak ilçe/il eşleştirmesini destekleyin.
- **Önerilen Komut**: `/impeccable optimize`

### [P3] Kartlardaki Teknik Eşleşme Terminolojisi
- **Sorun**: Liste kartlarında sıradan kullanıcı için anlamsız olan `AUTO_MATCHED` veya `Revizyon #2` gibi ham etiketler yer alıyor.
- **Neden Önemli**: İlk defa kullanan vatandaşlar veritabanı eşleşme terimlerini anlamakta zorlanabilir.
- **Çözüm**: `AUTO_MATCHED` yerine `✓ AFAD & Kandilli Doğrulandı` gibi güven veren sade bir rozet kullanın.
- **Önerilen Komut**: `/impeccable quieter`

---

## Persona Red Flags

- **Jordan (Depremi Hisseden Panik Halindeki Vatandaş)**: Sarsıntıyı hisseder hissetmez telefonunu açtığında yasal sorumluluk reddi, bağlantı durumu, filtre butonları ve açılır kutularla karşılaşıyor. Bursa'da ne olduğunu öğrenmek için birkaç saniye kaydırma yapmak zorunda kalıyor. Tek dokunuşla "Son Sarsıntılar" cevabını alabilmelidir.
- **Alex (Sismoloji Meraklısı / Araştırmacı)**: Detay modalındaki çoklu kurum karşılaştırma tablosunu takdir ediyor; fakat ana filtre çubuğunda "Yalnızca Kandilli" veya "Yalnızca İki Kurumca Doğrulanmışlar" filtresi bulamıyor.
- **Sam (Ekran Okuyucu / Klavye Bağımlı Kullanıcı)**: Klavye ile gezinirken üst üste dizilmiş 4 çip ve ardından 4 açılır kutu nedeniyle her filtre değişiminde çok sayıda Tab tuşuna basmak zorunda kalıyor; arama alanına hızlı erişim için kısayol tuşu bulunmuyor.

---

## Minor Observations
- Tabular rakamlar (`tabular-nums`) koordinat, saat ve büyüklük değerlerinde kusursuz uygulanmış; veri yenilendiğinde layout kayması olmuyor.
- Mobildeki dokunma hedefleri 44px erişilebilirlik standardına tam uyumlu.
- PWA yükleme daveti kullanıcıyı engellemeden zarif bir şekilde konumlandırılmış.

---

## Questions to Consider
1. *Kullanıcı deprem anında uygulamayı açtığında neden Harita ve Liste arasında sekme seçmek zorunda kalsın? Mobilde en son hissedilen kritik sarsıntı alt panelde (bottom sheet) her iki sekmede de sabit gösterilemez mi?*
2. *AI Özet bandı ayrı bir üst kutu olmak yerine, deprem akışının en tepesinde interaktif bir doğal dil durum kartı olarak eritilebilir mi?*
3. *Kullanıcının tarayıcı konum izniyle (veya IP tahminiyle) bulunduğu ile göre "Yakınınızdaki Hareketlilik" başlığı otomatik filtrelenebilir mi?*
