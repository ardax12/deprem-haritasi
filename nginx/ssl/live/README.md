# SSL Sertifikaları Dizini

Bu dizin Nginx container'ı tarafından bağlanır (`/etc/nginx/ssl/live/`).

İki yöntemden birini kullanabilirsiniz:

### Yöntem A: Cloudflare Origin CA (Önerilen - 15 Yıllık Ücretsiz SSL)
1. Cloudflare panelinizde **SSL/TLS > Origin Server** sekmesine gidin.
2. **Create Certificate** butonuna tıklayın.
3. Üretilen Sertifikayı `fullchain.pem` olarak bu klasöre kaydedin.
4. Üretilen Özel Anahtarı (Private Key) `privkey.pem` olarak bu klasöre kaydedin.
5. Cloudflare SSL/TLS modunu **Full (Strict)** yapın.

### Yöntem B: Let's Encrypt & Certbot (Otomatik Ücretsiz SSL)
Sunucunuzda `certbot` kuruluysa:
```bash
sudo certbot certonly --webroot -w /var/www/certbot -d alanadiniz.com -d www.alanadiniz.com
```
Oluşan sertifikaları buraya sembolik bağlayabilir veya kopyalayabilirsiniz.
