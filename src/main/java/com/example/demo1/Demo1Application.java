package com.example.demo1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController // 1. เพิ่ม Annotation นี้เพื่อบอกว่าเป็นตัวรับ Request
public class Demo1Application {

	public static void main(String[] args) {
		SpringApplication.run(Demo1Application.class, args);
	}

	// 2. เพิ่ม Method นี้เพื่อแสดงผลข้อความ
	@GetMapping("/hello")
	public String sayHello() {
		return "สวัสดี! นี่คือ Spring Boot ตัวแรกของฉัน";
	}
}