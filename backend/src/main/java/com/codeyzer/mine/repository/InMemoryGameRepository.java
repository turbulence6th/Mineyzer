package com.codeyzer.mine.repository;

import com.codeyzer.mine.model.Game;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryGameRepository {
    
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    
    public Game save(Game game) {
        games.put(game.getId(), game);
        return game;
    }
    
    public Optional<Game> findById(String id) {
        return Optional.ofNullable(games.get(id));
    }
    
    public List<Game> findAll() {
        return new ArrayList<>(games.values());
    }
    
    public void deleteById(String id) {
        games.remove(id);
    }

    /**
     * Zamanlayıcı kontrolü için uygun olan aktif oyunları bulur.
     * Sadece bitmemiş, 2 oyunculu ve sırası başlamış (turnStartTimeMillis > 0) oyunları döndürür.
     * @return Zamanlayıcı kontrolü gerektiren oyunların listesi.
     */
    public List<Game> findActiveGamesForTimerCheck() {
        List<Game> activeGames = new ArrayList<>();
        for (Game game : games.values()) {
            // getCurrentTurn() metodunun Game sınıfında var olduğunu varsayıyoruz.
            // Eğer yoksa veya turnStartTimeMillis getter'ı yoksa, ilgili getter'ları eklemelisiniz.
            if (!game.isGameOver() && game.getPlayers().size() == 2 && game.getTurnStartTimeMillis() > 0) {
                activeGames.add(game);
            }
        }
        return activeGames;
    }
} 