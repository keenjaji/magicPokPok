package com.example.demo1.auction.model;

public class AuctionMessage {
    private String gameId;
    private String playerId;
    private AuctionCommand command;
    
    public AuctionMessage() {}

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public AuctionCommand getCommand() { return command; }
    public void setCommand(AuctionCommand command) { this.command = command; }
}
