# Türkiye Deprem İzleme Haritası

Türkiye ve yakın çevresindeki depremleri güvenilir kaynaklardan (**AFAD** ve **Boğaziçi Üniversitesi Kandilli Rasathanesi / KOERI**) alıp kullanıcıya Türkçe, etkileşimli bir harita ve olay listesiyle sunan, açık kaynaklı ve kamuya açık bir izleme uygulamasıdır.

**Canlı Yayın:** [https://deprem-haritasi.onrender.com/](https://deprem-haritasi.onrender.com/)

---

## Özellikler

- **Çift Kaynaklı Doğrulama:** AFAD REST API ve Kandilli Rasathanesi bültenlerini eşzamanlı izleme, otomatik normalizasyon ve kompozit olay eşleştirme (`EventMatchingService`).
- **Etkileşimli Harita:** MapLibre GL JS ve OpenStreetMap altlığı üzerinde sismik olaylar, derinlik/büyüklük görselleştirmesi, kümeleme (clustering) ve MTA diri fay hatları katmanı.
- **Hissedilen Deprem Bildirimleri:** Kullanıcıların sarsıntı yoğunluğunu (EMS-98 ölçeği baz alınarak) bildirebileceği, IP sahteciliğine karşı korumalı ve veritabanı düzeyinde optimize edilmiş kitle kaynaklı (crowdsourced) anket sistemi.
- **İleri Düzey Arama & Filtreleme:** İl/ilçe bazlı arama, büyüklük, derinlik ve zaman aralığı (son 1 saatten son 30 güne) filtreleri.
- **SEO & Yapay Zeka Ajanı Keşfedilebilirliği:** 81 il için statik iniş rotaları, JSON-LD Schema.org grafikleri, `/llms.txt`, `/llms-full.txt` ve IndexNow entegrasyonu.
- **PWA & Mobil Uyum:** Çevrimdışı snapshot önbelleklemesi, Service Worker desteği ve mobil optimize edilmiş radar teması.

---

## Mimari ve Teknoloji Yığını

| Katman | Teknolojiler |
|---|---|
| **Backend** | Java 21, Spring Boot 4.1.1, Spring Data JPA, Flyway, PostgreSQL / PostGIS |
| **Frontend** | React 19, TypeScript, Vite 8, MapLibre GL 6 |
| **Tasarım Sistemi** | Sismik Radar / Koyu Tema (`DESIGN.md`), Vanilla CSS |
| **DevOps** | Docker (çok aşamalı derleme), Nginx, Render Blueprint (`render.yaml`), GitHub Actions CI |
| **Araştırma** | Python 3 (`research/`): $b$-değeri (Aki-Utsu MLE), $M_c$ tamlık büyüklüğü ve Benioff gerilme analizi |

---

## Yerel Geliştirme

### Gereksinimler
- Java 21+
- Node.js 20+ ve npm
- PostgreSQL 16+ (PostGIS eklentisiyle birlikte) veya Docker

### 1. Frontend Kurulumu ve Geliştirme
```bash
cd frontend
npm install
npm run dev
```
Geliştirme sunucusu `http://localhost:5173` adresinde çalışır ve `/api` isteklerini Spring Boot arka yüzüne (`localhost:8080`) yönlendirir.

### 2. Backend Kurulumu ve Çalıştırma
```bash
# Testleri çalıştır
./gradlew test

# Uygulamayı başlat
./gradlew bootRun
```
Uygulama `http://localhost:8080` adresinde ayağa kalkar.

---

## Proje Dokümanları

- [PRODUCT.md](PRODUCT.md): Ürün vizyonu, hedef kitle ve kabul kriterleri.
- [DESIGN.md](DESIGN.md): Tasarım ilkeleri, renk paleti ve tipografi belirteçleri (Design Tokens).
- [DEPLOYMENT_PLAYBOOK.md](DEPLOYMENT_PLAYBOOK.md): Docker, Nginx, Render ve canlı üretim ortamı dağıtım kılavuzu.
- [research/README.md](research/README.md): İstatistiki sismoloji ve AFAD veri araştırma araçları.

---

## Önemli Bilgilendirme ve Sorumluluk Reddi

> Bu uygulama **resmî bir acil durum uyarısı veya erken uyarı sistemi değildir**. Veriler sağlayıcı kuruluşların (AFAD ve Kandilli Rasathanesi) kamuya açık servislerinden temin edilir ve teknik gecikmeler içerebilir. Acil durumlarda ve resmî bilgilendirmelerde lütfen yetkili kamu kurumlarını (AFAD vb.) takip ediniz.
