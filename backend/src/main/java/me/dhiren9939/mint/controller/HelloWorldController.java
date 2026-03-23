package me.dhiren9939.mint.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class HelloWorldController {
    @GetMapping
    public ResponseEntity<Map<String,String>> handleHelloWorld(){
        return ResponseEntity.status(200).body(Map.of("message","Hello World"));
    }
}
