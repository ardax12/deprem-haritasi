# Türkiye Deprem İzleme - Canlı Dağıtım (Production) Kılavuzu

Bu belge, **Türkiye Deprem İzleme** projesinin satın aldığınız alan adı (domain) ile canlıya alınması, SSL sertifikalandırılması, Cloudflare entegrasyonu ve sunucu kurulumu için uçtan uca rehberdir.

---

## 1. Mimari Genel Bakış

```
[Kullanıcı / Mobil Tarayıcı]
            │
            ▼ (HTTPS / HTTP3)
[Cloudflare CDN & WAF] (DDoS Engelleme, Edge Caching, Hızlı DNS)
            │
            ▼ (HTTPS - Full Strict)
[Nginx Reverse Proxy] (:80 / :443) (Rate Limiting, Gzip, Güvenlik Başlıkları)
            │
            ▼ (Dahili Docker Ağı: deprem_net)
[Spring Boot 3 + Java 21] (:8080) (AFAD Otomatik Senkronizasyon, API, PWA Statik Servisi)
            │
            ▼ (JDBC Port 5432)
[PostgreSQL 16 Alpine] (Kalıcı 'pgdata' hacmi, Flyway veri tabanı migrasyonları)
```

---

## 2. Alan Adı ve Cloudflare Yapılandırması (Önerilen)

En yüksek hız, sıfır maliyetli DDoS koruması ve Let's Encrypt yenileme dertlerinden kurtulmak için **Cloudflare** kullanılması önerilir.

### Adım 1: Cloudflare'e Domain Ekleme
1. [Cloudflare](https://dash.cloudflare.com/) hesabınıza giriş yapın ve **"Add a site"** diyerek aldığınız domaini (örn: `depremizleme.com`) ekleyin (Ücretsiz Free plan seçin).
2. Domaini satın aldığınız firmanın (Namecheap, GoDaddy, Natro vb.) yönetim paneline gidin ve Nameserver (NS) adreslerini Cloudflare'in size verdiği 2 adrese yönlendirin.

### Adım 2: DNS Kayıtlarını Girin
Cloudflare **DNS > Records** sekmesine gidin:
- **Tip A**: `Name: @`, `IPv4 address: <SUNUCU_IP_ADRESINIZ>`, `Proxy: Proxied (Turuncu Bulut)`
- **Tip CNAME**: `Name: www`, `Target: @`, `Proxy: Proxied (Turuncu Bulut)`

### Adım 3: Cloudflare SSL/TLS Modu
- **SSL/TLS > Overview** menüsünden modu **"Full (Strict)"** yapın.
- **SSL/TLS > Origin Server** menüsüne gidin:
  - **Create Certificate** butonuna tıklayın.
  - Size verilen Sertifika metnini sunucunuzda `nginx/ssl/live/fullchain.pem` dosyasına kaydedin.
  - Özel Anahtar (Private Key) metnini `nginx/ssl/live/privkey.pem` dosyasına kaydedin.
  *(Bu sertifika 15 yıl geçerlidir ve asla Let's Encrypt gibi 90 günde bir patlama riski taşımaz).*

### Adım 4: Cloudflare Önbellek (Cache) Kuralı
Deprem anında sunucunuzun binlerce anlık ziyaretçide kilitlenmemesi için:
- **Caching > Cache Rules** menüsünden yeni bir kural ekleyin:
  - İsim: `Static Assets Cache`
  - Filtre: `URI Path starts with /assets/`
  - İşlem: **Cache Everything**, Edge TTL: `1 month`, Browser TTL: `1 month`
- İkinci Kural:
  - İsim: `Bypass API`
  - Filtre: `URI Path starts with /api/`
  - İşlem: **Bypass Cache** (Deprem verisi her zaman doğrudan Spring Boot'tan gelsin).

---

## 3. VPS Sunucu Kurulumu (Hetzner / DigitalOcean / AWS)

Önerilen sunucu: Ubuntu 22.04 veya 24.04 LTS (1-2 vCPU, 2GB RAM fazlasıyla yeterlidir).

### Adım 1: Docker ve Git Kurulumu
Sunucunuza SSH ile bağlanın:
```bash
# Paketleri güncelleyin
sudo apt update && sudo apt upgrade -y

# Docker ve Docker Compose yükleyin
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Yeni oturum açın veya:
newgrp docker
```

### Adım 2: Projeyi Klonlayın
```bash
git clone https://github.com/<kullanici_adiniz>/deprem.git
cd deprem
```

### Adım 3: Ortam Değişkenlerini (`.env`) Ayarlayın
```bash
cp .env.example .env
nano .env
```
Dosya içerisindeki şu alanları düzenleyin:
- `DOMAIN_NAME`: Kendi domaininiz (örn: `depremizleme.com`)
- `POSTGRES_PASSWORD`: Güçlü rastgele bir parola belirleyin.
- `APP_ADMIN_API_KEY`: Güçlü rastgele bir yönetici anahtarı belirleyin.

### Adım 4: SSL Sertifikalarını Ekleyin (Cloudflare Origin CA)
```bash
mkdir -p nginx/ssl/live
nano nginx/ssl/live/fullchain.pem   # Cloudflare'den kopyalanan sertifikayı yapıştırın
nano nginx/ssl/live/privkey.pem     # Cloudflare'den kopyalanan özel anahtarı yapıştırın
```

### Adım 5: Sistemi Başlatın
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Container'ların durumunu kontrol edin:
```bash
docker compose -f docker-compose.prod.yml ps
docker logs -f deprem_app
```
`deprem_app` loglarında `Started DepremApplication in ... seconds` yazısını gördüğünüzde sistem yayındadır!

---

## 4. Otomatik Veritabanı Yedeklemesi (Günlük Cron)

Olası sunucu çökmelerine karşı her gece saat 03:00'te otomatik PostgreSQL yedeği almak için:

Sunucuda bir script oluşturun:
```bash
sudo nano /usr/local/bin/backup-deprem.sh
```
İçeriğe şunu yazın:
```bash
#!/bin/bash
BACKUP_DIR="/var/backups/deprem"
mkdir -p $BACKUP_DIR
DATE=$(date +\%Y\%m\%d_\%H\%M\%S)
docker exec deprem_postgres pg_dump -U deprem_app depremdb | gzip > "$BACKUP_DIR/deprem_$DATE.sql.gz"
# 7 günden eski yedekleri sil
find $BACKUP_DIR -type f -name "*.sql.gz" -mtime +7 -delete
```

Çalıştırma izni verip crontab'a ekleyin:
```bash
sudo chmod +x /usr/local/bin/backup-deprem.sh
(crontab -l 2>/dev/null; echo "0 3 * * * /usr/local/bin/backup-deprem.sh") | crontab -
```

---

## 5. Sıfır Kesintiyle Yeni Sürüm Dağıtımı (Update Playbook)

Yeni bir özellik veya frontend değişikliği yaptığınızda canlıdaki sunucuyu güncellemek için:
```bash
cd ~/deprem
git pull origin main
docker compose -f docker-compose.prod.yml up -d --build app
```
Docker otomatik olarak yeni imajı oluşturur ve eski container'ı durdurup yenisini sıfır kesintiyle devreye alır.

---

## 6. Alternatif: 1-Click Platformlar (Render / Railway / Coolify)

Eğer kendi Linux VPS sunucunuzu yönetmek istemiyorsanız:
1. **Render**:
   - `render.yaml` dosyamız kök dizinde hazırdır.
   - Render.com'a GitHub reponuzu bağlayıp "Blueprint" seçtiğinizde Dockerfile üzerinden otomatik yayınlanır.
   - Domain ayarlarından aldığınız domaini ekleyip CNAME ile tek tıkla bağlayabilirsiniz.
2. **Coolify**:
   - Kendi sunucunuzda çalışan açık kaynaklı bir Heroku/Vercel alternatifidir. `docker-compose.prod.yml` dosyasını Coolify paneline yapıştırıp tek tıkla canlıya alabilirsiniz.
