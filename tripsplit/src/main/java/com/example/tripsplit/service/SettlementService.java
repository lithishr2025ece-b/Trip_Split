package com.example.tripsplit.service;

import com.example.tripsplit.dto.BalanceResponse;
import com.example.tripsplit.dto.SettlementResponse;
import com.example.tripsplit.entity.Participant;
import com.example.tripsplit.entity.Settlement;
import com.example.tripsplit.entity.Trip;
import com.example.tripsplit.exception.BusinessException;
import com.example.tripsplit.exception.ResourceNotFoundException;
import com.example.tripsplit.repository.SettlementRepository;
import com.example.tripsplit.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final TripRepository tripRepository;
    private final ExpenseService expenseService;

    public SettlementService(SettlementRepository settlementRepository,
                             TripRepository tripRepository,
                             ExpenseService expenseService) {
        this.settlementRepository = settlementRepository;
        this.tripRepository = tripRepository;
        this.expenseService = expenseService;
    }

    public List<SettlementResponse> getSettlements(Long tripId) {
        ensureTrip(tripId);
        return settlementRepository.findByTripIdOrderByIdAsc(tripId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<SettlementResponse> generate(Long tripId) {
        Trip trip = ensureTrip(tripId);
        List<BalanceResponse> balances = expenseService.calculateBalances(tripId);

        settlementRepository.deleteByTripId(tripId);

        List<BalanceItem> creditors = new ArrayList<>();
        List<BalanceItem> debtors = new ArrayList<>();
        for (BalanceResponse balance : balances) {
            int comparison = balance.getNetBalance().compareTo(BigDecimal.ZERO);
            if (comparison > 0) {
                creditors.add(new BalanceItem(balance.getParticipantId(), balance.getParticipantName(), balance.getNetBalance()));
            } else if (comparison < 0) {
                debtors.add(new BalanceItem(balance.getParticipantId(), balance.getParticipantName(), balance.getNetBalance().abs()));
            }
        }

        creditors.sort(Comparator.comparing(BalanceItem::amount).reversed());
        debtors.sort(Comparator.comparing(BalanceItem::amount).reversed());

        List<Settlement> created = new ArrayList<>();
        int creditorIndex = 0;
        int debtorIndex = 0;

        // Match one person who owes money with one person who should receive it.
        while (creditorIndex < creditors.size() && debtorIndex < debtors.size()) {
            BalanceItem creditor = creditors.get(creditorIndex);
            BalanceItem debtor = debtors.get(debtorIndex);
            BigDecimal amount = creditor.amount.min(debtor.amount).setScale(2, RoundingMode.HALF_UP);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Settlement calculation produced an invalid transaction");
            }

            Settlement settlement = new Settlement();
            settlement.setTrip(trip);
            settlement.setFromParticipant(findParticipant(trip, debtor.participantId()));
            settlement.setToParticipant(findParticipant(trip, creditor.participantId()));
            settlement.setAmount(amount);
            settlement.setCreatedAt(LocalDateTime.now());
            created.add(settlementRepository.save(settlement));

            creditor.setAmount(creditor.amount.subtract(amount));
            debtor.setAmount(debtor.amount.subtract(amount));

            if (creditor.amount.compareTo(BigDecimal.ZERO.setScale(2)) == 0) creditorIndex++;
            if (debtor.amount.compareTo(BigDecimal.ZERO.setScale(2)) == 0) debtorIndex++;
        }

        if (creditorIndex != creditors.size() || debtorIndex != debtors.size()) {
            throw new BusinessException("Settlement could not clear every participant balance");
        }
        return created.stream().map(this::toResponse).toList();
    }

    private Participant findParticipant(Trip trip, Long id) {
        return trip.getParticipants().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with id " + id));
    }

    private Trip ensureTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + tripId));
    }

    private SettlementResponse toResponse(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getFromParticipant().getId(),
                settlement.getFromParticipant().getName(),
                settlement.getToParticipant().getId(),
                settlement.getToParticipant().getName(),
                settlement.getAmount(),
                settlement.getCreatedAt());
    }

    private static class BalanceItem {
        private final Long participantId;
        private final String participantName;
        private BigDecimal amount;

        BalanceItem(Long participantId, String participantName, BigDecimal amount) {
            this.participantId = participantId;
            this.participantName = participantName;
            this.amount = amount;
        }
        Long participantId() { return participantId; }
        String participantName() { return participantName; }
        BigDecimal amount() { return amount; }
        void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
