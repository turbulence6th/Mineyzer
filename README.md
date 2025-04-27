# Çevrimiçi 2 Kişilik Mayın Tarlası

Bu proje, klasik Mayın Tarlası oyununun web tabanlı, iki oyunculu versiyonudur.

## Teknolojiler

*   **Frontend:** React (TypeScript), Vite
*   **Backend:** Spring Boot (Java), Maven

## Proje Yapısı

Proje iki ana bölümden oluşmaktadır:

*   `frontend/`: React ile geliştirilen kullanıcı arayüzü kodlarını içerir.
*   `backend/`: Spring Boot ile geliştirilen sunucu tarafı kodlarını içerir.

## Kurulum ve Çalıştırma

Gerekli Araçlar:
*   Node.js ve npm (veya yarn/pnpm)
*   Java Development Kit (JDK) (Spring Boot için uygun bir sürüm)
*   Apache Maven

Adımlar:

1.  **Backend Sunucusunu Çalıştırma:**
    ```bash
    cd backend
    mvn spring-boot:run
    ```
    Sunucu varsayılan olarak `http://localhost:8080` adresinde çalışacaktır.

2.  **Frontend Arayüzünü Çalıştırma:**
    ```bash
    cd frontend
    npm install  # veya yarn install / pnpm install
    npm run dev  # veya yarn dev / pnpm dev
    ```
    Arayüz varsayılan olarak `http://localhost:5173` (Vite varsayılanı) adresinde açılacaktır.

## Oyun Kuralları

*   Oyuncular sırayla hamle yapar (kapalı bir hücreyi açar).
*   Her oyuncunun seçilen zorluğa göre belirlenen bir toplam süresi vardır (örn: 8x8 için 30sn, 16x16 için 1.5dk). Süre sadece oyuncunun kendi sırası geldiğinde, hamlesini yapana kadar azalır.
*   Kapalı bir hücreye sağ tıklayarak bayrak koyulabilir/kaldırılabilir.
    *   Oyuncular sadece kendi koydukları bayrakları kaldırabilir.
    *   Bayraklar her iki oyuncu tarafından da görülür ancak kimin koyduğuna göre kenarlık rengi farklıdır (1. Oyuncu: Mavi, 2. Oyuncu: Kırmızı).
    *   Oyuncular kendi bayrakladıkları hücreleri açamazlar.
    *   Bayrak koymak/kaldırmak sırayı değiştirmez ve zamanlayıcıyı etkilemez.
*   Üzerinde sayı yazan güvenli bir hücreyi açmak, o sayı kadar puan kazandırır. Sıra ve zamanlayıcı kontrolü rakibe geçer.
*   Üzerinde '0' yazan bir hücre açıldığında 0 puan kazandırılır. Bu hücreye komşu '0' hücreleri ve onlara komşu ilk sıradaki sayılı hücreler otomatik olarak açılır (kaskad açılım). Bu otomatik açılım sırasında açılan sayılı hücrelerden ekstra puan kazanılmaz. Sıra ve zamanlayıcı kontrolü rakibe geçer.
*   Mayın içeren bir hücreyi açmak puan kazandırmaz (0 puan). Sıra ve zamanlayıcı kontrolü rakibe geçer.
*   Eğer bir oyuncu, rakibinin **yanlış** bayrakladığı (içinde mayın olmayan) bir hücreyi açarsa, bayrağı yanlış koyan rakip 1 puan kaybeder. (Açan oyuncu yine hücredeki sayı kadar puan alır). Sıra ve zamanlayıcı kontrolü rakibe geçer.
*   Oyun aşağıdaki durumlarda sona erer:
    *   Tüm mayın olmayan (güvenli) hücreler açıldığında. Bu durumda en yüksek puana sahip oyuncu kazanır. Puanlar eşitse oyun berabere biter.
    *   Bir oyuncunun toplam süresi bittiğinde. **(Güncel Kural: Süre bitince kalan açılmamış güvenli hücre puanları rakibe eklenir ve sonuca göre kazanan belirlenir)**
    *   Bir oyuncunun bağlantısı koptuğunda. Oyunda kalan oyuncu kazanır. 