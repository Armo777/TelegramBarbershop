package com.example.telegrambarbershop.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import com.example.telegrambarbershop.entity.Request;
import com.example.telegrambarbershop.repositories.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
public class RequestController {

    @Autowired
    private RequestRepository requestRepository;

    @PostMapping("/api/requests")
    @CrossOrigin(origins = "http://localhost:63342")
    public ResponseEntity<String> submitRequest(@RequestParam String name, @RequestParam String phone) {
        Request request = new Request();
        request.setName(name);
        request.setPhone(phone);
        request.setTimestamp(LocalDateTime.now());
        requestRepository.save(request);

        return ResponseEntity.ok("Request received");
    }
}

