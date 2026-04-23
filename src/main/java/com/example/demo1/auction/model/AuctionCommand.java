package com.example.demo1.auction.model;

public class AuctionCommand {
    private String actionType; // e.g. "START_GAME", "BID", "PICK_MARKET", "USE_SKILL"
    private String sourceCardId; 
    private String targetCardId;
    private int handSize;
    private boolean autoHandSize;
    
    public AuctionCommand() {}

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getSourceCardId() { return sourceCardId; }
    public void setSourceCardId(String sourceCardId) { this.sourceCardId = sourceCardId; }

    public String getTargetCardId() { return targetCardId; }
    public void setTargetCardId(String targetCardId) { this.targetCardId = targetCardId; }

    public int getHandSize() { return handSize; }
    public void setHandSize(int handSize) { this.handSize = handSize; }

    public boolean isAutoHandSize() { return autoHandSize; }
    public void setAutoHandSize(boolean autoHandSize) { this.autoHandSize = autoHandSize; }
}
