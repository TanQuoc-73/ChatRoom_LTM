package hau.edu.chat_room.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {
    @GetMapping("/api/main")
    public String mainEndpoint(){
        return "This is the main endpoint of the Chat Room application.";
    }
}
