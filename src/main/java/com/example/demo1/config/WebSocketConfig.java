package com.example.demo1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // เปิดการใช้งาน WebSocket server ร่วมกับ Message Broker (STOMP)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // กำหนด URL Endpoint สำหรับให้ Client เข้ามาทักทาย (Handshake) ครั้งแรก
        // .setAllowedOriginPatterns("*") อนุญาตให้โดเมนอื่นๆ (เช่น Railway) เชื่อมต่อได้
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // กำหนด Prefix หลักสำหรับห้องแชท (Topic) ที่จะให้ Client รอรับข้อความ (Subscribe)
        registry.enableSimpleBroker("/topic");
        
        // กำหนด Prefix สำหรับข้อความใดๆ ที่ Client จะส่งกลับขึ้นมาให้ Server ช่วยจัดการ
        registry.setApplicationDestinationPrefixes("/app");
    }
}
