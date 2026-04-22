package com.example.demo1.model;

public class Card {
    private String id; // e.g. "H_10", "S_A"
    private Suit suit;
    private int rank; // 1 = A, 11 = J, 12 = Q, 13 = K, 2-10 = Numbers
    private CardType type;
    
    // Status properties
    private boolean isResurrected = false; // ชุบชีวิต (ถ้าตายอีกจะไป Exile)
    private boolean isTapped = false;      // สถานะตะแคง (ใช้งานแล้ว)
    private boolean isDisabled = false;    // สถานะถูกล็อคคะแนน (โดนใบ 2 สาป)
    
    public Card() {}

    public Card(String id, Suit suit, int rank, CardType type) {
        this.id = id;
        this.suit = suit;
        this.rank = rank;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Suit getSuit() { return suit; }
    public void setSuit(Suit suit) { this.suit = suit; }
    
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    
    public CardType getType() { return type; }
    public void setType(CardType type) { this.type = type; }
    
    public boolean isResurrected() { return isResurrected; }
    public void setResurrected(boolean resurrected) { isResurrected = resurrected; }
    
    public boolean isTapped() { return isTapped; }
    public void setTapped(boolean tapped) { isTapped = tapped; }
    
    public boolean isDisabled() { return isDisabled; }
    public void setDisabled(boolean disabled) { isDisabled = disabled; }
}
