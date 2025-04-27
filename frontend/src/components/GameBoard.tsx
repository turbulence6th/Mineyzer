import React, { useEffect, useState } from 'react';
import Cell from './Cell';
import { Game, Player } from '../services/GameService';
import './GameBoard.css';
import { GameService } from '../services/GameService';
import PlayerScoreDisplay from './PlayerScoreDisplay';
import PlayerClock from './PlayerClock';

interface GameBoardProps {
    game: Game;
    currentPlayer: Player | null;
    onMakeMove: (row: number, col: number) => void;
    onExitGame: () => void;
}

const GameBoard: React.FC<GameBoardProps> = ({ game, currentPlayer, onMakeMove, onExitGame }) => {
    const [isCurrentPlayerTurn, setIsCurrentPlayerTurn] = useState(false);
    const [cellSize] = useState(30);
    const [statusMessage, setStatusMessage] = useState<string>("Oyuna hoş geldiniz!");

    useEffect(() => {
        if (!game) return;

        let message = "";

        if (game.lastEventMessage) {
            message = game.lastEventMessage;
        } else if (game.gameOver) {
            const winnerPlayer = game.players.sort((a, b) => b.score - a.score)[0];
            if (winnerPlayer) { message = `Oyun Bitti! Kazanan: ${winnerPlayer.username}`; } else { message = "Oyun Bitti!"; }
        } else if (game.players.length < 2) {
            message = "Rakip bekleniyor...";
        } else {
            const activePlayerUsername = game.players.find(p => p.id === game.currentTurn)?.username || 'Rakip';
            if(game.currentTurn === currentPlayer?.id) {
                message = "Sıra sende.";
            } else {
                message = `Sıra ${activePlayerUsername}'da bekliyor...`;
            }
        }
        
        setStatusMessage(message);

    }, [game, currentPlayer?.id]);

    useEffect(() => {
        if (currentPlayer && game?.currentTurn === currentPlayer.id) {
            setIsCurrentPlayerTurn(true);
        } else {
            setIsCurrentPlayerTurn(false);
        }
    }, [game?.currentTurn, currentPlayer]);

    const handleCellClick = (row: number, col: number) => {
        if (isCurrentPlayerTurn && !game?.gameOver) {
            onMakeMove(row, col);
        }
    };

    const handleToggleFlag = async (row: number, col: number) => {
        if (!isCurrentPlayerTurn || game?.gameOver) return;

        if (game && currentPlayer) {
            try {
                await GameService.toggleFlag(game.id, currentPlayer.id, row, col);
            } catch (err) {
                console.error("Bayrak değiştirme hatası:", err);
            }
        }
    };

    const cellGap = 3;
    const activePlayerId = game?.currentTurn;

    // Kazananı ve beraberlik durumunu game.winnerId'ye göre belirle
    const isCurrentPlayerWinner = game?.gameOver && game.winnerId !== null && game.winnerId === currentPlayer?.id;
    // const isGameOverAndNotWinner = game?.gameOver && !isCurrentPlayerWinner && game.winnerId !== null; // Bu değişkene gerek kalmadı gibi?
    const isTie = game?.gameOver && game.winnerId === null;

    const revealedMineCount = game?.board.flat().filter(cell => cell.revealed && cell.mine).length ?? 0;
    const remainingMineCount = (game?.mineCount ?? 0) - revealedMineCount;
    const isWaitingForOpponent = (game?.players.length ?? 0) < 2;

    const getGameInfoClass = () => {
        let className = "game-info";
        if (game.gameOver) {
            if (isCurrentPlayerWinner) {
                className += " game-info-win";
            } else if (isTie) {
                className += " game-info-tie"; // Beraberlik stili
            } else { // isLost durumu
                className += " game-info-loss";
            }
        } else if (isWaitingForOpponent) {
            className += " game-info-waiting";
        } else {
            className += " game-info-normal";
        }
        return className;
    };

    const firstPlayerId = game?.players[0]?.id;

    if (!game) return <div>Yükleniyor...</div>;

    return (
        <div className="game-board">
            <div 
                className={getGameInfoClass()}
            >
                <div className="game-info-header">
                    <button className="exit-game" onClick={onExitGame}>
                        Oyundan Çık
                    </button>
                </div>

                <div className="game-info-middle-row"> 
                    <div className={`turn-indicator ${'turn-indicator-large'}`}>
                        {game.gameOver ? (
                            <span className={isCurrentPlayerWinner ? 'winner-text' : (isTie ? 'tie-text' : 'loser-text')}>
                                {isCurrentPlayerWinner
                                    ? '🎉 Tebrikler! Kazandın! 🏆'
                                    : (isTie
                                        ? '🤝 Oyun Berabere Bitti! 🤝'
                                        : '😢 Maalesef kaybettin. 🔄') // isLost durumunu kapsar
                                }
                            </span>
                        ) : isWaitingForOpponent ? (
                            <span className="waiting-opponent">
                                ⌛ Rakip Bekleniyor...
                            </span>
                        ) : (
                            isCurrentPlayerTurn 
                                ? <span className="your-turn">✨ Senin Sıran! ✨</span>
                                : <span className="waiting">⏳ Rakibin Oynuyor...</span>
                        )}
                    </div>
                    
                    <div className="mine-counter">
                        <div className="mine-counter-pill">
                            <span role="img" aria-label="bomb" className="mine-icon">💣</span>
                            <span>{remainingMineCount}</span>
                        </div>
                    </div>
                    
                    <div className={`scores ${'scores-row'}`}>
                        {game.players.map((player, index) => (
                            <div key={player.id} className="player-info-container">
                                <PlayerScoreDisplay 
                                    player={player}
                                    isGameOver={game.gameOver}
                                    winnerId={game.winnerId} // Doğrudan game.winnerId kullan
                                    activePlayerId={activePlayerId}
                                    firstPlayerId={firstPlayerId}
                                />
                                <PlayerClock 
                                    timeLeftMillis={index === 0 ? game.player1TimeLeftMillis : game.player2TimeLeftMillis}
                                    isActive={game.players.length === 2 && player.id === activePlayerId && !game.gameOver}
                                    turnStartTimeMillis={player.id === activePlayerId ? game.turnStartTimeMillis : 0}
                                />
                            </div>
                        ))}
                    </div>
                </div>

                <div className="status-message">
                    {statusMessage}
                </div>
            </div>
            
            <div className="board-container">
                <div 
                    className="board"
                    style={{ 
                        gridTemplateColumns: `repeat(${game.columns}, ${cellSize}px)`,
                        gap: `${cellGap}px`,
                        gridAutoRows: `${cellSize}px`
                    }}
                >
                    {(game.gameOver || isWaitingForOpponent) && (
                        <div className="board-overlay" />
                    )}
                    
                    {game.board.flat().map((cell) => (
                        <Cell 
                            key={`${cell.row}-${cell.column}`}
                            cell={cell}
                            onClick={handleCellClick}
                            onToggleFlag={handleToggleFlag}
                            isCurrentPlayerTurn={isCurrentPlayerTurn && !game.gameOver && !isWaitingForOpponent}
                            firstPlayerId={firstPlayerId}
                            currentPlayerId={currentPlayer?.id}
                            lastMoveRow={game.lastMoveRow}
                            lastMoveCol={game.lastMoveCol}
                        />
                    ))}
                </div>
            </div>
        </div>
    );
};

export default GameBoard; 