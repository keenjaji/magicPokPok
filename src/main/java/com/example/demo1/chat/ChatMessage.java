package com.example.demo1.chat;

// คลาสนี้รับหน้าที่เป็น "ซองจดหมาย" สำหรับใส่ข้อมูลที่ส่งไปมาระหว่างเว็บ (Client) และเซิร์ฟเวอร์ (Server)
public class ChatMessage {
    
    // สถานะของข้อความ (เพื่อให้รู้ว่าข้อความนี้เป็น แชทปกติ หรือ เข้าห้อง หรือ ออกจากห้อง)
    private MessageType type;
    
    // เนื้อหาข้อความที่พิมพ์แชท
    private String content;
    
    // ชื่อของผู้ส่งข้อความ
    private String sender;

    // สร้างประเภทของข้อความ (Enum เป็นเหมือนหมวดหมู่ตายตัว)
    public enum MessageType {
        CHAT,  // ข้อความคุยกันปกติ
        JOIN,  // ข้อความระบบ: มีคนเข้าห้อง
        LEAVE  // ข้อความระบบ: มีคนออกจากห้อง
    }

    // --- ด้านล่างนี้คือ Getter และ Setter (เป็นวิธีดึงข้อมูลและใส่ข้อมูลลงใน "ซองจดหมาย" ซองนี้) ---

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
