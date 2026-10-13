# Türkiye Deprem Araştırma & Tahmin Çalışma Alanı (`research/`)

Bu klasör, AFAD tarihsel deprem kataloğu üzerinde istatistiksel sismoloji ve makine öğrenmesi algoritmaları kullanarak **b-değeri düşüş anomalileri, sismik enerji birikimi ve olasılıksal deprem tehlikesi tahminleme (seismic hazard forecasting)** modelleri geliştirmek için tasarlanmıştır.

---

## PyCharm ile Başlangıç

1. **Projeyi Açın:**
   - PyCharm'ı açın ve doğrudan `research` klasörünü (veya ana `deprem` projesini) açın:
     `C:\Users\LENOVO\IdeaProjects\deprem\research`
2. **Python Yorumlayıcısı (Virtualenv) Oluşturun:**
   - `Settings -> Project: research -> Python Interpreter -> Add Interpreter -> Add Local Interpreter`
   - Python 3.11 veya 3.12 seçin.
3. **Bağımlılıkları Yükleyin:**
   - Terminalde:
     ```bash
     pip install -r requirements.txt
     ```
4. **İlk Testi Çalıştırın:**
   - `main.py` dosyasını çalıştırarak son 30 günün AFAD kataloğunu indirin ve $b$-değeri analizini test edin:
     ```bash
     python main.py
     ```

---

## Modül Mimarisi

- `src/data_collector.py`: Tarihsel AFAD verisini toplu parçalarla indiren ve `.parquet` formatında saklayan ETL modülü.
- `src/seismology_metrics.py`: Gutenberg-Richter $b$-değeri (Aki-Utsu MLE), $M_c$ (Tamlık büyüklüğü) ve Benioff gerilme salınım hesaplayıcıları.
- `main.py`: Hızlı doğrulama ve analiz çalıştırma betiği.
