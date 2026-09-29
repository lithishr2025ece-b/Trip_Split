package com.example.tripsplit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SettlementResponse {
    private Long id;
    private Long fromParticipantId;
    private String fromParticipant;
    private Long toParticipantId;
    private String toParticipant;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public SettlementResponse(Long id, Long fromParticipantId, String fromParticipant,
                              Long toParticipantId, String toParticipant, BigDecimal amount,
                              LocalDateTime createdAt) {
        this.id = id;
        this.fromParticipantId = fromParticipantId;
        this.fromParticipant = fromParticipant;
        this.toParticipantId = toParticipantId;
        this.toParticipant = toParticipant;
        this.amount = amount;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public Long getFromParticipantId() { return fromParticipantId; }
    public String getFromParticipant() { return fromParticipant; }
    public Long getToParticipantId() { return toParticipantId; }
    public String getToParticipant() { return toParticipant; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
