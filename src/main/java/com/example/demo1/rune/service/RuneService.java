package com.example.demo1.rune.service;

import com.example.demo1.rune.model.RuneCard;
import com.example.demo1.rune.model.RuneGameState;
import com.example.demo1.rune.model.RunePlayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RuneService {
    private final Map<String, RuneGameState> games = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private void broadcast(String gameId) {
        RuneGameState state = games.get(gameId);
        if (state != null && messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/rune/" + gameId, state);
            } catch (Exception e) {
                System.err.println("Broadcast failed: " + e.getMessage());
            }
        }
    }

    public RuneGameState createGame() {
        String id = "RUNE-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        RuneGameState game = new RuneGameState();
        game.setGameId(id);
        games.put(id, game);
        System.out.println("Created game: " + id);
        return game;
    }

    public RuneGameState getGame(String id) {
        return games.get(id);
    }

    public RuneGameState joinGame(String gameId, String playerName) {
        RuneGameState game = games.get(gameId);
        if (game == null) {
            System.err.println("Join failed: Game not found " + gameId);
            return null;
        }
        
        synchronized (game.getPlayers()) {
            RunePlayer player = new RunePlayer();
            player.setId(UUID.randomUUID().toString());
            player.setName(playerName != null && !playerName.isBlank() ? playerName : "Player-" + (game.getPlayers().size() + 1));
            game.getPlayers().add(player);
        }
        
        broadcast(gameId);
        return game;
    }

    public void startBatch(RuneGameState game) {
        if (game.getPlayers().isEmpty()) {
            game.getLogs().add("⚠️ ไม่สามารถเริ่มเกมได้เนื่องจากไม่มีผู้เล่น");
            broadcast(game.getGameId());
            return;
        }
        game.setPhase("PLAY");
        game.getLogs().add("--- การต่อสู้เริ่มต้นขึ้น! ---");
        
        String[] types = {"FIRE", "WATER", "EARTH", "AIR", "LIGHT", "DARK"};
        game.getDeck().clear();
        for (int i = 0; i < 60; i++) {
            game.getDeck().add(new RuneCard(UUID.randomUUID().toString(), types[i % 6], (i % 3) + 1, false));
        }
        Collections.shuffle(game.getDeck());
        
        for (RunePlayer p : game.getPlayers()) {
            p.getHand().clear();
            p.setHealth(30);
            p.setMana(1);
            p.setShield(0);
            for (int j = 0; j < 5; j++) {
                drawCard(game, p);
            }
        }
        game.setActivePlayerId(game.getPlayers().get(0).getId());
        broadcast(game.getGameId());
    }

    public void playCard(String gameId, String playerId, String cardId) {
        RuneGameState game = games.get(gameId);
        if (game == null || !game.getActivePlayerId().equals(playerId)) return;

        RunePlayer player = findByPlayerId(game, playerId);
        RuneCard card = player.getHand().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst().orElse(null);

        if (card != null && player.getMana() >= card.getPower()) {
            player.setMana(player.getMana() - card.getPower());
            player.getHand().remove(card);
            applyRuneEffect(game, player, card);
            game.getLogs().add(player.getName() + " ร่ายรูน " + card.getRuneType() + " (Power " + card.getPower() + ")");
            checkWinCondition(game);
            broadcast(gameId);
        }
    }

    private void applyRuneEffect(RuneGameState game, RunePlayer player, RuneCard card) {
        RunePlayer opponent = game.getPlayers().stream()
                .filter(p -> !p.getId().equals(player.getId()))
                .findFirst().orElse(null);

        switch (card.getRuneType()) {
            case "FIRE":
                if (opponent != null) dealDamage(opponent, card.getPower() * 2);
                break;
            case "WATER":
                player.setHealth(Math.min(30, player.getHealth() + card.getPower() * 2));
                drawCard(game, player);
                break;
            case "EARTH":
                player.setShield(player.getShield() + card.getPower() * 3);
                break;
            case "AIR":
                player.setMana(player.getMana() + card.getPower());
                drawCard(game, player);
                drawCard(game, player);
                break;
            case "LIGHT":
                player.setHealth(Math.min(30, player.getHealth() + card.getPower()));
                if (opponent != null) dealDamage(opponent, card.getPower());
                break;
            case "DARK":
                if (opponent != null) {
                    dealDamage(opponent, card.getPower() * 2);
                    opponent.setMana(Math.max(0, opponent.getMana() - 1));
                }
                break;
        }
    }

    private void dealDamage(RunePlayer target, int damage) {
        if (target.getShield() >= damage) {
            target.setShield(target.getShield() - damage);
        } else {
            int remaining = damage - target.getShield();
            target.setShield(0);
            target.setHealth(Math.max(0, target.getHealth() - remaining));
        }
    }

    private void drawCard(RuneGameState game, RunePlayer player) {
        if (!game.getDeck().isEmpty()) {
            player.getHand().add(game.getDeck().remove(0));
        }
    }

    public void endTurn(String gameId, String playerId) {
        RuneGameState game = games.get(gameId);
        if (game == null || !game.getActivePlayerId().equals(playerId)) return;

        RunePlayer currentPlayer = findByPlayerId(game, playerId);
        
        int currentIndex = game.getPlayers().indexOf(currentPlayer);
        int nextIndex = (currentIndex + 1) % game.getPlayers().size();
        RunePlayer nextPlayer = game.getPlayers().get(nextIndex);
        
        game.setActivePlayerId(nextPlayer.getId());
        game.setTurn(game.getTurn() + 1);
        
        nextPlayer.setMana(Math.min(10, 1 + (game.getTurn() / 2)));
        drawCard(game, nextPlayer);
        
        game.getLogs().add("--- จบเทิร์น " + (game.getTurn()-1) + " : เริ่มเทิร์นของ " + nextPlayer.getName() + " ---");
        broadcast(gameId);
    }

    private RunePlayer findByPlayerId(RuneGameState game, String id) {
        return game.getPlayers().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    private void checkWinCondition(RuneGameState game) {
        for (RunePlayer p : game.getPlayers()) {
            if (p.getHealth() <= 0) {
                game.setPhase("END");
                RunePlayer winner = game.getPlayers().stream()
                        .filter(player -> player.getHealth() > 0)
                        .findFirst().orElse(null);
                game.getLogs().add("🎉 ผู้ชนะคือ: " + (winner != null ? winner.getName() : "เสมอ!"));
                broadcast(game.getGameId());
            }
        }
    }
}
