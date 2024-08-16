package com.example.telegrambarbershop.controller;

import com.example.telegrambarbershop.entity.Review;
import com.example.telegrambarbershop.entity.ReviewDTO;
import com.example.telegrambarbershop.repositories.ReviewRepository;
import com.example.telegrambarbershop.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @GetMapping("/api/reviews")
    public List<ReviewDTO> getReviews() {
        return reviewService.getAllReviews();
    }
}
