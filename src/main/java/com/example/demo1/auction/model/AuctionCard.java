package com.example.demo1.auction.model;

public class AuctionCard {
    private String id;
    private AuctionSuit suit;
    private int rank; // 1 = A, 11 = J, 12 = Q, 13 = K
    private boolean isFaceDown = false;
    private boolean isDoubleScore = false;
    private boolean isAceUnderdog = false;
    
    public AuctionCard() {}

    public AuctionCard(String id, AuctionSuit suit, int rank) {
        this.id = id;
        this.suit = suit;
        this.rank = rank;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public AuctionSuit getSuit() { return suit; }
    public void setSuit(AuctionSuit suit) { this.suit = suit; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public boolean isFaceDown() { return isFaceDown; }
    public void setFaceDown(boolean faceDown) { this.isFaceDown = faceDown; }

    public boolean isDoubleScore() { return isDoubleScore; }
    public void setDoubleScore(boolean doubleScore) { this.isDoubleScore = doubleScore; }

    public boolean isAceUnderdog() { return isAceUnderdog; }
    public void setAceUnderdog(boolean aceUnderdog) { this.isAceUnderdog = aceUnderdog; }
}
