package com.example.demo1.service;

import com.example.demo1.model.Card;
import com.example.demo1.model.CardType;
import com.example.demo1.model.Suit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DeckFactory {

    public List<Card> createStandardDeck() {
        List<Card> deck = new ArrayList<>();
        
        for (Suit suit : Suit.values()) {
            for (int rank = 1; rank <= 13; rank++) {
                CardType type;
                if (rank == 1 || rank == 13) {
                    type = CardType.VICTORY;   // A(1), K(13)
                } else if (rank == 11 || rank == 12) {
                    type = CardType.CHARACTER; // J(11), Q(12)
                } else {
                    type = CardType.NUMBER;    // 2-10
                }
                
                String id = suit.name().substring(0, 1) + "_" + rank;
                deck.add(new Card(id, suit, rank, type));
            }
        }
        
        Collections.shuffle(deck);
        return deck;
    }
}
