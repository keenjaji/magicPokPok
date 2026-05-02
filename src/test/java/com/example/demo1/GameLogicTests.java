package com.example.demo1;

import com.example.demo1.model.*;
import com.example.demo1.service.EventProcessorService;
import com.example.demo1.service.GameService;
import com.example.demo1.service.TurnEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class GameLogicTests {

    private TurnEngineService turnEngine;
    private GameService gameService;
    private EventProcessorService eventProcessor;
    private GameState game;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        gameService = Mockito.mock(GameService.class);
        eventProcessor = Mockito.mock(EventProcessorService.class);
        turnEngine = new TurnEngineService(gameService);

        game = new GameState();
        game.setGameId("test-game");
        
        player1 = new Player("p1", "Player 1");
        player2 = new Player("p2", "Player 2");
        
        List<Player> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);
        game.setPlayers(players);
        game.setCurrentPlayerIdx(0);
    }

    @Test
    void testAceUntapsOnNextTurn() {
        // Given: Player 1 has an Ace that is tapped
        Card ace = new Card("H_A", Suit.HEART, 1, CardType.VICTORY); // Ace is rank 1, VICTORY type
        ace.setTapped(true);
        player1.getBoard().add(ace);
        
        // When: Player 2 ends turn and it comes back to Player 1
        turnEngine.endTurn(game, eventProcessor); // Player 1 ends, now it's Player 2's turn
        turnEngine.endTurn(game, eventProcessor); // Player 2 ends, now it's Player 1's turn
        
        // Then: Player 1's Ace should be untapped
        assertFalse(player1.getBoard().get(0).isTapped(), "Ace should be untapped at the start of player's turn");
    }

    @Test
    void testCharacterDuplicateDetectionOnMarket() {
        // Given: Player 1 has a Jack (rank 11)
        Card jack1 = new Card("S_J", Suit.SPADE, 11, CardType.CHARACTER);
        player1.getBoard().add(jack1);
        
        // When: Player 1 takes another Jack from market
        Card jack2 = new Card("H_J", Suit.HEART, 11, CardType.CHARACTER);
        game.getMarket().add(jack2);
        
        player1.setActionsLeft(1);
        turnEngine.takeFromMarket(game, player1, "H_J");
        
        // Then: hasCharacterDuplicates should be true and mustDiscard should be true
        assertTrue(turnEngine.hasCharacterDuplicates(player1), "Should detect duplicate Jacks");
        assertTrue(player1.isMustDiscard(), "Player should be in mustDiscard state");
    }

    @Test
    void testCharacterDuplicateDetectionOnQueenSkill() {
        // Given: Player 1 has a Queen (rank 12)
        Card queen1 = new Card("S_Q", Suit.SPADE, 12, CardType.CHARACTER);
        player1.getBoard().add(queen1);

        // Player 2 has another Queen
        Card queen2 = new Card("H_Q", Suit.HEART, 12, CardType.CHARACTER);
        player2.getBoard().add(queen2);

        // ActionCommand to use queen1's skill to steal queen2
        ActionCommand cmd = new ActionCommand();
        cmd.setActionType("USE_SKILL");
        cmd.setSourceCardId("S_Q");
        cmd.setTargetPlayerId("p2");
        cmd.setTargetCardId("H_Q");
        
        // When: Player 1 uses Queen skill to steal Queen from Player 2
        player1.setActionsLeft(1);
        turnEngine.useSkill(game, player1, cmd);

        // Then: Player 1 should have 2 Queens and mustDiscard should be true
        long queenCount = player1.getBoard().stream().filter(c -> c.getRank() == 12).count();
        assertEquals(2, queenCount);
        assertTrue(player1.isMustDiscard(), "Player should be in mustDiscard state after stealing duplicate");
    }
}
