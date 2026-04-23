package com.example.demo1.auction.model;

import java.util.ArrayList;
import java.util.List;

public class AuctionPlayer {
    private String id;
    private String name;
    private List<AuctionCard> hand = new ArrayList<>();
    private List<AuctionCard> scorePile = new ArrayList<>();
    private AuctionCard currentBid; 
    private int totalScore = 0;
    private int roundScore = 0;
    private List<String> scoreBreakdown = new ArrayList<>();
    
    // Status
    private boolean isFirstPlace = false;
    private boolean isUnderdog = false;
    private boolean hasPickedMarket = false; 
    private boolean hasBid = false;
    private boolean isRich = false;
    private boolean isPoor = false;
    private String legacyCardId;

    public AuctionPlayer() {}

    public AuctionPlayer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<AuctionCard> getHand() { return hand; }
    public void setHand(List<AuctionCard> hand) { this.hand = hand; }

    public List<AuctionCard> getScorePile() { return scorePile; }
    public void setScorePile(List<AuctionCard> scorePile) { this.scorePile = scorePile; }

    public AuctionCard getCurrentBid() { return currentBid; }
    public void setCurrentBid(AuctionCard currentBid) { this.currentBid = currentBid; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public boolean isFirstPlace() { return isFirstPlace; }
    public void setFirstPlace(boolean firstPlace) { isFirstPlace = firstPlace; }

    public boolean isUnderdog() { return isUnderdog; }
    public void setUnderdog(boolean underdog) { isUnderdog = underdog; }

    public boolean isHasPickedMarket() { return hasPickedMarket; }
    public void setHasPickedMarket(boolean hasPickedMarket) { this.hasPickedMarket = hasPickedMarket; }

    public boolean isHasBid() { return hasBid; }
    public void setHasBid(boolean hasBid) { this.hasBid = hasBid; }

    public int getRoundScore() { return roundScore; }
    public void setRoundScore(int roundScore) { this.roundScore = roundScore; }

    public boolean isRich() { return isRich; }
    public void setRich(boolean rich) { this.isRich = rich; }

    public boolean isPoor() { return isPoor; }
    public void setPoor(boolean poor) { this.isPoor = poor; }

    public String getLegacyCardId() { return legacyCardId; }
    public void setLegacyCardId(String legacyCardId) { this.legacyCardId = legacyCardId; }

    public List<String> getScoreBreakdown() { return scoreBreakdown; }
    public void setScoreBreakdown(List<String> scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }
}
