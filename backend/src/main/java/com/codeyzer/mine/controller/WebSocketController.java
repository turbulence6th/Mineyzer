package com.codeyzer.mine.controller;

import com.codeyzer.mine.model.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastGameUpdate(Game game) {
        messagingTemplate.convertAndSend("/topic/games/" + game.getId(), game);
    }
} 