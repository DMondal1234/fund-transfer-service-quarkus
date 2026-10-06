package com.lbg.fundtransfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An immutable record of a single transfer between two accounts. Written once
 * per attempt; queried for transaction history. {@code type} distinguishes P2P
 * from MERCHANT payments; {@code reference} is an optional external key (e.g. a
 * HungerBox order reference) used for idempotency and status lookup.
 */
@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_txn_source", columnList = "sourceAccountNumber"),
                @Index(name = "idx_txn_timestamp", columnList = "timestamp"),
                @Index(name = "idx_txn_reference", columnList = "reference")
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceAccountNumber;

    @Column(nullable = false)
    private String destinationAccountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /** Optional external reference (e.g. HungerBox order reference). */
    private String reference;

    private String description;

    protected Transaction() {
        // JPA
    }

    /** Existing P2P constructor — unchanged callers default to type P2P, no reference. */
    public Transaction(String sourceAccountNumber, String destinationAccountNumber,
                       BigDecimal amount, LocalDateTime timestamp,
                       TransactionStatus status, String description) {
        this(sourceAccountNumber, destinationAccountNumber, amount, timestamp, status,
                TransactionType.P2P, null, description);
    }

    /** Full constructor used by merchant payments. */
    public Transaction(String sourceAccountNumber, String destinationAccountNumber,
                       BigDecimal amount, LocalDateTime timestamp,
                       TransactionStatus status, TransactionType type,
                       String reference, String description) {
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
        this.type = type;
        this.reference = reference;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public TransactionType getType() {
        return type;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }
}
