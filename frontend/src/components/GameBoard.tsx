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
    const [cellSize] = useState(30);
    const [statusMessage, setStatusMessage] = useState<string>("Oyuna hoş geldiniz!");

    const isCurrentPlayerTurn = !game?.gameOver && game?.status === 'IN_PROGRESS' && currentPlayer?.id === game?.currentTurn;
    const canInteractWithBoard = isCurrentPlayerTurn;
    const canToggleFlag = !game?.gameOver && game?.status === 'IN_PROGRESS' && currentPlayer != null;
    const isWaitingForOpponent = game?.status === 'WAITING_FOR_PLAYERS';
    const isWaitingForReady = game?.status === 'WAITING_FOR_READY';
    const isInProgress = game?.status === 'IN_PROGRESS';
    const isGameOver = game?.status === 'GAME_OVER';

    useEffect(() => {
        if (!game || !currentPlayer) return;

        let message = "";
        const activePlayer = game.players.find(p => p.id === game.currentTurn);
        const activePlayerUsername = activePlayer?.username || 'Rakip';

        if (game.lastEventMessage) {
            message = game.lastEventMessage;
        } else if (isGameOver) {
            const winnerPlayer = game.winnerId ? game.players.find(p => p.id === game.winnerId) : null;
            if (winnerPlayer) {
                message = `Oyun Bitti! Kazanan: ${winnerPlayer.username}`;
            } else if (game.winnerId === null) {
                message = "Oyun Berabere Bitti!";
            } else {
                message = "Oyun Bitti!";
            }
        } else if (isWaitingForOpponent) {
            message = "Rakip bekleniyor...";
        } else if (isWaitingForReady) {
            const otherPlayer = game.players.find(p => p.id !== currentPlayer?.id);
            const currentPlayerReady = game.players.find(p => p.id === currentPlayer?.id)?.ready;
            const otherPlayerReady = otherPlayer?.ready;

            if (!currentPlayerReady && !otherPlayerReady) {
                message = "Başlamak için 'Hazır' butonuna tıkla. Rakip bekleniyor...";
            } else if (!currentPlayerReady && otherPlayerReady) {
                message = `Rakibin (${otherPlayer?.username}) hazır. Başlamak için 'Hazır' butonuna tıkla.`;
            } else if (currentPlayerReady && !otherPlayerReady) {
                message = `Hazırsın! Rakibin (${otherPlayer?.username}) hazır olması bekleniyor...`;
            } else {
                message = "Oyun başlıyor...";
            }
        } else if (isInProgress) {
            if (isCurrentPlayerTurn) {
                message = "Sıra sende.";
            } else {
                message = `Sıra ${activePlayerUsername}'da bekliyor...`;
            }
        }
        
        setStatusMessage(message);

    }, [game, currentPlayer, isGameOver, isWaitingForOpponent, isWaitingForReady, isInProgress, isCurrentPlayerTurn]);

    const handleCellClick = (row: number, col: number) => {
        if (canInteractWithBoard) {
            onMakeMove(row, col);
        }
    };

    const handleToggleFlag = async (row: number, col: number) => {
        if (canToggleFlag && game && currentPlayer) {
            try {
                await GameService.toggleFlag(game.id, currentPlayer.id, row, col);
            } catch (err) {
                console.error("Bayrak değiştirme hatası:", err);
            }
        }
    };

    const handleReadyClick = async () => {
        if (game && currentPlayer && !currentPlayer.ready && isWaitingForReady) {
            try {
                await GameService.markPlayerReady(game.id, currentPlayer.id);
            } catch (error) {
                console.error("Hazır olma durumu gönderilemedi:", error);
            }
        }
    };

    const cellGap = 3;
    const activePlayerId = game?.currentTurn;

    const isCurrentPlayerWinner = isGameOver && game.winnerId !== null && game.winnerId === currentPlayer?.id;
    const isTie = isGameOver && game.winnerId === null;

    const revealedMineCount = game?.board.flat().filter(cell => cell.revealed && cell.mine).length ?? 0;
    const remainingMineCount = (game?.mineCount ?? 0) - revealedMineCount;

    const getGameInfoClass = () => {
        let className = "game-info";
        if (isGameOver) {
            if (isCurrentPlayerWinner) {
                className += " game-info-win";
            } else if (isTie) {
                className += " game-info-tie";
            } else {
                className += " game-info-loss";
            }
        } else if (isWaitingForOpponent || isWaitingForReady) {
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
                        {isGameOver ? (
                            <span className={isCurrentPlayerWinner ? 'winner-text' : (isTie ? 'tie-text' : 'loser-text')}>
                                {isCurrentPlayerWinner
                                    ? '🎉 Tebrikler! Kazandın! 🏆'
                                    : (isTie
                                        ? '🤝 Oyun Berabere Bitti! 🤝'
                                        : '😢 Maalesef kaybettin. 🔄')
                                }
                            </span>
                        ) : isWaitingForOpponent ? (
                            <span className="waiting-opponent">
                                ⌛ Rakip Bekleniyor...
                            </span>
                        ) : isWaitingForReady ? (
                            <span className="waiting-ready">
                                ✨ Oyuncuların Hazır Olması Bekleniyor...
                            </span>
                        ) : (
                            isCurrentPlayerTurn 
                                ? <span className="your-turn">✨ Senin Sıran! ✨</span>
                                : <span className="waiting">⏳ Rakibin Oynuyor...</span>
                        )}
                    </div>
                    
                    <div className="mine-counter">
                        {(isInProgress || isGameOver) && (
                            <div className="mine-counter-pill">
                                <span role="img" aria-label="bomb" className="mine-icon">💣</span>
                                <span>{remainingMineCount}</span>
                            </div>
                        )}
                        {isWaitingForReady && currentPlayer && !game.players.find(p => p.id === currentPlayer.id)?.ready && (
                            <button className="ready-button" onClick={handleReadyClick}>
                                Hazır
                            </button>
                        )}
                    </div>
                    
                    <div className={`scores ${'scores-row'}`}>
                        {game.players.map((player, index) => {
                            const isPlayerReady = player.ready;
                            return (
                                <div key={player.id} className="player-info-container">
                                    {isWaitingForReady && (
                                        <div className={`ready-status ${isPlayerReady ? 'ready' : 'not-ready'}`}>
                                            {isPlayerReady ? '✅ Hazır' : '⏳ Bekleniyor'}
                                        </div>
                                    )}
                                    <PlayerScoreDisplay 
                                        player={player}
                                        isGameOver={isGameOver}
                                        winnerId={game.winnerId}
                                        activePlayerId={activePlayerId}
                                        firstPlayerId={firstPlayerId}
                                    />
                                    <PlayerClock 
                                        timeLeftMillis={index === 0 ? game.player1TimeLeftMillis : game.player2TimeLeftMillis}
                                        isActive={isInProgress && player.id === activePlayerId && !isGameOver}
                                        turnStartTimeMillis={player.id === activePlayerId ? game.turnStartTimeMillis : 0}
                                    />
                                </div>
                            );
                        })}
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
                    {(isGameOver || isWaitingForOpponent || isWaitingForReady) && (
                        <div className="board-overlay" />
                    )}
                    
                    {game.board.flat().map((cell) => (
                        <Cell 
                            key={`${cell.row}-${cell.column}`}
                            cell={cell}
                            onClick={handleCellClick}
                            onToggleFlag={handleToggleFlag}
                            isCurrentPlayerTurn={canInteractWithBoard}
                            canToggleFlag={canToggleFlag}
                            firstPlayerId={firstPlayerId}
                            currentPlayerId={currentPlayer?.id}
                            lastMoveRow={game.lastMoveRow}
                            lastMoveCol={game.lastMoveCol}
                            disabled={!isInProgress}
                        />
                    ))}
                </div>
            </div>
        </div>
    );
};

export default GameBoard; 