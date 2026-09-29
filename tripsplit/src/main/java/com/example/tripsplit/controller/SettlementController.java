package com.example.tripsplit.controller;

import com.example.tripsplit.dto.SettlementResponse;
import com.example.tripsplit.service.SettlementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/settlements")
public class SettlementController {
    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/generate")
    public List<SettlementResponse> generate(@PathVariable Long tripId) {
        return settlementService.generate(tripId);
    }

    @GetMapping
    public List<SettlementResponse> get(@PathVariable Long tripId) {
        return settlementService.getSettlements(tripId);
    }
}
