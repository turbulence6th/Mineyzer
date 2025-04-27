package com.codeyzer.mine.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.codeyzer.mine.dto.GameConfigDTO;
import com.codeyzer.mine.dto.JoinGameRequestDTO;
import com.codeyzer.mine.dto.MakeMoveRequestDTO;
import com.codeyzer.mine.dto.ToggleFlagRequestDTO;
import com.codeyzer.mine.model.Game;
import com.codeyzer.mine.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "http://localhost:5173") // Frontend'in çalıştığı port
public class GameController {

    private final GameService gameService;
    @PostMapping
    public ResponseEntity<Game> createGame(@RequestBody GameConfigDTO gameConfig) {
        int rows = gameConfig.getRows();
        int columns = gameConfig.getColumns();
        int mineCount = gameConfig.getMineCount();

        Game newGame = gameService.createGame(rows, columns, mineCount);
        return ResponseEntity.ok(newGame);
    }

    @GetMapping
    public ResponseEntity<List<Game>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<Game> getGameById(@PathVariable String gameId) {
        Optional<Game> game = gameService.getGameById(gameId);
        return game.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<Game> joinGame(@PathVariable String gameId, @RequestBody JoinGameRequestDTO request) {
        String username = request.getUsername();
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Game game = gameService.joinGame(gameId, username);
        if (game != null) {
            return ResponseEntity.ok(game);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/{gameId}/move")
    public ResponseEntity<Game> makeMove(@PathVariable String gameId, @RequestBody MakeMoveRequestDTO request) {
        String playerId = request.getPlayerId();
        Integer row = request.getRow();
        Integer col = request.getCol();

        if (playerId == null || row == null || col == null) {
            return ResponseEntity.badRequest().build();
        }

        Game updatedGame = gameService.makeMove(gameId, playerId, row, col);
        if (updatedGame != null) {
            return ResponseEntity.ok(updatedGame);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/{gameId}/flag")
    public ResponseEntity<Game> toggleFlag(@PathVariable String gameId, @RequestBody ToggleFlagRequestDTO request) {
        String playerId = request.getPlayerId();
        Integer row = request.getRow();
        Integer col = request.getCol();

        if (playerId == null || row == null || col == null) {
            return ResponseEntity.badRequest().build();
        }

        Game updatedGame = gameService.toggleFlag(gameId, playerId, row, col);
        
        if (updatedGame != null) {
            return ResponseEntity.ok(updatedGame);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
} 