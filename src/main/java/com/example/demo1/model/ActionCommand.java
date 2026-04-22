package com.example.demo1.model;

public class ActionCommand {
    private String actionType; // "TAKE_MARKET", "USE_SKILL", "END_TURN"
    private String sourceCardId; // The card being used (from market or board)
    private String targetPlayerId; // For targeting player (e.g. Silence, Steal)
    private String targetCardId; // For targeting a specific card
    
    // Getters and Setters
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getSourceCardId() { return sourceCardId; }
    public void setSourceCardId(String sourceCardId) { this.sourceCardId = sourceCardId; }

    public String getTargetPlayerId() { return targetPlayerId; }
    public void setTargetPlayerId(String targetPlayerId) { this.targetPlayerId = targetPlayerId; }

    public String getTargetCardId() { return targetCardId; }
    public void setTargetCardId(String targetCardId) { this.targetCardId = targetCardId; }
}
