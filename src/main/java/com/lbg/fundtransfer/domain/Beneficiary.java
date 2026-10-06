package com.lbg.fundtransfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A payee saved by a customer (family or friend). The "logical link" from the
 * use case: a beneficiary points at a real {@link Account} in the bank via its
 * account number, and belongs to exactly one owning customer.
 */
@Entity
@Table(
        name = "beneficiaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "accountNumber"})
)
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String accountNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    protected Beneficiary() {
        // JPA
    }

    public Beneficiary(String nickname, String accountNumber, Customer customer) {
        this.nickname = nickname;
        this.accountNumber = accountNumber;
        this.customer = customer;
    }

    public void updateDetails(String nickname, String accountNumber) {
        this.nickname = nickname;
        this.accountNumber = accountNumber;
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Customer getCustomer() {
        return customer;
    }
}
