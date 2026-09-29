package com.example.tripsplit.repository;

import com.example.tripsplit.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByTripIdOrderByExpenseDateDescIdDesc(Long tripId);
    boolean existsByPayerId(Long participantId);
    boolean existsByParticipantsId(Long participantId);
}
