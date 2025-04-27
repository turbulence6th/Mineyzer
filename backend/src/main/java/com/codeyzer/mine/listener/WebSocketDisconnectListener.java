package com.codeyzer.mine.listener;

import com.codeyzer.mine.service.GameService;
import com.codeyzer.mine.service.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {

    private final WebSocketSessionRegistry sessionRegistry;
    private final GameService gameService;

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket session disconnected: {}", sessionId);

        WebSocketSessionRegistry.SessionInfo sessionInfo = sessionRegistry.unregisterSession(sessionId);

        if (sessionInfo != null) {
            log.info("Handling disconnect for Player ID: {}, Game ID: {}", sessionInfo.getPlayerId(), sessionInfo.getGameId());
            gameService.handlePlayerDisconnect(sessionInfo.getGameId(), sessionInfo.getPlayerId());
        } else {
            log.warn("No player/game information found for disconnected session: {}", sessionId);
        }
    }
} 