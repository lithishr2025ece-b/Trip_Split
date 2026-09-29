package com.example.tripsplit.repository;

import com.example.tripsplit.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> { }
