package com.example.demo.repositories;

import com.example.demo.entity.Citizen;
import com.example.demo.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByFeedbackBy(Citizen citizen);
}
