package com.example.demo1.rune.controller;

import com.example.demo1.rune.model.RuneGameState;
import com.example.demo1.rune.service.RuneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rune")
public class RuneRestController {

    @Autowired
    private RuneService runeService;

    @PostMapping("/create")
    public RuneGameState createGame() {
        return runeService.createGame();
    }

    @GetMapping("/{gameId}")
    public RuneGameState getGame(@PathVariable String gameId) {
        return runeService.getGame(gameId);
    }

    @PostMapping("/join/{gameId}")
    public RuneGameState joinGame(@PathVariable String gameId, @RequestParam(required = false, defaultValue = "ผู้พิทักษ์") String playerName) {
        return runeService.joinGame(gameId, playerName);
    }

    @PostMapping("/{gameId}/start")
    public RuneGameState startGame(@PathVariable String gameId) {
        RuneGameState game = runeService.getGame(gameId);
        if (game != null) {
            runeService.startBatch(game);
        }
        return game;
    }

    @PostMapping("/{gameId}/play")
    public RuneGameState playCard(@PathVariable String gameId, @RequestParam String playerId, @RequestParam String cardId) {
        runeService.playCard(gameId, playerId, cardId);
        return runeService.getGame(gameId);
    }

    @PostMapping("/{gameId}/end-turn")
    public RuneGameState endTurn(@PathVariable String gameId, @RequestParam String playerId) {
        runeService.endTurn(gameId, playerId);
        return runeService.getGame(gameId);
    }
}
