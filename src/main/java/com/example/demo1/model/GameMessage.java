package com.example.demo1.model;

public class GameMessage {
    private String gameId;
    private String playerId;
    private ActionCommand command;

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public ActionCommand getCommand() { return command; }
    public void setCommand(ActionCommand command) { this.command = command; }
}
