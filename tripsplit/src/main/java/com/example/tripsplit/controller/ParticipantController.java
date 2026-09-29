package com.example.tripsplit.controller;

import com.example.tripsplit.dto.ParticipantRequest;
import com.example.tripsplit.entity.Participant;
import com.example.tripsplit.service.ParticipantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ParticipantController {
    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping("/trips/{tripId}/participants")
    public ResponseEntity<Participant> add(@PathVariable Long tripId,
                                           @Valid @RequestBody ParticipantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(participantService.add(tripId, request));
    }

    @GetMapping("/trips/{tripId}/participants")
    public List<Participant> getByTrip(@PathVariable Long tripId) {
        return participantService.getByTrip(tripId);
    }

    @PutMapping("/participants/{id}")
    public Participant update(@PathVariable Long id, @Valid @RequestBody ParticipantRequest request) {
        return participantService.update(id, request);
    }

    @DeleteMapping("/participants/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        participantService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
