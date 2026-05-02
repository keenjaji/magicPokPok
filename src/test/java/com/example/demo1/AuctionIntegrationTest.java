package com.example.demo1;

import com.example.demo1.auction.model.AuctionCommand;
import com.example.demo1.auction.model.AuctionGameState;
import com.example.demo1.auction.model.AuctionMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuctionIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void testFullAuctionFlowWithBots() throws Exception {
        String baseUrl = "http://localhost:" + port;
        
        // 1. Create Game
        AuctionGameState initialGame = restTemplate.postForObject(baseUrl + "/api/auction/create", null, AuctionGameState.class);
        String gameId = initialGame.getGameId();
        System.out.println("Test Game Created: " + gameId);

        // 2. Bots Join (3 Bots)
        String[] botNames = {"Cyan_Bot", "Pyros_Bot", "Aurum_Bot"};
        List<String> playerIds = new ArrayList<>();
        for (String name : botNames) {
            AuctionGameState joined = restTemplate.postForObject(baseUrl + "/api/auction/join/" + gameId + "?playerName=" + name, null, AuctionGameState.class);
            playerIds.add(joined.getPlayers().get(joined.getPlayers().size() - 1).getId());
        }

        // 3. Connect STOMP for Monitoring
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        BlockingQueue<AuctionGameState> stateQueue = new LinkedBlockingDeque<>();
        StompSession session = stompClient.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        
        session.subscribe("/topic/auction/" + gameId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return AuctionGameState.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                stateQueue.offer((AuctionGameState) payload);
            }
        });

        // 4. Start Game (Host is player 0)
        restTemplate.postForObject(baseUrl + "/api/auction/start/" + gameId, null, Void.class);
        
        // Wait for first state update (BIDDING phase)
        AuctionGameState state = stateQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(state);
        System.out.println("Game Started. Phase: " + state.getPhase());

        // 5. Simulate Bidding for one round
        for (int i = 0; i < 3; i++) {
            String pId = playerIds.get(i);
            var player = state.getPlayers().stream().filter(p -> p.getId().equals(pId)).findFirst().get();
            String cardId = player.getHand().get(0).getId();
            
            AuctionMessage bidMsg = new AuctionMessage();
            bidMsg.setGameId(gameId);
            bidMsg.setPlayerId(pId);
            AuctionCommand cmd = new AuctionCommand();
            cmd.setActionType("BID");
            cmd.setSourceCardId(cardId);
            bidMsg.setCommand(cmd);
            
            session.send("/app/auction.action", bidMsg);
            System.out.println("Bot " + botNames[i] + " placed bid with card: " + cardId);
        }

        // Wait for RESOLUTION phase
        state = stateQueue.poll(5, TimeUnit.SECONDS);
        while(state != null && !"RESOLUTION".equals(state.getPhase())) {
            state = stateQueue.poll(5, TimeUnit.SECONDS);
        }
        
        assertNotNull(state);
        System.out.println("Phase reached: " + state.getPhase());
        System.out.println("Winner is: " + state.getPlayers().stream().filter(p -> p.isFirstPlace()).findFirst().get().getName());

        session.disconnect();
    }
}
