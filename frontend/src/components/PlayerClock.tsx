import React, { useState, useEffect, useRef } from 'react';
import './PlayerClock.css';

interface PlayerClockProps {
  timeLeftMillis: number;
  isActive: boolean;
  turnStartTimeMillis?: number;
}

// Milisaniyeyi SS.mmm formatına çeviren yardımcı fonksiyon
const formatTime = (millis: number): string => {
  if (millis <= 0) return '0.000';
  const totalSeconds = millis / 1000;
  return totalSeconds.toFixed(3);
};

const PlayerClock: React.FC<PlayerClockProps> = ({ timeLeftMillis, isActive, turnStartTimeMillis }) => {
  // Gösterilecek hesaplanmış süreyi tutan state
  const [displayMillis, setDisplayMillis] = useState(timeLeftMillis);
  // Animasyon frame'ini takip etmek için ref
  const requestRef = useRef<number | null>(null);
  // Bir önceki zaman damgasını takip etmek için ref
  const previousTimeRef = useRef<number | undefined>(undefined);
  // Backend'den gelen son değerleri ve başlangıç zamanını saklamak için ref
  const anchorTimeLeftRef = useRef(timeLeftMillis);
  const anchorTurnStartRef = useRef(turnStartTimeMillis);

  // Props değiştiğinde anchor değerlerini güncelle
  useEffect(() => {
    anchorTimeLeftRef.current = timeLeftMillis;
    anchorTurnStartRef.current = turnStartTimeMillis;
    // Eğer oyuncu aktif değilse veya süre zaten 0 ise, gösterilen süreyi doğrudan ayarla
    if (!isActive || timeLeftMillis <= 0) {
      setDisplayMillis(timeLeftMillis);
    }
  }, [timeLeftMillis, turnStartTimeMillis, isActive]);

  // Animasyon döngüsü fonksiyonu
  const animate = (time: number) => {
    if (previousTimeRef.current !== undefined) {
      // Şu anki zamanı hesapla
      const now = Date.now();
      // Başlangıç zamanı geçerli mi kontrol et
      const turnStartTime = anchorTurnStartRef.current;
      if (turnStartTime === undefined || turnStartTime === null || turnStartTime <= 0) {
          // Geçerli başlangıç zamanı yoksa veya sıfırsa animasyonu durdur
          if (requestRef.current) cancelAnimationFrame(requestRef.current);
          setDisplayMillis(anchorTimeLeftRef.current); // Son bilinen süreyi göster
          return; 
      }

      const timeElapsed = now - turnStartTime;
      const calculatedTimeLeft = anchorTimeLeftRef.current - timeElapsed;
      
      const newDisplayMillis = Math.max(0, calculatedTimeLeft); // 0'ın altına düşme
      setDisplayMillis(newDisplayMillis);

      // Eğer süre bitmediyse bir sonraki frame'i iste
      if (newDisplayMillis > 0) {
        requestRef.current = requestAnimationFrame(animate);
      } else {
        // Süre bitti, animasyonu durdur (ref'in değeri varsa iptal et)
        if (requestRef.current) {
             cancelAnimationFrame(requestRef.current); 
        }
      }
    } else {
         // İlk frame, sadece zaman damgasını kaydet
         requestRef.current = requestAnimationFrame(animate);
    }
    previousTimeRef.current = time;
  };

  // Animasyonu başlatma/durdurma effect'i
  useEffect(() => {
    // Başlangıç zamanının geçerli olduğunu kontrol et
    const turnStartTimeValid = anchorTurnStartRef.current !== undefined && anchorTurnStartRef.current !== null && anchorTurnStartRef.current > 0;
    
    if (isActive && anchorTimeLeftRef.current > 0 && turnStartTimeValid) {
      // Aktifse, süre varsa ve başlangıç zamanı geçerliyse animasyonu başlat
      previousTimeRef.current = undefined; // Bir önceki zamanı sıfırla
      requestRef.current = requestAnimationFrame(animate);
    } else {
      // Aktif değilse veya süre bittiyse, mevcut animasyonu iptal et
      if (requestRef.current) {
        cancelAnimationFrame(requestRef.current);
      }
      // Gösterilen süreyi son bilinen değere ayarla (prop'tan gelen)
      setDisplayMillis(anchorTimeLeftRef.current);
    }

    // Cleanup: Component unmount olduğunda animasyonu durdur
    return () => {
      // Ref'in değeri varsa animasyonu iptal et
      if (requestRef.current) {
        cancelAnimationFrame(requestRef.current);
      }
    };
  }, [isActive]); // Sadece isActive değiştiğinde animasyonu başlat/durdur

  const isLowTime = displayMillis <= 10 * 1000 && displayMillis > 0; // 10 saniyenin altı

  return (
    <div className={`player-clock ${isActive ? 'active' : ''} ${isLowTime ? 'low-time' : ''}`}>
      {formatTime(displayMillis)}s 
    </div>
  );
};

export default PlayerClock; 