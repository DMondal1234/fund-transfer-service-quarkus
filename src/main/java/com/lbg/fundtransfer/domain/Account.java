package com.lbg.fundtransfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/**
 * A bank account. Owned by EITHER a {@link Customer} (P2P) OR a {@link Merchant}
 * (settlement account) — exactly one is set. Holds the balance that transfers
 * debit and credit. Uses optimistic locking ({@link Version}) so concurrent
 * transfers cannot drive the balance negative through a lost update.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    /** Set for customer-owned (P2P) accounts; null for merchant accounts. */
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Set for merchant settlement accounts; null for customer accounts. */
    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Version
    private Long version;

    protected Account() {
        // JPA
    }

    /** Customer-owned account (existing P2P constructor — unchanged callers). */
    public Account(String accountNumber, AccountType type, BigDecimal balance, Customer customer) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.balance = balance;
        this.customer = customer;
    }

    /** Merchant settlement account. */
    public Account(String accountNumber, AccountType type, BigDecimal balance, Merchant merchant) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.balance = balance;
        this.merchant = merchant;
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public boolean hasSufficientFunds(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    public boolean isMerchantAccount() {
        return merchant != null;
    }

    /** Display name of whoever owns the account (customer full name or business name). */
    public String getOwnerName() {
        if (customer != null) {
            return customer.getFullName();
        }
        return merchant != null ? merchant.getBusinessName() : null;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public Long getVersion() {
        return version;
    }
}
