package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class DataGenerator {

    public static void main(String[] args) {
        System.out.println("Generating datasets in 'csv' folder...");
        
        generateTransactions("csv/transactions_500000.csv", 500000);
        generateSortedTransactions("csv/test_sorted_transactions.csv", 500000);
        generateReverseTransactions("csv/test_reverse_transactions.csv", 500000);
        generateSkewedTransactions("csv/test_skewed_transactions.csv", 500000);
        generateRangeQueries("csv/range_queries_200000.csv", 200000, 500000);
        generateUpdates("csv/account_updates_150000.csv", 150000, 500000);
        
        System.out.println("Finished generating all CSV files.");
    }

    private static void generateTransactions(String filename, int count) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("TransactionID,AccountID,Timestamp,Amount,Balance");
            Random rand = new Random(42);
            long initialTime = System.currentTimeMillis();

            for (int i = 1; i <= count; i++) {
                long transactionId = 1000000L + i;
                long accountId = 2000000L + rand.nextInt(10000);
                long timestamp = initialTime - rand.nextInt(10000000);
                double amount = rand.nextDouble() * 10000;
                double balance = rand.nextDouble() * 50000 + amount;

                writer.printf("%d,%d,%d,%.2f,%.2f\n", transactionId, accountId, timestamp, amount, balance);
            }
            System.out.println("Generated " + filename);
        } catch (IOException e) {
            System.err.println("Error writing " + filename + ": " + e.getMessage());
        }
    }

    private static void generateSortedTransactions(String filename, int count) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("TransactionID,AccountID,Timestamp,Amount,Balance");
            long initialTime = System.currentTimeMillis();
            for (int i = 1; i <= count; i++) {
                writer.printf("%d,%d,%d,%.2f,%.2f\n", 1000000L + i, 2000000L + (i%10000), initialTime++, 100.0, 5000.0);
            }
            System.out.println("Generated " + filename);
        } catch (IOException e) {}
    }

    private static void generateReverseTransactions(String filename, int count) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("TransactionID,AccountID,Timestamp,Amount,Balance");
            long initialTime = System.currentTimeMillis();
            for (int i = count; i >= 1; i--) {
                writer.printf("%d,%d,%d,%.2f,%.2f\n", 1000000L + i, 2000000L + (i%10000), initialTime++, 100.0, 5000.0);
            }
            System.out.println("Generated " + filename);
        } catch (IOException e) {}
    }

    private static void generateSkewedTransactions(String filename, int count) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("TransactionID,AccountID,Timestamp,Amount,Balance");
            Random rand = new Random(42);
            long initialTime = System.currentTimeMillis();
            for (int i = 1; i <= count; i++) {
                long transactionId = 1000000L + (rand.nextDouble() < 0.8 ? rand.nextInt(1000) : rand.nextInt(count));
                writer.printf("%d,%d,%d,%.2f,%.2f\n", transactionId, 2000000L + rand.nextInt(10), initialTime++, 100.0, 5000.0);
            }
            System.out.println("Generated " + filename);
        } catch (IOException e) {}
    }

    private static void generateRangeQueries(String filename, int count, int totalTransactions) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("StartTransactionID,EndTransactionID");
            Random rand = new Random(84);

            for (int i = 0; i < count; i++) {
                long startId = 1000000L + 1 + rand.nextInt(totalTransactions - 500);
                long endId = startId + rand.nextInt(500); // Query size up to 500
                writer.printf("%d,%d\n", startId, endId);
            }
            System.out.println("Generated " + filename);
        } catch (IOException e) {
            System.err.println("Error writing " + filename + ": " + e.getMessage());
        }
    }

    private static void generateUpdates(String filename, int count, int totalTransactions) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("TransactionID,NewAmount");
            Random rand = new Random(126);

            for (int i = 0; i < count; i++) {
                long targetId = 1000000L + 1 + rand.nextInt(totalTransactions);
                double newAmount = rand.nextDouble() * 10000;
                writer.printf("%d,%.2f\n", targetId, newAmount);
            }
            System.out.println("Generated " + filename);
        } catch (IOException e) {
            System.err.println("Error writing " + filename + ": " + e.getMessage());
        }
    }
}
