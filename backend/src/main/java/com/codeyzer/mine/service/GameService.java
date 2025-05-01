package com.codeyzer.mine.service;

import com.codeyzer.mine.repository.InMemoryGameRepository;
import com.codeyzer.mine.controller.WebSocketController;
import com.codeyzer.mine.model.*;
import static com.codeyzer.mine.model.GameStatus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private final InMemoryGameRepository gameRepository;
    private final WebSocketController webSocketController;
    private final BoardService boardService;

    @Autowired
    public GameService(InMemoryGameRepository gameRepository, WebSocketController webSocketController, BoardService boardService) {
        this.gameRepository = gameRepository;
        this.webSocketController = webSocketController;
        this.boardService = boardService;
    }

    public Game createGame(int rows, int columns, int mineCount) {
        Game newGame = new Game(rows, columns, mineCount);
        
        long initialTimeMillis;
        if (rows == 8 && columns == 8 && mineCount == 10) {
            initialTimeMillis = 30 * 1000;
        } else if (rows == 16 && columns == 16 && mineCount == 40) {
            initialTimeMillis = 90 * 1000;
        } else if (rows == 16 && columns == 20 && mineCount == 60) {
            initialTimeMillis = 105 * 1000;
        } else if (rows == 20 && columns == 24 && mineCount == 99) {
            initialTimeMillis = 150 * 1000;
        } else {
            log.warn("Unknown or custom difficulty settings ({}x{}, {} mines), defaulting to 1.5 minutes.", rows, columns, mineCount);
            initialTimeMillis = 90 * 1000;
        }
        
        newGame.initializeTime(initialTimeMillis);
        
        List<List<Cell>> board = boardService.initializeBoard(rows, columns);
        boardService.placeMines(board, rows, columns, mineCount);
        newGame.setBoard(board);
        
        Game savedGame = gameRepository.save(newGame);
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
            if (game.getStatus() == WAITING_FOR_PLAYERS && game.getPlayers().size() < 2) {
                Player newPlayer = new Player(username);
                if (game.joinGame(newPlayer)) {
                    Game savedGame = gameRepository.save(game);
                    webSocketController.broadcastGameUpdate(savedGame);
                    return savedGame;
                }
            } else {
                log.warn("Cannot join game {}. Status: {}, Player count: {}", gameId, game.getStatus(), game.getPlayers().size());
            }
        }
        return null;
    }

    public Game markPlayerReady(String gameId, String playerId) {
        Optional<Game> gameOptional = gameRepository.findById(gameId);
        if (gameOptional.isEmpty()) {
            log.error("Game not found with ID: {} while marking player ready", gameId);
            return null;
        }

        Game game = gameOptional.get();

        if (game.getStatus() != WAITING_FOR_READY) {
            log.warn("Cannot mark player {} ready for game {}: Game status is not WAITING_FOR_READY (current: {})", playerId, gameId, game.getStatus());
            return game;
        }

        Player player = game.getPlayerById(playerId);
        if (player == null) {
            log.error("Player {} not found in game {} while marking ready", playerId, gameId);
            return null;
        }

        if (!player.isReady()) {
            player.setReady(true);
            game.setLastEventMessage(player.getUsername() + " hazır.");
            log.info("Player {} marked as ready in game {}", player.getUsername(), gameId);

            boolean allReady = game.getPlayers().stream().allMatch(Player::isReady);
            if (game.getPlayers().size() == 2 && allReady) {
                startGame(game);
            }

            Game savedGame = gameRepository.save(game);
            webSocketController.broadcastGameUpdate(savedGame);
            return savedGame;
        } else {
            log.warn("Player {} was already marked as ready in game {}", player.getUsername(), gameId);
            return game;
        }
    }

    private void startGame(Game game) {
        if (game.getStatus() == WAITING_FOR_READY && game.getPlayers().size() == 2) {
            game.setStatus(IN_PROGRESS);
            game.setCurrentTurn(game.getPlayers().get(0).getId());
            game.setTurnStartTimeMillis(System.currentTimeMillis());
            game.setLastEventMessage("Tüm oyuncular hazır. Oyun başladı! Sıra: " + game.getPlayers().get(0).getUsername());
            log.info("Game {} started. First turn: {}", game.getId(), game.getPlayers().get(0).getUsername());
        }
    }

    public Game makeMove(String gameId, String playerId, int row, int col) {
        Optional<Game> gameOptional = gameRepository.findById(gameId);

        if (gameOptional.isEmpty()) return null;
        Game game = gameOptional.get();

        if (game.getStatus() != IN_PROGRESS) {
            log.warn("Invalid move: Game {} is not IN_PROGRESS (status: {}). Player: {}", gameId, game.getStatus(), playerId);
            return null;
        }
        if (!game.getCurrentTurn().equals(playerId)) {
            log.warn("Invalid move: Not player {}'s turn in game {}. Current turn: {}", playerId, gameId, game.getCurrentTurn());
            return null;
        }

        Player activePlayer = game.getPlayerById(playerId);
        if (activePlayer == null) {
            log.error("Player {} not found in game {} during makeMove", playerId, gameId);
            return null;
        }

        BoardService.RevealResult revealResult = boardService.revealCell(game, playerId, row, col);

        if (!revealResult.moveSuccess) {
            log.warn("Invalid move logic detected by BoardService for Player {} at ({}, {}) in Game {}", playerId, row, col, gameId);
            return null;
        }

        long timeNowAfterMove = System.currentTimeMillis();
        long timeSpentThisTurn = timeNowAfterMove - game.getTurnStartTimeMillis();

        String message = "";
        String penaltyMessage = "";

        if (revealResult.revealedOpponentFlagOwnerId != null) {
            Player opponent = game.getPlayerById(revealResult.revealedOpponentFlagOwnerId);
            if (opponent != null) {
                int previousOpponentScore = opponent.getScore();
                opponent.decreaseScore(1);
                int scoreChange = opponent.getScore() - previousOpponentScore;
                if (scoreChange < 0) {
                    penaltyMessage = " Rakip (" + opponent.getUsername() + ") yanlış bayraktan " + scoreChange + " puan kaybetti.";
                } else {
                    penaltyMessage = " Rakip (" + opponent.getUsername() + ") yanlış bayrak açtı (skoru zaten 0 idi).";
                }
            }
        }

        if (revealResult.mineHit) {
            message = activePlayer.getUsername() + " bir mayına bastı! (0 Puan)";
            log.info("Player {} hit a mine in game {}. Turn passes.", activePlayer.getUsername(), gameId);
        } else {
            int pointsGained = revealResult.pointsGained;
            if (pointsGained > 0) {
                int previousPlayerScore = activePlayer.getScore();
                activePlayer.increaseScore(pointsGained);
                message = activePlayer.getUsername() + " " + (activePlayer.getScore() - previousPlayerScore) + " puan değerinde bir hücre açtı.";
            } else {
                message = activePlayer.getUsername() + " güvenli bir bölge açtı.";
            }
        }
        if (!penaltyMessage.isEmpty()) {
            message += penaltyMessage;
        }
        game.setLastEventMessage(message);

        game.setLastMoveRow(row);
        game.setLastMoveCol(col);

        boolean isPlayer1 = activePlayer.getId().equals(game.getPlayers().get(0).getId());
        if (isPlayer1) {
            long newTimeLeft = game.getPlayer1TimeLeftMillis() - timeSpentThisTurn;
            game.setPlayer1TimeLeftMillis(Math.max(0, newTimeLeft));
        } else {
            long newTimeLeft = game.getPlayer2TimeLeftMillis() - timeSpentThisTurn;
            game.setPlayer2TimeLeftMillis(Math.max(0, newTimeLeft));
        }

        boolean justFinished = false;
        if (boardService.isGameFinished(game.getBoard(), game.getMineCount())) {
            checkAndUpdateGameOverNormal(game);
            justFinished = game.isGameOver();
        }
        
        if (!justFinished && (game.getPlayer1TimeLeftMillis() <= 0 || game.getPlayer2TimeLeftMillis() <= 0)) {
            handleTimeout(game);
            justFinished = game.isGameOver();
        }

        if (!justFinished) {
            game.switchTurn();
            game.setTurnStartTimeMillis(System.currentTimeMillis());
        } else {
            game.setTurnStartTimeMillis(0);
        }

        Game savedGame = gameRepository.save(game);
        webSocketController.broadcastGameUpdate(savedGame);
        return savedGame;
    }

    public Game toggleFlag(String gameId, String playerId, int row, int col) {
        Optional<Game> gameOptional = gameRepository.findById(gameId);

        if (gameOptional.isEmpty()) return null;
        Game game = gameOptional.get();

        if (game.getStatus() != IN_PROGRESS) {
            log.warn("Cannot toggle flag in game {}: Game is not IN_PROGRESS (status: {}). Player: {}", gameId, game.getStatus(), playerId);
            return game;
        }

        boolean flagToggled = game.toggleFlag(playerId, row, col);

        if (flagToggled) {
            log.info("Player {} toggled flag at ({}, {}) in Game {}", playerId, row, col, gameId);
            Game savedGame = gameRepository.save(game);
            webSocketController.broadcastGameUpdate(savedGame);
            return savedGame;
        } else {
            log.warn("Flag toggle failed for Player {} at ({}, {}) in Game {} (likely opponent flag)", playerId, row, col, gameId);
            return game;
        }
    }

    public void handlePlayerDisconnect(String gameId, String playerId) {
        Optional<Game> gameOptional = gameRepository.findById(gameId);
        if (gameOptional.isPresent()) {
            Game game = gameOptional.get();
            if (game.getStatus() == IN_PROGRESS || game.getStatus() == WAITING_FOR_READY || game.getStatus() == WAITING_FOR_PLAYERS) {
                Player disconnectedPlayer = game.getPlayerById(playerId);
                Player remainingPlayer = game.getPlayers().stream()
                                             .filter(p -> !p.getId().equals(playerId))
                                             .findFirst().orElse(null);

                log.info("Player {} disconnected from game {}", disconnectedPlayer != null ? disconnectedPlayer.getUsername() : playerId, gameId);

                if (remainingPlayer != null) {
                    game.setStatus(GAME_OVER);
                    game.setWinnerId(remainingPlayer.getId());
                    game.setTurnStartTimeMillis(0);
                    game.setLastEventMessage((disconnectedPlayer != null ? disconnectedPlayer.getUsername() : "Rakip") + " bağlantısı koptu. Kazanan: " + remainingPlayer.getUsername());
                    log.info("Game {} ended due to disconnect. Winner: {}", gameId, remainingPlayer.getUsername());
                } else {
                    game.setStatus(GAME_OVER);
                    game.setLastEventMessage((disconnectedPlayer != null ? disconnectedPlayer.getUsername() : "Oyuncu") + " bağlantısı koptu. Oyun bitti.");
                    log.info("Game {} ended due to disconnect. No remaining player.", gameId);
                }
                Game savedGame = gameRepository.save(game);
                webSocketController.broadcastGameUpdate(savedGame);
            }
        }
    }

    private void checkAndUpdateGameOverNormal(Game game) {
        if (game.getStatus() != IN_PROGRESS) return;

        if (boardService.isGameFinished(game.getBoard(), game.getMineCount())) {
            game.setStatus(GAME_OVER);
            game.setTurnStartTimeMillis(0);
            
            if (game.getPlayers().size() == 2) {
                Player p1 = game.getPlayers().get(0);
                Player p2 = game.getPlayers().get(1);
                if (p1.getScore() > p2.getScore()) {
                    game.setWinnerId(p1.getId());
                    game.setLastEventMessage("Tüm güvenli hücreler açıldı! Kazanan: " + p1.getUsername());
                    log.info("Game {} ended normally. Winner: {} with score: {}.", game.getId(), p1.getUsername(), p1.getScore());
                } else if (p2.getScore() > p1.getScore()) {
                    game.setWinnerId(p2.getId());
                    game.setLastEventMessage("Tüm güvenli hücreler açıldı! Kazanan: " + p2.getUsername());
                    log.info("Game {} ended normally. Winner: {} with score: {}.", game.getId(), p2.getUsername(), p2.getScore());
                } else {
                    game.setWinnerId(null);
                    game.setLastEventMessage("Tüm güvenli hücreler açıldı! Oyun berabere bitti.");
                    log.info("Game {} ended normally. It's a tie! Score: {}.", game.getId(), p1.getScore());
                }
            } else {
                Player winner = game.getPlayers().isEmpty() ? null : game.getPlayers().get(0);
                if (winner != null) {
                    game.setWinnerId(winner.getId());
                    game.setLastEventMessage("Tüm güvenli hücreler açıldı! Kazandın!");
                    log.info("Game {} ended normally. Single player {} wins.", game.getId(), winner.getUsername());
                } else {
                    game.setLastEventMessage("Tüm güvenli hücreler açıldı! Oyun bitti.");
                    log.info("Game {} ended normally. No players found?");
                }
            }
        }
    }

    @Scheduled(fixedRate = 1000)
    public void checkActiveGameTimers() {
        long currentTime = System.currentTimeMillis();
        List<Game> activeGames = gameRepository.findAll().stream()
                .filter(game -> game.getStatus() == IN_PROGRESS)
                .collect(Collectors.toList());

        for (Game game : activeGames) {
            if (game.getTurnStartTimeMillis() <= 0) continue;

            boolean player1TimedOut = false;
            boolean player2TimedOut = false;
            long timeElapsedSinceTurnStart = currentTime - game.getTurnStartTimeMillis();

            if (game.getPlayers().size() >= 1) {
                Player p1 = game.getPlayers().get(0);
                if (game.getCurrentTurn().equals(p1.getId())) {
                    if ((game.getPlayer1TimeLeftMillis() - timeElapsedSinceTurnStart) <= 0) {
                        player1TimedOut = true;
                        game.setPlayer1TimeLeftMillis(0);
                    }
                }
            }
            if (game.getPlayers().size() >= 2) {
                Player p2 = game.getPlayers().get(1);
                if (game.getCurrentTurn().equals(p2.getId())) {
                    if ((game.getPlayer2TimeLeftMillis() - timeElapsedSinceTurnStart) <= 0) {
                        player2TimedOut = true;
                        game.setPlayer2TimeLeftMillis(0);
                    }
                }
            }

            if (player1TimedOut || player2TimedOut) {
                log.info("Timeout detected in game {}. Player1: {}, Player2: {}. Handling timeout.", game.getId(), player1TimedOut, player2TimedOut);
                handleTimeout(game);
                Game savedGame = gameRepository.save(game);
                webSocketController.broadcastGameUpdate(savedGame);
            }
        }
    }

    void handleTimeout(Game game) {
        if (game.getStatus() != IN_PROGRESS) return;

        Player timedOutPlayer = null;
        Player remainingPlayer = null;

        if (game.getPlayer1TimeLeftMillis() <= 0) {
            if (game.getPlayers().size() > 0) timedOutPlayer = game.getPlayers().get(0);
            if (game.getPlayers().size() > 1) remainingPlayer = game.getPlayers().get(1);
        } else if (game.getPlayer2TimeLeftMillis() <= 0) {
            if (game.getPlayers().size() > 1) timedOutPlayer = game.getPlayers().get(1);
            if (game.getPlayers().size() > 0) remainingPlayer = game.getPlayers().get(0);
        }

        if (timedOutPlayer == null) {
            log.error("handleTimeout called for game {} but no timed out player found.", game.getId());
            return;
        }

        log.info("Player {} timed out in game {}", timedOutPlayer.getUsername(), game.getId());
        game.setStatus(GAME_OVER);
        game.setTurnStartTimeMillis(0);

        int remainingPoints = boardService.calculateRemainingPoints(game.getBoard());
        if (remainingPlayer != null) {
            log.info("Adding {} remaining points to player {}", remainingPoints, remainingPlayer.getUsername());
            remainingPlayer.increaseScore(remainingPoints);
        } else {
            log.warn("No remaining player found in game {} to add points after timeout.", game.getId());
        }

        if (remainingPlayer != null && remainingPlayer.getScore() > timedOutPlayer.getScore()) {
            game.setWinnerId(remainingPlayer.getId());
            game.setLastEventMessage(timedOutPlayer.getUsername() + " süresi bitti! Kalan puanlar (" + remainingPoints + ") " + remainingPlayer.getUsername() + " adlı oyuncuya eklendi. Kazanan: " + remainingPlayer.getUsername());
        } else if (remainingPlayer != null && timedOutPlayer.getScore() > remainingPlayer.getScore()) {
            game.setWinnerId(timedOutPlayer.getId());
            game.setLastEventMessage(timedOutPlayer.getUsername() + " süresi bitti! Ancak skoru daha yüksek olduğu için kazandı. Kazanan: " + timedOutPlayer.getUsername());
        } else {
            game.setWinnerId(null);
            game.setLastEventMessage(timedOutPlayer.getUsername() + " süresi bitti! Kalan puanlar eklendi. Oyun berabere bitti.");
        }
        log.info("Game {} ended due to timeout. Timed out: {}, Remaining: {}, Winner: {}", game.getId(), timedOutPlayer.getUsername(), (remainingPlayer != null ? remainingPlayer.getUsername() : "None"), game.getWinnerId());
    }
} 