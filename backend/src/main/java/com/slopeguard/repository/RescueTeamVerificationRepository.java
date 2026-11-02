package com.slopeguard.repository;

import com.slopeguard.model.RescueTeamVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RescueTeamVerificationRepository extends JpaRepository<RescueTeamVerification, Long> {

    List<RescueTeamVerification> findAllByRescueTeamIdOrderByReviewedAtDesc(Long rescueTeamId);
}
