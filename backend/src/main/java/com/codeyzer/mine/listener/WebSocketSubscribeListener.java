package com.codeyzer.mine.listener;

import com.codeyzer.mine.service.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSubscribeListener implements ApplicationListener<SessionSubscribeEvent> {

    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public void onApplicationEvent(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        // Check if the subscription is for a game topic
        if (sessionId != null && destination != null && destination.startsWith("/topic/games/")) {
            try {
                String gameId = destination.substring("/topic/games/".length());

                // Oyuncu ID'sini ve Oyun ID'sini başlıklardan al
                // Not: İstemcinin (frontend) bu başlıkları göndermesi gerekecek!
                String playerId = headerAccessor.getFirstNativeHeader("playerId");
                String headerGameId = headerAccessor.getFirstNativeHeader("gameId");

                if (playerId != null && headerGameId != null && headerGameId.equals(gameId)) {
                    log.info("Registering session {} for Player {} in Game {}", sessionId, playerId, gameId);
                    sessionRegistry.registerSession(sessionId, playerId, gameId);
                } else {
                    log.warn("Could not register session {}: Missing or mismatched playerId/gameId headers for destination {}", sessionId, destination);
                    if(playerId == null) log.warn("playerId header is missing");
                    if(headerGameId == null) log.warn("gameId header is missing");
                    if(headerGameId != null && !headerGameId.equals(gameId)) log.warn("gameId header ({}) does not match destination ({})", headerGameId, gameId);
                }
            } catch (Exception e) {
                log.error("Error processing subscription event for session {}: {}", sessionId, e.getMessage());
            }
        }
    }
} 