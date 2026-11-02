package com.slopeguard.repository;

import com.slopeguard.model.AlertDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertDeliveryRepository extends JpaRepository<AlertDelivery, Long> {

    List<AlertDelivery> findByAlertIdOrderByAttemptedAtDesc(String alertId);
}
