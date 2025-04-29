import React, { useState, useEffect, useCallback } from 'react';
import { Game, GameService, Player } from '../services/GameService';
import { useLoadingState } from '../hooks/useLoadingState';
import './GameSetup.css'; // Stil dosyasını paylaşıyoruz

interface CreateOrJoinGameProps {
    username: string;
    onGameStart: (game: Game, username: string) => void;
}

// Zorluk seviyeleri bu bileşene özel olabilir
const difficultyLevels = {
    easy: { rows: 8, columns: 8, mineCount: 10, label: "Kolay (8x8, 10 Mayın)" },
    medium: { rows: 16, columns: 16, mineCount: 40, label: "Orta (16x16, 40 Mayın)" },
    hard: { rows: 16, columns: 20, mineCount: 60, label: "Zor (16x20, 60 Mayın)" },
    expert: { rows: 20, columns: 24, mineCount: 99, label: "Çok Zor (20x24, 99 Mayın)" }
};
type DifficultyLevel = keyof typeof difficultyLevels;

const CreateOrJoinGame: React.FC<CreateOrJoinGameProps> = ({ username, onGameStart }) => {
    const [selectedDifficulty, setSelectedDifficulty] = useState<DifficultyLevel>('easy');
    const [availableGames, setAvailableGames] = useState<Game[]>([]);
    const { loading, error, wrapAsync, setError } = useLoadingState(); // Kendi yükleme/hata durumunu yönetir

    const loadAvailableGames = useCallback(() => {
        wrapAsync(async () => {
            const games = await GameService.getAllGames();
            setAvailableGames(games.filter(game => game.players.length === 1 && !game.gameOver));
        }, 'Mevcut oyunlar yüklenirken hata oluştu.');
    }, [wrapAsync]);

    useEffect(() => {
        loadAvailableGames();
    }, [loadAvailableGames]);

    const handleCreateGame = useCallback(() => {
        if (!username.trim()) {
            setError('Lütfen bir kullanıcı adı giriniz'); // Bu hata burada gösterilecek
            return;
        }
        wrapAsync(async () => {
            const { rows, columns, mineCount } = difficultyLevels[selectedDifficulty];
            const newGame = await GameService.createGame(rows, columns, mineCount);
            if (newGame) {
                const joinedGame = await GameService.joinGame(newGame.id, username);
                if (joinedGame) {
                    window.history.pushState(null, '', `/${joinedGame.id}`); // URL'yi değiştir
                    onGameStart(joinedGame, username); // Oyunu başlat
                }
            }
        }, 'Oyun oluşturulurken bir hata oluştu.');
    }, [username, selectedDifficulty, wrapAsync, onGameStart, setError]);

    const handleJoinGameFromList = useCallback((joinGameId: string) => {
        if (!username.trim()) {
            setError('Lütfen bir kullanıcı adı giriniz'); // Bu hata burada gösterilecek
            return;
        }
        wrapAsync(async () => {
            const joinedGame = await GameService.joinGame(joinGameId, username);
            if (joinedGame) {
                window.history.pushState(null, '', `/${joinedGame.id}`); // URL'yi değiştir
                onGameStart(joinedGame, username); // Oyunu başlat
            }
        }, 'Oyuna katılırken bir hata oluştu.');
    }, [username, wrapAsync, onGameStart, setError]);

    return (
        <>
            {/* Hata mesajı bu bileşenin kendi hatasını gösterir */}
            {error && <div className="error-message">{error}</div>}

            <div className="game-creation">
                <h2 className="game-creation-title">Yeni Oyun Oluştur</h2>
                <div className="difficulty-selector">
                     <label htmlFor="difficulty" className="setting-label">Zorluk Seviyesi:</label>
                     <select 
                         id="difficulty"
                         className="setting-input difficulty-select"
                         value={selectedDifficulty}
                         onChange={(e) => setSelectedDifficulty(e.target.value as DifficultyLevel)}
                     >
                         {Object.entries(difficultyLevels).map(([key, value]) => (
                             <option key={key} value={key}>
                                 {value.label}
                             </option>
                         ))}
                     </select>
                </div>
                <button 
                    className="create-game-btn"
                    onClick={handleCreateGame}
                    disabled={loading || !username.trim()}
                >
                    {loading ? 'Oluşturuluyor...' : 'Oyun Oluştur'}
                </button>
            </div>
            
            <div className="available-games">
                <h2 className="available-games-title">Mevcut Oyunlar</h2>
                <div className="refresh-button-container">
                    <button 
                        className="refresh-button"
                        onClick={loadAvailableGames} 
                        disabled={loading}
                    >
                        {loading ? 'Yükleniyor...' : 'Yenile'}
                    </button>
                </div>
                {loading ? (
                    <p className="loading-text">Yükleniyor...</p>
                ) : availableGames.length > 0 ? (
                    <ul className="games-list">
                        {availableGames.map(game => (
                            <li key={game.id} className="game-item">
                                <span className="game-info-text">
                                    <strong>{game.players[0]?.username}</strong>'in oyunu 
                                    ({game.rows}x{game.columns}, {game.mineCount} mayın)
                                </span>
                                <button 
                                    className="join-game-btn"
                                    onClick={() => handleJoinGameFromList(game.id)}
                                    disabled={loading || !username.trim()}
                                >
                                    Katıl
                                </button>
                            </li>
                        ))}
                    </ul>
                ) : (
                    <p className="no-games-message">
                        Şu anda katılabileceğiniz bir oyun bulunmuyor.
                    </p>
                )}
            </div>
        </>
    );
};

export default CreateOrJoinGame; 