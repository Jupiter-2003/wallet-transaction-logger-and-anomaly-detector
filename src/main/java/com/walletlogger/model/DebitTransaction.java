package com.walletlogger.model;

/**
 * Represents a debit (payment out) from a user's wallet.
 */
public class DebitTransaction extends Transaction {

    private static final long serialVersionUID = 1L;

    private DebitTransaction(Builder builder) {
        super(builder);
    }

    public static class Builder extends Transaction.Builder<Builder> {

        public Builder() {
            type(TransactionType.DEBIT);
        }

        @Override
        public DebitTransaction build() {
            return new DebitTransaction(this);
        }
    }
}
