package com.codeyzer.mine.service;

import com.codeyzer.mine.repository.InMemoryGameRepository;
import com.codeyzer.mine.controller.WebSocketController;
import com.codeyzer.mine.model.Game;
import com.codeyzer.mine.model.Player;
import com.codeyzer.mine.model.Cell;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Slf4j
public class GameService {

    private final InMemoryGameRepository gameRepository;
    private final WebSocketController webSocketController;
    private final BoardService boardService;

    public Game createGame(int rows, int columns, int mineCount) {
        Game newGame = new Game(rows, columns, mineCount);
        
        // Zorluk seviyesine göre başlangıç süresini belirle
        long initialTimeMillis;
        if (rows == 8 && columns == 8 && mineCount == 10) { // Kolay (8x8)
            initialTimeMillis = 30 * 1000; // 30 Saniye
        } else if (rows == 16 && columns == 16 && mineCount == 40) { // Orta (16x16)
            initialTimeMillis = 90 * 1000; // 90 Saniye (1.5 Dakika)
        } else if (rows == 16 && columns == 20 && mineCount == 60) { // Zor (16x20)
            initialTimeMillis = 105 * 1000; // 105 Saniye (1.75 Dakika)
        } else if (rows == 20 && columns == 24 && mineCount == 99) { // Çok Zor (20x24)
            initialTimeMillis = 150 * 1000; // 150 Saniye (2.5 Dakika)
        } else {
            // Eşleşmeyen veya özel ayarlar için varsayılan süre (örn: 1.5dk)
            log.warn("Unknown or custom difficulty settings ({}x{}, {} mines), defaulting to 1.5 minutes.", rows, columns, mineCount);
            initialTimeMillis = 90 * 1000; // Varsayılan 90 saniye
        }
        
        // Belirlenen süre ile oyunu başlat
        newGame.initializeTime(initialTimeMillis);
        
        // Board'u oluştur ve mayınları yerleştir (BoardService kullanarak)
        List<List<Cell>> board = boardService.initializeBoard(rows, columns);
        boardService.placeMines(board, rows, columns, mineCount);
        newGame.setBoard(board); // Lombok setter ile set et
        
        Game savedGame = gameRepository.save(newGame);
        webSocketController.broadcastGameUpdate(savedGame); // broadcast yerine sadece oluşturana gönderilebilir mi?
        return savedGame;
    }

    public Optional<Game> getGameById(String gameId) {
        return gameRepository.findById(gameId);
    }

    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    public Game joinGame(String gameId, String username) {
        Optional<Game> gameOptional = gameRepository.findById(gameId);

        if (gameOptional.isPresent()) {
            Game game = gameOptional.get();
            // Oyuna sadece başlamadıysa ve yer varsa katıl
            if (!game.isGameOver() && game.getPlayers().size() < 2) {
                Player newPlayer = new Player(username);
                if (game.joinGame(newPlayer)) {
                    // İkinci oyuncu katıldığında ve sıra ilk oyuncudaysa zamanı başlat
                    if (game.getPlayers().size() == 2 && game.getCurrentTurn().equals(game.getPlayers().get(0).getId())) {
                        game.setTurnStartTimeMillis(System.currentTimeMillis());
                    }
                    Game savedGame = gameRepository.save(game);
                    webSocketController.broadcastGameUpdate(savedGame);
                    return savedGame;
                }
            } else {
                log.warn("Cannot join game {}. Game over: {}, Player count: {}", gameId, game.isGameOver(), game.getPlayers().size());
            }
        }
        return null; // Oyun bulunamadı veya katılma başarısız oldu
    }

    public Game makeMove(String gameId, String playerId, int row, int col) {
        Optional<Game> gameOptional = gameRepository.findById(gameId);

        if (gameOptional.isPresent()) {
            Game game = gameOptional.get();

            // 1. Ön Kontroller (Oyun bitti mi? Sıra oyuncuda mı?)
            if (game.isGameOver()) {
                log.warn("Invalid move: Game {} is already over.", gameId);
                return null; // Veya mevcut game durumu
            }
            if (!game.getCurrentTurn().equals(playerId)) {
                log.warn("Invalid move: Not player {}'s turn in game {}. Current turn: {}", playerId, gameId, game.getCurrentTurn());
                return null; // Veya mevcut game durumu
            }

            Player activePlayer = game.getPlayerById(playerId);
            if (activePlayer == null) { // Güvenlik kontrolü
                log.error("Player {} not found in game {} during makeMove", playerId, gameId);
                return null;
            }

            // 2. Çekirdek Hamle Mantığı (BoardService)
            BoardService.RevealResult revealResult = boardService.revealCell(game, playerId, row, col);

            // 3. Hamle Geçerli miydi? (BoardService kontrol etti)
            if (!revealResult.moveSuccess) {
                log.warn("Invalid move logic detected by BoardService for Player {} at ({}, {}) in Game {}", playerId, row, col, gameId);
                return null; // Geçersiz hamle (zaten açık, kendi bayrağı vb.)
            }

            // 4. Hamle Sonuçlarını İşle
            long timeNowAfterMove = System.currentTimeMillis();
            long timeSpentThisTurn = timeNowAfterMove - game.getTurnStartTimeMillis();

            String message = "";
            String penaltyMessage = "";

            //  4a. Rakip Bayrağı Açıldıysa Ceza
            if (revealResult.revealedOpponentFlagOwnerId != null) {
                Player opponent = game.getPlayerById(revealResult.revealedOpponentFlagOwnerId);
                if (opponent != null) {
                    int previousOpponentScore = opponent.getScore();
                    opponent.decreaseScore(1); // Skoru düşür
                    int scoreChange = opponent.getScore() - previousOpponentScore;
                    if (scoreChange < 0) {
                        penaltyMessage = " Rakip (" + opponent.getUsername() + ") yanlış bayraktan " + scoreChange + " puan kaybetti.";
                    } else {
                        penaltyMessage = " Rakip (" + opponent.getUsername() + ") yanlış bayrak açtı (skoru zaten 0 idi).";
                    }
                }
            }

            //  4b. Puanlama ve Mesaj Oluşturma
            if (revealResult.mineHit) {
                message = activePlayer.getUsername() + " bir mayına bastı! (0 Puan)";
                log.info("Player {} hit a mine in game {}. Turn passes.", activePlayer.getUsername(), gameId);
            } else {
                // Güvenli hücre
                int pointsGained = revealResult.pointsGained;
                if (pointsGained > 0) {
                    int previousPlayerScore = activePlayer.getScore();
                    activePlayer.increaseScore(pointsGained); // Skoru artır
                    message = activePlayer.getUsername() + " " + (activePlayer.getScore() - previousPlayerScore) + " puan değerinde bir hücre açtı.";
                } else {
                    // Kaskad tetiklendi (0 puan)
                    message = activePlayer.getUsername() + " güvenli bir bölge açtı.";
                }
            }
            if (!penaltyMessage.isEmpty()) { // Ceza mesajını ekle
                message += penaltyMessage;
            }
            game.setLastEventMessage(message);

            //  4c. Son Hamle Koordinatlarını Güncelle
            game.setLastMoveRow(row);
            game.setLastMoveCol(col);

            // 5. Zamanı Düşür
            boolean isPlayer1 = activePlayer.getId().equals(game.getPlayers().get(0).getId());
            if (isPlayer1) {
                long newTimeLeft = game.getPlayer1TimeLeftMillis() - timeSpentThisTurn;
                game.setPlayer1TimeLeftMillis(Math.max(0, newTimeLeft));
            } else {
                long newTimeLeft = game.getPlayer2TimeLeftMillis() - timeSpentThisTurn;
                game.setPlayer2TimeLeftMillis(Math.max(0, newTimeLeft));
            }

            // 6. Oyun Bitti mi Kontrolü
            boolean justFinished = false;
            if (revealResult.mineHit) {
                // Mayına basıldı! Oyunu bitirme, sadece mesajı ayarla ve sırayı geçir.
                // game.setGameOver(true); // <<< KALDIRILDI
                // Player winner = game.getPlayers().stream().filter(p -> !p.getId().equals(playerId)).findFirst().orElse(null); // <<< KALDIRILDI
                // if (winner != null) { // <<< KALDIRILDI
                //     game.setWinnerId(winner.getId()); // <<< KALDIRILDI
                //     log.info("Game {} ended. Player {} hit a mine. Winner: {}.", gameId, activePlayer.getUsername(), winner.getUsername()); // <<< LOG DEĞİŞECEK
                // } else { // <<< KALDIRILDI
                //     game.setWinnerId(null); // <<< KALDIRILDI
                //     log.info("Game {} ended. Player {} hit a mine. No opponent found.", gameId, activePlayer.getUsername()); // <<< LOG DEĞİŞECEK
                // }
                // justFinished = true; // <<< KALDIRILDI (Oyun bitmedi)
            } else {
                // Mayına basılmadı, tüm güvenli hücreler açıldı mı kontrol et
                if (boardService.isGameFinished(game.getBoard(), game.getMineCount())) {
                    checkAndUpdateGameOverNormal(game); // Bu metod gameOver ve winnerId'yi ayarlar
                    justFinished = game.isGameOver(); // checkAndUpdate... metodu ayarladıysa true olur
                }
            }

            // 7. Sıra Değiştirme ve Tur Zamanı Ayarlama (Oyun Bitmediyse)
            if (!justFinished) {
                game.switchTurn();
                game.setTurnStartTimeMillis(System.currentTimeMillis()); // Yeni turun başlangıç zamanı
            } else {
                // Oyun bittiyse zamanlayıcıyı durdur
                game.setTurnStartTimeMillis(0);
            }

            // 8. Kaydet ve Yayınla
            Game savedGame = gameRepository.save(game);
            webSocketController.broadcastGameUpdate(savedGame);
            return savedGame;
        }

        return null; // Oyun bulunamadı
    }

    // Yeni metod: Bayrak durumunu değiştir
    public Game toggleFlag(String gameId, String playerId, int row, int col) {
        Optional<Game> gameOptional = gameRepository.findById(gameId);

        if (gameOptional.isPresent()) {
            Game game = gameOptional.get();

            if (game.isGameOver()) {
                log.warn("Cannot toggle flag in game {}: Game is over.", gameId);
                return game;
            }
            // TODO: Sırası olmayan oyuncu bayrak değiştirebilir mi? Kurala bak.
            // Kurallar: Bayrak koymak sırayı değiştirmez, zamanı etkilemez. Sıra kontrolü gereksiz.
            
            boolean flagToggled = game.toggleFlag(playerId, row, col);

            if (flagToggled) {
                log.info("Player {} toggled flag at ({}, {}) in Game {}", playerId, row, col, gameId);
                Game savedGame = gameRepository.save(game);
                webSocketController.broadcastGameUpdate(savedGame);
                return savedGame;
            } else {
                // Bayrak değiştirilemedi (örn: rakip bayrağı)
                log.warn("Flag toggle failed for Player {} at ({}, {}) in Game {} (likely opponent flag)", playerId, row, col, gameId);
                return game; // Durum değişmedi, mevcut oyunu döndür
            }
        }
        log.error("Game not found with ID: {} while trying to toggle flag", gameId);
        return null;
    }

    // Oyuncu bağlantısı kesildiğinde çağrılacak yeni metod
    public void handlePlayerDisconnect(String gameId, String playerId) {
        log.info("Handling disconnect for Player {} in Game {}", playerId, gameId);
        Optional<Game> gameOptional = gameRepository.findById(gameId);

        if (gameOptional.isPresent()) {
            Game game = gameOptional.get();
            boolean wasGameOver = game.isGameOver(); // Oyunun önceki durumunu al

            Player disconnectedPlayer = game.getPlayerById(playerId);

            if (disconnectedPlayer != null) {
                game.getPlayers().remove(disconnectedPlayer);
                log.info("Player {} removed from game {}. Remaining players: {}",
                         playerId, gameId, game.getPlayers().size());

                if (game.getPlayers().isEmpty()) {
                    log.info("Game {} is empty after disconnect. Deleting room.", gameId);
                    gameRepository.deleteById(gameId);
                    return;
                }

                if (!wasGameOver && game.getPlayers().size() == 1) {
                    log.info("Game {} ending due to disconnect. Remaining player wins.", gameId);
                    game.setGameOver(true);
                    game.setTurnStartTimeMillis(0); // Zamanlayıcıyı durdur
                    Player winner = game.getPlayers().get(0); // Kalan tek oyuncu kazanır
                    game.setWinnerId(winner.getId()); // Kazanan ID'sini ayarla
                    String message = disconnectedPlayer.getUsername() + " bağlantısı koptu. Kazanan: " + winner.getUsername();
                    game.setLastEventMessage(message);
                    Game savedGame = gameRepository.save(game);
                    webSocketController.broadcastGameUpdate(savedGame);
                } else if (!wasGameOver) {
                    if(game.getCurrentTurn().equals(disconnectedPlayer.getId())) {
                        game.switchTurn(); // Sırayı değiştirir ve turnStartTime'ı ayarlar
                    }
                    Game savedGame = gameRepository.save(game);
                    webSocketController.broadcastGameUpdate(savedGame);
                    log.info("Game {} updated after player {} disconnect. Player list changed.", gameId, playerId);
                }
                // Oyun zaten bitmişse bir şey yapmaya gerek yok, sadece oyuncu çıkarıldı.

            } else {
                log.warn("Player {} not found in game {} during disconnect handling.", playerId, gameId);
            }
        } else {
            log.warn("Game {} not found while handling disconnect for player {}.", gameId, playerId);
        }
    }

    // Oyunun normal bitişini kontrol eden yardımcı metod (mayınlar bitti mi?)
    private void checkAndUpdateGameOverNormal(Game game) {
        // Bu metod sadece tüm güvenli hücrelerin açıldığı normal bitiş durumunu kontrol eder.
        // Mayına basma durumu makeMove içinde ele alınır.
        if (!game.isGameOver() && boardService.isGameFinished(game.getBoard(), game.getMineCount())) {
            game.setGameOver(true);
            game.setTurnStartTimeMillis(0);
            log.info("Game {} finished normally (all safe cells revealed).", game.getId());

            String winnerId = null;
            String message;
            if (game.getPlayers().size() == 2) {
                Player p1 = game.getPlayers().get(0);
                Player p2 = game.getPlayers().get(1);
                if (p1.getScore() > p2.getScore()) {
                    winnerId = p1.getId();
                    message = "Oyun Bitti! Tüm güvenli hücreler açıldı. Kazanan: " + p1.getUsername();
                } else if (p2.getScore() > p1.getScore()) {
                    winnerId = p2.getId();
                    message = "Oyun Bitti! Tüm güvenli hücreler açıldı. Kazanan: " + p2.getUsername();
                } else {
                    message = "Oyun Bitti! Tüm güvenli hücreler açıldı. Skorlar berabere!";
                }
            } else if (game.getPlayers().size() == 1) {
                winnerId = game.getPlayers().get(0).getId();
                message = "Oyun Bitti! Tüm güvenli hücreler açıldı.";
            } else {
                message = "Oyun Bitti!";
            }

            game.setWinnerId(winnerId);
            game.setLastEventMessage(message);
        }
    }

    // Zaman aşımı kontrolü için zamanlanmış görev (3 saniyede 1 çalışır)
    @Scheduled(fixedRate = 3000)
    public void checkActiveGameTimers() {
        List<Game> gamesToCheck = gameRepository.findActiveGamesForTimerCheck();
        long currentTime = System.currentTimeMillis();

        for (Game game : gamesToCheck) {
            // Oyunun bu döngü sırasında başka bir thread tarafından bitirilmediğinden emin ol (opsiyonel double check)
            if (game.isGameOver()) continue;

            String currentPlayerId = game.getCurrentTurn();
            Player currentPlayer = game.getPlayerById(currentPlayerId);
            if (currentPlayer == null) {
                log.warn("Timer check: Player {} not found in game {}. Skipping timer check for this game.", currentPlayerId, game.getId());
                continue;
            }

            long timeSpentThisTurn = currentTime - game.getTurnStartTimeMillis();
            long timeLeft;
            boolean isPlayer1Turn = currentPlayer.getId().equals(game.getPlayers().get(0).getId());

            if (isPlayer1Turn) {
                timeLeft = game.getPlayer1TimeLeftMillis() - timeSpentThisTurn;
            } else {
                timeLeft = game.getPlayer2TimeLeftMillis() - timeSpentThisTurn;
            }

            if (timeLeft <= 0) {
                log.info("Time ran out for player {} in game {}. Calculating final scores...", currentPlayer.getUsername(), game.getId());
                game.setGameOver(true);
                game.setTurnStartTimeMillis(0);

                // Süresi biten oyuncunun zamanını sıfırla
                Player opponent;
                String timedOutPlayerUsername = currentPlayer.getUsername();
                if (isPlayer1Turn) {
                    game.setPlayer1TimeLeftMillis(0);
                    opponent = game.getPlayers().get(1);
                } else {
                    game.setPlayer2TimeLeftMillis(0);
                    opponent = game.getPlayers().get(0);
                }

                // Kalan güvenli hücrelerin puanını hesapla
                int remainingScore = 0;
                if (opponent != null) { // Rakip varsa puanı ona ekle
                    log.debug("Calculating remaining safe score for opponent {}", opponent.getUsername());
                    for (List<Cell> row : game.getBoard()) {
                        for (Cell cell : row) {
                            if (!cell.isRevealed() && !cell.isMine()) {
                                remainingScore += cell.getAdjacentMines();
                            }
                        }
                    }
                    log.debug("Remaining safe score: {}. Adding to {}'s score.", remainingScore, opponent.getUsername());
                    opponent.increaseScore(remainingScore); // Puanı rakibe ekle
                } else {
                     log.warn("Opponent not found for timed out player {} in game {}. Cannot add remaining score.", timedOutPlayerUsername, game.getId());
                }

                // Nihai skorlara göre kazananı belirle
                String winnerId = null;
                String message;
                int currentPlayerScore = currentPlayer.getScore();
                int opponentScore = (opponent != null) ? opponent.getScore() : -1; // Rakip yoksa -1 gibi geçersiz skor

                log.info("Final scores in game {}: {} = {}, {} = {}", game.getId(), timedOutPlayerUsername, currentPlayerScore, (opponent != null ? opponent.getUsername() : "N/A"), opponentScore);

                if (opponent == null) { // Rakip yoksa (bağlantı kopmuş olabilir)
                    message = timedOutPlayerUsername + " süresi doldu. Rakip bulunamadığından oyun bitti.";
                    winnerId = null; // Kazanan yok
                } else if (opponentScore > currentPlayerScore) {
                    winnerId = opponent.getId();
                    message = timedOutPlayerUsername + " süresi doldu! Kalan hücrelerden " + remainingScore + " puan " + opponent.getUsername() + " oyuncusuna eklendi. Kazanan: " + opponent.getUsername();
                } else if (currentPlayerScore > opponentScore) {
                    winnerId = currentPlayer.getId();
                    message = timedOutPlayerUsername + " süresi doldu! Kalan hücrelerden " + remainingScore + " puan " + opponent.getUsername() + " oyuncusuna eklendi, ancak yetmedi. Kazanan: " + timedOutPlayerUsername;
                } else {
                    // Berabere
                    winnerId = null;
                    message = timedOutPlayerUsername + " süresi doldu! Kalan hücrelerden " + remainingScore + " puan " + opponent.getUsername() + " oyuncusuna eklendi. Skorlar berabere!";
                }

                game.setWinnerId(winnerId);
                game.setLastEventMessage(message);

                Game savedGame = gameRepository.save(game);
                webSocketController.broadcastGameUpdate(savedGame);
            }
        }
    }
} 