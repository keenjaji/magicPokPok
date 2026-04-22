package com.example.demo1.service;

import com.example.demo1.model.Card;
import com.example.demo1.model.GameState;
import com.example.demo1.model.Player;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {
    
    private final ConcurrentHashMap<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final DeckFactory deckFactory;

    public GameService(DeckFactory deckFactory) {
        this.deckFactory = deckFactory;
    }

    public GameState createGame() {
        String gameId = UUID.randomUUID().toString().substring(0, 8);
        GameState game = new GameState(gameId);
        activeGames.put(gameId, game);
        return game;
    }

    public GameState joinGame(String gameId, String playerName) {
        GameState game = activeGames.get(gameId);
        if (game != null && game.getDeck().isEmpty()) { // Only allow joining before game starts (deck not initialized)
            String playerId = UUID.randomUUID().toString().substring(0, 8);
            Player player = new Player(playerId, playerName);
            game.getPlayers().add(player);
        }
        return game;
    }

    public GameState startGame(String gameId) {
        GameState game = activeGames.get(gameId);
        if (game != null && game.getPlayers().size() >= 2) {
            game.setDeck(deckFactory.createStandardDeck());
            refillMarket(game);
        }
        return game;
    }

    public void refillMarket(GameState game) {
        // Market always holds up to 5 cards if deck has enough cards
        while (game.getMarket().size() < 5 && !game.getDeck().isEmpty()) {
            Card drawn = game.getDeck().remove(0); // pop from top
            game.getMarket().add(drawn);
        }
    }
    
    public GameState getGame(String gameId) {
        return activeGames.get(gameId);
    }
}
