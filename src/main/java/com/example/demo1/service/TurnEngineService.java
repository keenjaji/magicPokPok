package com.example.demo1.service;

import com.example.demo1.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class TurnEngineService {

    private final GameService gameService;
    private final Random random = new Random();

    public TurnEngineService(GameService gameService) {
        this.gameService = gameService;
    }

    public void takeFromMarket(GameState game, Player player, String cardId) {
        if (player.getActionsLeft() < 1) return;
        
        Optional<Card> marketCardOpt = game.getMarket().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst();
                
        if (marketCardOpt.isPresent()) {
            Card card = marketCardOpt.get();
            
            game.getMarket().remove(card);
            player.getBoard().add(card);
            player.setActionsLeft(player.getActionsLeft() - 1);
            gameService.refillMarket(game); // Ensure market has 5 cards
        }
    }

    public boolean useSkill(GameState game, Player player, ActionCommand cmd) {
        // Find source card on player's board
        Optional<Card> sourceCardOpt = player.getBoard().stream()
                .filter(c -> c.getId().equals(cmd.getSourceCardId()))
                .findFirst();

        if (sourceCardOpt.isEmpty()) return false;
        Card card = sourceCardOpt.get();

        if (card.getType() == CardType.CHARACTER && player.isSilenced()) {
            return false; // Silenced, cannot use Character skills
        }
        if (card.getType() == CardType.VICTORY && game.isGlobalSilence()) {
            return false; // Global silence prevents victory conditions
        }

        boolean success = false;
        
        // Execute based on rank
        switch (card.getRank()) {
            case 1: // Ace (Energy)
                if (player.getActionsLeft() >= 1 && !card.isTapped()) {
                    card.setTapped(true);
                    player.setActionsLeft(player.getActionsLeft() - 1);
                    success = true;
                }
                break;
            case 13: // King (Finisher)
                if (player.getActionsLeft() >= 2) {
                    boolean hasTappedA = player.getBoard().stream().anyMatch(c -> c.getRank() == 1 && c.isTapped());
                    if (hasTappedA) {
                        // VICTORY! Handled by controller/broadcaster
                        player.setActionsLeft(0); // Optional: signal end
                        success = true;
                    }
                }
                break;
            case 11: // Jack (Destroyer)
                if (player.getActionsLeft() >= 1 && !card.isTapped()) {
                    success = executeJackSkill(game, player, card, cmd);
                    if (success) {
                        card.setTapped(true);
                        player.setActionsLeft(player.getActionsLeft() - 1);
                    }
                }
                break;
            case 12: // Queen (Thief)
                if (player.getActionsLeft() >= 1 && !card.isTapped()) {
                    success = executeQueenSkill(game, player, cmd);
                    if (success) {
                        card.setTapped(true);
                        player.setActionsLeft(player.getActionsLeft() - 1);
                    }
                }
                break;
            default: // Number Cards (2-10)
                if (player.getActionsLeft() >= 1) {
                    success = executeNumberSkill(game, player, card, cmd);
                    if (success) {
                        player.setActionsLeft(player.getActionsLeft() - 1);
                        player.getBoard().remove(card);
                        game.getGraveyard().add(card);
                    }
                }
                break;
        }

        return success;
    }

    private boolean executeJackSkill(GameState game, Player player, Card jack, ActionCommand cmd) {
        Player targetPlayer = getPlayerById(game, cmd.getTargetPlayerId());
        if (targetPlayer == null) return false;

        Optional<Card> targetOpt = targetPlayer.getBoard().stream()
                .filter(c -> c.getId().equals(cmd.getTargetCardId()))
                .findFirst();

        if (targetOpt.isPresent()) {
            Card target = targetOpt.get();
            // J destroys 2-10 (numbers) -> Just destroy target
            // OR J self-destructs to destroy J, Q, K
            if (target.getType() == CardType.NUMBER) {
                targetPlayer.getBoard().remove(target);
                game.getGraveyard().add(target);
                return true;
            } else if (target.getType() == CardType.CHARACTER) {
                // Self destruct J
                player.getBoard().remove(jack);
                game.getGraveyard().add(jack);
                // Destroy target
                targetPlayer.getBoard().remove(target);
                game.getGraveyard().add(target);
                return true;
            }
        }
        return false;
    }

    private boolean executeQueenSkill(GameState game, Player player, ActionCommand cmd) {
        Player targetPlayer = getPlayerById(game, cmd.getTargetPlayerId());
        if (targetPlayer == null) return false;

        Optional<Card> targetOpt = targetPlayer.getBoard().stream()
                .filter(c -> c.getId().equals(cmd.getTargetCardId()))
                .findFirst();

        if (targetOpt.isPresent()) {
            Card stolen = targetOpt.get();
            targetPlayer.getBoard().remove(stolen);
            player.getBoard().add(stolen);
            return true;
        }
        return false;
    }

    private boolean executeNumberSkill(GameState game, Player player, Card spell, ActionCommand cmd) {
        Player targetPlayer = getPlayerById(game, cmd.getTargetPlayerId());
        Card targetCard = null;
        if (targetPlayer != null && cmd.getTargetCardId() != null) {
            targetCard = targetPlayer.getBoard().stream()
                    .filter(c -> c.getId().equals(cmd.getTargetCardId()))
                    .findFirst().orElse(null);
        }

        switch (spell.getRank()) {
            case 2: // Curse (Steal highest point, default lock)
                if (targetPlayer != null) {
                    Card maxPointCard = null;
                    for (Card c : targetPlayer.getBoard()) {
                        if (c.getType() == CardType.NUMBER && (maxPointCard == null || c.getRank() > maxPointCard.getRank())) {
                            maxPointCard = c;
                        }
                    }
                    if (maxPointCard != null) {
                        targetPlayer.getBoard().remove(maxPointCard);
                        maxPointCard.setDisabled(true);
                        player.getBoard().add(maxPointCard);
                        return true;
                    }
                }
                return false;

            case 3: // Silence
                if (targetPlayer != null) {
                    targetPlayer.setSilenced(true);
                    return true;
                }
                return false;

            case 4: // Blind Draw (from deck)
                if (!game.getDeck().isEmpty()) {
                    Card drawn = game.getDeck().remove(0);
                    player.getBoard().add(drawn);
                    return true;
                }
                return false;

            case 5: // Refresh
                game.getMarket().clear();
                gameService.refillMarket(game);
                return true;

            case 6: // Odd Dig (3,5,7,9)
                if (player.isHasDugThisTurn()) return false;
                
                List<Card> oddTargets = game.getGraveyard().stream()
                        .filter(c -> List.of(3,5,7,9).contains(c.getRank()))
                        .collect(java.util.stream.Collectors.toList());
                        
                if (!oddTargets.isEmpty()) {
                    Card oddTarget = oddTargets.get(random.nextInt(oddTargets.size()));
                    game.getGraveyard().remove(oddTarget);
                    oddTarget.setResurrected(true);
                    player.getBoard().add(oddTarget);
                    player.setHasDugThisTurn(true);
                    return true;
                }
                return false;

            case 7: // Slow
                if (targetPlayer != null) {
                    targetPlayer.setSlowed(true);
                    return true;
                }
                return false;

            case 8: // Haste
                player.setActionsLeft(player.getActionsLeft() + 2);
                return true;

            case 9: // Revive J, Q, K
                if (player.isHasDugThisTurn() || cmd.getTargetCardId() == null) return false;
                
                // Filter ranks that player does not already have
                List<Integer> missingChars = new java.util.ArrayList<>();
                for (int r : List.of(11, 12, 13)) {
                    int checkR = r;
                    if (player.getBoard().stream().noneMatch(c -> c.getRank() == checkR)) {
                        missingChars.add(r);
                    }
                }
                if (missingChars.isEmpty()) return false;

                Card charTarget = findTargetInGraveyard(game, cmd.getTargetCardId(), missingChars);
                if (charTarget != null) {
                    game.getGraveyard().remove(charTarget);
                    charTarget.setResurrected(true);
                    player.getBoard().add(charTarget);
                    player.setHasDugThisTurn(true);
                    return true;
                }
                return false;

            case 10: // Even Dig (2,4,8)
                if (player.isHasDugThisTurn() || cmd.getTargetCardId() == null) return false;
                
                Card evenTarget = findTargetInGraveyard(game, cmd.getTargetCardId(), List.of(2,4,8));
                if (evenTarget != null) {
                    game.getGraveyard().remove(evenTarget);
                    evenTarget.setResurrected(true);
                    player.getBoard().add(evenTarget);
                    player.setHasDugThisTurn(true);
                    return true;
                }
                return false;
        }
        return false;
    }

    public String endTurn(GameState game, EventProcessorService eventProcessor) {
        if (game.getPlayers().isEmpty()) return "no_players";
        
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIdx());
        // Clean up current player end of turn states
        currentPlayer.setSilenced(false); 

        // Move to next player
        int nextPlayerIdx = (game.getCurrentPlayerIdx() + 1) % game.getPlayers().size();
        game.setCurrentPlayerIdx(nextPlayerIdx);
        
        Player nextPlayer = game.getPlayers().get(nextPlayerIdx);

        // Prep next player turn
        nextPlayer.setActionsLeft(nextPlayer.isSlowed() ? 1 : 2);
        nextPlayer.setSlowed(false); // consume slow effect
        nextPlayer.setHasDugThisTurn(false);
        
        // Untap Character cards at the start of their turn (A cards stay tapped)
        for (Card c : nextPlayer.getBoard()) {
            if (c.getType() == CardType.CHARACTER) {
                c.setTapped(false);
            }
        }

        String eventMessage = "Turn ended.";

        // If nextPlayer wraps to 0 -> Round ends! A-Phet sequence triggers here
        if (nextPlayerIdx == 0) {
            // Turn off global silence from last round
            game.setGlobalSilence(false);
            
            eventMessage = eventProcessor.triggerAPhet(game);
        }
        
        return eventMessage;
    }

    public boolean hasCharacterDuplicates(Player player) {
        long jCount = player.getBoard().stream().filter(c -> c.getRank() == 11).count();
        long qCount = player.getBoard().stream().filter(c -> c.getRank() == 12).count();
        long kCount = player.getBoard().stream().filter(c -> c.getRank() == 13).count();
        return jCount > 1 || qCount > 1 || kCount > 1;
    }

    public void discardDuplicate(GameState game, Player player, String cardId) {
        if (!hasCharacterDuplicates(player)) return;
        
        Optional<Card> cardToDiscard = player.getBoard().stream()
                .filter(c -> c.getId().equals(cardId) && c.getType() == CardType.CHARACTER)
                .findFirst();
                
        if (cardToDiscard.isPresent()) {
            player.getBoard().remove(cardToDiscard.get());
            game.getGraveyard().add(cardToDiscard.get());
        }
    }

    private Player getPlayerById(GameState game, String id) {
        if(id == null) return null;
        return game.getPlayers().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    private Card findTargetInGraveyard(GameState game, String targetId, List<Integer> validRanks) {
        if(targetId == null) return null;
        return game.getGraveyard().stream()
                .filter(c -> c.getId().equals(targetId) && validRanks.contains(c.getRank()))
                .findFirst().orElse(null);
    }
}
