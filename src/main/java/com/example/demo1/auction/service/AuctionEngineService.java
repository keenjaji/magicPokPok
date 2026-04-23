package com.example.demo1.auction.service;

import com.example.demo1.auction.model.AuctionCard;
import com.example.demo1.auction.model.AuctionGameState;
import com.example.demo1.auction.model.AuctionPlayer;
import com.example.demo1.auction.model.AuctionSuit;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuctionEngineService {

    public void resetGame(AuctionGameState game) {
        game.setPhase("WAITING_FOR_PLAYERS");
        game.setRound(1);
        game.setSubRound(1);
        game.setDeck(new ArrayList<>());
        game.setMarket(new ArrayList<>());
        game.setMasterCard(null);
        game.setPickOrderPlayerIds(new ArrayList<>());
        game.getJackSkillOptions().clear();
        
        for (AuctionPlayer p : game.getPlayers()) {
            p.getHand().clear();
            p.getScorePile().clear();
            p.setTotalScore(0);
            p.setRoundScore(0);
            p.setRich(false);
            p.setPoor(false);
            p.setLegacyCardId(null);
            p.setFirstPlace(false);
            p.setUnderdog(false);
            p.setHasBid(false);
            p.setHasPickedMarket(false);
            p.setCurrentBid(null);
            p.getScoreBreakdown().clear();
        }
    }

    public void startGame(AuctionGameState game) {
        if (game.getPlayers().size() < 2) return;
        
        game.setPhase("MASTER_REVEAL");
        game.setRound(1);
        game.setSubRound(1);
        
        // Build Deck
        game.getDeck().clear();
        for (AuctionSuit suit : AuctionSuit.values()) {
            for (int r = 1; r <= 13; r++) {
                String id = suit.name().substring(0, 1) + "_" + r + "_" + UUID.randomUUID().toString().substring(0,4);
                game.getDeck().add(new AuctionCard(id, suit, r));
            }
        }
        Collections.shuffle(game.getDeck());
        
        // Draw Master Card once per Big Round
        AuctionCard master = game.getDeck().remove(0);
        game.setMasterCard(master);
        applyMasterCardEvent(game, master);
        
        // Determine Hand Size & Market Size based on players
        int n = game.getPlayers().size();
        int handSize = game.getInitialHandSize();
        int marketSize = n - 1;
        
        if (game.isAutoHandSize()) {
            if (n == 2) handSize = 10;
            else if (n == 3) handSize = 8;
            else if (n == 4) handSize = 6;
            else if (n == 5) handSize = 5;
            else if (n == 6) handSize = 4;
            game.setInitialHandSize(handSize);
        }
        
        // Deal Hands
        for (AuctionPlayer p : game.getPlayers()) {
            p.getHand().clear();
            p.getScorePile().clear();
            p.setCurrentBid(null);
            p.setTotalScore(0);
            p.setFirstPlace(false);
            p.setUnderdog(false);
            
            // Rich/Poor logic for later rounds could go here
            for (int i = 0; i < handSize; i++) {
                p.getHand().add(game.getDeck().remove(0));
            }
        }
        
        setupSubRound(game, marketSize);
    }
    
    private void setupSubRound(AuctionGameState game, int marketSizeBase) {
        if (game.getDeck().isEmpty()) {
            game.setPhase("ROUND_END");
            return;
        }
        
        int marketSize = marketSizeBase;
        if (game.isBlackEvent()) {
            marketSize = game.getPlayers().size();
        }

        // Reset player status
        for (AuctionPlayer p : game.getPlayers()) {
            p.setFirstPlace(false);
            p.setUnderdog(false);
            p.setHasBid(false);
            p.setHasPickedMarket(false);
            p.setCurrentBid(null);
        }
        
        game.getPickOrderPlayerIds().clear();
        
        game.getMarket().clear();
        for (int i = 0; i < marketSize; i++) {
            if (!game.getDeck().isEmpty()) {
                AuctionCard c = game.getDeck().remove(0);
                if (game.isMysteryMarket()) c.setFaceDown(true);
                game.getMarket().add(c);
            }
        }
        
        game.setPhase("BIDDING");
    }

    private void applyMasterCardEvent(AuctionGameState game, AuctionCard master) {
        game.setMysteryMarket(false);
        game.setBlackEvent(false);
        game.setEventDescription("");
        
        if (master.getRank() >= 10 || master.getRank() == 1) {
            boolean isRed = master.getSuit() == AuctionSuit.HEART || master.getSuit() == AuctionSuit.DIAMOND;
            if (isRed) {
                game.setMysteryMarket(true);
                game.setEventDescription("🔴 MYSTERY MARKET: Market cards are face down!");
            } else {
                game.setBlackEvent(true);
                game.setEventDescription("⚫ BLACK EVENT: Everyone gets a card! (No Underdog x2)");
            }
        }
    }

    public void resetToLobby(AuctionGameState game) {
        game.setRound(1);
        game.setSubRound(1);
        game.setPhase("WAITING_FOR_PLAYERS");
        game.setDeck(new ArrayList<>());
        game.setMarket(new ArrayList<>());
        game.setMasterCard(null);
        game.setJackSkillOptions(new ArrayList<>());
        
        for (AuctionPlayer p : game.getPlayers()) {
            p.getHand().clear();
            p.getScorePile().clear();
            p.setCurrentBid(null);
            p.setTotalScore(0);
            p.setRoundScore(0);
            p.getScoreBreakdown().clear();
            p.setFirstPlace(false);
            p.setUnderdog(false);
            p.setHasPickedMarket(false);
            p.setHasBid(false);
            p.setRich(false);
            p.setPoor(false);
            p.setLegacyCardId(null);
        }
    }
    
    public void placeBid(AuctionGameState game, String playerId, String cardId) {
        if (!game.getPhase().equals("BIDDING")) return;
        
        AuctionPlayer player = null;
        for (AuctionPlayer p : game.getPlayers()) {
            if (p.getId().equals(playerId)) {
                player = p; break;
            }
        }
        if (player == null || player.isHasBid()) return;
        
        AuctionCard bidCard = null;
        for (int i = 0; i < player.getHand().size(); i++) {
            if (player.getHand().get(i).getId().equals(cardId)) {
                bidCard = player.getHand().remove(i);
                break;
            }
        }
        
        if (bidCard != null) {
            player.setCurrentBid(bidCard);
            player.setHasBid(true);
        }
        
        // Check if all players who HAVE cards have bid
        boolean allBid = true;
        for (AuctionPlayer p : game.getPlayers()) {
            if (!p.getHand().isEmpty() && !p.isHasBid()) {
                allBid = false;
                break;
            }
        }
        
        if (allBid) {
            resolveSubRound(game);
        }
    }
    
    private void resolveSubRound(AuctionGameState game) {
        game.setPhase("RESOLUTION");
        
        List<AuctionPlayer> activePlayers = new ArrayList<>();
        for (AuctionPlayer p : game.getPlayers()) {
            if (p.isHasBid()) activePlayers.add(p);
        }
        
        if (activePlayers.isEmpty()) {
            game.setSubRound(game.getSubRound() + 1);
            setupSubRound(game, game.getPlayers().size() - 1);
            return;
        }

        activePlayers.sort((p1, p2) -> {
            AuctionCard c1 = p1.getCurrentBid();
            AuctionCard c2 = p2.getCurrentBid();
            
            // Power: K(13)>Q(12)>J(11)>10..>2>A(1)
            int r1 = c1.getRank() == 1 ? 1 : c1.getRank();
            int r2 = c2.getRank() == 1 ? 1 : c2.getRank();
            
            if (r1 != r2) {
                return Integer.compare(r2, r1); // Descending
            }
            
            // Tie breaker: SPADE > HEART > DIAMOND > CLUB
            int s1 = getSuitValue(c1.getSuit());
            int s2 = getSuitValue(c2.getSuit());
            return Integer.compare(s2, s1);
        });
        
        for (AuctionPlayer p : activePlayers) p.setFirstPlace(false);
        for (AuctionPlayer p : activePlayers) p.setUnderdog(false);
        
        activePlayers.get(0).setFirstPlace(true);
        if (activePlayers.size() > 1) {
            activePlayers.get(activePlayers.size() - 1).setUnderdog(true);
        }
        
        AuctionCard firstBid = activePlayers.get(0).getCurrentBid();
        
        // Q and K Auto-skills
        if (firstBid != null && !game.getDeck().isEmpty()) {
            if (firstBid.getRank() == 12) { // Queen
                activePlayers.get(0).getHand().add(game.getDeck().remove(0));
            } else if (firstBid.getRank() == 13) { // King
                activePlayers.get(0).getScorePile().add(game.getDeck().remove(0));
            }
        }
        
        game.getPickOrderPlayerIds().clear();
        for (AuctionPlayer p : activePlayers) {
            game.getPickOrderPlayerIds().add(p.getId());
        }
        
        // J Skill Check BEFORE picking
        if (firstBid != null && firstBid.getRank() == 11 && !game.getMarket().isEmpty() && !game.getDeck().isEmpty()) {
            game.setPhase("J_SKILL");
            game.getJackSkillOptions().clear();
            game.getJackSkillOptions().add(game.getDeck().remove(0));
            if (!game.getDeck().isEmpty()) {
                game.getJackSkillOptions().add(game.getDeck().remove(0));
            }
            return; // Wait for J skill swap, then they will pick
        }
        
        advancePickQueue(game);
    }
    
    private int getSuitValue(AuctionSuit suit) {
        if (suit == AuctionSuit.SPADE) return 4;
        if (suit == AuctionSuit.HEART) return 3;
        if (suit == AuctionSuit.DIAMOND) return 2;
        return 1; // CLUB
    }

    public void pickMarketCard(AuctionGameState game, String playerId, String cardId) {
        if (!game.getPhase().equals("RESOLUTION")) return;
        
        // Ensure it's this player's turn to pick
        if (game.getPickOrderPlayerIds().isEmpty()) return;
        
        AuctionPlayer player = null;
        for (AuctionPlayer p : game.getPlayers()) {
            if (p.getId().equals(playerId)) {
                player = p; break;
            }
        }
        if (player == null) return;
                               
        if (player.isUnderdog() && !game.isBlackEvent()) {
            // Can't pick, automatically resolved
            finishSubRoundForPlayer(player, game, null);
            game.getPickOrderPlayerIds().remove(playerId);
        } else if (game.getMarket().isEmpty()) {
            finishSubRoundForPlayer(player, game, null);
            game.getPickOrderPlayerIds().remove(playerId);
        } else if (game.getPickOrderPlayerIds().get(0).equals(playerId)) {
            // It's their turn
            AuctionCard picked = null;
            if (cardId != null) {
                for (int i=0; i<game.getMarket().size(); i++) {
                    if (game.getMarket().get(i).getId().equals(cardId)) {
                        picked = game.getMarket().remove(i);
                        picked.setFaceDown(false);
                        break;
                    }
                }
            }
            if (picked != null) {
                finishSubRoundForPlayer(player, game, picked);
                game.getPickOrderPlayerIds().remove(0);
            }
        }
        
        advancePickQueue(game);
        
        if (game.getPickOrderPlayerIds().isEmpty()) {
            // End of subround
            game.setSubRound(game.getSubRound() + 1);
            
            // Check if ALL players are out of cards
            boolean allEmpty = game.getPlayers().stream().allMatch(p -> p.getHand().isEmpty());
            if (allEmpty) {
                calculateScores(game);
            } else {
                setupSubRound(game, game.getPlayers().size() - 1);
            }
        }
    }
    
    private void advancePickQueue(AuctionGameState game) {
        while (!game.getPickOrderPlayerIds().isEmpty()) {
            String nextId = game.getPickOrderPlayerIds().get(0);
            AuctionPlayer nextP = game.getPlayers().stream().filter(p -> p.getId().equals(nextId)).findFirst().orElse(null);
            if (nextP != null && nextP.isUnderdog() && !game.isBlackEvent()) {
                finishSubRoundForPlayer(nextP, game, null);
                game.getPickOrderPlayerIds().remove(0);
            } else if (game.getMarket().isEmpty()) {
                finishSubRoundForPlayer(nextP, game, null);
                game.getPickOrderPlayerIds().remove(0);
            } else {
                break;
            }
        }
    }
    
    private void finishSubRoundForPlayer(AuctionPlayer p, AuctionGameState game, AuctionCard marketCard) {
        // Everyone adds their bid card to score pile
        AuctionCard bidCard = p.getCurrentBid();
        if (bidCard != null) {
            if (p.isUnderdog() && !game.isBlackEvent()) {
                bidCard.setDoubleScore(true);
                if (bidCard.getRank() == 1) {
                    bidCard.setAceUnderdog(true);
                }
            }
            p.getScorePile().add(bidCard);
        }

        // Winners also add market card
        if (marketCard != null) {
            p.getScorePile().add(marketCard);
        }
        p.setHasPickedMarket(true);
    }
    
    public void updateSettings(AuctionGameState game, int handSize, boolean isAuto) {
        if (!game.getPhase().equals("WAITING_FOR_PLAYERS")) return;
        game.setInitialHandSize(handSize);
        game.setAutoHandSize(isAuto);
    }
    
    private void calculateScores(AuctionGameState game) {
        game.setPhase("ROUND_END");
        
        AuctionCard master = game.getMasterCard();
        int luckyRank = master != null ? master.getRank() : -1;
        AuctionSuit luckySuit = master != null ? master.getSuit() : null;
        
        int maxSuitCount = 0;
        Map<String, Integer> playerSuitCounts = new HashMap<>();
        
        for (AuctionPlayer p : game.getPlayers()) {
            p.getScoreBreakdown().clear();
            int roundScore = 0;
            int suitCount = 0;
            
            for (AuctionCard c : p.getScorePile()) {
                int basePts = c.getRank();
                if (basePts > 10) basePts = 10; // J, Q, K = 10
                
                int pts = basePts;
                String rankStr = String.valueOf(c.getRank());
                if (c.getRank()==1) rankStr="A";
                if (c.getRank()==11) rankStr="J";
                if (c.getRank()==12) rankStr="Q";
                if (c.getRank()==13) rankStr="K";
                
                String desc = c.getSuit() + " " + rankStr;
                
                if (c.isAceUnderdog()) {
                    pts = 20;
                    desc += " (Ace Underdog)";
                } else {
                    if (c.isDoubleScore()) {
                        pts = basePts * 2;
                        desc += " (x2 Underdog)";
                    }
                    if (c.getRank() == luckyRank) {
                        pts += 10;
                        desc += " (Lucky Rank +10)";
                    }
                }
                roundScore += pts;
                p.getScoreBreakdown().add(desc + ": " + pts + " pts");
                if (c.getSuit() == luckySuit) suitCount++;
            }
            
            p.setRoundScore(roundScore);
            p.setTotalScore(p.getTotalScore() + roundScore);
            playerSuitCounts.put(p.getId(), suitCount);
            if (suitCount > maxSuitCount) maxSuitCount = suitCount;
        }
        
        if (maxSuitCount > 0) {
            for (AuctionPlayer p : game.getPlayers()) {
                if (playerSuitCounts.get(p.getId()) == maxSuitCount) {
                    p.setRoundScore(p.getRoundScore() + 20);
                    p.setTotalScore(p.getTotalScore() + 20);
                    p.getScoreBreakdown().add("Lucky Suit Bonus: +20 pts");
                }
            }
        }
        
        List<AuctionPlayer> rankedByScore = new ArrayList<>(game.getPlayers());
        rankedByScore.sort((p1, p2) -> Integer.compare(p2.getRoundScore(), p1.getRoundScore())); // Base legacy on round score
        
        for (AuctionPlayer p : game.getPlayers()) {
            p.setRich(false);
            p.setPoor(false);
        }
        
        if (game.getRound() < 4) {
            rankedByScore.get(0).setRich(true);
            rankedByScore.get(rankedByScore.size() - 1).setPoor(true);
            game.setPhase("LEGACY_PICK");
        } else {
            game.setPhase("GAME_OVER");
        }
    }

    public void pickLegacyCard(AuctionGameState game, String playerId, String cardId) {
        if (!game.getPhase().equals("LEGACY_PICK")) return;
        
        AuctionPlayer player = game.getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElse(null);
        if (player == null) return;
        
        // Ensure card is in their score pile
        boolean exists = player.getScorePile().stream().anyMatch(c -> c.getId().equals(cardId));
        if (exists) {
            player.setLegacyCardId(cardId);
        }
    }

    public void nextRound(AuctionGameState game) {
        if (!game.getPhase().equals("LEGACY_PICK") && !game.getPhase().equals("ROUND_END")) return;
        
        // If in Legacy Pick, everyone MUST have picked if they have cards
        if (game.getPhase().equals("LEGACY_PICK")) {
            for (AuctionPlayer p : game.getPlayers()) {
                if (!p.getScorePile().isEmpty() && p.getLegacyCardId() == null) {
                    return; // Not ready
                }
            }
        }
        
        game.setRound(game.getRound() + 1);
        game.setSubRound(1);
        game.setPhase("MASTER_REVEAL");
        
        game.getDeck().clear();
        for (AuctionSuit suit : AuctionSuit.values()) {
            for (int r = 1; r <= 13; r++) {
                String id = suit.name().substring(0, 1) + "_" + r + "_" + UUID.randomUUID().toString().substring(0,4);
                game.getDeck().add(new AuctionCard(id, suit, r));
            }
        }
        Collections.shuffle(game.getDeck());
        AuctionCard master = game.getDeck().remove(0);
        game.setMasterCard(master);
        applyMasterCardEvent(game, master);
        
        // Deduplicate deck against ALL cards currently in players' hands (Legacy cards)
        for (AuctionPlayer p : game.getPlayers()) {
            for (AuctionCard handCard : p.getHand()) {
                final int rank = handCard.getRank();
                final AuctionSuit suit = handCard.getSuit();
                game.getDeck().removeIf(c -> c.getRank() == rank && c.getSuit() == suit);
            }
        }

        int n = game.getPlayers().size();
        int baseHandSize = game.getInitialHandSize();
        int marketSize = n - 1;
        
        if (game.isAutoHandSize()) {
            if (n == 2) baseHandSize = 10;
            else if (n == 3) baseHandSize = 8;
            else if (n == 4) baseHandSize = 6;
            else if (n == 5) baseHandSize = 5;
            else if (n == 6) baseHandSize = 4;
        }
        
        for (AuctionPlayer p : game.getPlayers()) {
            p.getHand().clear(); // Clear hand first to avoid duplicates
            p.setCurrentBid(null);
            p.setRoundScore(0);
            
            int targetHandSize = baseHandSize;
            if (p.isRich()) targetHandSize--; 
            if (p.isPoor()) targetHandSize++;
            
            // Extract Legacy Card before clearing score pile
            if (p.getLegacyCardId() != null) {
                final String lid = p.getLegacyCardId();
                AuctionCard legacy = p.getScorePile().stream()
                        .filter(c -> c.getId().equals(lid))
                        .findFirst().orElse(null);
                if (legacy != null) {
                    legacy.setDoubleScore(false);
                    legacy.setAceUnderdog(false);
                    p.getHand().add(legacy);
                }
                p.setLegacyCardId(null);
            }
            
            p.getScorePile().clear();

            // Draw until reaching targetHandSize
            while (p.getHand().size() < targetHandSize && !game.getDeck().isEmpty()) {
                p.getHand().add(game.getDeck().remove(0));
            }
            
            p.setRich(false);
            p.setPoor(false);
        }
        
        setupSubRound(game, marketSize);
    }
    
    public void useJackSkill(AuctionGameState game, String playerId, String deckCardId, String marketCardId) {
        if (!game.getPhase().equals("J_SKILL")) return;
        
        AuctionCard chosenDeckCard = null;
        AuctionCard unchosenDeckCard = null;
        for (AuctionCard c : game.getJackSkillOptions()) {
            if (c.getId().equals(deckCardId)) chosenDeckCard = c;
            else unchosenDeckCard = c;
        }
        
        if (chosenDeckCard != null && marketCardId != null) {
            for (int i = 0; i < game.getMarket().size(); i++) {
                if (game.getMarket().get(i).getId().equals(marketCardId)) {
                    AuctionCard removedMarket = game.getMarket().remove(i);
                    chosenDeckCard.setFaceDown(removedMarket.isFaceDown());
                    game.getMarket().add(i, chosenDeckCard);
                    
                    // Removed market card is DISCARDED (goes nowhere)
                    break;
                }
            }
        }
        
        // Unchosen deck card goes to BOTTOM of deck
        if (unchosenDeckCard != null) {
            game.getDeck().add(unchosenDeckCard);
        }
        
        game.getJackSkillOptions().clear();
        game.setPhase("RESOLUTION");
        // Do not advance queue, it's still their turn to pick!
    }
}
