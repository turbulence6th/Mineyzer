import React from 'react';
import { Player } from '../services/GameService';

// PlayerScoreDisplay için Props arayüzü
interface PlayerScoreDisplayProps {
  player: Player;
  isGameOver: boolean;
  winnerId: string | null | undefined; // Kazanan oyuncunun ID'si
  activePlayerId: string | null | undefined; // Aktif oyuncunun ID'si
  firstPlayerId: string | null | undefined; // İlk oyuncunun ID'si (stil için)
}

const PlayerScoreDisplay: React.FC<PlayerScoreDisplayProps> = ({
  player,
  isGameOver,
  winnerId,
  activePlayerId,
  firstPlayerId,
}) => {
  // Skor sınıfını hesaplama mantığı GameBoard'dan buraya taşındı
  let playerScoreClass = 'player-score player-score-desktop';

  if (isGameOver) {
    playerScoreClass += player.id === winnerId ? ' player-score-winner' : ' player-score-loser';
  } else {
    if (player.id === activePlayerId) {
      if (player.id === firstPlayerId) {
        playerScoreClass += ' active-player-1';
      } else {
        playerScoreClass += ' active-player-2';
      }
    } else {
      playerScoreClass += ' inactive-player';
    }
  }

  return (
    <div 
        key={player.id} // Key prop'u map içinde kullanılacak, burada tekrar gerek yok ama zararı olmaz
        className={playerScoreClass}
        title={`${player.username}: ${player.score}`}
    >
        <span className="player-name">{player.username}</span>: 
        <span className={`score ${isGameOver && player.id === winnerId ? 'score-winner' : ''}`}>
            {player.score}
        </span>
    </div>
  );
};

export default PlayerScoreDisplay; 