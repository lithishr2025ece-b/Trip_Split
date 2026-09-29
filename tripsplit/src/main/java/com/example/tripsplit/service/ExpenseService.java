package com.example.tripsplit.service;

import com.example.tripsplit.dto.BalanceResponse;
import com.example.tripsplit.dto.ExpenseRequest;
import com.example.tripsplit.entity.Expense;
import com.example.tripsplit.entity.Participant;
import com.example.tripsplit.entity.Trip;
import com.example.tripsplit.exception.BusinessException;
import com.example.tripsplit.exception.ResourceNotFoundException;
import com.example.tripsplit.repository.ExpenseRepository;
import com.example.tripsplit.repository.ParticipantRepository;
import com.example.tripsplit.repository.SettlementRepository;
import com.example.tripsplit.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final ParticipantRepository participantRepository;
    private final TripRepository tripRepository;
    private final SettlementRepository settlementRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          ParticipantRepository participantRepository,
                          TripRepository tripRepository,
                          SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
        this.expenseRepository = expenseRepository;
        this.participantRepository = participantRepository;
        this.tripRepository = tripRepository;
    }

    public List<Expense> getByTrip(Long tripId) {
        ensureTrip(tripId);
        return expenseRepository.findByTripIdOrderByExpenseDateDescIdDesc(tripId);
    }

    public Expense getExpense(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id " + id));
    }

    @Transactional
    public Expense create(Long tripId, ExpenseRequest request) {
        Trip trip = ensureTrip(tripId);
        validateAmount(request.getAmount());
        Participant payer = getParticipantFromTrip(request.getPayerId(), tripId);
        Set<Participant> participants = getParticipantsFromTrip(request.getParticipantIds(), tripId);

        if (!participants.contains(payer)) {
            throw new BusinessException("Payer should be included among the expense participants");
        }

        Expense expense = new Expense();
        copyRequest(expense, request);
        expense.setTrip(trip);
        expense.setPayer(payer);
        expense.setParticipants(participants);
        settlementRepository.deleteByTripId(tripId); // saved settlement is outdated now
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense update(Long id, ExpenseRequest request) {
        Expense expense = getExpense(id);
        Long tripId = expense.getTrip().getId();
        validateAmount(request.getAmount());
        Participant payer = getParticipantFromTrip(request.getPayerId(), tripId);
        Set<Participant> participants = getParticipantsFromTrip(request.getParticipantIds(), tripId);

        if (!participants.contains(payer)) {
            throw new BusinessException("Payer should be included among the expense participants");
        }

        copyRequest(expense, request);
        expense.setPayer(payer);
        expense.setParticipants(participants);
        settlementRepository.deleteByTripId(tripId); // saved settlement is outdated now
        return expenseRepository.save(expense);
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = getExpense(id);
        settlementRepository.deleteByTripId(expense.getTrip().getId());
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> calculateBalances(Long tripId) {
        Trip trip = ensureTrip(tripId);
        List<Participant> participants = participantRepository.findByTripId(tripId);
        List<Expense> expenses = expenseRepository.findByTripIdOrderByExpenseDateDescIdDesc(tripId);

        Map<Long, BigDecimal> paid = new HashMap<>();
        Map<Long, BigDecimal> owed = new HashMap<>();
        for (Participant participant : participants) {
            paid.put(participant.getId(), BigDecimal.ZERO.setScale(2));
            owed.put(participant.getId(), BigDecimal.ZERO.setScale(2));
        }

        for (Expense expense : expenses) {
            Long payerId = expense.getPayer().getId();
            paid.put(payerId, paid.get(payerId).add(expense.getAmount()));
            addExactShares(owed, expense);
        }

        List<BalanceResponse> result = new ArrayList<>();
        BigDecimal totalBalance = BigDecimal.ZERO.setScale(2);
        for (Participant participant : participants) {
            BigDecimal totalPaid = money(paid.get(participant.getId()));
            BigDecimal totalOwed = money(owed.get(participant.getId()));
            BigDecimal net = money(totalPaid.subtract(totalOwed));
            totalBalance = totalBalance.add(net);
            result.add(new BalanceResponse(participant.getId(), participant.getName(), totalPaid, totalOwed, net));
        }

        // A correct expense split always produces a zero total net balance.
        if (money(totalBalance).compareTo(BigDecimal.ZERO.setScale(2)) != 0) {
            throw new BusinessException("Invalid trip balances: the sum of net balances must equal zero");
        }
        return result;
    }

    private void addExactShares(Map<Long, BigDecimal> owed, Expense expense) {
        List<Participant> people = new ArrayList<>(expense.getParticipants());
        people.sort(Comparator.comparing(Participant::getId));
        long cents = expense.getAmount().movePointRight(2).longValueExact();
        long base = cents / people.size();
        long remainder = cents % people.size();

        for (int i = 0; i < people.size(); i++) {
            long shareCents = base + (i < remainder ? 1 : 0);
            BigDecimal share = BigDecimal.valueOf(shareCents, 2);
            Long participantId = people.get(i).getId();
            owed.put(participantId, owed.get(participantId).add(share));
        }
    }

    private Participant getParticipantFromTrip(Long participantId, Long tripId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with id " + participantId));
        if (!participant.getTrip().getId().equals(tripId)) {
            throw new BusinessException("Participant does not belong to this trip");
        }
        return participant;
    }

    private Set<Participant> getParticipantsFromTrip(List<Long> ids, Long tripId) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("At least one participant must share the expense");
        }
        Set<Participant> result = new HashSet<>();
        for (Long id : ids) {
            if (id == null) {
                throw new BusinessException("Participant id cannot be null");
            }
            result.add(getParticipantFromTrip(id, tripId));
        }
        if (result.isEmpty()) {
            throw new BusinessException("At least one participant must share the expense");
        }
        return result;
    }

    private Trip ensureTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + tripId));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero");
        }
        if (amount.scale() > 2) {
            throw new BusinessException("Amount can have at most two decimal places");
        }
    }

    private void copyRequest(Expense expense, ExpenseRequest request) {
        expense.setDescription(request.getDescription().trim());
        expense.setAmount(money(request.getAmount()));
        expense.setExpenseDate(request.getExpenseDate());
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
