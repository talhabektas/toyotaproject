package com.example.kafkaconsumer.repository;

import com.example.kafkaconsumer.model.RateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for RateEntity
 */
@Repository
public interface RateRepository extends JpaRepository<RateEntity, Long> {

    /**
     * Find the latest rate by rate name
     * @param rateName Rate name
     * @return Latest rate entity or null if not found
     */
    RateEntity findTopByRateNameOrderByRateUpdateTimeDesc(String rateName);
}