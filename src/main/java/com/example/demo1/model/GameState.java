package com.example.demo1.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private String gameId;
    private List<Player> players;
    private int currentPlayerIdx;
    private List<Card> market;
    private List<Card> deck;
    private List<Card> graveyard;
    private boolean globalSilence;

    public GameState() {
        this.players = new ArrayList<>();
        this.market = new ArrayList<>();
        this.deck = new ArrayList<>();
        this.graveyard = new ArrayList<>();
        this.currentPlayerIdx = 0;
        this.globalSilence = false;
    }

    public GameState(String gameId) {
        this();
        this.gameId = gameId;
    }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }

    public int getCurrentPlayerIdx() { return currentPlayerIdx; }
    public void setCurrentPlayerIdx(int currentPlayerIdx) { this.currentPlayerIdx = currentPlayerIdx; }

    public List<Card> getMarket() { return market; }
    public void setMarket(List<Card> market) { this.market = market; }

    public List<Card> getDeck() { return deck; }
    public void setDeck(List<Card> deck) { this.deck = deck; }

    public List<Card> getGraveyard() { return graveyard; }
    public void setGraveyard(List<Card> graveyard) { this.graveyard = graveyard; }

    public boolean isGlobalSilence() { return globalSilence; }
    public void setGlobalSilence(boolean globalSilence) { this.globalSilence = globalSilence; }
}
