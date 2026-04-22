package com.example.demo1.controller;

import com.example.demo1.model.GameState;
import com.example.demo1.service.GameService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
public class GameRestController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameRestController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/create")
    public GameState createGame() {
        return gameService.createGame();
    }

    @PostMapping("/join/{gameId}")
    public GameState joinGame(@PathVariable String gameId, @RequestParam String playerName) {
        GameState game = gameService.joinGame(gameId, playerName);
        if (game != null) {
            messagingTemplate.convertAndSend("/topic/game/" + gameId, game);
        }
        return game;
    }

    @PostMapping("/start/{gameId}")
    public GameState startGame(@PathVariable String gameId) {
        GameState game = gameService.startGame(gameId);
        if (game != null) {
            messagingTemplate.convertAndSend("/topic/game/" + gameId, game);
        }
        return game;
    }

    @GetMapping("/{gameId}")
    public GameState getGame(@PathVariable String gameId) {
        return gameService.getGame(gameId);
    }
}
