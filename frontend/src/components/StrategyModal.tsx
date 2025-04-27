import React from 'react';
import './StrategyModal.css'; // Oluşturduğumuz CSS dosyasını import et

interface StrategyModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const StrategyModal: React.FC<StrategyModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}> {/* Dışarı tıklayınca kapat */}
      <div className="modal-content" onClick={e => e.stopPropagation()}> {/* İçeri tıklayınca kapanmasın */}
        <button className="modal-close-button" onClick={onClose}>&times;</button>
        <h2>Oyun Stratejileri</h2>
        <ul>
          <li>
            <strong>Kesin Puanlara Odaklan:</strong>
            <p>Garantili güvenli hücreleri aç. Birden fazla seçenek varsa, puanı garantilemek için '0' yerine sayılı hücreyi açmak genellikle daha iyidir.</p>
          </li>
          <li>
            <strong>Rakibin Hatalarını Cezalandır:</strong>
            <p>Rakibin bayrakladığı bir hücrenin kesin güvenli olduğunu ispatlayabiliyorsan, o hücreyi aç! Rakip 1 puan kaybeder, sen puan kazanırsın. Skorda fark yaratmanın en iyi yollarından biridir.</p>
          </li>
          <li>
            <strong>Risk Yönetimi:</strong>
            <p>Emin olmadığın hücrelere tıklamaktan kaçın. Mayına basmak sıra kaybettirir. Sadece çok gerideysen veya bilgiye ihtiyacın varsa risk al.</p>
          </li>
          <li>
            <strong>Doğru Bayrak Kullanımı:</strong>
            <p>Sadece %100 emin olduğun mayınlara bayrak koy. Bu, hem kendini korur hem de komşu hücreleri yorumlamana yardımcı olur. Emin olmadığın yere bayrak koymak (blöf yapmak) risklidir, rakip açarsa puan kaybedersin.</p>
          </li>
          <li>
            <strong>Mayınlardan Kaçın (Genellikle):</strong>
            <p>Oyun sonu güvenli hücreler açıldığında biter, mayınları açmak puan vermez. Öndeysen, bırak mayınları rakip açsın veya sona kalsın.</p>
          </li>
          <li>
            <strong>Oyun Sonunu Düşün:</strong>
            <p>Az hücre kaldığında durum değişir. Öndeysen risk alma. Gerideysen yüksek puanlı hücreleri veya rakibin yanlış bayraklarını hedefle.</p>
          </li>
          <li>
            <strong>'0' Hücrelerinin Değeri:</strong>
            <p>'0' açmak puan vermez ama geniş bir alanı açarak bilgi sağlar. Puan veren garantili hücre varsa genellikle o daha önceliklidir.</p>
          </li>
        </ul>
      </div>
    </div>
  );
};

export default StrategyModal; 