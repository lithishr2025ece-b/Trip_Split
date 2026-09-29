package com.example.tripsplit.dto;

import java.math.BigDecimal;

public class BalanceResponse {
    private Long participantId;
    private String participantName;
    private BigDecimal totalPaid;
    private BigDecimal totalOwed;
    private BigDecimal netBalance;

    public BalanceResponse(Long participantId, String participantName, BigDecimal totalPaid,
                           BigDecimal totalOwed, BigDecimal netBalance) {
        this.participantId = participantId;
        this.participantName = participantName;
        this.totalPaid = totalPaid;
        this.totalOwed = totalOwed;
        this.netBalance = netBalance;
    }
    public Long getParticipantId() { return participantId; }
    public String getParticipantName() { return participantName; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public BigDecimal getTotalOwed() { return totalOwed; }
    public BigDecimal getNetBalance() { return netBalance; }
}
