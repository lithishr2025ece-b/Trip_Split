package com.example.tripsplit.controller;

import com.example.tripsplit.dto.ExpenseRequest;
import com.example.tripsplit.entity.Expense;
import com.example.tripsplit.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ExpenseController {
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/trips/{tripId}/expenses")
    public ResponseEntity<Expense> add(@PathVariable Long tripId,
                                       @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(tripId, request));
    }

    @GetMapping("/trips/{tripId}/expenses")
    public List<Expense> getByTrip(@PathVariable Long tripId) {
        return expenseService.getByTrip(tripId);
    }

    @PutMapping("/expenses/{id}")
    public Expense update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(id, request);
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
