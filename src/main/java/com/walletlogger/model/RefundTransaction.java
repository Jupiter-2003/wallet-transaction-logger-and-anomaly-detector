package com.walletlogger.model;

/**
 * Represents a refund back into a user's wallet.
 */
public class RefundTransaction extends Transaction {

    private static final long serialVersionUID = 1L;

    private RefundTransaction(Builder builder) {
        super(builder);
    }

    public static class Builder extends Transaction.Builder<Builder> {

        public Builder() {
            type(TransactionType.REFUND);
        }

        @Override
        public RefundTransaction build() {
            return new RefundTransaction(this);
        }
    }
}
