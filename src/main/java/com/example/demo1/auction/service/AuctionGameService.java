package com.example.demo1.auction.service;

import com.example.demo1.auction.model.AuctionGameState;
import com.example.demo1.auction.model.AuctionPlayer;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuctionGameService {
    private final Map<String, AuctionGameState> games = new ConcurrentHashMap<>();

    public AuctionGameState createGame() {
        String gameId = "AUC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        AuctionGameState game = new AuctionGameState(gameId);
        games.put(gameId, game);
        return game;
    }

    public AuctionGameState getGame(String gameId) {
        return games.get(gameId);
    }

    public AuctionGameState joinGame(String gameId, String playerName) {
        AuctionGameState game = games.get(gameId);
        if (game == null) return null;
        
        if (game.getPhase().equals("WAITING_FOR_PLAYERS") && game.getPlayers().size() < 6) {
            String playerId = UUID.randomUUID().toString();
            AuctionPlayer player = new AuctionPlayer(playerId, playerName);
            game.getPlayers().add(player);
        }
        return game;
    }
}
