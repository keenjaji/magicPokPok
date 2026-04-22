package com.example.demo1.chat;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

// @Controller ประกาศเพื่อให้ Spring Boot ทราบว่าคลาสนี้คือ "พนักงานรับ-ส่งจดหมาย"
@Controller
public class ChatController {

    // @MessageMapping แปลว่า ถ้าหน้าเว็บส่งข้อมูลมาที่ URL "/chat.sendMessage" ให้มาทำงานที่ฟังก์ชันนี้
    // @SendTo แปลว่า เมื่อทำงานฟังก์ชันนี้เสร็จแล้ว ให้กระจายข้อความ (Broadcast) ต่อไปที่ห้อง "/topic/public" เพื่อให้ทุกคนเห็น
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        // @Payload คือการแกะกล่องจดหมาย (ข้อความแชท) ที่ส่งมา
        // ในกรณีนี้ รับข้อความมาปุ๊บ เราส่งคืนกลับไปให้กระจายให้ทุกคนทันที
        return chatMessage;
    }

    // ฟังก์ชันนี้ใช้งานเมื่อ "มีคนเข้าร่วมห้องแชทครั้งแรก"
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, 
                               SimpMessageHeaderAccessor headerAccessor) {
        // จำชื่อคนที่เข้ามาใหม่เอาไว้ใน session ของเซิร์ฟเวอร์ (เหมือนลงทะเบียนผู้เยี่ยมชม)
        // เพื่อที่เวลาเขาปิดเว็บออกไป เราจะได้รู้ว่าใครออก
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        
        // ส่งข้อความไปบอกทุกคนใน /topic/public ว่า "นาย A เข้าร่วมห้องแล้ว!"
        return chatMessage;
    }
}
