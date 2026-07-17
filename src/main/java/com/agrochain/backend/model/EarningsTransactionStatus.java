package com.agrochain.backend.model;

// Named distinctly from the existing TransactionStatus (which belongs to
// Payment/Paystack charges) to avoid colliding with that unrelated concept.
public enum EarningsTransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED
}
