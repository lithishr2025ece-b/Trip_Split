package com.example.tripsplit.service;

import com.example.tripsplit.dto.ParticipantRequest;
import com.example.tripsplit.entity.Participant;
import com.example.tripsplit.entity.Trip;
import com.example.tripsplit.exception.BusinessException;
import com.example.tripsplit.exception.ResourceNotFoundException;
import com.example.tripsplit.repository.ExpenseRepository;
import com.example.tripsplit.repository.ParticipantRepository;
import com.example.tripsplit.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ParticipantService {
    private final ParticipantRepository participantRepository;
    private final TripRepository tripRepository;
    private final ExpenseRepository expenseRepository;

    public ParticipantService(ParticipantRepository participantRepository, TripRepository tripRepository,
                              ExpenseRepository expenseRepository) {
        this.participantRepository = participantRepository;
        this.tripRepository = tripRepository;
        this.expenseRepository = expenseRepository;
    }

    public List<Participant> getByTrip(Long tripId) {
        ensureTrip(tripId);
        return participantRepository.findByTripId(tripId);
    }

    public Participant getParticipant(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with id " + id));
    }

    @Transactional
    public Participant add(Long tripId, ParticipantRequest request) {
        Trip trip = ensureTrip(tripId);
        Participant participant = new Participant();
        participant.setName(request.getName().trim());
        participant.setEmail(blankToNull(request.getEmail()));
        participant.setTrip(trip);
        return participantRepository.save(participant);
    }

    @Transactional
    public Participant update(Long id, ParticipantRequest request) {
        Participant participant = getParticipant(id);
        participant.setName(request.getName().trim());
        participant.setEmail(blankToNull(request.getEmail()));
        return participantRepository.save(participant);
    }

    @Transactional
    public void delete(Long id) {
        Participant participant = getParticipant(id);
        if (expenseRepository.existsByPayerId(id) || expenseRepository.existsByParticipantsId(id)) {
            throw new BusinessException("This participant is used in an expense. Delete or edit those expenses first.");
        }
        participantRepository.delete(participant);
    }

    private Trip ensureTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + tripId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
