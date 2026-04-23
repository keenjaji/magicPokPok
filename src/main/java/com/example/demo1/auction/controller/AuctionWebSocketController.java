package com.example.demo1.auction.controller;

import com.example.demo1.auction.model.AuctionCommand;
import com.example.demo1.auction.model.AuctionGameState;
import com.example.demo1.auction.model.AuctionMessage;
import com.example.demo1.auction.service.AuctionEngineService;
import com.example.demo1.auction.service.AuctionGameService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class AuctionWebSocketController {

    private final AuctionGameService gameService;
    private final AuctionEngineService engineService;
    private final SimpMessagingTemplate messagingTemplate;

    public AuctionWebSocketController(AuctionGameService gameService, AuctionEngineService engineService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.engineService = engineService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/auction.action")
    public void handleAction(AuctionMessage message) {
        AuctionGameState game = gameService.getGame(message.getGameId());
        if (game == null) return;

        AuctionCommand cmd = message.getCommand();
        if (cmd == null) return;

        if ("BID".equals(cmd.getActionType())) {
            engineService.placeBid(game, message.getPlayerId(), cmd.getSourceCardId());
        } else if ("PICK_MARKET".equals(cmd.getActionType())) {
            engineService.pickMarketCard(game, message.getPlayerId(), cmd.getSourceCardId());
        } else if ("USE_SKILL_J".equals(cmd.getActionType())) {
            engineService.useJackSkill(game, message.getPlayerId(), cmd.getSourceCardId(), cmd.getTargetCardId());
        } else if ("PICK_LEGACY".equals(cmd.getActionType())) {
            engineService.pickLegacyCard(game, message.getPlayerId(), cmd.getSourceCardId());
        } else if ("UPDATE_SETTINGS".equals(cmd.getActionType())) {
            engineService.updateSettings(game, cmd.getHandSize(), cmd.isAutoHandSize());
        }

        messagingTemplate.convertAndSend("/topic/auction/" + game.getGameId(), game);
    }
}
