package com.example.demo1.rune.controller;

import com.example.demo1.rune.model.RuneGameState;
import com.example.demo1.rune.service.RuneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class RuneWebSocketController {

    @Autowired
    private RuneService runeService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/rune/{gameId}/action")
    public void handleAction(@DestinationVariable String gameId, String action) {
        // Broadcast the update to all players in the room
        RuneGameState state = runeService.getGame(gameId);
        if (state != null) {
            messagingTemplate.convertAndSend("/topic/rune/" + gameId, state);
        }
    }
}
