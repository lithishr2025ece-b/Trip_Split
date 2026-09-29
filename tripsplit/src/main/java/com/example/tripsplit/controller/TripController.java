package com.example.tripsplit.controller;

import com.example.tripsplit.dto.BalanceResponse;
import com.example.tripsplit.dto.TripRequest;
import com.example.tripsplit.entity.Trip;
import com.example.tripsplit.service.ExpenseService;
import com.example.tripsplit.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripService tripService;
    private final ExpenseService expenseService;

    public TripController(TripService tripService, ExpenseService expenseService) {
        this.tripService = tripService;
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<Trip> getAll() { return tripService.getAllTrips(); }

    @GetMapping("/{id}")
    public Trip getOne(@PathVariable Long id) { return tripService.getTrip(id); }

    @PostMapping
    public ResponseEntity<Trip> create(@Valid @RequestBody TripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.createTrip(request));
    }

    @PutMapping("/{id}")
    public Trip update(@PathVariable Long id, @Valid @RequestBody TripRequest request) {
        return tripService.updateTrip(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tripId}/balances")
    public List<BalanceResponse> balances(@PathVariable Long tripId) {
        return expenseService.calculateBalances(tripId);
    }
}
