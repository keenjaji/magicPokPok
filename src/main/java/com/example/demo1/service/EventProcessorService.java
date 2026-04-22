package com.example.demo1.service;

import com.example.demo1.model.Card;
import com.example.demo1.model.CardType;
import com.example.demo1.model.GameState;
import com.example.demo1.model.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventProcessorService {

    public String triggerAPhet(GameState game) {
        if (game.getDeck().isEmpty()) return "deck_empty";

        Card aphetCard = game.getDeck().remove(0);

        switch (aphetCard.getSuit()) {
            case SPADE:
                processRobinHood(game);
                return "อาเพศโพดำทำงาน: โรบินฮู้ดปล้นคนรวยช่วยคนจน!";
            case HEART:
                processWheelOfKarma(game);
                return "อาเพศโพแดงทำงาน: วงเวียนกรรมส่งมอบไพ่ไปทางขวา!";
            case DIAMOND:
                processTax(game);
                return "อาเพศข้าวหลามตัดทำงาน: คนรวยไพ่ล้นบอร์ดถูกเก็บภาษีแจกจ่าย!";
            case CLUB:
                processSilence(game);
                return "อาเพศดอกจิกทำงาน: ความเงียบร่ายมนตร์ใส่ทุกคนบนฟิลด์!";
        }
        return "เปิดอาเพศไม่สำเร็จ";
    }

    private void processRobinHood(GameState game) {
        calculateScores(game);
        
        int maxScore = -1;
        int minScore = 9999;
        for (Player p : game.getPlayers()) {
            if (p.getScore() > maxScore) maxScore = p.getScore();
            if (p.getScore() < minScore) minScore = p.getScore();
        }

        int finalMax = maxScore;
        int finalMin = minScore;
        List<Player> maxPlayers = game.getPlayers().stream().filter(p -> p.getScore() == finalMax).collect(Collectors.toList());
        List<Player> minPlayers = game.getPlayers().stream().filter(p -> p.getScore() == finalMin).collect(Collectors.toList());

        if (maxScore > minScore && maxPlayers.size() == 1 && !minPlayers.isEmpty()) {
            Player topPlayer = maxPlayers.get(0);
            Player bottomPlayer = minPlayers.get(0);
            
            Card highestCard = null;
            for (Card c : topPlayer.getBoard()) {
                if (c.getRank() >= 2 && c.getRank() <= 10) {
                    if (highestCard == null || c.getRank() > highestCard.getRank()) {
                        highestCard = c;
                    }
                }
            }
            
            if (highestCard != null) {
                topPlayer.getBoard().remove(highestCard);
                bottomPlayer.getBoard().add(highestCard);
            }
        }
        calculateScores(game); // Recalculate after shift
    }

    // ♥️ โพแดง (Wheel of Karma): บังคับผู้เล่นทุกคน ส่งการ์ดเบอร์ต่ำสุด/สุ่ม ตัวเอง 1 ใบไปให้คนข้างๆ
    private void processWheelOfKarma(GameState game) {
        List<Player> players = game.getPlayers();
        int size = players.size();
        if (size < 2) return;

        // Temporary array to hold the cards passing
        List<Card> cardsToPass = new ArrayList<>();
        
        for (Player p : players) {
            if (!p.getBoard().isEmpty()) {
                // Pick the first card for simplicity (can be randomized or logic-driven)
                Card passedCard = p.getBoard().get(0);
                cardsToPass.add(passedCard);
                p.getBoard().remove(passedCard);
            } else {
                cardsToPass.add(null);
            }
        }

        // Shift Right
        for (int i = 0; i < size; i++) {
            Card cardReceived = cardsToPass.get(i);
            if (cardReceived != null) {
                int getNextIdx = (i + 1) % size;
                players.get(getNextIdx).getBoard().add(cardReceived);
            }
        }
    }

    // ♦️ ข้าวหลามตัด (Tax): หากใครมีการ์ดบนบอร์ด >= 6 ใบ และมีมากที่สุดคนเดียว แจกการ์ดตัวเองให้คนอื่นคนละหยิบ 1 ใบ
    private void processTax(GameState game) {
        int maxCards = game.getPlayers().stream().mapToInt(p -> p.getBoard().size()).max().orElse(0);
        
        if (maxCards >= 6) {
            List<Player> maxPlayers = game.getPlayers().stream()
                    .filter(p -> p.getBoard().size() == maxCards)
                    .collect(Collectors.toList());
                    
            if (maxPlayers.size() == 1) {
                Player richPlayer = maxPlayers.get(0);
                
                // Distribute to others
                for (Player other : game.getPlayers()) {
                    if (!other.getId().equals(richPlayer.getId()) && !richPlayer.getBoard().isEmpty()) {
                        Card taxCard = richPlayer.getBoard().remove(0);
                        other.getBoard().add(taxCard);
                    }
                }
            }
        }
    }

    // ♣️ ดอกจิก (Silence): GlobalState.setSilenceAll(true) ทุกคนใช้สกิลหน้าคนไม่ได้ในรอบถัดไป
    private void processSilence(GameState game) {
        game.setGlobalSilence(true);
        // Will need to be reset after 1 round, tracked in TurnEngineService (Next Phase)
    }

    public void calculateScores(GameState game) {
        for (Player p : game.getPlayers()) {
            int score = 0;
            for (Card c : p.getBoard()) {
                if (c.getRank() >= 2 && c.getRank() <= 10 && !c.isDisabled()) {
                    score += c.getRank();
                }
            }
            p.setScore(score);
        }
    }
}
