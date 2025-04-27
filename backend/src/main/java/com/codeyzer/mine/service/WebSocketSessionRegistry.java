package com.codeyzer.mine.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {

    @Getter
    @Setter
    public static class SessionInfo {
        private String playerId;
        private String gameId;

        public SessionInfo(String playerId, String gameId) {
            this.playerId = playerId;
            this.gameId = gameId;
        }

        public String getPlayerId() {
            return playerId;
        }

        public String getGameId() {
            return gameId;
        }
    }

    // Oturum ID -> Oyuncu Bilgisi
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, String playerId, String gameId) {
        sessions.put(sessionId, new SessionInfo(playerId, gameId));
        System.out.println("Session Registered: " + sessionId + " -> Player: " + playerId + ", Game: " + gameId); // Loglama
    }

    public SessionInfo unregisterSession(String sessionId) {
        SessionInfo info = sessions.remove(sessionId);
        if (info != null) {
            System.out.println("Session Unregistered: " + sessionId + " -> Player: " + info.getPlayerId() + ", Game: " + info.getGameId()); // Loglama
        } else {
            System.out.println("Session Unregister Attempt - Not Found: " + sessionId); // Loglama
        }
        return info;
    }

    public SessionInfo getSessionInfo(String sessionId) {
        return sessions.get(sessionId);
    }
} 