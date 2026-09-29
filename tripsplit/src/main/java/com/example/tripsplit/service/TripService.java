package com.example.tripsplit.service;

import com.example.tripsplit.dto.TripRequest;
import com.example.tripsplit.entity.Trip;
import com.example.tripsplit.exception.BusinessException;
import com.example.tripsplit.exception.ResourceNotFoundException;
import com.example.tripsplit.repository.ExpenseRepository;
import com.example.tripsplit.repository.ParticipantRepository;
import com.example.tripsplit.repository.SettlementRepository;
import com.example.tripsplit.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TripService {
    private final TripRepository tripRepository;

    private final ExpenseRepository expenseRepository;
    private final ParticipantRepository participantRepository;
    private final SettlementRepository settlementRepository;

    public TripService(TripRepository tripRepository, ExpenseRepository expenseRepository,
                       ParticipantRepository participantRepository, SettlementRepository settlementRepository) {
        this.tripRepository = tripRepository;
        this.expenseRepository = expenseRepository;
        this.participantRepository = participantRepository;
        this.settlementRepository = settlementRepository;
    }

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public Trip getTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + id));
    }

    public Trip createTrip(TripRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        Trip trip = new Trip();
        copyRequest(trip, request);
        return tripRepository.save(trip);
    }

    public Trip updateTrip(Long id, TripRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        Trip trip = getTrip(id);
        copyRequest(trip, request);
        return tripRepository.save(trip);
    }

    @Transactional
    public void deleteTrip(Long id) {
        Trip trip = getTrip(id);
        // Delete children in the correct order so MySQL foreign keys are never violated.
        settlementRepository.deleteByTripId(id);
        settlementRepository.flush();
        expenseRepository.deleteAll(expenseRepository.findByTripIdOrderByExpenseDateDescIdDesc(id));
        expenseRepository.flush();
        participantRepository.deleteAll(participantRepository.findByTripId(id));
        participantRepository.flush();
        tripRepository.delete(trip);
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BusinessException("End date cannot be before start date");
        }
    }

    private void copyRequest(Trip trip, TripRequest request) {
        trip.setName(request.getName().trim());
        trip.setDescription(request.getDescription());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
    }
}
