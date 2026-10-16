# AFAD–Kandilli olay eşleştirme uygulama planı

**Durum:** Uygulama öncesi plan  
**Tarih:** 14 Ekim 2026  
**Kapsam:** AFAD ve Kandilli (KOERI) kaynak raporlarının aynı fiziksel depremi bildirip bildirmediğini değerlendirmek; kaynak raporlarını, revizyonları ve eşleştirme kararlarını kayıpsız ve izlenebilir biçimde sunmak.

Bu belge bir uygulama planıdır; eşleştirme algoritmasını veya veri sağlayıcı entegrasyonunu henüz uygulamaz. Kandilli erişim bulguları için [KANDILLI_DATA_ACCESS.md](KANDILLI_DATA_ACCESS.md) notuna bakın.

## 1. Amaç ve temel kararlar

AFAD ve KOERI aynı depremi farklı zaman, koordinat, derinlik veya büyüklükle raporlayabilir. Ayrıca kısa aralıkta, birbirine yakın iki ayrı deprem olabilir. Bu yüzden yalnızca zaman/mesafe yakınlığına bakarak iki raporu birleştirmek yanlış birleştirme riski taşır.

Uygulamanın hedefi:

- Her kaynağın özgün raporunu ve kaynak içi revizyonlarını ayrı saklamak.
- Kaynak raporları arasında eşleşme **adaylarını**, otomatik kararları ve gerektiğinde insan kararını ayrı bir katmanda tutmak.
- Yalnızca güçlü ve çelişkisiz eşleşmeleri arayüzde aynı olay altında göstermek.
- Belirsizlikte iki olayı ayrı göstermeyi tercih etmek; yanlış birleştirmeyi yanlış ayırmadan daha maliyetli kabul etmek.
- Eşleşme kararını gerekçesi, algoritma sürümü ve kullanılan rapor revizyonlarıyla tekrar üretilebilir kılmak.

### İlk sürüm için model kararı

`SourceReport` kaynak raporu olmaya devam eder. Kaynak raporları hiçbir zaman tek bir raporda birleştirilmez. `EventMatch` ikili rapor ilişkisi/kararı olarak genişletilir; aynı kaynakta birden fazla raporun tek olaya bağlanabilmesi ihtimali nedeniyle, arayüzde gruplanmış olayı temsil etmek üzere ayrıca bir `SeismicEvent` (veya eşdeğer `EventGroup`) ve üyelik tablosu eklenir.

Bir eşleşme adayı, kabul edilmiş eşleşme ve kullanıcı/operatör kararı farklı kavramlardır; ayrı durumlarla temsil edilir. Aday kenarları tek başına arayüzde birleştirme yapmaz. Kabul edilen kümeler aynı kaynaktan iki ayrı raporu otomatik olarak tek fiziksel olay gibi gösteremez; böyle bir küme manuel inceleme ister. Transitif zincirleme (A–B ve B–C eşleşti diye A–C'nin de otomatik eşleşmesi) yasaktır.

## 2. Bugünkü kod tabanının sağladıkları ve açıkları

- `SourceReport` üzerindeki `(source, source_event_id)` benzersizliği kaynak içi kimlikleri ayırıyor. Her raporun geçerli revizyonu `currentRevisionId` ile gösteriliyor.
- `SourceReportRevision` zaman, koordinat, derinlik, büyüklük/tür, konum metni ve hash'leri saklıyor. Böylece AFAD ve KOERI değerlerini aynı alana ezmeden muhafaza etmek mümkün.
- `EventMatch` ve `event_match` tablosu mevcut; ancak şu anda yalnızca rapor çifti, algoritma sürümü, zaman farkı, mesafe ve oluşturulma zamanını saklıyor. Durum, puan/gerekçe, kullanılan revizyon kimlikleri, karar kaynağı, küme ve karar geçmişi yok.
- `EventMatchRepository` boş bir CRUD arayüzü. Eşleştirme/aday bulma akışı ve veri bütünlüğü kuralları henüz yok.
- `EarthquakeQueryService` şu anda her kaynak raporunun etkin revizyonunu doğrudan listeliyor; eşleşmiş olay gruplaması yok. Tazelik ve sistem durumu da AFAD'a sabitlenmiş.
- Önemli alanlar uygulama ve arayüzde AFAD'a sabitlenmiş. Örnekler: `AfadApiClient`, `AfadDataNormalizer`, `AfadScheduledPoller`, `IngestionService.ingestAfadEvents`, `EarthquakeQueryService` ve `frontend/src/components/Header.tsx` / `InfoModal.tsx`.
- Flyway kullanılıyor. Mevcut başlangıç şeması `V1__initial_schema.sql`; yeni geliştirme yeni ve ileriye dönük bir migration ile yapılmalı, V1 değiştirilmemeli.

İlgili başlangıç noktaları: `src/main/java/com/ag/deprem/model/entity/EventMatch.java`, `SourceReport.java`, `SourceReportRevision.java`; `src/main/resources/db/migration/V1__initial_schema.sql`; `src/main/java/com/ag/deprem/service/EarthquakeQueryService.java`; `frontend/src/types/earthquake.ts`.

## 3. Eşleştirme iş akışı

### 3.1 Ön işleme ve rapor revizyonu

1. Her rapor kaynak kimliğiyle içeri alınır. Önerilen sabit kodlar `AFAD` ve `KOERI` olmalı; görünen marka adı ayrı çevrilebilir metin olarak tutulmalı.
2. Saatler `Instant` olarak saklanmadan önce kaynak saat dilimi doğrulanır. KOERI Türkçe liste saat dilimini açıkça belirtmiyorsa UTC varsayımı yapılmaz; makineye bağlı yerel saat kullanımı da yapılmaz. KOERI'den teyit alınana veya güvenilir bir resmî açıklama bulunana kadar bu belirsizlik kayıt altına alınır ve ilgili saatler otomatik eşleştirmede kullanılmaz ya da düşük güvenli sayılır. İngilizce KOERI listesinde UTC ibaresi bulunduğu bilgisi, Türkçe kaynağın saat dilimini kendiliğinden çözmez.
3. Kaynak büyüklük tipi korunur: KOERI'nin Md/Ml/Mw değerleri birbiri yerine yazılmaz; AFAD tipi ile KOERI tipi ayrı raporlarda kalır. `-.-` / eksik değerler null olur.
4. Kaynak raporunun etkin revizyonu değişince, bu raporu içeren otomatik eşleşmeler yeniden değerlendirme kuyruğuna girer. Önceki karar ve kullanılan revizyon kimlikleri geçmişte tutulur.

### 3.2 Aday üretimi

İlk sürümde sadece farklı kaynaklar arasında (AFAD–KOERI) aday üretilir. Aynı kaynağın raporları birbirine eşleştirilmez. Kaynak raporunun zamanı ve geçerli koordinatları yoksa aday üretilemez; eksik büyüklük veya derinlik tek başına adayı elemez.

Başlangıç aday pencereleri konfigürasyon olmalı ve kalibrasyon öncesi **geçici** değerler olarak ele alınmalıdır:

- Zaman farkı: mutlak fark için 120 saniyeye kadar aday arama.
- Yatay mesafe: 50 km'ye kadar aday arama (Haversine veya eşdeğer küresel jeodezik hesap).
- Aşırı zaman/mesafe çiftleri için aday üretilmez; eşiğin dışında kalan bir kayıt yine listede kendi başına görünür.

Bu sayılar doğrulanmış bilimsel eşikler değildir; veri örneklemiyle ölçülecek başlangıç aralığıdır. Fay geometrisi, çözüm niteliği, olay büyüklüğü, ağ gecikmesi ve kaynak saat dilimi belirsizliği farklı vakalarda tolerans ihtiyacını etkileyebilir. Üretim öncesi gerçek eşleşmiş ve eşleşmemiş örneklerle seçilmelidir.

Aday araması indeksli/verimli olmalı: zaman aralığıyla veritabanından daraltma, ardından doğru jeodezik mesafe. Her rapor için sınırsız tarihçe taraması yapılmamalı. İlk canlı penceresi kadar, kaynak yeni rapor veya revizyon verdiğinde yeniden hesaplanacak geçmiş aralığı da açıkça belirlenmelidir.

### 3.3 Aday puanı ve açıklama

Her aday için ayrı özellikler saklanır:

- `absTimeDeltaSeconds`
- `distanceKm`
- varsa derinlik farkı
- yalnızca karşılaştırılabilir magnitüd tiplerinde magnitüd farkı; farklı tipler doğrudan eşdeğer sayılmaz
- konum metni/province benzerliği gibi yardımcı sinyaller (tek başına karar verdirmez)
- çözüm niteliği/öncül-otomatik çözüm bilgisi, eğer kaynak bunu güvenilir biçimde sağlıyorsa
- saat dilimi ve veri kalite belirsizlikleri

Puanlama basit, deterministik ve sürümlü olmalı. İlk aday sıralaması normalize edilmiş zaman ve mesafe uyumunu ağırlıklı bir skorla birleştirebilir; derinlik, büyüklük ve yer metni yalnızca yardımcı sinyal olmalı. Eksik veri skoru sıfırlamak yerine o özelliğin katkısını devre dışı bırakmalı ve kalan ağırlıkları yeniden normalize etmeli. Skorun yanında kullanıcıya okunabilir gerekçe saklanmalı (ör. `zaman: 34 sn`, `mesafe: 8.1 km`, `büyüklük tipi karşılaştırılmadı`).

Tek bir toplam skor otomatik kabul için yeterli değildir. Ayrıca şu şartlar aranır:

- **Karşılıklı en iyi aday:** A raporunun en iyi adayı B olmalı ve B'nin en iyi adayı A olmalı.
- **Skor marjı:** En iyi adayın ikinci adaydan farkı konfigüre edilmiş bir marjı aşmalı. Yakın skorlar `AMBIGUOUS` kalır.
- **Çakışmasızlık:** Bir kaynak raporu aynı anda birbiriyle yarışan birden çok kabul edilmiş olaya bağlanamaz.
- **Aynı kaynak koruması:** Bir olay kümesinde aynı kaynağın birden fazla raporu varsa, sistem bunu otomatik olarak birleştirmez; inceleme ister.
- **Sert çelişki:** Zaman veya mesafe gibi güvenilir sinyallerin açıkça çelişmesi otomatik kabulü engeller; büyüklük/derinlik farkı tek başına sert çelişki değildir.

Bu koşullardan biri sağlanmazsa aday reddedilmez; otomatik birleştirme yerine belirsiz/inceleme durumunda bırakılır. Eşleşme öncesi ve sonrası eşiklerle ilgili tüm değerler algoritma sürümüyle birlikte saklanır.

### 3.4 Karar durumları

`EventMatch` en azından aşağıdaki durumları ayırt etmelidir:

- `CANDIDATE`: eşik içinde, karar verilmemiş aday.
- `AUTO_MATCHED`: yüksek güven, karşılıklı en iyi, marj ve çakışma koşullarını geçen otomatik kabul.
- `REVIEW_REQUIRED`: birden fazla makul aday, revizyon çakışması, aynı kaynakta çoklu rapor veya saat/veri kalitesi belirsizliği.
- `MANUAL_MATCHED`: operatör tarafından aynı olay olarak onaylanmış.
- `REJECTED`: operatör tarafından farklı olay olarak işaretlenmiş veya sürümlü kuralla otomatik aday dışında bırakılmış (manuel ret ile otomatik eleme ayrı kodlanmalı).
- `SUPERSEDED`: daha yeni rapor revizyonu/algoritma sonucu nedeniyle geçerli karar olmaktan çıkmış.

Durum değişikliği önceki kararı silmemeli. Kararı veren tür (`AUTO`, `MANUAL`, `SYSTEM`), zaman, gerekçe ve algoritma sürümü kaydedilmelidir.

## 4. Veri modeli ve migration planı

### 4.1 Önerilen kalıcı model

Mevcut `event_match` tablosunu büyütün ve olay gruplaması için iki kavram ekleyin:

1. **`seismic_event` / `event_group`:** UI ve sorguların gruplayacağı olay kimliği, oluşturulma zamanı, varsa gösterim/karar metadatası. Kaynak raporunun yerine geçmez.
2. **`event_group_member`:** grup–kaynak raporu üyeliği; üyelik türü/karar durumu, bağlanma zamanı ve kaynağı (otomatik/manuel). Otomatik kural aynı kaynaktan birden fazla rapor üyeliğini engeller veya inceleme gerektirir. Manuel karar açıkça izin verebilir.
3. **`event_match`:** iki kaynak raporu arasındaki aday/karar kaydı. Alanlar: iki rapor FK'si, rapor çiftinde kanonik sıralama, durum, skor ve açıklanabilir özellikler, kullanılan iki revizyon FK'si veya revizyon kimlikleri, `algorithm_version`, karar kaynağı, karar zamanı, created/updated zamanı, opsiyonel operatör notu ve devreden çıkarma bağlantısı.
4. **Karar geçmişi:** ayrı `event_match_decision` tablosu veya değişmez karar olayları. Güncel durum satırda tutulabilir, ancak önceki otomatik ve manuel kararların denetim izi korunmalıdır.

Otomatik puan/özellikler JSON olarak saklanırsa şema sürümü ve boyut sınırı tanımlanmalı; temel sorgulama ve bütünlük kısıtları JSON'a bırakılmamalıdır. Alternatif olarak temel ölçümler ayrı kolonlarda tutulup gerekçe JSON metninde saklanabilir.

### 4.2 Bütünlük kısıtları

- Aynı rapor kendisiyle eşleşemez.
- Bir kaynak çifti kanonik sıralanır (ör. UUID sırasına göre left/right) ve aynı iki rapor için yalnızca bir etkin eşleşme kaydı olur.
- Üyelikte aynı rapor aynı anda birden fazla etkin fiziksel olay grubunda bulunamaz.
- Otomatik kümede aynı kaynak kodundan birden fazla üye engellenir; zorunlu/manuel istisna gerekçelendirilir.
- FK silme davranışı, kaynak kayıt saklama politikasına uygun olmalı; kaynak raporu silindiğinde karar geçmişi sessizce yok olmamalı. Üretim verisi için fiziksel silme yerine soft-delete/retention yaklaşımı değerlendirilir.
- Migration yeni ve ileri yönlü olmalı; `V1__initial_schema.sql` değiştirilmemeli.

### 4.3 Revizyon güvenliği

Bir eşleşme kararı, aday hesaplanırken kullanılan iki `SourceReportRevision` kimliğine bağlanır. Raporun `currentRevisionId` değeri değiştiğinde kararın eski ölçümlerle güncel görünmesi önlenir. Yeniden eşleştirme yeni bir sonuç üretir; eski karar `SUPERSEDED` olur ve geçmişte kalır.

## 5. Backend uygulama adımları

### A. Kaynak başına veri alımı

- Kandilli erişimi [KANDILLI_DATA_ACCESS.md](KANDILLI_DATA_ACCESS.md) uyarınca ayrı KOERI adapter'ı, normalizer ve poller ile eklenir; HTML/metin ayrıştırma backend'de kalır.
- Sayfanın sabit sütunları için fixture tabanlı parser yapılır; kolon değişimi, boş büyüklük, Türkçe karakter, satır sonu/encoding ve bozuk satır güvenli şekilde ele alınır.
- Kaynak tazeliği `SourceStatusTracker` içindeki tek AFAD durumundan çıkarılıp kaynak adına göre izlenir. `StatusResponseDto` her kaynağın son deneme/başarı/hata/tazelik ve kapsama bilgisini döndürür.
- KOERI zaman dilimi belirsizliği çözülmeden zaman alanı yanlış UTC ile normalleştirilmez. İzin verilen fallback'in üründeki etkisi planlanıp konfigüre edilir.

### B. Eşleştirme çekirdeği

- Yeni, dışarıdan küçük bir arayüze sahip `EventMatchingService`/`EventMatcher` modülü: geçerli rapor revizyonu geldiğinde adayları bulur, deterministik özellik/skor üretir, karar kurallarını uygular ve sonuçları saklar.
- Aday arama, jeodezik hesap, özellik çıkarımı, puanlama ve karar politikasını ayrı iç fonksiyonlarda tutun; ancak diğer modüllere tek bir yüksek seviyeli çağrı sunun. Saat/mesafe eşikleri `application.properties`/environment ile sürümlü konfigürasyon olmalı.
- Kaynak içe alımı ve eşleştirme veri tutarlılığı aynı transaction'a bağlanamıyorsa, outbox/iş kuyruğu veya yeniden çalıştırılabilir işaretleme kullanın. Yeni rapor kaydedilip eşleştirme başarısız olduğunda rapor kaybolmamalı; eşleştirme tekrar denenebilmelidir.
- İdempotency: aynı poll tekrar çalıştığında rapor/revizyon/aday kopyaları üretmemeli. Aynı algoritma sürümü ve aynı revizyon çifti için sonuç deterministik olmalı.
- Başlangıçta gerçek zamanlı ingest sonrasında sınırlı bir yeniden değerlendirme penceresi ve idempotent bir backfill/recompute komutu planlayın. Tüm tarihçeyi her poll'da hesaplamayın.

### C. Sorgu ve API

- Mevcut `/earthquakes` sözleşmesini bozmayın. Varsayılan görünümün rapor satırlarını mı olay gruplarını mı döndüreceği açıkça seçilmeli. Öneri: yeni bir grup görünümü veya `grouped=true` gibi geriye uyumlu opsiyon eklemek; mevcut kaynak raporu/detail endpoint'ini korumak.
- Gruplanmış olay DTO'su: kararlı event/group ID, koordinat seçme politikası, raporların listesi (source, kendi occurredAt/magnitude/type/depth/location, revision, freshness), eşleşme durumu ve varsa belirsizlik açıklaması.
- Kanonik tek bir büyüklük değeri üretmeyin. Harita simgesi için seçilen büyüklük kaynağı görünür/konfigüre edilir; detayda AFAD ve KOERI değerleri yan yana ve tipleriyle sunulur.
- Eşleşme detayını, karar gerekçesini, kullanılan revizyonları ve kaynak URL/atıf bilgisini UI'ın ihtiyaç duyacağı biçimde sunun.
- Eşleşme için yönetim endpoint'i gerekli görülürse yalnızca yetkili/korumalı endpoint üzerinden manuel onay/ret sunun; halka açık uçtan operatör kararı alınmamalı.

### D. Kaynak durumu, arşiv ve telemetri

- `/status` yanıtında AFAD ve KOERI ayrı görünür; bir kaynak gecikince diğerinin tazeliği bozulmuş gibi gösterilmez.
- Log/metric: çekim başarı oranı, ayrıştırma reddi, son başarılı çekim yaşı, aday sayısı, otomatik eşleşme, belirsiz aday, manuel karar, sonradan değişen eşleşme ve recompute kuyruğu.
- Ham payload veya yeniden ayrıştırma için gerekli satır metni mevcut `rawPayloadRef`/saklama mimarisiyle uyumlu biçimde tutulur. Hassas veri yok varsayımıyla sınırsız saklama yapılmaz; kaynak kullanım koşulu ve disk sınırı değerlendirilir.
- `ArchiveExporterService` şu anda `SOURCE_COVERAGE = "AFAD"` sabitini kullanıyor. Arşiv manifest/katalog şeması çoklu kaynak kapsamasını belirtmeli ve eşleşme verisini de dışa aktarıyorsa şema sürümü yükseltilmelidir.

## 6. Kalibrasyon ve operasyon prosedürü

Otomatik eşleştirmeyi açmadan önce etiketli bir doğrulama kümesi oluşturun:

1. En az birkaç yüz aday çift içerecek şekilde; aynı olay olduğu doğrulananlar, birbirine yakın ama ayrı depremler, artçı/deprem dizileri, farklı büyüklük tipleri ve revize edilmiş kayıtlar kapsansın. Örnek sayısını gerçek veri hacmine göre artırın.
2. Örnekleri iki kaynak kataloğuna göre insan tarafından etiketleyin: `SAME_EVENT`, `DISTINCT_EVENTS`, `UNCERTAIN`. Belirsiz örnekleri zorla pozitif/negatif etiketlemeyin.
3. İki kaynaktan bağımsız ikinci inceleme veya anlaşmazlık çözümü kullanın; etiket gerekçesi ve kullanılan kaynak revizyonları saklansın.
4. Zaman penceresi, mesafe eşiği, skor ağırlığı ve otomatik kabul marjını bu kümede ölçün. Özellikle otomatik eşleşmelerde precision/yanlış birleşme oranı öncelikli metriktir; belirsiz bırakılanların oranı da raporlanır.
5. Eşik değişikliklerini algoritma sürümüyle deneme/kayıt kipinde çalıştırın; canlı karar üretmeden eski ve yeni sonuç farkını inceleyin.
6. İlk aşamada `CANDIDATE`/`REVIEW_REQUIRED` gösteren gözlem kipi kullanın. Sonuçlar kabul edilebilir olduktan sonra yalnızca yüksek güvenli `AUTO_MATCHED` açılır; eşik dışındaki vakalar ayrı kalır.

Temel hata maliyetleri:

- **Yanlış birleşme (false positive):** iki farklı deprem tek olay gibi görünür; kullanıcıyı yanıltır ve kaynak raporları arasındaki farkı gizler. Öncelikle kaçınılması gereken hata.
- **Yanlış ayırma (false negative):** aynı deprem iki satır görünür; kaynakları şeffaf göstererek daha kolay fark edilir ve sonradan düzeltilebilir.
- **Saat dilimi hatası:** sistematik yanlış aday veya eşleşme üretir; KOERI saat semantiği netleşmeden otomatik karar açılmamalı.
- **Transitif zincirleme:** A–B ve B–C kabulünü A–C doğrulanmış gibi yorumlayıp ayrı depremleri tek kümede toplayabilir; küme kuralları ve aynı kaynak kısıtı bunu engellemeli.
- **Kaynak revizyonu:** eski zaman/koordinata dayanmış karar sessizce geçerli kalabilir; revizyon kimlikleriyle yeniden değerlendirme zorunlu olmalı.
- **Tek kaynağın kesintisi:** yeni rapor gelmemesi deprem yokluğu olarak yorumlanmamalı; kaynak bazlı freshness ve eski raporları koruma şart.

## 7. UI davranışı

- Liste ve haritada grup görünümünde kaynak rozeti/rapor sayısı gösterin. Tek kaynaklı olay açıkça tek kaynaklı kalır.
- Eşleşmiş olay detayında “AFAD raporu” ve “Kandilli raporu” ayrı bölümlerde, değerlerin geldiği kaynak açıkça görünür.
- Saat farkı, koordinat farkı, büyüklük türü ve çözüm niteliği (varsa) kısa ve anlaşılır biçimde gösterilir.
- `CANDIDATE` veya `REVIEW_REQUIRED` otomatik olarak birleşmiş tek kayıt gibi sunulmaz. İki ayrı satırın yanında “eşleşme değerlendiriliyor” bağlantısı/işareti olabilir.
- Haritada tek nokta göstermek gerekiyorsa açık bir tercih politikası uygulanır (örn. kaynak önceliği veya koordinat ortalaması yoksa ayrı noktalar). Koordinatların aritmetik ortalamasını gerçek bir üçüncü çözüm gibi göstermekten kaçının. Ayrı kaynak marker'ları ve ortak detay paneli en şeffaf ilk davranıştır.
- Başlık, kaynak açıklaması, paylaşım metni, SEO/Dataset metaverisi ve PWA açıklaması AFAD'a özel tek-kaynak ifadelerinden çoklu-kaynak/rapor diline dönüştürülür.

## 8. Aşamalı teslimat

### Faz 0 — Katalog ve ölçüm

- KOERI saat dilimi, sayfa formatı/yenileme davranışı ve makine erişimi koşullarını KOERI ile teyit edin.
- AFAD–KOERI eşleşmiş, eşleşmemiş ve belirsiz etiketli örnek kümesini oluşturun.
- Başlangıç eşiklerini ve false positive toleransını onaylayın.

**Çıkış ölçütü:** Otomatik eşleşmede kullanılacak saat semantiği ve veri örneklemi belgeli.

### Faz 1 — Kaynakları ayrı alma ve saklama

- KOERI parser/client/poller/normalizer, kaynak bazlı status ve migration ile raporları alıp ayrı kaydet.
- Parser, tarih, `-.-`, büyüklük tipi, koordinat sınırları ve encoding için test/fixture seti oluştur.
- Henüz kayıt gruplaması yapma.

**Çıkış ölçütü:** KOERI raporları AFAD kayıtlarını değiştirmeden idempotent biçimde saklanıyor; kaynak tazeliği ayrı raporlanıyor.

### Faz 2 — Eşleşme adayları ve gözlem kipi

- `EventMatch` aday/karar alanlarını ve revizyon bağlarını ekle; candidate scoring ve çakışma incelemesini çalıştır.
- Sonuçları UI'da birleştirmeden yalnızca iç metrik/korumalı inceleme görünümünde sun.
- Etiketli kümede precision/recall ve belirsizlik oranını ölç; farklı algoritma sürümlerini karşılaştır.

**Çıkış ölçütü:** Adayların gerekçesi tekrarlanabilir, revizyon değişimi algılanıyor ve false positive kabul eşiği karşılanıyor.

### Faz 3 — Güvenli gruplanmış görünüm

- Event group/membership ve geriye uyumlu API görünümünü ekle.
- Yalnızca yüksek güvenli, çakışmasız otomatik eşleşmeleri grupla; belirsizleri ayrı tut.
- UI'da rapor değerlerini ve kaynakları yan yana göster; tek koordinat/büyüklük seçimini açık hale getir.

**Çıkış ölçütü:** Mevcut clients çalışmaya devam ediyor; grup detayı kaynak değerlerini saklamıyor/gizlemiyor.

### Faz 4 — Manuel karar ve yeniden değerlendirme

- Korunan operatör onay/ret akışı, karar geçmişi, geri alma ve gerekçelendirme ekle.
- Algoritma sürümü veya kaynak revizyonu değiştiğinde yeniden değerlendirme/backfill iş akışını devreye al.
- Arşiv formatı ve kaynak kapsamasını güncelle.

**Çıkış ölçütü:** Yanlış otomatik karar geri alınabilir; manuel kararlar sonraki otomatik hesapta sessizce ezilmez.

## 9. Doğrulama kapsamı (uygulama sırasında)

Plan uygulandığında aşağıdaki kontroller otomatik testlerle kapsanmalıdır:

- Aynı olay: zaman/konum farkı varken doğru aday ve yüksek skorlu eşleşme.
- Yakın zamanda ve yakın yerde iki ayrı deprem: karşılıklı en iyi aday, ikinci aday marjı veya aynı kaynak çatışması sebebiyle otomatik birleşmenin engellenmesi.
- Bir KOERI raporuna yakın iki AFAD raporu (ve tersi): belirsiz/inceleme durumu; transitive cluster oluşmaması.
- Eksik büyüklük/derinlik; farklı magnitüd tipleri; negatif/boş/bozuk değerler.
- Zaman dilimi belirsizliği; sınır zamanlar; UTC dönüşümü; hatalı saat.
- AFAD veya KOERI revizyonu; eşleşme yeniden hesaplama, önceki kararın supersede olması.
- Aynı poll'un tekrarı, paralel poll, veritabanı transaction hatası ve yarım kalmış matching işinin yeniden denenmesi.
- Parser kolonlarının değişmesi, boş/HTML hata cevabı, timeout, HTTP 4xx/5xx, rate-limiting ve encoding bozulması.
- Migration'ın mevcut veritabanına uygulanması ve eski API yanıtlarının geriye uyumluluğu.
- Kaynaklardan biri stale/offline iken diğerinin verisi ve durumu doğru kalması.
- Gruplanmış liste, harita, detay, paylaşım, filtre, sayfalama ve arşiv davranışları.

## 10. Yayına alma ve geri dönüş

- Önce migration ve KOERI ingestion; sonra aday üretimi feature flag arkasında; en son gruplanmış UI.
- İlk dağıtımda otomatik eşleşmeleri kullanıcıya tek olay gibi göstermeyin. Adayları ölçün ve karşılaştırın.
- Gruplama açılırken feature flag ile kapatılabilir olmalı. Kapatma kaynak raporlarını veya eşleşme geçmişini silmemeli.
- Migration geri dönüşü veri kaybı yaratacaksa otomatik down migration uygulamayın; genişlet/taşı/değiştir yaklaşımı ve yedekleme prosedürü izleyin.
- Eşik/algoritma değişimi veri migration'ı gibi ele alınmalı: sürümü artırın, dry run farklarını inceleyin, yeniden hesaplamayı sınırlı pencerede yapın.

## 11. İlk implementasyon PR'ı için önerilen dosya alanı

Bu liste muhtemel değişiklik yüzeyidir; uygulama sırasında mimari seçimlerle netleştirilir:

- Yeni backend KOERI raw DTO/parser/client/normalizer/poller ve konfigürasyon: `src/main/java/com/ag/deprem/service/` ve `model/dto/`.
- Kaynak bazlı ortak ingest arayüzü veya mevcut `IngestionService` genişletmesi: `src/main/java/com/ag/deprem/service/IngestionService.java`.
- Kaynak freshness/status yeniden tasarımı: `SourceStatusTracker.java`, `EarthquakeQueryService.java`, `StatusResponseDto.java`.
- Aday üretme/puanlama/karar modülü ve repository'leri: yeni `EventMatchingService`, eşleşme repository/DTO/entity sınıfları.
- Yeni Flyway migration: `src/main/resources/db/migration/V2__...sql` (sıradaki boş sürüm numarası doğrulanmalı).
- API ve kaynak raporu/grup DTO'ları: `EarthquakeController.java`, `EarthquakeItemDto.java`, `frontend/src/types/earthquake.ts`.
- Arayüz kaynak gösterimi ve grup detayları: `frontend/src/components/EarthquakeList.tsx`, `EarthquakeMap.tsx`, `EarthquakeDetailModal.tsx`, `Header.tsx`, `InfoModal.tsx`.
- Arşiv kaynağı kapsaması: `ArchiveExporterService.java`, archive DTO/schema.
- Ingest, parser, matcher, repo, API ve UI doğrulamaları: `src/test/` ve frontend test altyapısının mevcut düzeni.

## 12. Uygulamaya geçmeden önce karara bağlanacaklar

1. KOERI Türkçe son deprem listesindeki saat dilimi nedir ve otomatik eşleştirme için yeterince açık/resmî olarak teyit edilebilir mi?
2. Haritada eşleşmiş olay için ayrı iki marker mı gösterilecek, yoksa varsayılan kaynak önceliğiyle tek marker mı? Öneri ilk sürümde ayrı marker + ortak detay.
3. İnsan inceleme ekranı gerçekten gerekli mi? Gerekliyse kimler kullanacak ve hangi mevcut yetkilendirme modeliyle korunacak?
4. Eşleştirme yalnız AFAD–KOERI arasında mı kalacak, ileride USGS/EMSC gibi kaynaklara genişleyecek mi? Genişleme olasılığı kaynaklar arası grup modelini baştan genelleştirmeyi destekler.
5. Hatalı otomatik eşleşme için kabul edilebilir üst sınır nedir? Bu eşik ve değerlendirme veri kümesi olmadan otomatik gruplanmış görünüm açılmamalı.

## Kaynaklar

- Proje notu: [Kandilli veri erişimi ve kullanım koşulları](KANDILLI_DATA_ACCESS.md)
- [KOERI son depremler (Türkçe)](https://www.koeri.boun.edu.tr/scripts/lst0.asp) — son 500 hızlı çözüm kaydı ve kullanım/atıf metni.
- [KOERI Recent Earthquakes (English)](https://www.koeri.boun.edu.tr/scripts/lasteq.asp) — İngilizce liste ve UTC bilgisi.
- [KOERI EIDA/WebDC3](https://eida.koeri.boun.edu.tr/) ve [yardım](https://eida.koeri.boun.edu.tr/webinterface/help.html) — waveform/istasyon arşivi kapsamı; halka açık son deprem olay API'si olarak değerlendirilmemeli.
- [FDSN KOERI Data Center kaydı](https://www.fdsn.org/datacenters/detail/KOERI/) — kayıtlı FDSN servislerinin kapsamı.
