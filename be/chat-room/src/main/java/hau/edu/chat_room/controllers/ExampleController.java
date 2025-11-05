package hau.edu.chat_room.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleController {
    @GetMapping("/api/hello")
    public String helloWorld(){
        return "Hello from backend springboot <3";
    }
}
