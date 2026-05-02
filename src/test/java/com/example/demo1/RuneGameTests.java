package com.example.demo1;

import com.example.demo1.rune.model.RuneCard;
import com.example.demo1.rune.model.RuneGameState;
import com.example.demo1.rune.model.RunePlayer;
import com.example.demo1.rune.service.RuneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

public class RuneGameTests {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RuneService runeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRuneGameLogic() {
        RuneGameState game = runeService.createGame();
        
        runeService.joinGame(game.getGameId(), "Player 1");
        runeService.joinGame(game.getGameId(), "Player 2");
        
        runeService.startBatch(game);
        
        RunePlayer p1 = game.getPlayers().get(0);
        RunePlayer p2 = game.getPlayers().get(1);
        
        assertEquals(30, p1.getHealth());
        assertEquals(5, p1.getHand().size());
        
        // Mock a FIRE card
        RuneCard fireCard = new RuneCard("mock-fire", "FIRE", 1, false);
        p1.getHand().add(fireCard);
        p1.setMana(1);
        
        runeService.playCard(game.getGameId(), p1.getId(), "mock-fire");
        
        // Fire Power 1 deals 2 damage
        assertEquals(28, p2.getHealth());
        assertEquals(0, p1.getMana());
        
        runeService.endTurn(game.getGameId(), p1.getId());
        assertEquals(p2.getId(), game.getActivePlayerId());
        assertTrue(p2.getMana() > 0);
    }
}
