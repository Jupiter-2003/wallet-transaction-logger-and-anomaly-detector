package com.walletlogger.model;

/**
 * Represents a credit (top-up) to a user's wallet.
 */
public class CreditTransaction extends Transaction {

    private static final long serialVersionUID = 1L;

    private CreditTransaction(Builder builder) {
        super(builder);
    }

    public static class Builder extends Transaction.Builder<Builder> {

        public Builder() {
            type(TransactionType.CREDIT);
        }

        @Override
        public CreditTransaction build() {
            return new CreditTransaction(this);
        }
    }
}
