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
        if (player.getActionsLeft() < 1 || game.isGameOver()) return;
        
        Optional<Card> marketCardOpt = game.getMarket().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst();
                
        if (marketCardOpt.isPresent()) {
            Card card = marketCardOpt.get();
            
            game.getMarket().remove(card);
            player.getBoard().add(card);
            player.setActionsLeft(player.getActionsLeft() - 1);
            
            // Check for duplicates
            if (hasCharacterDuplicates(player)) {
                player.setMustDiscard(true);
            }
            
            gameService.refillMarket(game); // Ensure market has 5 cards
        }
    }

    public boolean useSkill(GameState game, Player player, ActionCommand cmd) {
        if (game.isGameOver()) return false;

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
                        game.setGameOver(true);
                        game.setWinnerId(player.getId());
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
                        moveToGraveyardOrExile(game, card);
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
            if (target.getType() == CardType.NUMBER) {
                targetPlayer.getBoard().remove(target);
                moveToGraveyardOrExile(game, target);
                return true;
            } else if (target.getType() == CardType.CHARACTER) {
                // Self destruct J
                player.getBoard().remove(jack);
                moveToGraveyardOrExile(game, jack);
                // Destroy target
                targetPlayer.getBoard().remove(target);
                moveToGraveyardOrExile(game, target);
                return true;
            }
        }
        return false;
    }

    private boolean executeQueenSkill(GameState game, Player player, ActionCommand cmd) {
        Player targetPlayer = getPlayerById(game, cmd.getTargetPlayerId());
        if (targetPlayer == null || targetPlayer.getId().equals(player.getId())) return false;

        Optional<Card> targetOpt = targetPlayer.getBoard().stream()
                .filter(c -> c.getId().equals(cmd.getTargetCardId()))
                .findFirst();

        if (targetOpt.isPresent()) {
            Card stolen = targetOpt.get();
            targetPlayer.getBoard().remove(stolen);
            stolen.setTapped(true); // Queen Nerf: Stolen card is tapped (cannot use immediately)
            player.getBoard().add(stolen);
            
            // Check for duplicates
            if (hasCharacterDuplicates(player)) {
                player.setMustDiscard(true);
            }
            
            return true;
        }
        return false;
    }

    private boolean executeNumberSkill(GameState game, Player player, Card spell, ActionCommand cmd) {
        Player targetPlayer = getPlayerById(game, cmd.getTargetPlayerId());
        
        switch (spell.getRank()) {
            case 2: // Curse
                if (targetPlayer != null && !targetPlayer.getId().equals(player.getId())) {
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

            case 5: // Refresh & Cleanse
                game.getMarket().clear();
                gameService.refillMarket(game);
                // Also cleanse target player (or self if no target)
                Player cleanseTarget = targetPlayer != null ? targetPlayer : player;
                for (Card c : cleanseTarget.getBoard()) {
                    c.setDisabled(false);
                }
                return true;

            case 6: // Odd Dig (3,5,7,9)
                if (player.isHasDugThisTurn()) return false;
                
                Card oddTarget = null;
                if (cmd.getTargetCardId() != null) {
                    oddTarget = findTargetInGraveyard(game, cmd.getTargetCardId(), List.of(3,5,7,9));
                } else {
                    List<Card> oddTargets = game.getGraveyard().stream()
                            .filter(c -> List.of(3,5,7,9).contains(c.getRank()))
                            .collect(java.util.stream.Collectors.toList());
                    if (!oddTargets.isEmpty()) {
                        oddTarget = oddTargets.get(random.nextInt(oddTargets.size()));
                    }
                }
                        
                if (oddTarget != null) {
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

            case 8: // Haste (Nerf: +1 Action)
                player.setActionsLeft(player.getActionsLeft() + 1);
                return true;

            case 9: // Revive J, Q, K
                if (player.isHasDugThisTurn() || cmd.getTargetCardId() == null) return false;
                
                Card charTarget = findTargetInGraveyard(game, cmd.getTargetCardId(), List.of(11, 12, 13));
                if (charTarget != null) {
                    game.getGraveyard().remove(charTarget);
                    charTarget.setResurrected(true);
                    player.getBoard().add(charTarget);
                    player.setHasDugThisTurn(true);
                    
                    if (hasCharacterDuplicates(player)) {
                        player.setMustDiscard(true);
                    }
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
        if (game.getPlayers().isEmpty() || game.isGameOver()) return "no_players";
        
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIdx());
        currentPlayer.setSilenced(false); 

        int nextPlayerIdx = (game.getCurrentPlayerIdx() + 1) % game.getPlayers().size();
        game.setCurrentPlayerIdx(nextPlayerIdx);
        
        Player nextPlayer = game.getPlayers().get(nextPlayerIdx);

        nextPlayer.setActionsLeft(nextPlayer.isSlowed() ? 1 : 2);
        nextPlayer.setSlowed(false);
        nextPlayer.setHasDugThisTurn(false);
        
        for (Card c : nextPlayer.getBoard()) {
            c.setTapped(false);
        }

        String eventMessage = "Turn ended.";

        if (nextPlayerIdx == 0) {
            game.setGlobalSilence(false);
            if (game.getDeck().isEmpty()) {
                calculateFinalScores(game);
                return "สำรับหมดแล้ว! ตัดสินผู้ชนะด้วยคะแนนรวม";
            }
            eventMessage = eventProcessor.triggerAPhet(game);
        }
        
        return eventMessage;
    }

    private void calculateFinalScores(GameState game) {
        game.setGameOver(true);
        Player winner = null;
        int maxScore = -1;
        for (Player p : game.getPlayers()) {
            int score = 0;
            for (Card c : p.getBoard()) {
                if (c.getType() == CardType.NUMBER && !c.isDisabled()) {
                    score += c.getRank();
                }
            }
            p.setScore(score);
            if (score > maxScore) {
                maxScore = score;
                winner = p;
            }
        }
        if (winner != null) {
            game.setWinnerId(winner.getId());
        }
    }

    private void moveToGraveyardOrExile(GameState game, Card card) {
        if (card.isResurrected()) {
            // EXILE: Card is removed from play entirely
            return;
        }
        game.getGraveyard().add(card);
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
            moveToGraveyardOrExile(game, cardToDiscard.get());
            
            if (!hasCharacterDuplicates(player)) {
                player.setMustDiscard(false);
            }
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
