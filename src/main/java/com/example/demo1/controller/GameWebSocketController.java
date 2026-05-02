package com.example.demo1.controller;

import com.example.demo1.model.ActionCommand;
import com.example.demo1.model.GameMessage;
import com.example.demo1.model.GameState;
import com.example.demo1.model.Player;
import com.example.demo1.service.EventProcessorService;
import com.example.demo1.service.GameService;
import com.example.demo1.service.TurnEngineService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketController {

    private final TurnEngineService turnEngine;
    private final GameService gameService;
    private final EventProcessorService eventProcessor;
    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketController(TurnEngineService turnEngine, GameService gameService, 
                                   EventProcessorService eventProcessor, SimpMessagingTemplate messagingTemplate) {
        this.turnEngine = turnEngine;
        this.gameService = gameService;
        this.eventProcessor = eventProcessor;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game.action")
    public void handleGameAction(GameMessage message) {
        GameState game = gameService.getGame(message.getGameId());
        if (game == null || game.getPlayers().isEmpty()) return;

        // Verify if it is the sender's turn
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIdx());
        if (!currentPlayer.getId().equals(message.getPlayerId())) {
            return; // Not their turn
        }

        ActionCommand cmd = message.getCommand();
        if (cmd == null) return;

        boolean actionSuccessful = false;
        String eventMsg = "";

        // Block everything if player has pending duplicates (unless they are executing DISCARD_DUPLICATE)
        if (!"DISCARD_DUPLICATE".equals(cmd.getActionType()) && currentPlayer.isMustDiscard()) {
            return; // Ignore action, wait for discard
        }

        if ("DISCARD_DUPLICATE".equals(cmd.getActionType())) {
            turnEngine.discardDuplicate(game, currentPlayer, cmd.getSourceCardId());
            actionSuccessful = true;
        } else if ("TAKE_MARKET".equals(cmd.getActionType())) {
            turnEngine.takeFromMarket(game, currentPlayer, cmd.getSourceCardId());
            actionSuccessful = true;
        } else if ("USE_SKILL".equals(cmd.getActionType())) {
            actionSuccessful = turnEngine.useSkill(game, currentPlayer, cmd);
        } else if ("END_TURN".equals(cmd.getActionType())) {
            eventMsg = turnEngine.endTurn(game, eventProcessor);
            actionSuccessful = true;
        }

        if (actionSuccessful) {
            // Check King Sudden Death victory locally
            boolean suddenlyWon = currentPlayer.getActionsLeft() == 0 && cmd.getActionType() != null && cmd.getActionType().equals("USE_SKILL") 
                                  && cmd.getSourceCardId() != null; 
            
            eventProcessor.calculateScores(game); // Ensure scores are updated on every action
            // Broadcast the entire game state to the room
            messagingTemplate.convertAndSend("/topic/game/" + game.getGameId(), game);
            
            if (!eventMsg.isEmpty() && !eventMsg.equals("Turn ended.")) {
                // Broadcast A-Phet event notification
                messagingTemplate.convertAndSend("/topic/events/" + game.getGameId(), eventMsg);
            }
        }
    }
}
