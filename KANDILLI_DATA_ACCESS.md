# Kandilli (KOERI) verisini uygulamaya ekleme planı

**Belge türü:** Uygulama planı  
**Tarih:** 14 Ekim 2026  
**Durum:** Uygulama öncesi; kod değişikliği değildir  
**Kapsam:** KOERI son depremler listesinden veriyi sunucu tarafında alma, ayrıştırma, kaynak raporu olarak saklama, durumunu izleme ve kullanıcıya kaynak atfıyla sunma. AFAD–KOERI olay eşleştirme algoritmasının ayrıntıları [KANDILLI_MATCHING_IMPLEMENTATION_PLAN.md](KANDILLI_MATCHING_IMPLEMENTATION_PLAN.md) belgesindedir.

## 1. Özet karar

KOERI'nin halka açık güncel olay listesi [Türkçe Son Depremler sayfasıdır](https://www.koeri.boun.edu.tr/scripts/lst0.asp). Sayfa Türkiye ve yakın çevresindeki son 500 kaydı sabit genişlikli bir tablo olarak sunar. Bu liste için belgelenmiş resmî JSON/GeoJSON/CSV olay API'si bulamadım. Bu nedenle uygulanabilir başlangıç yolu, sayfayı **backend'den ölçülü biçimde çekmek**, içeriği doğrulayıp ayrıştırmak ve her kaydı mevcut kaynak raporu modelinde `KOERI` kaynağıyla saklamaktır.

KOERI raporları AFAD kayıtlarının üzerine yazılmaz. Her kaynağın kimliği, revizyonu, magnitüd tipi, zamanı ve koordinatı ayrı kalır. İki raporun aynı olayı gösterip göstermediğine [eşleştirme planı](KANDILLI_MATCHING_IMPLEMENTATION_PLAN.md) karar verir; veri alımı modülü bu kararı kendi başına vermez.

Resmî sayfa veriyi “hızlı çözümler” olarak tanımlar ve yapay sarsıntı analizinin yapılmadığını belirtir. Bu nedenle kullanıcıya KOERI çözüm niteliği gösterilmeli, KOERI verisi AFAD'ın doğrulanmış katalog kaydıyla eşdeğer veya tek ve kesin çözüm gibi sunulmamalıdır. [KOERI Son Depremler](https://www.koeri.boun.edu.tr/scripts/lst0.asp)

## 2. Kaynak araştırması ve sınırlar

### 2.1 Güncel liste

KOERI sayfası tarih, saat, enlem, boylam, derinlik, Md, Ml, Mw, yer ve çözüm niteliği alanlarını gösterir; son 500 olayı listeler. Kaynakta `-.-` gibi eksik alanlar görülebilir. Sabit sütunlu sunum, başlık ve sütun aralıklarında değişiklik olabileceği için parser'ın kırılgan bir HTML CSS seçicisine ya da sabit byte kolonlarına bağlanmaması gerekir. [Türkçe liste](https://www.koeri.boun.edu.tr/scripts/lst0.asp), [İngilizce liste](https://www.koeri.boun.edu.tr/scripts/lasteq.asp)

### 2.2 Makine erişimi

Resmî sayfada bu listeye yönelik belgelenmiş bir olay API'si, API kotası, SLA, rate limit veya önerilen otomatik polling sıklığı bulamadım. KOERI'nin [FDSN veri merkezi kaydı](https://www.fdsn.org/datacenters/detail/KOERI/) waveform (dataselect), station metadata, availability, WFCatalog ve PSD servislerini listeliyor; halka açık olay servisi listede görünmüyor. [WebDC3](https://eida.koeri.boun.edu.tr/) ve [yardım sayfası](https://eida.koeri.boun.edu.tr/webinterface/help.html) arşiv/waveform ve istasyon iş akışlarını açıklar; güncel son deprem listesinin olay API'si olarak kabul edilmemelidir.

Bu yüzden uygulama başlamadan önce sayfanın HTTP cevabı gerçek istemciyle kontrol edilmeli; içerik türü, charset, tabloyu kapsayan bölüm ve yenileme davranışı fixture olarak kaydedilmelidir. İzin verilen otomatik erişim ve makul istek aralığı belirsizse KOERI ile teyit edilmelidir. Bu plandaki polling aralıkları yalnızca başlangıç konfigürasyon önerisidir, KOERI tarafından yayımlanmış bir limit değildir.

### 2.3 Zaman dilimi

İngilizce liste UTC bilgisi verir; Türkçe liste için aynı bilginin açık olduğu teyit edilmeden saatleri UTC kabul etmeyin. Saat semantiği yanlışsa raporlar AFAD ile saat farkı taşır ve eşleştirme yanlış sonuç verebilir. KOERI'den açık teyit alınana veya güvenilir resmî doküman bulunana kadar bu belirsizlik giderilmeden otomatik zaman bazlı eşleştirme açılmamalıdır. Belirsiz timestamp'e makinenin yerel timezone'unu uygulamak da kabul edilebilir fallback değildir.

### 2.4 Kullanım ve atıf

KOERI sayfası verilerin ve haritaların telifinin Boğaziçi Üniversitesi Rektörlüğü'ne ait olduğunu, kaynak gösterilerek kullanılabileceğini ve **ticari amaçlı kullanım için yazılı izin gerektiğini** söylüyor. Bu uygulama ticari olmayacak olsa da arayüzde görünür kaynak atfı ve orijinal liste bağlantısı bulunmalı. Önerilen metin:

> Kaynak: Boğaziçi Üniversitesi Kandilli Rasathanesi ve Deprem Araştırma Enstitüsü, Bölgesel Deprem-Tsunami İzleme ve Değerlendirme Merkezi (KOERI-BDTİM).

Atfı yalnızca uygulamanın hakkında penceresine gizlemeyin; KOERI raporunun gösterildiği detay/listede de görünür tutun. [KOERI kullanım koşulu](https://www.koeri.boun.edu.tr/scripts/lst0.asp). İngilizce sayfa ayrıca bilimsel yayın ve genel amaçlı raporlarda KOERI-RETMC atfını ister. [KOERI Recent Earthquakes](https://www.koeri.boun.edu.tr/scripts/lasteq.asp)

## 3. Mevcut uygulama uyumu

Projede entegrasyon için kullanılabilecek yapı taşları var:

- `SourceReport`, raporları `source + sourceEventId` ile ayırıyor; aynı kaynakta kaynak kimliği benzersiz.
- `SourceReportRevision`, raporun zaman, koordinat, derinlik, magnitüd ve tipini ayrı revizyonlarla saklıyor.
- `NormalizedReportDto` ortak iç veri şekli sunuyor.
- `IngestionService` yeni rapor ve değişen rapor revizyonlarını kaydediyor; fakat normalizer ve ingest metotları şu an AFAD'a göre adlandırılmış/bağlı.
- `AfadApiClient`, `AfadScheduledPoller` ve `application.properties` AFAD'a özgü HTTP ve polling yapılandırmasını içeriyor.
- `SourceStatusTracker` tek kaynağın durumunu tutuyor; `EarthquakeQueryService.getStatus()` ve freshness yanıtı AFAD'a sabit.
- `ArchiveExporterService` arşiv kaynak kapsamını `AFAD` sabit değeriyle bildiriyor.
- Flyway etkin, Hibernate `ddl-auto=validate`; şema değişiklikleri yeni migration gerektiriyor.
- Frontend `EarthquakeItem.source` alanını zaten alıyor; ancak Header, InfoModal, paylaşım metni ve SEO metaverisi AFAD'a özel ifadeler içeriyor.

İlgili dosyalar: `src/main/java/com/ag/deprem/service/AfadApiClient.java`, `AfadDataNormalizer.java`, `AfadScheduledPoller.java`, `IngestionService.java`, `SourceStatusTracker.java`, `EarthquakeQueryService.java`, `ArchiveExporterService.java`; `src/main/resources/application.properties`, `src/main/resources/db/migration/V1__initial_schema.sql`; `frontend/src/types/earthquake.ts` ve `frontend/src/components/`.

## 4. Uygulama ilkeleri

1. **Backend'den çek:** Tarayıcı KOERI'ye doğrudan istek atmasın. CORS, üçüncü taraf erişilebilirliği ve kaynak kullanım kontrolü backend'de olsun.
2. **Kaynağı koru:** KOERI kaydını `KOERI` kaynak koduyla sakla; AFAD raporuna dönüştürme veya üzerine yazma.
3. **Ortak iç model, kaynak özel adapter:** İçe aktarma akışının ortak kısmı `NormalizedReportDto`/ingestion kullanabilir; HTTP ve parse mantığı KOERI adapter'ında kalmalı.
4. **Güvenli hata davranışı:** Fetch/parse hatasında son doğru kayıtları silme veya boş katalog diye sunma. Kaynak stale/error durumuna geçsin, önceki kayıtlar gösterilmeye devam etsin.
5. **Değişiklik görünür olsun:** Tablo formatı beklenmedik biçimde değişirse sessizce yanlış kolonları kaydetme; batch'i reddet, hata ve örnek satırı (gizli/veri boyutu kontrollü) logla.
6. **Nazik istek sıklığı:** Cache/ETag/Last-Modified varsa kullan; sabit sıklığı konfigüre et; hata durumunda exponential backoff ve jitter uygula. Resmî limit varmış gibi davranma.
7. **İzlenebilirlik:** Her kaynak raporunun `sourceEventId`, payload hash'i, normalization ve kaynak içi revizyon geçmişi korunmalı.

## 5. Uygulama iş paketleri

### Faz 0 — Erişim ve saat doğrulaması

1. Türkçe ve İngilizce sayfaları backend'in bulunduğu ortamdan birkaç zaman aralığında isteyin. HTTP status, `Content-Type`, charset, body boyutu, cache başlıkları ve timeout davranışını kaydedin.
2. Gerçek cevapta verinin `<pre>` içinde mi, düz metin mi yoksa HTML tablo mu olduğunu saptayın; browser DOM'undan değil raw HTTP gövdesinden fixture alın.
3. KOERI ile Türkçe listenin saat dilimini ve otomatik istemci erişim/polling beklentisini teyit edin. Teyit yazılı/resmî kaynakla dokümante edilsin.
4. Kaynak kullanım koşullarındaki atıf gereğini ekipçe UI metnine ve veri akışına dönüştürün.

**Çıkış koşulu:** Kaynak formatı, charset, zaman semantiği ve erişim yaklaşımı bilinmeden canlı ingestion açılmaz.

### Faz 1 — KOERI adapter ve parser

Yeni sınıfları AFAD sınıflarıyla aynı adlandırma ve sorumluluk düzeyinde oluşturun:

- `KoeriApiClient` veya `KoeriPageClient`: URL'yi konfigürasyondan alır, timeout ve HTTP başlıklarını uygular, ham body/response metadata döndürür. `RestClient` oluşturulması bir factory/config ile test edilebilir olmalı.
- `KoeriRawEventDto`: tablo satırındaki alanları kayıpsız ve kaynak terminolojisine yakın temsil eder; ör. `date`, `time`, `latitude`, `longitude`, `depthKm`, `md`, `ml`, `mw`, `location`, `solutionQuality`, varsa kaynak satırı/kimliği.
- `KoeriPageParser`: HTTP/charset verisinden olay satırlarını deterministik parse eder. Fetch ve parser ayrı tutulur ki parser offline fixture ile çalışabilsin.
- `KoeriDataNormalizer`: raw kaydı ortak `NormalizedReportDto` şekline getirir; `source="KOERI"` atar ve doğrulama notlarını üretir.
- `KoeriScheduledPoller`: yeni ve son 500 içindeki değişen kayıtları periyodik alıp ortak ingestion hattına gönderir.

Parser tasarım kararları:

- Mümkün olduğunda satırları başlıkların kolon aralıklarından bölmek yerine whitespace kümeleri ve alan formatlarına göre parse edin. Sabit kolon aralığı zorunluysa satır sonu, Unicode karakter ve format varyantlarını kapsayan testlerle koruyun.
- Tarih, saat ve koordinatlar beklenen biçime uymuyorsa satırı sessizce kısmi kaydetmeyin. Hatalı satır sayısı/örneğini raporlayın; tüm tablo parse edilemezse batch'i başarısız sayın.
- `-.-`, boş değer ve varsa diğer sentinel değerler `null` olur.
- KOERI'nin üç magnitüd tipi varsa bunları ayrı tutun. Mevcut ortak `NormalizedReportDto` tek `magnitudeValue`/`magnitudeType` taşıyor. Kaynak raporun bir kanonik büyüklük seçme kuralı ürün kararıdır. Öneri: katalogda ana gösterim için Mw varsa Mw, yoksa Ml, yoksa Md seçmek; seçilen türü `magnitudeType` içinde korumak ve tüm özgün Md/Ml/Mw değerlerini ham payload veya genişletilmiş kaynak-özel payload içinde saklamak. Bu öncelik KOERI'nin bilimsel eşdeğerlik kuralı değildir; ekip onayı olmadan kesin büyüklük gibi sunulmasın. Daha doğru seçenek, revizyon şemasına magnitüd ölçümleri koleksiyonu eklemek.
- KOERI satırında açık ve kararlı olay kimliği yoksa kimliği deterministik kuralla üretin. Önerilen bileşik anahtar: normalize edilmiş kaynak timestamp metni + koordinatların kaynak hassasiyetinde gösterilen değerleri + konum metni/çözüm kimliği. Hash sadece bu kanonik bileşik anahtarın depolama biçimi olabilir; zamanla kayan koordinatı yuvarlayıp farklı kayıtları çakıştırmayın. Aynı timestamp/koordinatla iki olay olasılığı fixture/örneklerle incelenmeli.
- Kaynak olayı son 500 listesinden düştüğünde kaydı silmeyin; bu yalnızca polling listesinin kapsam sınırıdır, kaydın iptal edildiği anlamına gelmez.
- HTML içeriğini UI'a ham olarak yansıtmayın; konum metnini sanitize edin. Parser loglarında tüm sayfayı veya sınırsız satırları yazdırmayın.

### Faz 2 — Normalizasyon ve ingest

- `IngestionService.ingestAfadEvents` gibi kaynağa özel yöntem yerine ortak `ingestReports(source, normalizedReports)` akışına geçişi değerlendirin. Eğer bu aşamada genel arayüz çıkarmak kapsamı gereksiz büyütüyorsa, geçici olarak `ingestKoeriEvents` ekleyip ortak kalıcılaştırma mantığını tek bir private/ortak yöntemde tutun.
- Kaynak kodu normalizer'dan gelişigüzel kabul edilmemeli; adapter'ın sabit kaynağıyla doğrulanmalı.
- KOERI `sourceEventId` aynı kalırken zaman/konum/büyüklük değişirse yeni `SourceReportRevision` üretilmeli. Payload hash'i değişip normalize içerik değişmediyse gereksiz revizyon oluşturulmamalı.
- Polling aynı 500 satırı tekrar getirince insert/revision kopyaları oluşmamalı.
- Bir batch'te bazı satırlar bozuk diye diğer geçerli satırlar güvenli biçimde ingest edilebilir; fakat format kaymasıyla geniş çaplı yanlış parse tespit edilirse batch'i kısmi başarı olarak gizlemeyin. Kabul eşiği konfigüre ve gözlenebilir olmalı.
- Yeni rapor alımı ile eşleştirme (ileriki faz) hata durumunda raporun kaydedilmesini engellememeli. Eşleştirme yeniden denenebilir iş olmalı.

### Faz 3 — Config, zamanlama ve HTTP dayanıklılığı

`application.properties` için AFAD ayarlarından ayrı KOERI ayarları ekleyin; örnek isimlendirme:

```properties
koeri.page.url=${KOERI_PAGE_URL:https://www.koeri.boun.edu.tr/scripts/lst0.asp}
koeri.sync.enabled=${KOERI_SYNC_ENABLED:false}
koeri.sync.interval-ms=${KOERI_SYNC_INTERVAL_MS:600000}
koeri.http.connect-timeout-ms=${KOERI_CONNECT_TIMEOUT_MS:10000}
koeri.http.read-timeout-ms=${KOERI_READ_TIMEOUT_MS:20000}
koeri.sync.initial-delay-ms=${KOERI_INITIAL_DELAY_MS:15000}
```

Örnek interval, resmî rate limit değildir. `10` dakika AFAD'ın mevcut poll aralığına denk düşen yalnızca başlangıç taslağıdır; KOERI ile teyit ve operasyon gözleminden sonra seçilmelidir. Varsayılan `enabled` için yerel geliştirme/izin kararı belirlenmeli; ilk dağıtımda false veya feature flag ile kapalı başlatıp kontrollü açmak önerilir.

HTTP istemcisi:

- connect/read timeout zorunlu; sınırsız bekleme yok.
- Retry yalnızca geçici ağ hatası, 429 ve uygun 5xx için; sabit, yoğun retry yok. `Retry-After` varsa saygı gösterin.
- 4xx/429/5xx durumunda son başarılı snapshot ve DB kayıtları korunur.
- Koşullu istek başlıkları (`If-None-Match`, `If-Modified-Since`) sadece kaynak cevapları destekliyorsa uygulanır.
- Polling thread'i uzun süre takılı kalırsa aynı kaynağa paralel fetch başlatmayın; tek uçuş kilidi veya scheduled job tekil çalışma güvencesi kullanın.
- Uygulama instance sayısı birden fazlaysa her instance'ın aynı poll'u yapmaması için dağıtık scheduler lock/leader ya da merkezi job mekanizması gerekir.

### Faz 4 — Kaynak durumu ve gözlemlenebilirlik

`SourceStatusTracker` tek ortak durum tuttuğundan kaynak bazlı duruma çevrilmeli; örneğin `SourceStatusRegistry` içinde `AFAD` ve `KOERI` anahtarlarıyla tracker saklanmalı. Mevcut `StatusResponseDto` zaten `sources` listesi taşıyor; `EarthquakeQueryService.getStatus()` şu an tek AFAD kaydı oluşturuyor. KOERI için ayrı source status ekleyin.

Her kaynak için izlenecekler:

- son deneme ve son başarılı fetch zamanı;
- status (`current`, `delayed`, `stale`, `error`, ilk kurulumda `unknown` gibi mevcut sözleşmeyle uyumlu durumlar);
- HTTP status, fetch süresi, alınan satır sayısı, parse edilen/atlanmış/reddedilmiş kayıt sayısı;
- parser format hataları ve payload hash/ETag değişimleri;
- son başarılı snapshot'taki satır sayısı ve kapsama sınırı (`last 500`, tarih aralığı tam katalog kapsamı değildir);
- son hata mesajı kullanıcıya güvenli biçimde özetlenmiş.

`FreshnessDto` şu an tek `source` alanı alıyor. API'nin mevcut sözleşmesini bozmayın: ya freshness kaynak bazlı listeye genişletin, ya da kullanıcıya tazelik göstergesi gereken yerde source status endpoint/DTO'sunu kullanın. “Son çekim başarılı” bilgisini “katalog tüm olayları kapsıyor” diye yorumlamayın.

Loglarda kullanıcı/operatör teşhisi için kaynak ve hata türü olsun; body'nin tamamı loglanmasın. Metrikler üzerinden hata artışı, parse başarısızlığı ve kaynak stale kalma durumu izlenmeli. Bir poll başarısızlığı global API sağlığını otomatik down yapmamalı; ayrı source degraded durumu göstermeli.

### Faz 5 — API, arayüz, metin ve atıf

- `EarthquakeItem.source` frontend'de zaten mevcut; kaynak satırı/marker'ında `KOERI` için anlaşılır “Kandilli” etiketi gösterin, teknik sabit kodu kullanıcıya göstermeyin.
- KOERI kaydının detayında kaynak atfı ve KOERI listesine bağlantı görünür olmalı.
- Büyüklük türü açık gösterilmeli (ör. `ML 2.4`, `MW 3.1`); yalnız sayı gösterip kaynak büyüklük tipini atmayın.
- Çözüm niteliği (`İlksel` vb.) ham kaynaktan normalize edilip kullanıcıya aktarılmalı; bilinmeyen değerler bozulmadan ama güvenli biçimde sunulmalı.
- Header, InfoModal, SEO başlık/açıklama, paylaşım metinleri, manifest ve açıklama alanlarında AFAD tek kaynak izlenimi veren ifadeler çoklu kaynak yapısına göre güncellenmeli. Kaynak otoritesini yanlış ima etmeyin; uygulamanın bağımsız olduğunu belirtin.
- AFAD'a özel mesafe/konum hesaplamaları ya kaynakla etiketli kalmalı ya da gerçekten genel hale getirilmeli; KOERI koordinatını sessizce AFAD koordinatı gibi değerlendirmeyin.
- Filtreler magnitude type bazında kaynaklar arasında karşılaştırılabilir olmayabilir. UI'da farklı tiplerin karşılaştırılabilirliği açık değilse tür filtresi kaynak raporu değerine göre uygulanmalı ve bu davranış API dokümanında açıklanmalı.
- CORS, public frontend'den KOERI'ye erişim veya browser referrer gerektirmemeli; tek veri yolu uygulama backend'idir.

### Faz 6 — Migration, archive ve retention

- Mevcut `source` kolonu 32 karakter, `source_event_id` 64 karakter; `KOERI` kodu uzunluklara sığıyor.
- Parser'ın saklaması gereken tüm alanlar `SourceReportRevision`'da mevcut değil: KOERI `solutionQuality` ve Md/Ml/Mw'nin üçünü birden tutma ihtiyacı değerlendirilmelidir. Sadece bir büyüklük gösterilecekse bile ham payload erişilebilirliği korunmalı. Gerekirse yeni migration ile source-specific observation tablosu veya `raw_payload_ref` kullanımı tanımlanmalı.
- `V1__initial_schema.sql` değiştirilmemeli; Flyway'in bir sonraki boş sürüm numarasıyla ileri migration ekleyin. Hibernate `ddl-auto=validate` olduğundan entity/schema senkronizasyonu zorunlu.
- `ArchiveExporterService` içindeki `SOURCE_COVERAGE = "AFAD"` sabiti çoklu kaynak kapsamı belirtecek şekilde değişmeli. Manifest formatının tek string yerine kaynak listesi/coverage map gerektirip gerektirmediğine karar verin ve arşiv `schemaVersion` artırın.
- Retention ve export öncesi arşiv süreci KOERI raporlarını da kapsamalı. Kaynak rapor silinmesi eşleşme kararlarıyla ilişkiyi bozmasın; archive/export test verisi KOERI'yi içersin.
- Geçmiş KOERI verisini geriye dönük doldurmak mevcut 500 kayıt listesinden mümkün değildir. Tarihsel kataloğa ihtiyaç doğarsa KOERI'den uygun erişim ve izin ayrıca istenmelidir; scraping ile eski arşiv varmış gibi davranmayın.

## 6. Test ve doğrulama planı (uygulama sırasında)

Parser fixture'ları gerçek, kişisel olmayan kaynak cevabından alınmalı; testler ağa çıkmamalı. Aşağıdaki durumları kapsayın:

### Parser ve normalizer

- normal satır, farklı çözüm niteliği ve üç büyüklük tipi;
- `-.-`, boş magnitüd/derinlik;
- ondalık ayırıcı, negatif/limit dışı koordinat, hatalı sayı;
- Unicode/Türkçe karakter ve UTF-8/charset varyasyonu;
- başlık/yatay ayraç/footer, boş liste;
- satırdaki kolon kayması, fazladan boşluk ve yeni/bozuk format;
- bozuk tarih/saat ve saat dilimi kuralı;
- aynı kaynak olayı için kararlı ve çakışmayan `sourceEventId` üretimi;
- HTML injection içerebilecek konum metni ve log güvenliği.

### Client/poller ve ingest

- timeout, DNS/connection failure, 304, 429 + Retry-After, 5xx, 4xx, HTML error page;
- retry sonrası başarı, tüm retry'lar başarısız ve başarısızken önceki başarılı verinin korunması;
- poll tekrarlandığında duplicate oluşmaması;
- kaynak kaydı değişince yeni revision; normalize veri değişmediyse gereksiz revision olmaması;
- parser'ın tüm gövdeyi yanlış anlaması halinde başarı durumunun güncellenmemesi;
- aynı kaynağa eşzamanlı scheduler girişinin engellenmesi.

### API/UI/migration/archive

- status endpoint'te AFAD ve KOERI durumlarının birbirinden bağımsız olması;
- KOERI kaydının DTO'su, kaynak etiketi, büyüklük tipi, çözüm niteliği ve görünür atıf;
- eski frontend/API istemcilerinin bozulmaması;
- Migration'ın H2/yerel ve PostgreSQL üretim şemalarında uygulanması; `ddl-auto=validate` geçişi;
- arşiv kaynak kapsamı, manifest schema version, retention ve export öncesi koruma;
- KOERI stale/offline iken AFAD verisinin ve tazeliğinin normal kalması.

## 7. Devreye alma sırası ve geri alma

1. Parser/normalizer/ingest testleri ve fixture'lar tamamlanır; dış kaynağa giden test kullanılmaz.
2. Migration ve KOERI kaynak status alanları dağıtılır; KOERI polling kapalı tutulur.
3. Kontrollü ortamdaki tek instance üzerinden polling açılır; request hacmi, sayfa formatı, parse oranı ve tazelik gözlemlenir.
4. İlk aşamada veri yalnız arka uçta/inceleme amaçlı görünür; kaynak atfı ve çoklu kaynak UI metinleri hazır olmadan kullanıcıya açılmaz.
5. Atıf, freshness, parse alarmı ve arşiv kapsamı doğrulandıktan sonra KOERI UI görünümü etkinleştirilir.
6. Daha sonra eşleştirme planındaki gözlem modu ve aday değerlendirmesi başlatılır; eşleştirme otomatik kararının açılması bu veri kaynağı entegrasyonundan ayrı bir rollout kararıdır.

Rollback durumunda `KOERI_SYNC_ENABLED=false` ile polling kapatılır ve frontend'de KOERI gösterimi feature flag ile gizlenir. Saklanmış KOERI raporları ve revizyonları sırf polling kapatıldı diye silinmez. Veri kaybı yaratacak schema geri dönüşü otomatik yapılmaz; migration değişikliği backup ve ileri düzeltme migration'ı ile ele alınır. Parser formatı bozulursa yanlış veri ingest etmek yerine kaynağı stale/error işaretleyip son başarılı snapshot'ı koruyun.

## 8. Bitti sayılma ölçütleri

- KOERI fetch işlemi yalnız backend'den ve konfigüre edilmiş, gözlenebilir aralıkta yapılır.
- Kaynak saati ve formatı doğrulanmış veya belirsizlik açıkça bloklanmış durumda.
- Parser gerçek fixture'larıyla deterministik; bozuk içerik yanlış alanları sessizce kaydetmiyor.
- KOERI raporu `source=KOERI` ile, idempotent ve revizyon geçmişi korunarak saklanıyor.
- AFAD verisi ve tazeliği KOERI fetch hatasından etkilenmiyor.
- Kullanıcı KOERI hızlı çözüm niteliğini, büyüklük türünü ve görünür kaynak atfını görüyor.
- Status, logs/metrics, archive, retention ve migration çoklu kaynağı destekliyor.
- Polling kapatılarak hızlı geri dönüş mümkün; önceki veriler ve karar geçmişi korunuyor.
- Eşleştirme algoritması ayrı plan doğrultusunda kalibre edilmeden kaynak kayıtları tek olay olarak otomatik birleştirilmiyor.

## 9. Sıralı dosya değişikliği alanı

Uygulamaya geçildiğinde beklenen başlıca dosyalar:

1. `src/main/java/com/ag/deprem/model/dto/KoeriRawEventDto.java` — kaynak satırı DTO'su.
2. `src/main/java/com/ag/deprem/service/KoeriPageParser.java` — saf, fixture ile test edilebilir parser.
3. `src/main/java/com/ag/deprem/service/KoeriDataNormalizer.java` — ortak rapor DTO'suna dönüşüm ve kalite notları.
4. `src/main/java/com/ag/deprem/service/KoeriApiClient.java` — HTTP/timeout/retry/header yönetimi.
5. `src/main/java/com/ag/deprem/service/KoeriScheduledPoller.java` — periyodik ingestion.
6. `IngestionService.java`, `SourceStatusTracker.java`, `EarthquakeQueryService.java`, `StatusResponseDto.java` — ortak ingest ve kaynak bazlı durum.
7. `application.properties` — KOERI URL, enable flag, interval, timeout ve retry ayarları.
8. `src/main/resources/db/migration/V2__...sql` — domain kararı gerektiren source-specific field/metadata; V1'i değiştirmeden.
9. `ArchiveExporterService.java` ve archive DTO/schema — çoklu kaynak kapsamı ve sürüm.
10. Frontend `types`, list/map/detail/header/info modal, SEO ve PWA metinleri — kaynak gösterimi ve AFAD'a sabitli metinler.
11. `src/test/resources/fixtures/koeri/` ve backend testleri — ham sayfa fixture'ları ve parser/poller/ingest/status doğrulamaları.

## 10. Açık kararlar (implementasyondan önce)

1. Türkçe KOERI sayfasının saat dilimi KOERI tarafından nasıl teyit edilecek? Bu, eşleştirme için kritik ön koşuldur.
2. KOERI'nin son 500 satırındaki kalıcı bir olay ID'si var mı? Yoksa bileşik `sourceEventId` kuralı fixture ve canlı gözlemle doğrulanmalı.
3. AFAD/ortak model tek magnitüd alanı sunuyor. KOERI'nin Md, Ml ve Mw değerlerinin tümünü saklamak için revizyon şeması mı genişletilecek, yoksa kaynak payload referansı mı kullanılacak? Öneri, özgün üç değeri kaybetmemektir.
4. Otomatik istemci erişimi ve polling sıklığı için KOERI'den izin/öneri alınacak mı? Resmî sitede bir kota/SLA/poll aralığı bulamadım.
5. Ürün ticari olmayacak olsa bile görünür atıf metni ve kaynak bağlantısı hangi UI noktalarında gösterilecek? Bu plan liste, harita/detay ve kaynak bilgi penceresini kapsayacak şekilde önerir.

## Kaynaklar

- [KOERI Türkçe Son Depremler](https://www.koeri.boun.edu.tr/scripts/lst0.asp) — son 500 hızlı çözüm listesi, atıf ve kullanım koşulları.
- [KOERI İngilizce Recent Earthquakes](https://www.koeri.boun.edu.tr/scripts/lasteq.asp) — İngilizce liste, UTC ibaresi ve KOERI-RETMC atfı.
- [KOERI EIDA/WebDC3](https://eida.koeri.boun.edu.tr/) ve [WebDC3 yardımı](https://eida.koeri.boun.edu.tr/webinterface/help.html) — waveform/istasyon arşivi işlevleri.
- [FDSN KOERI Data Center kaydı](https://www.fdsn.org/datacenters/detail/KOERI/) — kayıtlı FDSN servislerinin kapsamı.
- Proje içi eşleştirme planı: [KANDILLI_MATCHING_IMPLEMENTATION_PLAN.md](KANDILLI_MATCHING_IMPLEMENTATION_PLAN.md).
