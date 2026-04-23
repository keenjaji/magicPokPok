package com.example.demo1.auction.model;

import java.util.ArrayList;
import java.util.List;

public class AuctionGameState {
    private String gameId;
    private List<AuctionPlayer> players = new ArrayList<>();
    private List<AuctionCard> deck = new ArrayList<>();
    private List<AuctionCard> market = new ArrayList<>();
    private AuctionCard masterCard;
    
    private String phase = "WAITING_FOR_PLAYERS"; // WAITING_FOR_PLAYERS, MASTER_REVEAL, BIDDING, RESOLUTION, ROUND_END, GAME_OVER
    private int round = 1; // Max 4 big rounds
    private int subRound = 1;
    
    // Pick queue for resolution phase
    private List<String> pickOrderPlayerIds = new ArrayList<>();
    
    // Jack Skill state
    private List<AuctionCard> jackSkillOptions = new ArrayList<>();
    
    // Settings
    private int initialHandSize = 6;
    private boolean autoHandSize = true;
    
    // Special Events
    private boolean mysteryMarket = false;
    private boolean blackEvent = false;
    private String eventDescription = "";

    public AuctionGameState() {}

    public AuctionGameState(String gameId) {
        this.gameId = gameId;
    }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public List<AuctionPlayer> getPlayers() { return players; }
    public void setPlayers(List<AuctionPlayer> players) { this.players = players; }

    public List<AuctionCard> getDeck() { return deck; }
    public void setDeck(List<AuctionCard> deck) { this.deck = deck; }

    public List<AuctionCard> getMarket() { return market; }
    public void setMarket(List<AuctionCard> market) { this.market = market; }

    public AuctionCard getMasterCard() { return masterCard; }
    public void setMasterCard(AuctionCard masterCard) { this.masterCard = masterCard; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }

    public int getSubRound() { return subRound; }
    public void setSubRound(int subRound) { this.subRound = subRound; }

    public List<String> getPickOrderPlayerIds() { return pickOrderPlayerIds; }
    public void setPickOrderPlayerIds(List<String> pickOrderPlayerIds) { this.pickOrderPlayerIds = pickOrderPlayerIds; }

    public List<AuctionCard> getJackSkillOptions() { return jackSkillOptions; }
    public void setJackSkillOptions(List<AuctionCard> jackSkillOptions) { this.jackSkillOptions = jackSkillOptions; }

    public int getInitialHandSize() { return initialHandSize; }
    public void setInitialHandSize(int initialHandSize) { this.initialHandSize = initialHandSize; }

    public boolean isAutoHandSize() { return autoHandSize; }
    public void setAutoHandSize(boolean autoHandSize) { this.autoHandSize = autoHandSize; }

    public boolean isMysteryMarket() { return mysteryMarket; }
    public void setMysteryMarket(boolean mysteryMarket) { this.mysteryMarket = mysteryMarket; }

    public boolean isBlackEvent() { return blackEvent; }
    public void setBlackEvent(boolean blackEvent) { this.blackEvent = blackEvent; }

    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }
}
