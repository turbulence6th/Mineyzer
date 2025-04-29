import React, { useState, useEffect, useCallback } from 'react';
import { Game, GameService } from '../services/GameService';
import { useLoadingState } from '../hooks/useLoadingState';
import './GameSetup.css'; // Stil dosyasını GameSetup ile paylaşabiliriz

interface JoinGameByIdProps {
    gameId: string;
    username: string;
    onGameStart: (game: Game, username: string) => void;
}

const JoinGameById: React.FC<JoinGameByIdProps> = ({ gameId, username, onGameStart }) => {
    const [gameDetails, setGameDetails] = useState<Game | null>(null);
    const { loading, error, wrapAsync, setError, setLoading } = useLoadingState();

    // gameId değiştiğinde oyun detaylarını yükle
    useEffect(() => {
        setLoading(true); // Yüklemeyi başlat
        setError(null); // Önceki hataları temizle
        GameService.getGame(gameId)
            .then((game: Game | null) => {
                if (game) {
                    setGameDetails(game);
                    // Oyuna zaten 2 kişi katılmışsa veya bitmişse hata ver
                    if (game.players.length >= 2) {
                       setError('Bu oyun dolu veya çoktan başlamış.');
                    } else if (game.gameOver) {
                       setError('Bu oyun bitmiş.');
                    }
                } else {
                    setError('Oyun bulunamadı.');
                    setGameDetails(null); // Detayları temizle
                }
            })
            .catch((err: Error) => {
                console.error("Oyun detayları alınırken hata:", err);
                setError('Oyun detayları alınırken bir hata oluştu.');
                setGameDetails(null);
            })
            .finally(() => {
                setLoading(false); // Yüklemeyi bitir
            });
    }, [gameId, setLoading, setError]); // gameId değiştiğinde tetikle

    // Belirli bir oyuna katılma işlemi
    const handleJoinSpecificGame = useCallback(() => {
        if (!username.trim()) {
            setError('Lütfen bir kullanıcı adı giriniz');
            return;
        }
        wrapAsync(async () => {
            const joinedGame = await GameService.joinGame(gameId, username);
            if (joinedGame) {
                // URL'yi pushState ile değiştirmeye gerek yok, çünkü zaten bu URL'deyiz.
                // Sadece onGameStart'ı çağır.
                onGameStart(joinedGame, username);
            }
        }, 'Oyuna katılırken bir hata oluştu.');
    }, [gameId, username, wrapAsync, onGameStart, setError]);


    return (
        <div className="game-creation"> {/* game-creation sınıfını kullanıyoruz */}
            <h2 className="game-creation-title">Oyuna Katıl</h2>
            {loading && !gameDetails && <p className="loading-text">Oyun detayları yükleniyor...</p>}
            {error && <div className="error-message">{error}</div>}
            {gameDetails && !error && (
                <div className="game-details-for-join">
                    <p><span className="setting-label">Kurucu:</span> {gameDetails.players[0]?.username ?? 'Bilinmiyor'}</p>
                    <p><span className="setting-label">Boyut:</span> {gameDetails.rows}x{gameDetails.columns}</p>
                    <p><span className="setting-label">Mayın Sayısı:</span> {gameDetails.mineCount}</p>
                </div>
            )}
            {/* Hata yoksa ve oyun dolu/bitmiş değilse katıl butonunu göster */}
            {gameDetails && !error && gameDetails.players.length < 2 && !gameDetails.gameOver && (
                <button
                    className="create-game-btn"
                    onClick={handleJoinSpecificGame} // Yeni handler'ı kullan
                    disabled={loading || !username.trim() || !!error}
                >
                    {loading ? 'Katılıyor...' : 'Katıl'}
                </button>
            )}
        </div>
    );
};

export default JoinGameById; 