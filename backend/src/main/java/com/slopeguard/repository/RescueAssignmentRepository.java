package com.slopeguard.repository;

import com.slopeguard.model.RescueAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RescueAssignmentRepository extends JpaRepository<RescueAssignment, Long> {

    List<RescueAssignment> findAllByRescueTeamIdOrderByAssignedAtDesc(Long rescueTeamId);

    List<RescueAssignment> findAllByIncidentReportIdOrderByAssignedAtDesc(Long incidentReportId);

    Optional<RescueAssignment> findFirstByIncidentReportIdOrderByAssignedAtDesc(Long incidentReportId);

    default Optional<RescueAssignment> findByIncidentReportId(Long incidentReportId) {
        return findFirstByIncidentReportIdOrderByAssignedAtDesc(incidentReportId);
    }

    @Query("SELECT ra FROM RescueAssignment ra WHERE ra.incidentReport.id = :reportId AND ra.assignmentStatus IN ('ASSIGNED', 'ACCEPTED', 'EN_ROUTE', 'ON_SCENE')")
    Optional<RescueAssignment> findActiveAssignmentForReport(@Param("reportId") Long reportId);

    @Query("SELECT COUNT(ra) FROM RescueAssignment ra WHERE ra.rescueTeam.id = :teamId AND ra.assignmentStatus IN ('ASSIGNED', 'ACCEPTED', 'EN_ROUTE', 'ON_SCENE')")
    long countActiveAssignmentsForTeam(@Param("teamId") Long teamId);

    @Query("SELECT ra FROM RescueAssignment ra ORDER BY ra.assignedAt DESC")
    List<RescueAssignment> findAllOrdered();
}
