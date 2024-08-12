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

//    @PostMapping("/submit")
//    public ResponseEntity<String> submitReview(
//            @RequestParam Integer barberId,
//            @RequestParam Double rating,
//            @RequestParam String comment) {
//        reviewService.handleReview(barberId, rating, comment);
//        return ResponseEntity.ok("Отзыв успешно добавлен");
//    }

    @GetMapping("/api/reviews")
    public List<ReviewDTO> getReviews() {
        return reviewService.getAllReviews();
    }

//    @GetMapping("/api/reviews")
//    @CrossOrigin(origins = "http://localhost:63342")
//    public List<Review> getReviews() {
//        List<Review> reviews = reviewRepository.findAll();
//        for (Review review : reviews) {
//            System.out.println("Review ID: " + review.getId() + ", Barber: " + (review.getBarber() != null ? review.getBarber().getName() : "null"));
//        }
//        return reviews;
//    }

//    @GetMapping("/reviews")
//    @ResponseBody
//    @CrossOrigin(origins = "http://localhost:63342")
//    public List<Review> getReviews() {
//        return reviewRepository.findAll();
//    }
}
