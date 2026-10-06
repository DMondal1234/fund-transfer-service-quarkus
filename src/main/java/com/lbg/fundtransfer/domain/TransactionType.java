package com.lbg.fundtransfer.domain;

/**
 * Distinguishes a person-to-person transfer from a customer-to-merchant payment.
 */
public enum TransactionType {
    P2P,
    MERCHANT
}
