package com.slopeguard.repository;

import com.slopeguard.model.RescueTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RescueTeamRepository extends JpaRepository<RescueTeam, Long> {

    Optional<RescueTeam> findByUserId(Long userId);

    Optional<RescueTeam> findByTeamName(String teamName);

    List<RescueTeam> findAllByVerificationStatus(String verificationStatus);

    @Query("SELECT r FROM RescueTeam r WHERE r.verificationStatus = 'APPROVED' AND r.operationalStatus = 'ACTIVE'")
    List<RescueTeam> findEligibleActiveTeams();

    boolean existsByTeamNameIgnoreCase(String teamName);
}
