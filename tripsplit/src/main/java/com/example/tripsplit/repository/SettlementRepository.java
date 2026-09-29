package com.example.tripsplit.repository;

import com.example.tripsplit.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByTripIdOrderByIdAsc(Long tripId);
    void deleteByTripId(Long tripId);
}
