package com.slopeguard.repository;

import com.slopeguard.model.AlertReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertReviewRepository extends JpaRepository<AlertReview, Long> {

    List<AlertReview> findByAlertIdOrderByCreatedAtAsc(String alertId);
}
