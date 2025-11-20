package com.nhom8.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;


@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class RoomchatRealtimeApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoomchatRealtimeApplication.class, args);
	}

}

