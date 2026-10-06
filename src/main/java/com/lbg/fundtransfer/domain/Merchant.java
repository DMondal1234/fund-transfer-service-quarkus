package com.lbg.fundtransfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A business that receives merchant payments (e.g. a HungerBox food vendor).
 * A merchant owns one or more {@link Account}s (its settlement accounts). This
 * is the lightweight, additive alternative to a full AccountHolder refactor:
 * an {@code Account} may be owned by a {@link Customer} (P2P) OR a Merchant.
 */
@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String merchantCode;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private boolean active;

    protected Merchant() {
        // JPA
    }

    public Merchant(String merchantCode, String businessName) {
        this.merchantCode = merchantCode;
        this.businessName = businessName;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public String getBusinessName() {
        return businessName;
    }

    public boolean isActive() {
        return active;
    }
}
