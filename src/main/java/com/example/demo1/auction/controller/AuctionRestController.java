package com.example.demo1.auction.controller;

import com.example.demo1.auction.model.AuctionGameState;
import com.example.demo1.auction.service.AuctionEngineService;
import com.example.demo1.auction.service.AuctionGameService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auction")
public class AuctionRestController {

    private final AuctionGameService gameService;
    private final AuctionEngineService engineService;
    private final SimpMessagingTemplate messagingTemplate;

    public AuctionRestController(AuctionGameService gameService, AuctionEngineService engineService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.engineService = engineService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/create")
    public AuctionGameState createGame() {
        return gameService.createGame();
    }

    @PostMapping("/join/{gameId}")
    public AuctionGameState joinGame(@PathVariable String gameId, @RequestParam String playerName) {
        AuctionGameState game = gameService.joinGame(gameId, playerName);
        if (game != null) {
            messagingTemplate.convertAndSend("/topic/auction/" + gameId, game);
        }
        return game;
    }

    @PostMapping("/start/{gameId}")
    public AuctionGameState startGame(@PathVariable String gameId) {
        AuctionGameState game = gameService.getGame(gameId);
        if (game != null) {
            engineService.startGame(game);
            messagingTemplate.convertAndSend("/topic/auction/" + gameId, game);
        }
        return game;
    }

    @PostMapping("/next/{gameId}")
    public AuctionGameState nextRound(@PathVariable String gameId) {
        AuctionGameState game = gameService.getGame(gameId);
        if (game != null) {
            engineService.nextRound(game);
            messagingTemplate.convertAndSend("/topic/auction/" + gameId, game);
        }
        return game;
    }

    @PostMapping("/reset/{gameId}")
    public AuctionGameState resetGame(@PathVariable String gameId) {
        AuctionGameState game = gameService.getGame(gameId);
        if (game != null) {
            engineService.resetToLobby(game);
            messagingTemplate.convertAndSend("/topic/auction/" + gameId, game);
        }
        return game;
    }

    @GetMapping("/{gameId}")
    public AuctionGameState getGame(@PathVariable String gameId) {
        return gameService.getGame(gameId);
    }
}
