package models;

public class Transaction {
    public long transactionId;
    public long accountId;
    public long timestamp;
    public double amount;
    public double balance;

    public Transaction(long transactionId, long accountId, long timestamp, double amount, double balance) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.timestamp = timestamp;
        this.amount = amount;
        this.balance = balance;
    }

    @Override
    public String toString() {
        return transactionId + "," + accountId + "," + timestamp + "," + amount + "," + balance;
    }
}
