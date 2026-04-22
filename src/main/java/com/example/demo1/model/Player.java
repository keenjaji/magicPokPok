package com.example.demo1.model;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String id;
    private String name;
    private int actionsLeft;
    private boolean isSilenced;
    private boolean isSlowed;
    private int score;
    private boolean hasDugThisTurn;
    private List<Card> board;

    public Player() {
        this.actionsLeft = 2; // Default actions
        this.isSilenced = false;
        this.isSlowed = false;
        this.hasDugThisTurn = false;
        this.score = 0;
        this.board = new ArrayList<>();
    }

    public Player(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getActionsLeft() { return actionsLeft; }
    public void setActionsLeft(int actionsLeft) { this.actionsLeft = actionsLeft; }

    public boolean isSilenced() { return isSilenced; }
    public void setSilenced(boolean silenced) { this.isSilenced = silenced; }

    public boolean isSlowed() { return isSlowed; }
    public void setSlowed(boolean slowed) { this.isSlowed = slowed; }

    public boolean isHasDugThisTurn() { return hasDugThisTurn; }
    public void setHasDugThisTurn(boolean hasDugThisTurn) { this.hasDugThisTurn = hasDugThisTurn; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public List<Card> getBoard() { return board; }
    public void setBoard(List<Card> board) { this.board = board; }
}
